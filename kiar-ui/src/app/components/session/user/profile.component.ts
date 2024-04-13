import {AfterViewInit, Component} from "@angular/core";
import {FormControl, FormGroup, Validators} from "@angular/forms";
import {Role, SessionService, SessionStatus, User} from "../../../../../openapi";
import {Observer} from "rxjs";
import {MatSnackBar, MatSnackBarConfig} from "@angular/material/snack-bar";
import {PASSWORD_VALIDATOR} from "../../../utilities/password";

@Component({
  selector: 'app-user-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements AfterViewInit {


  /** The {@link FormControl} that backs this {@link AddJobTemplateDialogComponent}. */
  public formControl: FormGroup =  new FormGroup({
    id: new FormControl('', [Validators.required]),
    username: new FormControl({value: '', disabled: true}, [Validators.required, Validators.minLength(3)]),
    email: new FormControl('', [Validators.email]),
    role: new FormControl({value: '', disabled: true}, [Validators.required]),
    institution: new FormControl({value: '', disabled: true}, [Validators.required]),
    password: new FormGroup({
      passwordFirst: new FormControl(''),
      passwordSecond: new FormControl('')
    },{ validators: PASSWORD_VALIDATOR })
  })

  constructor(private service: SessionService, private snackBar: MatSnackBar) {
  }


  ngAfterViewInit() {
    this.reload()
  }


  /**
   * Reloads the user data that backs this form.
   */
  public reload() {
    this.service.getUser().subscribe(u => {
      this.formControl.get('id')?.setValue(u.id)
      this.formControl.get('username')?.setValue(u.username)
      this.formControl.get('email')?.setValue(u.email)
      this.formControl.get('role')?.setValue(u.role)
      this.formControl.get('institution')?.setValue(u.institution)
    })
  }

  /**
   * Submits update user data to the server.
   */
  public update() {
    if (this.formControl.valid) {
        const observer = {
          next: (value) => {
            this.snackBar.open("Successfully updates user profile.", "Dismiss", { duration: 2000 } as MatSnackBarConfig)
          },
          error: (err) => {
            this.snackBar.open(`Error occurred while trying to update user profile: ${err?.error?.description}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig)
          }
        } as Observer<SessionStatus>

        this.service.putUpdateCurrentUser({
          id: this.formControl.get('id')?.value,
          username: this.formControl.get('username')?.value,
          email: this.formControl.get('email')?.value,
          role: this.formControl.get('role')?.value as Role,
          password: this.formControl.get('password')?.get('passwordFirst')?.value,
          institution: this.formControl.get('institution')?.value,
          active: true
        } as User).subscribe(observer)
      }
    }
}