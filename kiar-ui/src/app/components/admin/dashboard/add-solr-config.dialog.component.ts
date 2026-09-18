import {Component, inject} from "@angular/core";
import {FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators} from "@angular/forms";
import {MatDialogActions, MatDialogContent, MatDialogRef, MatDialogTitle} from "@angular/material/dialog";
import {ApacheSolrConfig} from "../../../../../openapi";
import {MatFormField, MatInput, MatLabel} from "@angular/material/input";
import {MatButton} from "@angular/material/button";

@Component({
    selector: 'kiar-add-solr-config-dialog',
    templateUrl: './add-solr-config.dialog.component.html',
  imports: [MatDialogTitle, MatDialogContent, FormsModule, ReactiveFormsModule, MatFormField, MatLabel, MatInput, MatDialogActions, MatButton]
})
export class AddSolrConfigDialogComponent {
  /** The {@link MatDialogRef} used to interact with and close this dialog. */
  private dialogRef = inject<MatDialogRef<AddSolrConfigDialogComponent>>(MatDialogRef);

  /** The {@link FormControl} that backs this {@link AddEntityMappingDialogComponent}. */
  public formControl: FormGroup =  new FormGroup({
    name: new FormControl('', [Validators.required, Validators.minLength(3)]),
    description: new FormControl(''),
    server: new FormControl('', [Validators.required, Validators.pattern('(https?://)?([\\da-z.-]+)\\.([a-z.]{2,6})[/\\w .-]*/?')]),
    username: new FormControl('', [Validators.required]),
    password: new FormControl('', [Validators.required])
  })

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
        collections: [],
        deployments: []
      } as ApacheSolrConfig)
    }
  }
}