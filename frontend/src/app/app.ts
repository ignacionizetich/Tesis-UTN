import { Component, signal, OnInit, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { Footer } from "./components/footer/footer";
import { isPlatformBrowser } from '@angular/common';
import { CommonModule } from '@angular/common';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Footer, CommonModule],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit, OnDestroy {
  protected readonly title = signal('frontend');
  showGlobalFooter = true;

  constructor(
    @Inject(PLATFORM_ID) private platformId: Object,
    private router: Router
  ) {}

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      this.initMouseTracking();
      this.initSmoothTransitions();
    }

    // Footer de autores (app-footer): ocultar en pantallas auth que usan app-global-footer
    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event: NavigationEnd) => {
        const url = event.urlAfterRedirects || event.url;
        this.showGlobalFooter = !['/register', '/login', '/forgot', '/resend', '/reset-password']
          .some((path) => url.includes(path));
      });
  }

  ngOnDestroy() {
    if (isPlatformBrowser(this.platformId)) {
      document.removeEventListener('mousemove', this.handleMouseMove);
    }
  }

  private initSmoothTransitions() {
    // Añadir clase para transiciones suaves al body
    document.body.classList.add('smooth-transitions');
  }

  private initMouseTracking() {
    // Optimizar mouse tracking con throttling
    let ticking = false;

    const updateMouse = (e: MouseEvent) => {
      if (!ticking) {
        requestAnimationFrame(() => {
          const x = (e.clientX / window.innerWidth) * 100;
          const y = (e.clientY / window.innerHeight) * 100;

          document.documentElement.style.setProperty('--mouse-x', `${x}%`);
          document.documentElement.style.setProperty('--mouse-y', `${y}%`);

          ticking = false;
        });
        ticking = true;
      }
    };

    document.addEventListener('mousemove', updateMouse, { passive: true });
  }

  private handleMouseMove = (e: MouseEvent) => {
    const x = (e.clientX / window.innerWidth) * 100;
    const y = (e.clientY / window.innerHeight) * 100;

    document.documentElement.style.setProperty('--mouse-x', `${x}%`);
    document.documentElement.style.setProperty('--mouse-y', `${y}%`);
  }
}
