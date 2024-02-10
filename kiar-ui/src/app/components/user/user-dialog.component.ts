import {Component, Inject} from "@angular/core";
import {FormControl, FormGroup, Validators} from "@angular/forms";
import {Observable, shareReplay} from "rxjs";
import {Institution, InstitutionService, Role, User, UserService} from "../../../../openapi";
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {PASSWORD_MIN_LENGTH, PASSWORD_VALIDATOR} from "../../utilities/password";

@Component({
  selector: 'kiar-user-dialog',
  templateUrl: './user-dialog.component.html',
  styleUrls: ['./user-dialog.component.scss']
})
export class UserDialogComponent {

  /** The {@link FormControl} that backs this {@link AddEntityMappingDialogComponent}. */
  public formControl: FormGroup

  /** An {@link Observable} of available {@link Institution}s. */
  public readonly institutions: Observable<Array<String>>

  /** An {@link Observable} of available {@link Role}s. */
  public readonly roles: Observable<Array<Role>>

  constructor(private user: UserService, private institution: InstitutionService, private dialogRef: MatDialogRef<UserDialogComponent>, @Inject(MAT_DIALOG_DATA) private data: User | null) {
    this.formControl = new FormGroup({
      username: new FormControl(this.data?.username || '', [Validators.required, Validators.minLength(4)]),
      email: new FormControl(this.data?.email || '', [Validators.email]),
      password: new FormGroup({
        passwordFirst: new FormControl('', [Validators.minLength(PASSWORD_MIN_LENGTH)]),
        passwordSecond: new FormControl('', [Validators.minLength(PASSWORD_MIN_LENGTH)])
      },{ validators: PASSWORD_VALIDATOR }),
      active: new FormControl(this.data?.active),
      role: new FormControl(this.data?.role || ''),
      institutionName: new FormControl(this.data?.institution || ''),
    })

    /* Get list of institutions. */
    this.institutions = this.institution.getInstitutionNames().pipe(shareReplay(1))
    this.roles = this.user.getListRoles().pipe(shareReplay(1))
  }

  /**
   * Saves the data in this {@link AddEntityMappingDialogComponent}.
   */
  public save() {
    if (this.formControl.valid) {
      let object = {
        id: this.data?.id || undefined,
        username: this.formControl.get('username')?.value,
        email: this.formControl.get('email')?.value,
        password: this.formControl.get('password')?.get('passwordFirst')?.value,
        role: this.formControl.get('role')?.value,
        institution: this.formControl.get('institutionName')?.value,
        active: (this.formControl.get('active')?.value || false)
      } as User
      this.dialogRef.close(object)
    }
  }
}