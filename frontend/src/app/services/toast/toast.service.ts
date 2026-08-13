import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

export type ToastType = 'success' | 'error' | 'info' | 'warning';

@Injectable({
  providedIn: 'root',
})
export class ToastService {
  constructor(@Inject(PLATFORM_ID) private platformId: Object) {}

  show(message: string, type: ToastType): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    let toastContainer = document.getElementById('toast-container');
    if (!toastContainer) {
      toastContainer = document.createElement('div');
      toastContainer.id = 'toast-container';
      toastContainer.className = 'toast-container';
      document.body.appendChild(toastContainer);
    }

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.innerHTML = `
      <div class="toast-content">
        <div class="toast-icon">
          ${this.getIcon(type)}
        </div>
        <div class="toast-message">${message}</div>
        <button class="toast-close" type="button" aria-label="Cerrar">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
            <path d="M18 6L6 18M6 6L18 18" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </button>
      </div>
      <div class="toast-progress"></div>
    `;

    const closeBtn = toast.querySelector('.toast-close');
    closeBtn?.addEventListener('click', () => this.removeToast(toast));

    toastContainer.appendChild(toast);

    setTimeout(() => {
      toast.classList.add('toast-show');
    }, 100);

    const progressBar = toast.querySelector('.toast-progress') as HTMLElement | null;
    if (progressBar) {
      setTimeout(() => {
        progressBar.style.width = '0%';
      }, 200);
    }

    setTimeout(() => {
      this.removeToast(toast);
    }, 4000);
  }

  private getIcon(type: ToastType): string {
    const icons: Record<ToastType, string> = {
      success: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                  <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/>
                  <path d="M9 12l2 2 4-4" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                </svg>`,
      error: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/>
                <path d="M15 9l-6 6M9 9l6 6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>`,
      info: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none">
               <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/>
               <path d="M12 16v-4M12 8h.01" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
             </svg>`,
      warning: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                  <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" stroke="currentColor" stroke-width="2"/>
                  <path d="M12 9v4M12 17h.01" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                </svg>`,
    };
    return icons[type];
  }

  private removeToast(toast: HTMLElement): void {
    toast.classList.add('toast-hide');
    setTimeout(() => {
      toast.parentElement?.removeChild(toast);
    }, 300);
  }
}
