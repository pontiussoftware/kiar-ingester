import {Component} from "@angular/core";
import {FormControl, FormGroup, Validators} from "@angular/forms";
import {ConfigService, Institution} from "../../../../openapi";
import {MatDialogRef} from "@angular/material/dialog";
import {Observable, shareReplay} from "rxjs";

@Component({
  selector: 'kiar-add-institution-dialog',
  templateUrl: './add-institution-dialog.component.html'
})
export class AddInstitutionDialogComponent {

  /** The {@link FormControl} that backs this {@link AddEntityMappingDialogComponent}. */
  public formControl: FormGroup = new FormGroup({
    name: new FormControl('', [Validators.required, Validators.minLength(10)]),
    displayName: new FormControl('', [Validators.required, Validators.minLength(10)]),
    description: new FormControl(''),
    participantName: new FormControl('', [Validators.required]),
    street: new FormControl('', [Validators.required]),
    zip: new FormControl('', [Validators.required]),
    city: new FormControl('', [Validators.required]),
    canton: new FormControl('', [Validators.required]),
    email: new FormControl('', [Validators.required, Validators.email]),
    homepage: new FormControl(''),
    publish: new FormControl(true, [Validators.required])
  })

  /** List of eligible cantons. */
  public readonly cantons = ['Aargau', 'Basel-Landschaft', 'Basel-Stadt', 'Bern', 'Luzern', 'Solothurn']

  /** An {@link Observable} of available participants. */
  public readonly participants: Observable<Array<String>>

  constructor(private service: ConfigService, private dialogRef: MatDialogRef<AddInstitutionDialogComponent>) {
    this.participants = this.service.getListParticipants().pipe(shareReplay(1, 30000))
  }

  /**
   * Saves the data in this {@link AddEntityMappingDialogComponent}.
   */
  public save() {
    if (this.formControl.valid) {
      let object = {
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