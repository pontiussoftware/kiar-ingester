import {Component, inject} from "@angular/core";
import {FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators} from "@angular/forms";
import {MatDialogActions, MatDialogContent, MatDialogRef, MatDialogTitle} from "@angular/material/dialog";
import {MatFormField, MatInput, MatLabel} from "@angular/material/input";
import {MatButton} from "@angular/material/button";

@Component({
    selector: 'kiar-add-participant-dialog',
    templateUrl: './add-participant-dialog.component.html',
    imports: [MatDialogTitle, MatDialogContent, FormsModule, ReactiveFormsModule, MatFormField, MatLabel, MatInput, MatDialogActions, MatButton]
})
export class AddParticipantDialogComponent {
  /** The {@link MatDialogRef} used to interact with and close this dialog. */
  private dialogRef = inject<MatDialogRef<AddParticipantDialogComponent>>(MatDialogRef);

  /** The {@link FormControl} that backs this {@link AddParticipantDialogComponent}. */
  public formControl: FormGroup =  new FormGroup({
    name: new FormControl('', [Validators.required, Validators.minLength(3)]),
  })

  /**
   * Saves the data in this {@link AddParticipantDialogComponent}.
   */
  public save() {
    if (this.formControl.valid) {
      this.dialogRef.close(this.formControl.get('name')?.value)
    }
  }
}