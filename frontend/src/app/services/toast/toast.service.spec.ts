import { TestBed } from '@angular/core/testing';
import { PLATFORM_ID } from '@angular/core';
import { ToastService } from './toast.service';

describe('ToastService', () => {
  afterEach(() => {
    document.getElementById('toast-container')?.remove();
  });

  it('no renderiza fuera del browser', () => {
    TestBed.configureTestingModule({
      providers: [ToastService, { provide: PLATFORM_ID, useValue: 'server' }],
    });
    const service = TestBed.inject(ToastService);
    service.show('hola', 'info');
    expect(document.getElementById('toast-container')).toBeNull();
  });

  it('crea un toast en el DOM en browser', () => {
    TestBed.configureTestingModule({
      providers: [ToastService, { provide: PLATFORM_ID, useValue: 'browser' }],
    });
    const service = TestBed.inject(ToastService);
    service.show('ok', 'success');
    const container = document.getElementById('toast-container');
    expect(container).toBeTruthy();
    expect(container?.querySelector('.toast-success')).toBeTruthy();
    expect(container?.textContent).toContain('ok');
  });
});
