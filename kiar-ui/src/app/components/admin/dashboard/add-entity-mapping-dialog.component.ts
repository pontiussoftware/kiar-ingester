import {Component} from "@angular/core";
import {FormControl, FormGroup, Validators} from "@angular/forms";
import {MatDialogRef} from "@angular/material/dialog";
import {EntityMapping, MappingType} from "../../../../../openapi";

@Component({
  selector: 'kiar-add-entity-mapping-dialog',
  templateUrl: './add-entity-mapping-dialog.component.html'
})
export class AddEntityMappingDialogComponent {

  /** The {@link FormControl} that backs this {@link AddEntityMappingDialogComponent}. */
  public formControl: FormGroup =  new FormGroup({
    name: new FormControl('', [Validators.required, Validators.minLength(3)]),
    description: new FormControl(''),
    type: new FormControl('', [Validators.required]),
  })

  constructor(private dialogRef: MatDialogRef<AddEntityMappingDialogComponent>) {}

  /**
   * Saves the data in this {@link AddEntityMappingDialogComponent}.
   */
  public save() {
    if (this.formControl.valid) {
      let object = {
        name: this.formControl.get('name')?.value,
        description: this.formControl.get('description')?.value,
        type: this.formControl.get('type')?.value as MappingType,
        attributes: []
      } as EntityMapping
      this.dialogRef.close(object)
    }
  }
}