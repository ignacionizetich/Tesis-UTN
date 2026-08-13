import { TestBed } from '@angular/core/testing';
import { ModalService } from './modal.service';

describe('ModalService', () => {
  let service: ModalService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ModalService);
  });

  it('abre y cierra modales', () => {
    expect(service.isModalOpen()).toBeFalse();
    service.openModal('transfer');
    expect(service.getCurrentModal()).toBe('transfer');
    expect(service.isModalOpen()).toBeTrue();
    service.closeModal();
    expect(service.getCurrentModal()).toBeNull();
  });

  it('actualiza loading sin cerrar el modal', () => {
    service.openModal('profile');
    service.setLoading(true);
    expect(service.getCurrentModal()).toBe('profile');
  });
});
