import {Component, inject} from "@angular/core";
import {toSignal} from "@angular/core/rxjs-interop";
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {FormArray, FormControl, FormGroup} from "@angular/forms";
import {EntityMappingService, ValueParser} from "../../../../../openapi";

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
    standalone: false
})
export class AttributeMappingDialogComponent {
  /** The {@link MatDialogRef} used to interact with and close this dialog. */
  private dialogRef = inject<MatDialogRef<AttributeMappingDialogComponent>>(MatDialogRef);

  /** The {@link EntityMappingService} used to access entity mappings, parsers and mapping formats. */
  private _service = inject(EntityMappingService);

  /** The {@link AttributeMappingData} provided as dialog data. */
  protected data = inject<AttributeMappingData>(MAT_DIALOG_DATA);

  /** A signal of the available {@link ValueParser}s. */
  public readonly parsers = toSignal(this._service.getListParsers(), {initialValue: [] as Array<ValueParser>})

  /**
   * Accessor for the {@link FormArray} holding parameter values.
   */
  get parameterForms(): FormArray<FormGroup> {
    return this.data.form.get('parameters') as FormArray<FormGroup>
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