import {inject, Injectable} from "@angular/core";
import {LoginRequest, Role, SessionService, SessionStatus, SuccessStatus} from "../../../openapi";
import {BehaviorSubject, catchError, firstValueFrom, map, Observable, of, shareReplay, tap} from "rxjs";
import {ActivatedRouteSnapshot, CanActivateFn, Router, RouterStateSnapshot, UrlTree} from "@angular/router";






@Injectable({
  providedIn: 'root',
})
export class AuthenticationService {

  /** A {@link BehaviorSubject} that indicates if currently, a user is logged in. */
  private _loggedIn = new BehaviorSubject<boolean>(false)

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
        tap(() => {
            this._loggedIn.next(true);
            console.log(`User was logged in.`);
        })
    );
  }

  /**
   * Tries to logout the current user with the provided credentials.
   */
  public logout(): Observable<SuccessStatus> {
    return this.session.logout().pipe(
        tap(() => {
          this._loggedIn.next(false);
          console.log(`User was logged out.`);
        })
    );
  }

  /**
   * Queries and returns the {@link SessionStatus} with the API.
   *
   * @return An {@link Observable} containing the current {@link SessionStatus}.
   */
  public status(): Observable<SessionStatus> {
    return this.session.status().pipe(
        catchError((err, caught) => {
          if (err.status == 401 || err.status == 403) {
            this._loggedIn.next(false) /* Automatically log-out. */
          }
          return caught
        }),
        shareReplay(1, 60000) /* Is cached for 60s. */
    );
  }
  
  /**
   * Returns the current login state as Observable.
   */
  get isLoggedIn(): Observable<boolean> {
    return this._loggedIn.asObservable()
  }

  /**
   * This function is used to check if a particular route can be activated. It is
   * used by the {@link CanActivateFn} defined in guards.ts
   *
   * @param rolesAllows The list of {@link Role}s allowed
   * @param route
   * @param state
   */
  public canActivate(rolesAllows: Array<Role>, route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Promise<boolean | UrlTree> {
    return firstValueFrom(this.session.status().pipe(
        map(s => {
            this._loggedIn.next(true) /* Automatically set status to 'logged-in'. */
            if (rolesAllows.indexOf(s.role) > -1) {
              return true
            } else {
              return this.router.parseUrl('/forbidden')
            }
        }),
        catchError((err, caught) => {
          if (err.status == 401 || err.status == 403) {
            this._loggedIn.next(false) /* Automatically log-out. */
            return of(this.router.parseUrl(`/login?returnUrl=${state.url}`))
          }
          return of(false)
        })
    ))
  }
}