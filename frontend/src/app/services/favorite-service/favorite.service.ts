import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, lastValueFrom } from 'rxjs';
import { CacheService } from '../cache-service/cache.service';
import { CacheConfig } from '../../models/cache.interface';
import { TransferData } from '../../models/transfer.interface';
import { SessionStore } from '../../core/session/session.store';
import { environment } from '../../../environments/environment';

/**
 * Favoritos: HTTP + caché local + estado en memoria.
 * Sin toasts — la UI (modales / páginas) notifica al usuario.
 */
@Injectable({
  providedIn: 'root',
})
export class FavoriteService {
  private readonly baseUrl = environment.apiUrl;

  private favoriteContactsSubject = new BehaviorSubject<any[]>([]);
  public favoriteContacts$ = this.favoriteContactsSubject.asObservable();

  private selectedFavoriteSubject = new BehaviorSubject<any | null>(null);
  public selectedFavorite$ = this.selectedFavoriteSubject.asObservable();

  private readonly cacheConfig: CacheConfig = {
    key: 'arcash_favorites_cache',
    expiryKey: 'arcash_favorites_cache_expiry',
    duration: 5 * 60 * 1000,
  };

  constructor(
    private http: HttpClient,
    private sessionStore: SessionStore,
    private cacheService: CacheService
  ) {}

  async loadFavoriteContacts(forceReload: boolean = false): Promise<void> {
    try {
      if (!forceReload) {
        const cachedData = this.cacheService.getCache<any[]>(this.cacheConfig);
        if (cachedData) {
          this.favoriteContactsSubject.next(cachedData);
          return;
        }
      }

      const favorites = await this.fetchFavoriteContacts();
      this.cacheService.setCache(this.cacheConfig, favorites);
      this.favoriteContactsSubject.next(favorites);
    } catch (error) {
      console.error('Error cargando contactos favoritos:', error);
      throw error;
    }
  }

  async loadFavoriteContactsOrdered(): Promise<void> {
    try {
      const favorites = await this.fetchFavoriteContactsOrderedByUsage();
      this.favoriteContactsSubject.next(favorites);
    } catch (error) {
      console.error('Error cargando contactos favoritos ordenados:', error);
      throw error;
    }
  }

  getFavoriteContacts(): any[] {
    return this.favoriteContactsSubject.value;
  }

  selectFavorite(favorite: any): void {
    this.selectedFavoriteSubject.next(favorite);
  }

  getSelectedFavorite(): any | null {
    return this.selectedFavoriteSubject.value;
  }

  clearSelectedFavorite(): void {
    this.selectedFavoriteSubject.next(null);
  }

  async addFavoriteContact(accountId: number, alias: string, description?: string): Promise<boolean> {
    let success: boolean;
    try {
      success = await this.postFavoriteContact(accountId, alias, description);
    } catch (error: any) {
      console.error('Error agregando a favoritos:', error);
      throw new Error(this.mapAddFavoriteError(error));
    }

    if (!success) {
      throw new Error(
        'No se pudo agregar el contacto. Verifica que no sea tu propia cuenta o que ya esté en favoritos.'
      );
    }

    await this.loadFavoriteContacts(true);
    return true;
  }

  async updateFavoriteContact(id: number, alias: string, description?: string): Promise<boolean> {
    let success: boolean;
    try {
      success = await this.putFavoriteContact(id, alias, description);
    } catch (error) {
      console.error('Error actualizando contacto:', error);
      throw new Error('Error al actualizar contacto');
    }

    if (!success) {
      throw new Error('Error al actualizar contacto');
    }

    await this.loadFavoriteContacts(true);
    return true;
  }

  async removeFavoriteContact(id: number, _contactName?: string): Promise<boolean> {
    let success: boolean;
    try {
      success = await this.deleteFavoriteContact(id);
    } catch (error) {
      console.error('Error eliminando contacto favorito:', error);
      throw new Error('Error al eliminar contacto');
    }

    if (!success) {
      throw new Error('Error al eliminar contacto');
    }

    await this.loadFavoriteContacts(true);
    return true;
  }

