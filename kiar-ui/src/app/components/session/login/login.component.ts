import {AfterViewInit, Component, inject} from '@angular/core';
import {SuccessStatus} from "../../../../../openapi";
import {FormControl, FormGroup, FormsModule, ReactiveFormsModule} from "@angular/forms";
import {ActivatedRoute, Router} from "@angular/router";
import {MatSnackBar} from "@angular/material/snack-bar";
import {AuthenticationService} from "../../../services/authentication.service";
import {MatCard, MatCardContent, MatCardTitle} from '@angular/material/card';
import {MatFormField, MatInput, MatLabel} from '@angular/material/input';
import {MatButton} from '@angular/material/button';

@Component({
    selector: 'app-login',
    templateUrl: './login.component.html',
    styleUrls: ['./login.component.scss'],
    imports: [MatCard, MatCardTitle, MatCardContent, FormsModule, ReactiveFormsModule, MatFormField, MatLabel, MatInput, MatButton]
})
export class LoginComponent implements AfterViewInit {
  /** The {@link AuthenticationService} used to query and change the login state. */
  private authentication = inject(AuthenticationService);

  /** The {@link Router} used for navigation. */
  private router = inject(Router);

  /** The {@link ActivatedRoute} used to read route parameters. */
  private route = inject(ActivatedRoute);

  /** The {@link MatSnackBar} used to display notifications. */
  private snackBar = inject(MatSnackBar);

  /** The {@link FormGroup} for the user to enter their credentials. */
  public readonly form: FormGroup = new FormGroup({
    username: new FormControl(''),
    password: new FormControl(''),
  });

  /** The URL to return to once login was successful. */
  private readonly returnUrl: string;

  /**
   * Default constructor
   */
  constructor() {
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '';
  }
  /**
   * Initializes the return URL based on the referrer and triggers navigation, if the user has already been logged in.
   */
  public ngAfterViewInit(): void {
    if (this.authentication.isLoggedIn()) {
      this.router.navigateByUrl(this.returnUrl, {skipLocationChange: true}).then(r => { /* No op. */});
    }
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