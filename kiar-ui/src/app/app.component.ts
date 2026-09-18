import {Component, inject} from '@angular/core';
import {AuthenticationService} from "./services/authentication.service";
import {Router} from "@angular/router";
import {MatDialog} from "@angular/material/dialog";
import {ProfileComponent} from "./components/session/user/profile.component";

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss'],
    standalone: false
})
export class AppComponent {
  /** The {@link AuthenticationService} used to query and change the login state. */
  private authentication = inject(AuthenticationService);

  /** The {@link MatDialog} service used to open dialogs. */
  private dialog = inject(MatDialog);

  /** The {@link Router} used for navigation. */
  private router = inject(Router);

  /** A signal of the current login status. */
  public readonly isLoggedIn = this.authentication.isLoggedIn

  /** A signal of the username of the currently active user. */
  public readonly username = this.authentication.username

  /** A signal that indicates, if current user is an admin. */
  public readonly isAdmin = this.authentication.isAdmin

  /** A signal that indicates, if current user is a manager (or higher). */
  public readonly isManager = this.authentication.isManager

  /** A signal that indicates, if current user is a viewer (or higher). */
  public readonly isViewer = this.authentication.isViewer

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
