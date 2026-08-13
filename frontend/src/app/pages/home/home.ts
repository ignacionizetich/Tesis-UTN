import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { ThemeToggleComponent } from '../../components/ui/theme-toggle/theme-toggle';

type PreviewCurrency = 'ARS' | 'USD';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [ThemeToggleComponent],
  templateUrl: './home.html',
  styleUrls: ['./home.css'],
})
export class Home {
  /** Moneda mostrada en el preview interactivo del hero. */
  previewCurrency: PreviewCurrency = 'ARS';

  private readonly balances: Record<PreviewCurrency, string> = {
    ARS: '$ 128.450,00',
    USD: 'U$S 312,40',
  };

  constructor(private router: Router) {}

  get previewAmount(): string {
    return this.balances[this.previewCurrency];
  }

  get otherCurrency(): PreviewCurrency {
    return this.previewCurrency === 'ARS' ? 'USD' : 'ARS';
  }

  togglePreviewCurrency(): void {
    this.previewCurrency = this.otherCurrency;
  }

  goTo(path: string): void {
    this.router.navigate([`/${path}`]);
  }
}
