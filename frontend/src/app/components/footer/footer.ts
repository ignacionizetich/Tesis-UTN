import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/** Footer de marketing en shell (`app.html`). Distinto de `app-global-footer` (contacto en auth). */
@Component({
  selector: 'app-footer',
  imports: [RouterLink],
  templateUrl: './footer.html',
  styleUrl: './footer.css',
})
export class Footer {
  readonly year = new Date().getFullYear();

  readonly authors = [
    { name: 'Tomás Valero', url: 'https://github.com/ValeroTomas' },
    { name: 'Ignacio Nizetich', url: 'https://github.com/ignacionizetich' },
    { name: 'Nahuel Fornillo', url: 'https://github.com/#' },
    { name: 'Lautaro Arschak', url: 'https://github.com/Arshichak' },
    { name: 'Rufino Figueroa', url: 'https://github.com/Rufinofg2' },
  ] as const;
}
