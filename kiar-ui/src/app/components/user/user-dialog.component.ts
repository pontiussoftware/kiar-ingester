import {Component, inject} from "@angular/core";
import {toSignal} from "@angular/core/rxjs-interop";
import {FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators} from "@angular/forms";
import {first, map, tap} from "rxjs";
import {Institution, InstitutionService, Role, User, UserService} from "../../../../openapi";
import {
  MAT_DIALOG_DATA,
  MatDialogActions,
  MatDialogContent,
  MatDialogRef,
  MatDialogTitle
} from "@angular/material/dialog";
import {PASSWORD_VALIDATOR} from "../../utilities/password";
import {MatError, MatFormField, MatInput, MatLabel} from "@angular/material/input";
import {MatOption, MatSelect} from "@angular/material/select";
import {MatCheckbox} from "@angular/material/checkbox";
import {MatButton} from "@angular/material/button";

@Component({
    selector: 'kiar-user-dialog',
    templateUrl: './user-dialog.component.html',
    styleUrls: ['./user-dialog.component.scss'],
  imports: [MatDialogTitle, MatDialogContent, FormsModule, ReactiveFormsModule, MatFormField, MatLabel, MatInput, MatError, MatSelect, MatOption, MatCheckbox, MatDialogActions, MatButton]
})
export class UserDialogComponent {
  /** The {@link UserService} used to load and edit user information. */
  private user = inject(UserService);

  /** The {@link InstitutionService} used to load institution information. */
  private institution = inject(InstitutionService);

  /** The provided input data. */
  private data = inject<User | null>(MAT_DIALOG_DATA);

  /** The {@link MatDialogRef} used to interact with the dialog. */
  private dialogRef = inject<MatDialogRef<UserDialogComponent>>(MatDialogRef);

  /** The {@link FormControl} that backs this {@link AddEntityMappingDialogComponent}. */
  public formControl: FormGroup

  /** A signal of the available {@link Institution}s. */
  public readonly institutions = toSignal(this.institution.getInstitutions(0, 1000).pipe(
        first(),
        map(r => r.results),
        tap(institutions => {
          this.formControl.get('institution')?.setValue(institutions.find(i => i.id == this.data?.institution?.id) ?? null)
        })), {initialValue: [] as Array<Institution>})

  /** A signal of the available {@link Role}s. */
  public readonly roles = toSignal(this.user.getListRoles().pipe(first()), {initialValue: [] as Array<Role>})
  constructor() {
    this.formControl = new FormGroup({
      username: new FormControl(this.data?.username || '', [Validators.required, Validators.minLength(4)]),
      email: new FormControl(this.data?.email || '', [Validators.email]),
      password: new FormGroup({
        passwordFirst: new FormControl(''),
        passwordSecond: new FormControl('')
      },{ validators: PASSWORD_VALIDATOR }),
      active: new FormControl(this.data?.active),
      role: new FormControl(this.data?.role || ''),
      institution: new FormControl<Institution | null>(null),
    })
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
        institution: this.formControl.get('institution')?.value,
        active: (this.formControl.get('active')?.value || false),
        createdAt: -1,
        changedAt: -1
      } as User
      this.dialogRef.close(object)
    }
  }
}