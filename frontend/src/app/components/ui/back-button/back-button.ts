import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Location } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-back-button',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './back-button.html',
  styleUrls: ['./back-button.css'],
})
export class BackButtonComponent {
  /** Ruta explícita. Ej: '/' */
  @Input() explicitRoute: string | null = null;

  /** `fab` = botón fijo (legacy); `inline` = enlace bajo el pitch */
  @Input() variant: 'fab' | 'inline' = 'fab';

  @Input() label = 'Volver al inicio';

  constructor(
    private location: Location,
    private router: Router
  ) {}

  goBack(): void {
    if (this.explicitRoute) {
      this.router.navigate([this.explicitRoute]);
    } else {
      this.location.back();
    }
  }
}
