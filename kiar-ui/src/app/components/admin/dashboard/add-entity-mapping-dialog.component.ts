import {Component, inject} from "@angular/core";
import {TranslatePipe} from "@ngx-translate/core";
import {toSignal} from "@angular/core/rxjs-interop";
import {FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators} from "@angular/forms";
import {MatDialogActions, MatDialogContent, MatDialogRef, MatDialogTitle} from "@angular/material/dialog";
import {EntityMapping, EntityMappingService, MappingFormat} from "../../../../../openapi";
import {MatFormField, MatInput, MatLabel} from "@angular/material/input";
import {MatOption, MatSelect} from "@angular/material/select";
import {MatButton} from "@angular/material/button";

@Component({
    selector: 'kiar-add-entity-mapping-dialog',
    templateUrl: './add-entity-mapping-dialog.component.html',
  imports: [MatDialogTitle, MatDialogContent, FormsModule, ReactiveFormsModule, MatFormField, MatLabel, MatInput, MatSelect, MatOption, MatDialogActions, MatButton, TranslatePipe]
})
export class AddEntityMappingDialogComponent {
  /** The {@link MatDialogRef} used to interact with and close this dialog. */
  private dialogRef = inject<MatDialogRef<AddEntityMappingDialogComponent>>(MatDialogRef);

  /** The {@link EntityMappingService} used to access entity mappings, parsers and mapping formats. */
  private service = inject(EntityMappingService);

  /** The {@link FormControl} that backs this {@link AddEntityMappingDialogComponent}. */
  public formControl: FormGroup =  new FormGroup({
    name: new FormControl('', [Validators.required, Validators.minLength(3)]),
    description: new FormControl(''),
    type: new FormControl('', [Validators.required]),
  })

  /** A signal of the available {@link MappingFormat}s. */
  public readonly mappingFormats = toSignal(this.service.getListMappingFormats(), {initialValue: [] as Array<MappingFormat>})

  /**
   * Saves the data in this {@link AddEntityMappingDialogComponent}.
   */
  public save() {
    if (this.formControl.valid) {
      let object = {
        name: this.formControl.get('name')?.value,
        description: this.formControl.get('description')?.value,
        type: this.formControl.get('type')?.value as MappingFormat,
        attributes: []
      } as EntityMapping
      this.dialogRef.close(object)
    }
  }
}