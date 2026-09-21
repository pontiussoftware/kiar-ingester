import {computed, inject, Injectable, signal} from "@angular/core";
import {LoginRequest, Role, SessionService, SessionStatus, SuccessStatus} from "../../../openapi";
import {catchError, firstValueFrom, map, Observable, of, tap} from "rxjs";
import {ActivatedRouteSnapshot, CanActivateFn, Router, RouterStateSnapshot, UrlTree} from "@angular/router";

@Injectable({
  providedIn: 'root',
})
export class AuthenticationService {
  /** The {@link SessionService} used to access the current session. */
  private session = inject(SessionService);

  /** The {@link Router} used for navigation. */
  private router = inject(Router);

  /** The current {@link SessionStatus} (null if no user is logged in). */
  private readonly _status = signal<SessionStatus | null>(null)

  /** A read-only signal of the current {@link SessionStatus}. */
  public readonly status = this._status.asReadonly()

  /** A signal indicating whether a user is currently logged in. */
  public readonly isLoggedIn = computed(() => this._status() != null)

  /** A signal of the username of the currently logged-in user. */
  public readonly username = computed(() => this._status()?.username)

  /** A signal indicating whether the current user is an administrator. */
  public readonly isAdmin = computed(() => this.hasRole(Role.ADMINISTRATOR))

  /** A signal indicating whether the current user is a manager (or higher). */
  public readonly isManager = computed(() => this.hasRole(Role.ADMINISTRATOR, Role.MANAGER))

  /** A signal indicating whether the current user is a viewer (or higher). */
  public readonly isViewer = computed(() => this.hasRole(Role.ADMINISTRATOR, Role.MANAGER, Role.VIEWER))

  /**
   * Tries to login the current user with the provided credentials.
   *
   * @param username
   * @param password
   * @return {@link Observable}
   */
  public login(username: string, password: string): Observable<SuccessStatus> {
    return this.session.login({username: username, password: password} as LoginRequest).pipe(
        tap(() => console.log(`User was logged in.`))
    );
  }

  /**
   * Tries to logout the current user with the provided credentials.
   */
  public logout(): Observable<SuccessStatus> {
    return this.session.logout().pipe(
        tap(() => {
          this._status.set(null);
          console.log(`User was logged out.`);
        })
    );
  }

  /**
   * This function is used to check if a particular route can be activated. It is
   * used by the {@link CanActivateFn} defined in guards.ts
   *
   * @param rolesAllowed The list of {@link Role}s allowed
   * @param route
   * @param state
   */
  public canActivate(rolesAllowed: Array<Role>, route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Promise<boolean | UrlTree> {
    return firstValueFrom(this.session.status().pipe(
        map(s => {
            this._status.set(s);
            if (rolesAllowed.length == 0 || rolesAllowed.indexOf(s.role) > -1) {
              return true
            } else {
              return this.router.parseUrl('/forbidden')
            }
        }),
        catchError((err, caught) => {
            if (err.status == 401 || err.status == 403) {
              this._status.set(null) /* Automatically log-out. */
              return of(this.router.parseUrl(`/login?returnUrl=${encodeURIComponent(state.url)}`))
            }
            return of(this.router.parseUrl('/forbidden'))
        })
    ))
  }

  /**
   * Checks whether the current user has one of the given {@link Role}s.
   *
   * @param roles The {@link Role}s to check for.
   */
  private hasRole(...roles: Array<Role>): boolean {
    const status = this._status()
    return status != null && roles.indexOf(status.role) > -1
  }
}
