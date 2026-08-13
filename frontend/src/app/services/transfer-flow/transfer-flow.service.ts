import { Injectable } from '@angular/core';
import { TransferApi, AccountSearchResult } from '../transfer-api/transfer.api';
import { UserDataStore } from '../user-data-store/user-data.store';
import { FavoriteService } from '../favorite-service/favorite.service';
import { TransferData } from '../../models/transfer.interface';

export type TransferFlowErrorCode =
  | 'EMPTY_INPUT'
  | 'SELF_TRANSFER'
  | 'INVALID_AMOUNT'
  | 'INSUFFICIENT_FUNDS'
  | 'INVALID_DESTINATION'
  | 'FAVORITE_LOOKUP_FAILED'
  | 'SEARCH_FAILED'
  | 'TRANSFER_FAILED';

export class TransferFlowError extends Error {
  constructor(
    public readonly code: TransferFlowErrorCode,
    message: string,
    public readonly currency?: 'ARS' | 'USD'
  ) {
    super(message);
    this.name = 'TransferFlowError';
  }
}

export interface SearchDestinationOptions {
  /** IDs de cuentas propias a rechazar (default: idAccount del perfil). */
  ownAccountIds?: Array<string | number | null | undefined>;
  /** Marcar isFromFavorite si aplica (default true). */
  checkFavorites?: boolean;
}

export interface ExecuteTransferParams {
  destination: TransferData;
  amount: number | null;
  balance: number;
  currency: 'ARS' | 'USD';
}

export interface ExecuteTransferResult {
  destinationId: string;
  destinationIdNumber: number;
  alreadyFavorite: boolean;
  completedData: TransferData & { idaccount: number };
}

/**
 * Caso de uso de transferencia: búsqueda, self-guard, favoritos, resolve ID, execute.
 * Sin toasts ni UI — las pages orquestan feedback.
 */
@Injectable({
  providedIn: 'root',
})
export class TransferFlowService {
  constructor(
    private transferApi: TransferApi,
    private userDataStore: UserDataStore,
    private favoriteService: FavoriteService
  ) {}

  async searchDestination(
    input: string,
    options: SearchDestinationOptions = {}
  ): Promise<TransferData> {
    const trimmed = input?.trim() ?? '';
    if (!trimmed) {
      throw new TransferFlowError('EMPTY_INPUT', 'Por favor ingrese un Alias o CVU');
    }

    let account: AccountSearchResult;
    try {
      account = await this.transferApi.buscarCuenta(trimmed);
    } catch (error: any) {
      throw new TransferFlowError(
        'SEARCH_FAILED',
        error?.message || 'Cuenta no encontrada'
      );
    }

    this.assertNotSelf(account.idaccount, options.ownAccountIds);

    const data: TransferData = {
      idaccount: account.idaccount,
      alias: account.alias,
      cvu: account.cvu,
      user: account.user,
      isFromFavorite: false,
    };

    if (options.checkFavorites !== false) {
      const accountId = parseInt(String(account.idaccount), 10);
      if (!isNaN(accountId)) {
        data.isFromFavorite = await this.isFavorite(accountId, account.cvu);
      }
    }

    return data;
  }

  assertNotSelf(
    destinationAccountId: string | number,
    ownAccountIds?: Array<string | number | null | undefined>
  ): void {
    const own =
      ownAccountIds?.filter((id) => id != null && id !== '') ??
      [this.userDataStore.getCurrent()?.idAccount].filter(
        (id) => id != null && id !== ''
      );

    const dest = String(destinationAccountId);
    if (own.some((id) => String(id) === dest)) {
      throw new TransferFlowError(
        'SELF_TRANSFER',
        'No puedes transferir dinero a tu misma cuenta'
      );
    }
  }

  async isFavorite(accountId: number, cvu?: string): Promise<boolean> {
    try {
      await this.favoriteService.loadFavoriteContacts();
      return this.favoriteService.getFavoriteContacts().some((fav) => {
        if (fav.favoriteAccount && fav.favoriteAccount.idAccount === accountId) {
          return true;
        }
        if (fav.accountCbu && cvu && fav.accountCbu === cvu) {
          return true;
        }
        return false;
      });
    } catch (error) {
      console.error('Error verificando favoritos:', error);
      return false;
    }
  }

  async resolveDestinationId(
    destination: TransferData
  ): Promise<{ id: string; idNumber: number }> {
    if (destination.isFromFavorite) {
      try {
        const accountData = await this.transferApi.buscarCuenta(destination.cvu);
        const id = accountData.idaccount.toString();
        const idNumber = parseInt(id, 10);
        if (isNaN(idNumber)) {
          throw new TransferFlowError(
            'INVALID_DESTINATION',
            'ID de cuenta inválido obtenido de favorito.'
          );
        }
        return { id, idNumber };
      } catch (error) {
        if (error instanceof TransferFlowError) {
          throw error;
        }
        throw new TransferFlowError(
          'FAVORITE_LOOKUP_FAILED',
          'Error al buscar info de la cuenta favorita'
        );
      }
    }

    const id = destination.idaccount.toString();
    const idNumber = parseInt(id, 10);
    if (isNaN(idNumber)) {
      throw new TransferFlowError(
        'INVALID_DESTINATION',
        'ID de cuenta inválido obtenido de búsqueda/QR.'
      );
    }
    return { id, idNumber };
  }

  validateAmount(
    amount: number | null,
    balance: number,
    currency?: 'ARS' | 'USD'
  ): asserts amount is number {
    if (amount == null || amount <= 0) {
      throw new TransferFlowError(
        'INVALID_AMOUNT',
        'Por favor ingrese un monto válido'
      );
    }
    if (amount > balance) {
      const label =
        currency === 'USD'
          ? 'dólares'
          : currency === 'ARS'
            ? 'pesos'
            : 'cuenta';
      throw new TransferFlowError(
        'INSUFFICIENT_FUNDS',
        currency
          ? `Saldo insuficiente en tu cuenta de ${label}`
          : 'Saldo insuficiente',
        currency
      );
    }
  }

  async executeTransfer(
    params: ExecuteTransferParams
  ): Promise<ExecuteTransferResult> {
    this.validateAmount(params.amount, params.balance, params.currency);

    const { id, idNumber } = await this.resolveDestinationId(params.destination);

    const alreadyFavorite = await this.isFavorite(
      idNumber,
      params.destination.cvu
    );

    try {
      await this.transferApi.realizarTransferencia(
        id,
        params.amount!,
        params.currency
      );
    } catch (error: any) {
      throw new TransferFlowError(
        'TRANSFER_FAILED',
        error?.message || 'Error al realizar la transferencia'
      );
    }

    return {
      destinationId: id,
      destinationIdNumber: idNumber,
      alreadyFavorite,
      completedData: {
        ...params.destination,
        idaccount: idNumber,
      },
    };
  }
}
