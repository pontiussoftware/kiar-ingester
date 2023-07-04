import {AfterViewInit, Component} from "@angular/core";
import {AbstractControl, FormControl, FormGroup, ValidationErrors, ValidatorFn, Validators} from "@angular/forms";
import {Role, SessionService, SessionStatus, SuccessStatus, User} from "../../../../../openapi";
import {Observable, Observer, tap} from "rxjs";
import {MatSnackBar, MatSnackBarConfig} from "@angular/material/snack-bar";

@Component({
  selector: 'app-user-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements AfterViewInit {

  /** A customg {@link ValidatorFn} that makes sure, that two passwords are the same. */
  private static PASSWORD_VALIDATOR: ValidatorFn = (group: AbstractControl):  ValidationErrors | null => {
    let passwordFirst = group.get('passwordFirst')?.value;
    let passwordSecond = group.get('passwordSecond')?.value
    return passwordFirst === passwordSecond ? null : { notSame: true }
  }

  /** */
  public readonly passwordMinLength = 8

  /** The {@link FormControl} that backs this {@link AddJobTemplateDialogComponent}. */
  public formControl: FormGroup =  new FormGroup({
    id: new FormControl('', [Validators.required]),
    username: new FormControl({value: '', disabled: true}, [Validators.required, Validators.minLength(3)]),
    role: new FormControl({value: '', disabled: true}, [Validators.required]),
    institution: new FormControl({value: '', disabled: true}, [Validators.required]),
    email: new FormControl('', [Validators.email]),
    password: new FormGroup({
      passwordFirst: new FormControl('', [Validators.minLength(this.passwordMinLength)]),
      passwordSecond: new FormControl('', [Validators.minLength(this.passwordMinLength)])
    },{ validators: ProfileComponent.PASSWORD_VALIDATOR })
  })

  constructor(private service: SessionService, private snackBar: MatSnackBar) {
  }


  ngAfterViewInit() {
    this.reload()
  }


  /**
   *
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
   *
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


        this.service.putUpdateUser({
          id: this.formControl.get('id')?.value,
          username: this.formControl.get('username')?.value,
          email: this.formControl.get('email')?.value,
          role: this.formControl.get('role')?.value as Role,
          password: this.formControl.get('password')?.get('passwordFirst')?.value,
          institution: this.formControl.get('institution')?.value
        } as User).subscribe(observer)
      }
    }
}