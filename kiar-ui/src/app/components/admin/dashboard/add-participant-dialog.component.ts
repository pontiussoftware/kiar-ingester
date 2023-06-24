import {Component} from "@angular/core";
import {FormControl, FormGroup, Validators} from "@angular/forms";
import {MatDialogRef} from "@angular/material/dialog";

@Component({
  selector: 'kiar-add-participant-dialog',
  templateUrl: './add-participant-dialog.component.html'
})
export class AddParticipantDialogComponent {

  /** The {@link FormControl} that backs this {@link AddParticipantDialogComponent}. */
  public formControl: FormGroup =  new FormGroup({
    name: new FormControl('', [Validators.required, Validators.minLength(3)]),
  })

  constructor(private dialogRef: MatDialogRef<AddParticipantDialogComponent>) {}

  /**
   * Saves the data in this {@link AddParticipantDialogComponent}.
   */
  public save() {
    if (this.formControl.valid) {
      this.dialogRef.close(this.formControl.get('name')?.value)
    }
  }
}