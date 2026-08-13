import { Component, signal, OnInit } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { Footer } from './components/footer/footer';
import { CommonModule } from '@angular/common';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Footer, CommonModule],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnInit {
  protected readonly title = signal('frontend');
  showGlobalFooter = true;

  constructor(private router: Router) {}

  ngOnInit() {
    // Footer de marketing en shell: solo home (404/validate/auth lo incluyen en su página)
    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe((event: NavigationEnd) => {
        const url = (event.urlAfterRedirects || event.url).split('?')[0];
        this.showGlobalFooter = url === '/' || url === '';
      });
  }
}
