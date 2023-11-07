import {Component, Inject} from "@angular/core";
import {FormArray, FormControl, FormGroup, Validators} from "@angular/forms";
import {ApacheSolrCollection, Canton, ConfigService, Institution, InstitutionService, MasterdataService, RightStatement} from "../../../../openapi";
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {map, Observable, shareReplay} from "rxjs";
import {MatSnackBar, MatSnackBarConfig} from "@angular/material/snack-bar";

@Component({
  selector: 'kiar-add-institution-dialog',
  templateUrl: './institution-dialog.component.html',
  styleUrls: ['./institution-dialog.component.scss']
})
export class InstitutionDialogComponent {

  /** The {@link FormControl} that backs this {@link AddEntityMappingDialogComponent}. */
  public formControl: FormGroup

  /** An {@link Observable} of available participants. */
  public readonly participants: Observable<Array<String>>

  /** An {@link Observable} of available {@link RightStatement}s. */
  public readonly rightStatements: Observable<Array<RightStatement>>

  /** An {@link Observable} of available {@link ApacheSolrCollection}s. */
  public readonly collections: Observable<Array<ApacheSolrCollection>>

  /** An {@link Observable} of available {@link Canton}s. */
  public readonly cantons: Observable<Array<Canton>>

  /** A list of all collections. */
  public allCollections: Array<ApacheSolrCollection> = []

  /** A list of all available collections. */
  public availableCollections: Array<ApacheSolrCollection> = []

  /** A list of available collections. */
  public availableCollectionsForms: Array<FormControl> = []

  /** A list of selected collections. */
  public selectedCollectionsForms: Array<FormControl> = []

  constructor(
      private config: ConfigService,
      private masterdata: MasterdataService,
      private institution: InstitutionService,
      private dialogRef: MatDialogRef<InstitutionDialogComponent>,
      private snackBar: MatSnackBar,
      @Inject(MAT_DIALOG_DATA) private institutionId: string | null
  ) {
    /* Get list of available participants. */
    this.participants = this.config.getListParticipants().pipe(shareReplay(1, 30000))

    /* Get masterdata. */
    this.rightStatements = this.masterdata.getListRightStatements().pipe(shareReplay(1))
    this.cantons = this.masterdata.getListCantons().pipe(shareReplay(1))

    /* Reload institution data. */
    if (this.institutionId) {
      this.reload(this.institutionId!!)
    }
  }

  /**
   * Saves the data in this {@link AddEntityMappingDialogComponent}.
   */
  public save() {
    if (this.formControl.valid) {
      let availableCollections: Array<string> = []
      let selectedCollections: Array<string> = []

      this.availableCollectionsForms.forEach((c, i) => {
        if (c.value == true) {
          availableCollections.push(this.allCollections[i].name)
        }
      })
      this.selectedCollectionsForms.forEach((c, i) => {
        if (c.value == true) {
          selectedCollections.push(this.availableCollections[i].name)
        }
      })

      let institution = {
        id: this.institutionId || undefined,
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
        publish: this.formControl.get('publish')?.value,
        defaultRightStatement: this.formControl.get('defaultRightStatement')?.value,
        defaultCopyright: this.formControl.get('defaultCopyright')?.value,
        availableCollections: availableCollections,
        selectedCollections: selectedCollections,
      } as Institution

      /* Save institution. */
      if (institution.id) {
        this.institution.putUpdateInstitution(institution.id, institution).subscribe({
          next: (value) => {
            this.snackBar.open(value.description, "Dismiss", { duration: 2000 } as MatSnackBarConfig);
            this.dialogRef.close(institution);
          },
          error: (err) => this.snackBar.open(`Error occurred while trying to update institution: ${err?.error?.description}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig),
        })
      } else {
        this.institution.postCreateInstitution(institution).subscribe({
          next: (value) => {
            this.snackBar.open(value.description, "Dismiss", { duration: 2000 } as MatSnackBarConfig);
            this.dialogRef.close(institution);
          },
          error: (err) => this.snackBar.open(`Error occurred while trying to create institution: ${err?.error?.description}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig),
        })
      }
    }
  }

  /**
   * Opens the 'file open' dialog and starts image upload.
   */
  public uploadImage() {
    if (this.institutionId != null) {
      const fileInput: HTMLInputElement = document.createElement('input');
      fileInput.type = 'file';
      fileInput.addEventListener('change', async (event: Event) => {
        const target = event.target as HTMLInputElement;
        const file: File | null = target.files?.[0] || null;
        if (file) {
          this.institution.postUploadImage(this.institutionId!!, file).subscribe({
            next: () => {
              this.snackBar.open("Successfully uploaded institution image.", "Dismiss", {duration: 2000} as MatSnackBarConfig)
              this.reload(this.institutionId!!)
            },
            error: (err) => this.snackBar.open(`Error occurred while trying to upload image: ${err?.error?.description}.`, "Dismiss", {duration: 2000} as MatSnackBarConfig)
          });
        }
      });
      fileInput.click();
    }
  }

  /**
   *
   * @private
   */
  private reload(id: string) {
    this.institution.getInstitution(id, {} as Institution).subscribe({
      next: (value) => {
        this.formControl = new FormGroup({
          name: new FormControl(value.name || '', [Validators.required, Validators.minLength(10)]),
          imageName: new FormControl({value: value?.imageName || '', disabled: true}),
          displayName: new FormControl(value.displayName || '', [Validators.required, Validators.minLength(10)]),
          description: new FormControl(value.description || ''),
          participantName: new FormControl(value.participantName || '', [Validators.required]),
          street: new FormControl(value.street || '', [Validators.required]),
          zip: new FormControl(value.zip || '', [Validators.required]),
          city: new FormControl(value.city || '', [Validators.required]),
          canton: new FormControl(value.canton || '', [Validators.required]),
          email: new FormControl(value.email || '', [Validators.required, Validators.email]),
          homepage: new FormControl(value?.homepage || ''),
          publish: new FormControl(value.publish || true, [Validators.required]),
          defaultRightStatement: new FormControl(value.defaultRightStatement || ''),
          defaultCopyright: new FormControl(value.defaultCopyright || ''),
          availableCollections: new FormArray(this.availableCollectionsForms),
          selectedCollections: new FormArray(this.selectedCollectionsForms)
        })

        /* Get collections. */
        this.config.getListSolrConfiguration().pipe(
            map(config => config.flatMap(c => c.collections).filter(c => c.type == 'OBJECT'))
        ).subscribe(collections => {
          for (const c of collections) {
            this.allCollections.push(c)
            const isAvailable = (value.availableCollections || []).findIndex(s => s === c.name) > -1
            this.availableCollectionsForms.push(new FormControl(isAvailable))
            if (isAvailable) {
              this.availableCollections.push(c)
              const isSelected = (value.selectedCollections || []).findIndex(s => s === c.name) > -1
              this.selectedCollectionsForms.push(new FormControl(isSelected))
            }
          }
        })
      },
      error: (err) => this.snackBar.open(`Error occurred while trying to create institution: ${err?.error?.description}.`, "Dismiss", {duration: 2000} as MatSnackBarConfig)
    })
  }
}