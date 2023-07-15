import {Component, Inject} from "@angular/core";
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {AttributeMappingDialogComponent} from "../mapping/attribute-mapping-dialog.component";
import {FormArray, FormControl, FormGroup} from "@angular/forms";
import {Observable, shareReplay} from "rxjs";
import {ConfigService, TransformerType} from "../../../../../openapi";

@Component({
  selector: 'transformer-dialog',
  templateUrl: 'transformer-dialog.component.html',
})
export class TransformerDialogComponent {

  /** An {@link Observable} of available {@link TransformerType}. */
  public readonly transformerTypes: Observable<Array<TransformerType>>

  constructor(
      private dialogRef: MatDialogRef<AttributeMappingDialogComponent>,
      private service: ConfigService,
      @Inject(MAT_DIALOG_DATA) public formGroup: FormGroup) {
    this.transformerTypes = this.service.getListTransformerTypes().pipe(shareReplay(1, 30000))
  }

  /**
   * Accessor for the {@link FormArray} holding parameter values.
   */

  get parameterForms(): FormArray {
    return this.formGroup.get('parameters') as FormArray
  }

  /**
   * Adds a {@link FormGroup} for a new parameter.
   */
  public addParameter() {
    this.parameterForms.push(new FormGroup({
      key: new FormControl(),
      value: new FormControl()
    }))
  }

  /**
   * Removes a parameter {@link FormGroup} at the provided index.
   *
   * @param index
   */
  public removeParameter(index: number) {
    this.parameterForms.removeAt(index)
  }

  /**
   * Closes this {@link AttributeMappingDialogComponent}.
   */
  public close() {
    this.dialogRef.close(this.formGroup);
  }
}