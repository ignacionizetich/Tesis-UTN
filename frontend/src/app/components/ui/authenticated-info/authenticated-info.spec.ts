import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AuthenticatedInfoComponent } from './authenticated-info';

describe('AuthenticatedInfoComponent', () => {
  let component: AuthenticatedInfoComponent;
  let fixture: ComponentFixture<AuthenticatedInfoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AuthenticatedInfoComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(AuthenticatedInfoComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
