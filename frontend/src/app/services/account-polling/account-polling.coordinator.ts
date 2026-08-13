import { Injectable } from '@angular/core';
import { Observable, Subscription, forkJoin, interval, of } from 'rxjs';
import { switchMap } from 'rxjs/operators';

export type PollLoader = () => Observable<unknown>;

/**
 * Polling compartido dashboard / usd-account.
 * Evita duplicar interval + forkJoin en cada page god.
 */
@Injectable({
  providedIn: 'root',
})
export class AccountPollingCoordinator {
  start(
    intervalMs: number,
    loaders: PollLoader[],
    options?: {
      skip?: () => boolean;
      onTick?: () => void;
      onError?: (err: unknown) => void;
    }
  ): Subscription {
    return interval(intervalMs)
      .pipe(
        switchMap(() => {
          if (options?.skip?.()) {
            return of(null);
          }
          options?.onTick?.();
          return forkJoin(loaders.map((load) => load()));
        })
      )
      .subscribe({
        next: () => undefined,
        error: (err) => options?.onError?.(err),
      });
  }
}
