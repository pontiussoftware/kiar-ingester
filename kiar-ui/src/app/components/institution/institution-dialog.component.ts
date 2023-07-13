import {Component, Inject} from "@angular/core";
import {FormControl, FormGroup, Validators} from "@angular/forms";
import {ConfigService, Institution} from "../../../../openapi";
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {Observable, shareReplay} from "rxjs";

@Component({
  selector: 'kiar-add-institution-dialog',
  templateUrl: './institution-dialog.component.html'
})
export class InstitutionDialogComponent {

  /** The {@link FormControl} that backs this {@link AddEntityMappingDialogComponent}. */
  public formControl: FormGroup

  /** List of eligible cantons. */
  public readonly cantons = ['Aargau', 'Basel-Landschaft', 'Basel-Stadt', 'Bern', 'Luzern', 'Solothurn']

  /** An {@link Observable} of available participants. */
  public readonly participants: Observable<Array<String>>

  constructor(private service: ConfigService, private dialogRef: MatDialogRef<InstitutionDialogComponent>, @Inject(MAT_DIALOG_DATA) private data: Institution | null) {
    this.participants = this.service.getListParticipants().pipe(shareReplay(1, 30000))
    this.formControl = new FormGroup({
      name: new FormControl(this.data?.name || '', [Validators.required, Validators.minLength(10)]),
      displayName: new FormControl(this.data?.displayName || '', [Validators.required, Validators.minLength(10)]),
      description: new FormControl(this.data?.description || ''),
      participantName: new FormControl(this.data?.participantName || '', [Validators.required]),
      street: new FormControl(this.data?.street || '', [Validators.required]),
      zip: new FormControl(this.data?.zip || '', [Validators.required]),
      city: new FormControl(this.data?.city || '', [Validators.required]),
      canton: new FormControl(this.data?.canton || '', [Validators.required]),
      email: new FormControl(this.data?.email || '', [Validators.required, Validators.email]),
      homepage: new FormControl(this.data?.homepage || ''),
      publish: new FormControl(this.data?.publish || true, [Validators.required])
    })
  }

  /**
   * Saves the data in this {@link AddEntityMappingDialogComponent}.
   */
  public save() {
    if (this.formControl.valid) {
      let object = {
        id: this.data?.id || undefined,
        name: this.formControl.get('name')?.value,
        displayName: this.formControl.get('displayName')?.value,
        description: this.formControl.get('description')?.value,
        participantName: this.formControl.get('participantName')?.value,
        isil: this.formControl.get('isil')?.value,
        street: this.formControl.get('street')?.value,
        zip: this.formControl.get('zip')?.value,
        city: this.formControl.get('city')?.value,
        canton: this.formControl.get('canton')?.value,
        email: this.formControl.get('email')?.value,
        homepage: this.formControl.get('homepage')?.value,
        publish: this.formControl.get('publish')?.value
      } as Institution
      this.dialogRef.close(object)
    }
  }
}