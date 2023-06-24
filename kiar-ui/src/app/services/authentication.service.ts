import {Injectable} from "@angular/core";
import {LoginRequest, Role, SessionService, SessionStatus, SuccessStatus} from "../../../openapi";
import {BehaviorSubject, catchError, firstValueFrom, map, Observable, of, shareReplay, tap} from "rxjs";
import {ActivatedRouteSnapshot, CanActivateFn, Router, RouterStateSnapshot, UrlTree} from "@angular/router";

@Injectable({
  providedIn: 'root',
})
export class AuthenticationService {

  /** A {@link BehaviorSubject} of the current {@link SessionStatus}. */
  private _status = new BehaviorSubject<SessionStatus | null>(null)

  constructor(private session: SessionService, private router: Router) {}

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
          this._status.next(null);
          console.log(`User was logged out.`);
        })
    );
  }

  /**
   * Returns an {@link Observable} of the current {@link SessionStatus}
   *
   * @return {@link Observable}
   */
  get status() {
    return this._status.asObservable()
  }

  /**
   * Returns an {@link Observable} of the current login state.
   */
  get isLoggedIn(): Observable<boolean> {
    return this._status.pipe(map(s => s != null))
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
            this._status.next(s);
            if (rolesAllowed.length == 0 || rolesAllowed.indexOf(s.role) > -1) {
              return true
            } else {
              return this.router.parseUrl('/forbidden')
            }
        }),
        catchError((err, caught) => {
            if (err.status == 401 || err.status == 403) {
              this._status.next(null) /* Automatically log-out. */
              return of(this.router.parseUrl(`/login?returnUrl=${state.url}`))
            }
            return of(this.router.parseUrl('/forbidden'))
        })
    ))
  }
}