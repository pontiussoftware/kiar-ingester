import {Component} from "@angular/core";
import {FormControl, FormGroup, Validators} from "@angular/forms";
import {MatDialogRef} from "@angular/material/dialog";
import {EntityMapping, EntityMappingService, MappingFormat} from "../../../../../openapi";
import {Observable, shareReplay} from "rxjs";

@Component({
    selector: 'kiar-add-entity-mapping-dialog',
    templateUrl: './add-entity-mapping-dialog.component.html',
    standalone: false
})
export class AddEntityMappingDialogComponent {

  /** The {@link FormControl} that backs this {@link AddEntityMappingDialogComponent}. */
  public formControl: FormGroup =  new FormGroup({
    name: new FormControl('', [Validators.required, Validators.minLength(3)]),
    description: new FormControl(''),
    type: new FormControl('', [Validators.required]),
  })

  /** An {@link Observable} of available {@link MappingFormat}. */
  public readonly mappingFormats: Observable<Array<MappingFormat>>

  constructor(private dialogRef: MatDialogRef<AddEntityMappingDialogComponent>, private service: EntityMappingService,) {
    this.mappingFormats = this.service.getListMappingFormats().pipe(shareReplay(1))
  }

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