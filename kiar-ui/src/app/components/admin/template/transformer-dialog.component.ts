import {Component, inject} from "@angular/core";
import {toSignal} from "@angular/core/rxjs-interop";
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {AttributeMappingDialogComponent} from "../mapping/attribute-mapping-dialog.component";
import {FormArray, FormControl, FormGroup} from "@angular/forms";
import {ConfigService, TransformerType} from "../../../../../openapi";

@Component({
    selector: 'transformer-dialog',
    templateUrl: 'transformer-dialog.component.html',
    standalone: false
})
export class TransformerDialogComponent {
  /** The {@link MatDialogRef} used to interact with the dialog. */
  private dialogRef = inject<MatDialogRef<AttributeMappingDialogComponent>>(MatDialogRef);

  /** The {@link ConfigService} instance used to access application configuration. */
  private service = inject(ConfigService);

  /** The provided input data. */
  protected formGroup = inject<FormGroup>(MAT_DIALOG_DATA);

  /** A signal of the available {@link TransformerType}s. */
  public readonly transformerTypes = toSignal(this.service.getListTransformerTypes(), {initialValue: [] as Array<TransformerType>})

  /**
   * Accessor for the {@link FormArray} holding parameter values.
   */
  get parameterForms(): FormArray<FormGroup> {
    return this.formGroup.get('parameters') as FormArray<FormGroup>
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