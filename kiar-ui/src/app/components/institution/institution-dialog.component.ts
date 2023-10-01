import {Component, Inject} from "@angular/core";
import {FormArray, FormControl, FormGroup, Validators} from "@angular/forms";
import {ApacheSolrCollection, Canton, ConfigService, Institution, MasterdataService, RightStatement} from "../../../../openapi";
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {map, Observable, shareReplay} from "rxjs";

@Component({
  selector: 'kiar-add-institution-dialog',
  templateUrl: './institution-dialog.component.html'
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

  constructor(private config: ConfigService, private masterdata: MasterdataService, private dialogRef: MatDialogRef<InstitutionDialogComponent>, @Inject(MAT_DIALOG_DATA) private data: Institution | null) {
    this.participants = this.config.getListParticipants().pipe(shareReplay(1, 30000))
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
      publish: new FormControl(this.data?.publish || true, [Validators.required]),
      defaultRightStatement: new FormControl(this.data?.defaultRightStatement || ''),
      defaultCopyright: new FormControl(this.data?.defaultCopyright || ''),
      availableCollections: new FormArray(this.availableCollectionsForms),
      selectedCollections: new FormArray(this.selectedCollectionsForms)
    })

    /* Get collections. */
    this.config.getListSolrConfiguration().pipe(
        map(config => config.flatMap(c => c.collections).filter(c => c.type == 'OBJECT'))
    ).subscribe(collections => {
      for (const c of collections) {
        this.allCollections.push(c)
        const isAvailable = (this.data?.availableCollections || []).findIndex(s => s === c.name) > -1
        this.availableCollectionsForms.push(new FormControl(isAvailable))
        if (isAvailable) {
          this.availableCollections.push(c)
          const isSelected = (this.data?.selectedCollections || []).findIndex(s => s === c.name) > -1
          this.selectedCollectionsForms.push(new FormControl(isSelected))
        }
      }
    })

    /* Get masterdata. */
    this.rightStatements = this.masterdata.getListRightStatements().pipe(shareReplay(1))
    this.cantons = this.masterdata.getListCantons().pipe(shareReplay(1))
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
        publish: this.formControl.get('publish')?.value,
        defaultRightStatement: this.formControl.get('defaultRightStatement')?.value,
        defaultCopyright: this.formControl.get('defaultCopyright')?.value,
        availableCollections: availableCollections,
        selectedCollections: selectedCollections,
      } as Institution
      this.dialogRef.close(object)
    }
  }
}