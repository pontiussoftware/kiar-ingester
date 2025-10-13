import {AfterViewInit, Component, OnInit} from '@angular/core';
import {SuccessStatus} from "../../../../../openapi";
import {FormControl, FormGroup} from "@angular/forms";
import {ActivatedRoute, Router} from "@angular/router";
import {MatSnackBar} from "@angular/material/snack-bar";
import {first, Observer} from "rxjs";
import {AuthenticationService} from "../../../services/authentication.service";

@Component({
    selector: 'app-login',
    templateUrl: './login.component.html',
    styleUrls: ['./login.component.scss'],
    standalone: false
})
export class LoginComponent implements AfterViewInit {

  /** The {@link FormGroup} for the user to enter their credentials. */
  public readonly form: FormGroup = new FormGroup({
    username: new FormControl(''),
    password: new FormControl(''),
  });

  /** The URL to return to once login was successful. */
  private readonly returnUrl: string;

  /**
   *
   * @param authentication
   * @param router
   * @param route
   * @param snackBar
   */
  constructor(private authentication: AuthenticationService, private router: Router, private route: ActivatedRoute, private snackBar: MatSnackBar) {
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '';
  }
  /**
   * Initializes the return URL based on the referrer and triggers navigation, if the user has already been logged in.
   */
  public ngAfterViewInit(): void {
    this.authentication.isLoggedIn.pipe(first()).subscribe((b) => {
      if (b) {
        this.router.navigateByUrl(this.returnUrl, {skipLocationChange: true}).then(r => { /* No op. */});
      }
    });
  }

  /**
   * Handles form submit (= login).
   */
  public submit() {
    if (this.form.valid) {
      this.authentication.login(this.form.controls['username'].value, this.form.controls['password'].value).subscribe({
        next: (r: SuccessStatus) => {
          this.snackBar.open(`Login successful!`, undefined, { duration: 5000 })
          this.router.navigateByUrl(this.returnUrl).then(s => {});
        },
        error: (err) => {
          if (err?.error) {
            this.snackBar.open(`Login failed: ${err?.error?.description}!`, undefined, { duration: 5000 });
          } else {
            this.snackBar.open(`Login failed due to a connection issue!`, undefined, { duration: 5000 });
          }
        }
      });
    }
  }
}