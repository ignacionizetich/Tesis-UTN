import { TestBed } from '@angular/core/testing';
import { SessionStore } from './session.store';

describe('SessionStore', () => {
  let store: SessionStore;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
    store = TestBed.inject(SessionStore);
  });

  afterEach(() => localStorage.clear());

  it('persiste y lee sesión', () => {
    expect(store.hasSession()).toBeFalse();
    store.setSession({ accessToken: 'tok', accountId: 7, role: 'USER' });
    expect(store.getAccessToken()).toBe('tok');
    expect(store.getAccountId()).toBe('7');
    expect(store.hasSession()).toBeTrue();
    expect(store.isAdmin()).toBeFalse();
  });

  it('detecta rol admin', () => {
    store.setSession({ accessToken: 't', accountId: 1, role: 'ADMIN' });
    expect(store.isAdmin()).toBeTrue();
  });
});
