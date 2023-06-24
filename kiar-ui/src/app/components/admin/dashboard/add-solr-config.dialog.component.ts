import {Component} from "@angular/core";
import {FormControl, FormGroup, Validators} from "@angular/forms";
import {MatDialogRef} from "@angular/material/dialog";
import {SolrConfig} from "../../../../../openapi";

@Component({
  selector: 'kiar-add-solr-config-dialog',
  templateUrl: './add-solr-config.dialog.component.html'
})
export class AddSolrConfigDialogComponent {

  /** The {@link FormControl} that backs this {@link AddEntityMappingDialogComponent}. */
  public formControl: FormGroup =  new FormGroup({
    name: new FormControl('', [Validators.required, Validators.minLength(3)]),
    description: new FormControl(''),
    server: new FormControl('', [Validators.required, Validators.pattern('(https?://)?([\\da-z.-]+)\\.([a-z.]{2,6})[/\\w .-]*/?')]),
    username: new FormControl('', [Validators.required]),
    password: new FormControl('', [Validators.required])
  })

  constructor(private dialogRef: MatDialogRef<AddSolrConfigDialogComponent>) {}

  /**
   * Saves the data in this {@link AddEntityMappingDialogComponent}.
   */
  public save() {
    if (this.formControl.valid) {
      this.dialogRef.close({
        name: this.formControl.get('name')?.value,
        description: this.formControl.get('description')?.value,
        server: this.formControl.get('type')?.value,
        username: this.formControl.get('username')?.value,
        password: this.formControl.get('password')?.value,
        collections: []
      } as SolrConfig)
    }
  }
}