import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { FavoriteService } from './favorite.service';
import { SessionStore } from '../../core/session/session.store';
import { CacheService } from '../cache/cache.service';
import { FavoriteContact } from '../../models/favorite-contact';

describe('FavoriteService', () => {
  let service: FavoriteService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        FavoriteService,
        SessionStore,
        CacheService,
      ],
    });
    service = TestBed.inject(FavoriteService);
  });

  it('createTransferDataFromFavorite mapea titular y cbu', () => {
    const favorite: FavoriteContact = {
      id: 3,
      contactAlias: 'Amigo',
      description: '',
      creationDate: '2024-01-01',
      active: true,
      accountOwnerName: 'Ana Lopez',
      accountOwnerAlias: 'ana',
      accountCbu: '000999',
      accountAlias: 'ana.usd',
      accountType: 'PESOS',
    };

    const data = service.createTransferDataFromFavorite(favorite);
    expect(data.cvu).toBe('000999');
    expect(data.alias).toBe('ana.usd');
    expect(data.user.nombre).toBe('Ana');
    expect(data.user.apellido).toBe('Lopez');
    expect(data.isFromFavorite).toBeTrue();
    expect(data.favoriteId).toBe(3);
  });
});
