import { Component } from '@angular/core';
import {AuthenticationService} from "./services/authentication.service";
import {Observable} from "rxjs";
import {Router} from "@angular/router";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  constructor(private authentication: AuthenticationService, private router: Router) {
  }

  /**
   * Checks, if the user is currently logged in.
   */
  get isLoggedIn(): Observable<boolean> {
    return this.authentication.isLoggedIn
  }

  /**
   * Logs the current user out.
   */
  public logout() {
    this.authentication.logout().subscribe(e => {
      this.router.navigateByUrl('/').then(r => { /* No op. */});
    })
  }
}
