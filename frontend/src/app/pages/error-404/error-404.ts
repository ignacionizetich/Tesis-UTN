import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { ThemeToggleComponent } from '../../components/ui/theme-toggle/theme-toggle';
import { BrandLogoComponent } from '../../components/ui/brand-logo/brand-logo';
import { Footer } from '../../components/footer/footer';

@Component({
  selector: 'app-error-404',
  standalone: true,
  imports: [ThemeToggleComponent, BrandLogoComponent, Footer],
  templateUrl: './error-404.html',
  styleUrls: ['./error-404.css'],
})
export class Error404Component {
  constructor(private router: Router) {}

  goToHome(): void {
    this.router.navigate(['/']);
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }
}
