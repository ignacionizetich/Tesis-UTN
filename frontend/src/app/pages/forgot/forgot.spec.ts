import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ForgotComponent } from './forgot';

describe('ForgotComponent', () => {
  let component: ForgotComponent;
  let fixture: ComponentFixture<ForgotComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ForgotComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(ForgotComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
