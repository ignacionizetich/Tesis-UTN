import { TestBed } from '@angular/core/testing';
import { CacheService } from './cache.service';
import { CacheConfig } from '../../models/cache.interface';

describe('CacheService', () => {
  let service: CacheService;
  const config: CacheConfig = {
    key: 'test_cache_key',
    expiryKey: 'test_cache_expiry',
    duration: 60_000,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(CacheService);
    localStorage.removeItem(config.key);
    localStorage.removeItem(config.expiryKey);
  });

  afterEach(() => {
    localStorage.removeItem(config.key);
    localStorage.removeItem(config.expiryKey);
  });

  it('guarda y recupera datos válidos', () => {
    service.setCache(config, { a: 1 });
    expect(service.getCache<{ a: number }>(config)).toEqual({ a: 1 });
  });

  it('devuelve null si expiró', () => {
    service.setCache(config, { a: 1 });
    localStorage.setItem(config.expiryKey, String(Date.now() - 1));
    expect(service.getCache(config)).toBeNull();
  });
});
