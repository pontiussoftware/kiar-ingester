import { Component } from '@angular/core';
import {AuthenticationService} from "./services/authentication.service";
import {map, Observable} from "rxjs";
import {Router} from "@angular/router";
import {Role} from "../../openapi";
import {MatDialog} from "@angular/material/dialog";
import {ProfileComponent} from "./components/session/user/profile.component";

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss'],
    standalone: false
})
export class AppComponent {
  constructor(private authentication: AuthenticationService, private dialog: MatDialog, private router: Router) {
  }

  /**
   * Returns an {@link Observable} of the current login status.
   *
   * @return {@link Observable} of the current login status.
   */
  get isLoggedIn(): Observable<boolean> {
    return this.authentication.isLoggedIn
  }

  /**
   * Returns an {@link Observable} of the username of the currently active user.
   *
   * @return {@link Observable} of {@link Role}
   */
  get username(): Observable<string | undefined> {
    return this.authentication.status.pipe(
        map(s => s?.username)
    )
  }

  /**
   * Returns an {@link Observable} that indicates, if current user is an admin.
   *
   * @return {@link Observable}
   */
  get isAdmin(): Observable<boolean> {
    return this.authentication.status.pipe(
        map(s => s != null && s.role == Role.ADMINISTRATOR)
    )
  }

  /**
   * Returns an {@link Observable} that indicates, if current user is a manager (or higher).
   *
   * @return {@link Observable}
   */
  get isManager(): Observable<boolean> {
    return this.authentication.status.pipe(
        map(s => s != null && (s.role == Role.ADMINISTRATOR || s.role == Role.MANAGER))
    )
  }

  /**
   * Returns an {@link Observable} that indicates, if current user is a viewer (or higher).
   *
   * @return {@link Observable}
   */
  get isViewer(): Observable<boolean> {
    return this.authentication.status.pipe(
        map(s => s != null && (s.role == Role.ADMINISTRATOR || s.role == Role.MANAGER || s.role == Role.VIEWER))
    )
  }

  /**
   * Opens the user profile dialog.
   */
  public userProfile() {
    this.dialog.open(ProfileComponent, {width: '500px'})
  }

  /**
   * Logs the current user out.
   */
  public logout(): void {
    this.authentication.logout().subscribe(e => {
      void this.router.navigate(['login']);
    })
  }
}