  invalidateCache(): void {
    this.cacheService.clearCache(this.cacheConfig);
  }

  createTransferDataFromFavorite(favorite: any): TransferData {
    return {
      idaccount: favorite.accountCbu,
      alias: favorite.accountAlias,
      cvu: favorite.accountCbu,
      user: {
        nombre: favorite.accountOwnerName.split(' ')[0] || 'Usuario',
        apellido: favorite.accountOwnerName.split(' ').slice(1).join(' ') || '',
        dni: '',
      },
      isFromFavorite: true,
      favoriteId: favorite.id,
    };
  }

  private mapAddFavoriteError(error: any): string {
    if (error?.status === 400) {
      if (error.error?.message) {
        return error.error.message;
      }
      return 'Datos inválidos. Verifica que el contacto no esté ya en favoritos o que el ID de cuenta sea válido.';
    }
    if (error?.status === 401) {
      return 'Sesión expirada. Por favor inicia sesión nuevamente.';
    }
    if (error?.status === 404) {
      return 'Cuenta no encontrada.';
    }
    if (error?.status === 500) {
      return 'Error del servidor. Intenta nuevamente.';
    }
    return 'Error al agregar contacto a favoritos';
  }

  private async fetchFavoriteContacts(): Promise<any[]> {
    this.requireToken();
    try {
      const response = await lastValueFrom(this.http.get<any>(`${this.baseUrl}/favorites/list`));
      return response?.favorites || [];
    } catch (error) {
      console.error('Error obteniendo favoritos:', error);
      throw error;
    }
  }

  private async fetchFavoriteContactsOrderedByUsage(): Promise<any[]> {
    this.requireToken();
    try {
      const response = await lastValueFrom(
        this.http.get<any>(`${this.baseUrl}/favorites/list/recent`)
      );
      return response?.favorites || [];
    } catch (error) {
      console.error('Error obteniendo favoritos ordenados:', error);
      throw error;
    }
  }

  private async postFavoriteContact(
    accountId: number,
    contactAlias: string,
    description?: string
  ): Promise<boolean> {
    this.requireToken();
    try {
      const body = {
        accountId,
        contactAlias,
        description: description || '',
      };

      const response = await lastValueFrom(
        this.http.post<any>(`${this.baseUrl}/favorites/add`, body, { observe: 'response' })
      );

      return (
        response.status === 200 ||
        response.status === 201 ||
        response.body?.status === 'SUCCESS' ||
        response.body?.success === true
      );
    } catch (error: any) {
      console.error('>>> Error DETALLADO agregando favorito:', {
        status: error.status,
        statusText: error.statusText,
        error: error.error,
        url: error.url,
      });
      if (error.error?.message) {
        console.error('>>> Mensaje del backend:', error.error.message);
      }
      throw error;
    }
  }

  private async putFavoriteContact(
    contactId: number,
    contactAlias?: string,
    description?: string
  ): Promise<boolean> {
    this.requireToken();
    try {
      const body: any = {};
      if (contactAlias) {
        body.contactAlias = contactAlias;
      }
      if (description !== undefined) {
        body.description = description;
      }

      const response = await lastValueFrom(
        this.http.put<any>(`${this.baseUrl}/favorites/update/${contactId}`, body)
      );
      return response?.status === 'SUCCESS';
    } catch (error) {
      console.error('Error actualizando favorito:', error);
      throw error;
    }
  }

  private async deleteFavoriteContact(favoriteId: number): Promise<boolean> {
    this.requireToken();
    try {
      const response = await lastValueFrom(
        this.http.delete<any>(`${this.baseUrl}/favorites/${favoriteId}`)
      );
      return response?.status === 'SUCCESS';
    } catch (error) {
      console.error('Error eliminando favorito:', error);
      throw error;
    }
  }

  private requireToken(): void {
    if (!this.sessionStore.hasAccessToken()) {
      throw new Error('No hay token');
    }
  }
}
