import {Component, inject} from "@angular/core";
import {toSignal} from "@angular/core/rxjs-interop";
import {FormArray, FormControl, FormGroup, Validators} from "@angular/forms";
import {
  ApacheSolrCollection,
  Canton,
  ConfigService,
  Institution,
  InstitutionService,
  MasterdataService,
  RightStatement
} from "../../../../openapi";
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {combineLatestWith, first, map} from "rxjs";
import {MatSnackBar, MatSnackBarConfig} from "@angular/material/snack-bar";

@Component({
    selector: 'kiar-add-institution-dialog',
    templateUrl: './institution-dialog.component.html',
    styleUrls: ['./institution-dialog.component.scss'],
    standalone: false
})
export class InstitutionDialogComponent {
  /** The {@link ConfigService} used to access application configuration (templates, mappings, Solr configurations, participants). */
  private config = inject(ConfigService);

  /** The {@link MasterdataService} used to load master data such as cantons and right statements. */
  private masterdata = inject(MasterdataService);

  /** The {@link InstitutionService} used to access institution data. */
  private institution = inject(InstitutionService);

  /** The {@link MatDialogRef} used to interact with and close this dialog. */
  private dialogRef = inject<MatDialogRef<InstitutionDialogComponent>>(MatDialogRef);

  /** The {@link MatSnackBar} used to display notifications. */
  private snackBar = inject(MatSnackBar);

  /** The ID of the {@link Institution} to edit, provided as dialog data (null when creating a new one). */
  protected institutionId = inject<number | null>(MAT_DIALOG_DATA);

  /** The {@link FormControl} that backs this {@link AddEntityMappingDialogComponent}. */
  public formControl: FormGroup

  /** A signal of the available participant names. */
  public readonly participants = toSignal(this.config.getListParticipants(), {initialValue: [] as Array<string>})

  /** A signal of the available {@link RightStatement}s. */
  public readonly rightStatements = toSignal(this.masterdata.getListRightStatements(), {initialValue: [] as Array<RightStatement>})

  /** A signal of the available {@link Canton}s. */
  public readonly cantons = toSignal(this.masterdata.getListCantons(), {initialValue: [] as Array<Canton>})
  /** A list of all collections. */
  public allCollections: Array<ApacheSolrCollection> = []

  /** A list of available collections. */
  public availableCollections: Array<ApacheSolrCollection> = []

  /** A list of available collections. */
  public availableCollectionsForms: Array<FormControl> = []

  /** A list of selected collections. */
  public selectedCollectionsForms: Array<FormControl> = []

  constructor() {
    /* Prepare empty form. */
    this.formControl = new FormGroup({
      name: new FormControl(null, [Validators.required, Validators.minLength(5)]),
      imageName: new FormControl({value: null, disabled: true}),
      displayName: new FormControl(null, [Validators.required, Validators.minLength(5)]),
      description: new FormControl(null),
      participantName: new FormControl(null, [Validators.required]),
      street: new FormControl(null, [Validators.required]),
      zip: new FormControl(null, [Validators.required]),
      city: new FormControl(null, [Validators.required]),
      canton: new FormControl(null, [Validators.required]),
      longitude: new FormControl(null, [Validators.min(-180), Validators.max(180)]),
      latitude: new FormControl(null, [Validators.min(-180), Validators.max(180)]),
      email: new FormControl(null, [Validators.required, Validators.email]),
      homepage: new FormControl(null),
      publish: new FormControl(true, [Validators.required]),
      defaultRightStatement: new FormControl(null),
      defaultCopyright: new FormControl(null),
      defaultObjectUrl: new FormControl(null),
      availableCollections: new FormArray(this.availableCollectionsForms),
      selectedCollections: new FormArray(this.selectedCollectionsForms)
    })

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

      /* Create institution. */
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
        longitude: this.formControl.get('longitude')?.value,
        latitude: this.formControl.get('latitude')?.value,
        email: this.formControl.get('email')?.value,
        homepage: this.formControl.get('homepage')?.value,
        publish: this.formControl.get('publish')?.value,
        defaultRightStatement: this.formControl.get('defaultRightStatement')?.value,
        defaultCopyright: this.formControl.get('defaultCopyright')?.value,
        defaultObjectUrl: this.formControl.get('defaultObjectUrl')?.value,
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
          this.institution.postInstitutionImage(this.institutionId!!, file).subscribe({
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
  private reload(id: number) {
    this.config.getListSolrCollections().pipe(
        map(c => c.filter(c => c.type == 'OBJECT')),
        combineLatestWith(this.institution.getInstitution(id)),
        first()
    ).subscribe({
      next: ([collections, institution]) => {
        /* Update form control. */
        this.formControl.get('name')?.setValue(institution.name)
        this.formControl.get('displayName')?.setValue(institution.displayName)
        this.formControl.get('imageName')?.setValue(institution.imageName)
        this.formControl.get('description')?.setValue(institution.description)
        this.formControl.get('participantName')?.setValue(institution.participantName)
        this.formControl.get('street')?.setValue(institution.street)
        this.formControl.get('zip')?.setValue(institution.zip)
        this.formControl.get('city')?.setValue(institution.city)
        this.formControl.get('canton')?.setValue(institution.canton)
        this.formControl.get('longitude')?.setValue(institution.longitude)
        this.formControl.get('latitude')?.setValue(institution.latitude)
        this.formControl.get('email')?.setValue(institution.email)
        this.formControl.get('homepage')?.setValue(institution.homepage)
        this.formControl.get('publish')?.setValue(institution.publish)
        this.formControl.get('defaultRightStatement')?.setValue(institution.defaultRightStatement)
        this.formControl.get('defaultCopyright')?.setValue(institution.defaultCopyright)
        this.formControl.get('defaultObjectUrl')?.setValue(institution.defaultObjectUrl)

        /* Assign collections. */
        this.allCollections.length = 0
        this.availableCollections.length = 0
        this.availableCollectionsForms.length = 0
        this.selectedCollectionsForms.length = 0
        for (const c of collections) {
          const isAvailable = (institution.availableCollections || []).findIndex(s => s === c.name) > -1
          this.allCollections.push(c)
          this.availableCollectionsForms.push(new FormControl(isAvailable))
          if (isAvailable) {
            this.availableCollections.push(c)
            const isSelected = (institution.selectedCollections || []).findIndex(s => s === c.name) > -1
            this.selectedCollectionsForms.push(new FormControl(isSelected))
          }
        }
      },
      error: (err) => this.snackBar.open(`Error occurred while trying to create institution: ${err?.error?.description}.`, "Dismiss", {duration: 2000} as MatSnackBarConfig)
    })
  }
}