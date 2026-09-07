import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { ActivatedRouteSnapshot } from '@angular/router';
import { validateGuard } from './validate.guard';
import { ValidationService } from '../services/validation/validation.service';

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

  it('should allow access when a token is present, leaving the validation to the component', () => {
    route.queryParams = { token: 'valid-token-123' };

    const result = TestBed.runInInjectionContext(() => validateGuard(route, {} as any));

    expect(result).toBe(true);
    expect(router.navigate).not.toHaveBeenCalled();
    // El guard solo controla que el enlace traiga token: quien lo valida contra el backend
    // es el componente, que necesita el resultado para mostrar el mensaje correcto.
    expect(validationService.validateEmailToken).not.toHaveBeenCalled();
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

  it('should redirect to 404 when the token is only whitespace', () => {
    route.queryParams = { token: '   ' };

    const result = TestBed.runInInjectionContext(() => validateGuard(route, {} as any));

    expect(result).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/404']);
  });
});
