import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { ActivatedRouteSnapshot } from '@angular/router';
import { validateGuard } from './validate.guard';
import { ValidationService } from '../services/validation-service/validation.service';
import { of, throwError } from 'rxjs';

describe('ValidateGuard', () => {
  let router: jasmine.SpyObj<Router>;
  let validationService: jasmine.SpyObj<ValidationService>;
  let route: ActivatedRouteSnapshot;

  beforeEach(() => {
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    const validationServiceSpy = jasmine.createSpyObj('ValidationService', ['validateEmailToken']);
    
    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: routerSpy },
        { provide: ValidationService, useValue: validationServiceSpy }
      ]
    });
    
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    validationService = TestBed.inject(ValidationService) as jasmine.SpyObj<ValidationService>;
    route = new ActivatedRouteSnapshot();
  });

  it('should be created', () => {
    expect(validateGuard).toBeTruthy();
  });

  it('should allow access when token is valid', (done) => {
    // Simular query params con token válido
    route.queryParams = { token: 'valid-token-123' };
    
    // Mock respuesta exitosa del servicio
    validationService.validateEmailToken.and.returnValue(of({ success: true, message: 'Token válido' }));
    
    const result = TestBed.runInInjectionContext(() => validateGuard(route, {} as any));
    
    if (result instanceof Promise) {
      result.then(canActivate => {
        expect(canActivate).toBe(true);
        expect(router.navigate).not.toHaveBeenCalled();
        done();
      });
    } else if (typeof result === 'object' && 'subscribe' in result) {
      result.subscribe(canActivate => {
        expect(canActivate).toBe(true);
        expect(router.navigate).not.toHaveBeenCalled();
        done();
      });
    }
  });

  it('should redirect to 404 when token is missing', () => {
    // Simular query params sin token
    route.queryParams = {};
    
    const result = TestBed.runInInjectionContext(() => validateGuard(route, {} as any));
    
    expect(result).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/404']);
  });

  it('should redirect to 404 when token is empty string', () => {
    // Simular query params con token vacío
    route.queryParams = { token: '' };
    
    const result = TestBed.runInInjectionContext(() => validateGuard(route, {} as any));
    
    expect(result).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/404']);
  });

  it('should redirect to 404 when token validation fails', (done) => {
    // Simular query params con token inválido
    route.queryParams = { token: 'invalid-token' };
    
    // Mock respuesta de fallo del servicio
    validationService.validateEmailToken.and.returnValue(of({ success: false, message: 'Token expirado' }));
    
    const result = TestBed.runInInjectionContext(() => validateGuard(route, {} as any));
    
    if (typeof result === 'object' && 'subscribe' in result) {
      result.subscribe(canActivate => {
        expect(canActivate).toBe(false);
        expect(router.navigate).toHaveBeenCalledWith(['/404']);
        done();
      });
    }
  });

  it('should redirect to 404 when server returns error', (done) => {
    // Simular query params con token
    route.queryParams = { token: 'some-token' };
    
    // Mock error del servicio
    validationService.validateEmailToken.and.returnValue(throwError({ status: 404 }));
    
    const result = TestBed.runInInjectionContext(() => validateGuard(route, {} as any));
    
    if (typeof result === 'object' && 'subscribe' in result) {
      result.subscribe(canActivate => {
        expect(canActivate).toBe(false);
        expect(router.navigate).toHaveBeenCalledWith(['/404']);
        done();
      });
    }
  });
});
