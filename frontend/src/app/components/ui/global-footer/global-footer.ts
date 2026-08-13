import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

/** Footer de contacto (email) en pantallas auth. Distinto de `app-footer` (créditos en shell). */
@Component({
  selector: 'app-global-footer',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './global-footer.html',
  styleUrls: ['./global-footer.css']
})
export class GlobalFooterComponent {}
