import { TestBed } from '@angular/core/testing';
import { TransferFlowService, TransferFlowError } from './transfer-flow.service';
import { TransferApi } from '../transfer-api/transfer.api';
import { UserDataStore } from '../user-data-store/user-data.store';
import { FavoriteService } from '../favorite/favorite.service';

describe('TransferFlowService', () => {
  let service: TransferFlowService;
  let transferApi: jasmine.SpyObj<TransferApi>;
  let userDataStore: jasmine.SpyObj<UserDataStore>;
  let favoriteService: jasmine.SpyObj<FavoriteService>;

  beforeEach(() => {
    transferApi = jasmine.createSpyObj('TransferApi', ['buscarCuenta', 'realizarTransferencia']);
    userDataStore = jasmine.createSpyObj('UserDataStore', ['getCurrent']);
    favoriteService = jasmine.createSpyObj('FavoriteService', [
      'loadFavoriteContacts',
      'getFavoriteContacts',
    ]);

    userDataStore.getCurrent.and.returnValue({
      name: '',
      lastName: '',
      dni: '',
      email: '',
      alias: '',
      cvu: '',
      username: '',
      balance: 0,
      idAccount: '10',
    });
    favoriteService.loadFavoriteContacts.and.resolveTo();
    favoriteService.getFavoriteContacts.and.returnValue([]);

    TestBed.configureTestingModule({
      providers: [
        TransferFlowService,
        { provide: TransferApi, useValue: transferApi },
        { provide: UserDataStore, useValue: userDataStore },
        { provide: FavoriteService, useValue: favoriteService },
      ],
    });
    service = TestBed.inject(TransferFlowService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('validateAmount rechaza monto inválido', () => {
    expect(() => service.validateAmount(null, 100)).toThrowError(TransferFlowError);
    expect(() => service.validateAmount(0, 100)).toThrowError(TransferFlowError);
  });

  it('validateAmount rechaza saldo insuficiente', () => {
    try {
      service.validateAmount(50, 10, 'ARS');
      fail('expected throw');
    } catch (e) {
      expect(e instanceof TransferFlowError).toBeTrue();
      expect((e as TransferFlowError).code).toBe('INSUFFICIENT_FUNDS');
    }
  });

  it('assertNotSelf bloquea transferencia a cuenta propia', () => {
    expect(() => service.assertNotSelf('10')).toThrowError(TransferFlowError);
    expect(() => service.assertNotSelf('99', ['10', '99'])).toThrowError(TransferFlowError);
  });

  it('searchDestination falla con input vacío', async () => {
    await expectAsync(service.searchDestination('   ')).toBeRejectedWith(
      jasmine.objectContaining({ code: 'EMPTY_INPUT' })
    );
  });

  it('searchDestination mapea cuenta encontrada', async () => {
    transferApi.buscarCuenta.and.resolveTo({
      idaccount: '42',
      alias: 'juan.arcash',
      cvu: '000111',
      currency: 'ARS',
      user: { nombre: 'Juan', apellido: 'Perez', dni: '1' },
    });

    const result = await service.searchDestination('juan.arcash', {
      ownAccountIds: ['10'],
      checkFavorites: false,
    });

    expect(result.alias).toBe('juan.arcash');
    expect(result.idaccount).toBe('42');
    expect(result.isFromFavorite).toBeFalse();
  });
});
