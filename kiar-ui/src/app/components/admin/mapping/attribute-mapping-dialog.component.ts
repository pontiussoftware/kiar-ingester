import {Component, Inject} from "@angular/core";
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {FormArray, FormControl, FormGroup} from "@angular/forms";
import {EntityMappingService, ValueParser} from "../../../../../openapi";
import {Observable, shareReplay} from "rxjs";


/**
 * Data describing the attribute data as handed to the {@link AttributeMappingDialogComponent}.
 */
export interface AttributeMappingData {
  form: FormGroup,
  new: boolean
}

@Component({
  selector: 'attribute-mapping-dialog',
  templateUrl: 'attribute-mapping-dialog.component.html',
})
export class AttributeMappingDialogComponent {

  /** An {@link Observable} of the list of available {@link ValueParser}s. */
  public readonly parsers: Observable<Array<ValueParser>>

  constructor(
      private dialogRef: MatDialogRef<AttributeMappingDialogComponent>,
      @Inject(MAT_DIALOG_DATA) public data: AttributeMappingData,
      private _service: EntityMappingService) {
    this.parsers = this._service.getListParsers().pipe(shareReplay(1, 30000))
  }

  /**
   * Accessor for the {@link FormArray} holding parameter values.
   */

  get parameterForms(): FormArray {
    return this.data.form.get('parameters') as FormArray
  }

  /**
   * The title of this {@link AttributeMappingDialogComponent}
   */
  get title(): string {
    if (this.data.new) {
      return "Create Attribute Mapping";
    } else {
      return "Edit Attribute Mapping";
    }
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
    this.dialogRef.close(this.data);
  }
}