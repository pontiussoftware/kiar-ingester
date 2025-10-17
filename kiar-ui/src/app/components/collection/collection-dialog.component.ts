import {Component, ElementRef, Inject, ViewChild} from "@angular/core";
import {FormArray, FormControl, FormGroup, Validators} from "@angular/forms";
import {CollectionService, Institution, InstitutionService, ObjectCollection,} from "../../../../openapi";
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {combineLatest, first, map, Observable, shareReplay} from "rxjs";
import {MatSnackBar, MatSnackBarConfig} from "@angular/material/snack-bar";

@Component({
    selector: 'kiar-collection-dialog',
    templateUrl: './collection-dialog.component.html',
    styleUrls: ['./collection-dialog.component.scss'],
    standalone: false
})
export class CollectionDialogComponent {

  /** The {@link FormControl} that backs this {@link AddEntityMappingDialogComponent}. */
  public formControl: FormGroup

  /** An {@link Observable} of available participants. */
  public readonly institutions: Observable<Array<Institution>>

  /** List of images that should be displayed. */
  public images: Array<string> = []

  /** Reference to the file input. */
  @ViewChild('fileInput')
  fileInput: ElementRef<HTMLInputElement>;


  constructor(
      private institutionService: InstitutionService,
      private collectionService: CollectionService,
      private dialogRef: MatDialogRef<CollectionDialogComponent>,
      private snackBar: MatSnackBar,
      @Inject(MAT_DIALOG_DATA) protected collectionId: number | null
  ) {
    /* Prepare empty form. */
    this.formControl = new FormGroup({
      name: new FormControl(null, [Validators.required, Validators.minLength(5)]),
      displayName: new FormControl(null, [Validators.required, Validators.minLength(5)]),
      description: new FormControl(null, [Validators.required]),
      institution: new FormControl<Institution | null>(null, [Validators.required]),
      publish: new FormControl(true, [Validators.required]),
      filters: new FormArray([
          new FormControl(null, [Validators.required]),
      ], [Validators.minLength(1)]),
    })

    /* Get list of available participants. */
    this.institutions = this.institutionService.getInstitutions(0, 1000).pipe(
        first(),
        map(r => r.results),
        shareReplay(1)
    )

    /* Reload institution data. */
    if (this.collectionId) {
      this.reload(this.collectionId!!)
    }
  }

  /**
   * Lists filter criteria in this form.
   */
  get filters(): Array<FormControl> {
    return (this.formControl.get('filters') as FormArray<FormControl>).controls
  }

  /**
   * Adds a new filter criterion to the form.
   */
  public addFilter() {
    (this.formControl.get('filters') as FormArray<FormControl>).push(new FormControl(null, [Validators.required, Validators.minLength(5)]));
  }

  /**
   * Removes the filter criterion at the given index.
   *
   * @param index Of the criterion to remove.
   */
  public removeFilter(index: number) {
    (this.formControl.get('filters') as FormArray<FormControl>).removeAt(index);
  }

  /**
   * Saves the data in this {@link AddEntityMappingDialogComponent}.
   */
  public save() {
    if (this.formControl.valid) {
      /* Create institution. */
      let collection = {
        id: this.collectionId || undefined,
        name: this.formControl.get('name')?.value,
        displayName: this.formControl.get('displayName')?.value,
        description: this.formControl.get('description')?.value,
        institution: this.formControl.get('institution')?.value,
        publish: this.formControl.get('publish')?.value,
        filters: this.filters.map(f => f.value),
        images: [],
      } as ObjectCollection

      /* Save collection. */
      if (collection.id) {
        this.collectionService.putUpdateCollection(collection.id, collection).subscribe({
          next: (value) => {
            this.snackBar.open(value.description, "Dismiss", { duration: 2000 } as MatSnackBarConfig);
            this.dialogRef.close(collection);
          },
          error: (err) => this.snackBar.open(`Error occurred while trying to update collection: ${err?.error?.description}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig),
        })
      } else {
        this.collectionService.postCreateCollection(collection).subscribe({
          next: (value) => {
            this.snackBar.open(value.description, "Dismiss", { duration: 2000 } as MatSnackBarConfig);
            this.dialogRef.close(collection);
          },
          error: (err) => this.snackBar.open(`Error occurred while trying to create collection: ${err?.error?.description}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig),
        })
      }
    }
  }

  /**
   * Opens the 'file open' dialog and starts image upload.
   */
  public uploadImage() {
    if (this.collectionId != null) {
      const fileInput: HTMLInputElement = document.createElement('input');
      fileInput.type = 'file';
      fileInput.addEventListener('change', async (event: Event) => {
        const target = event.target as HTMLInputElement;
        const file: File | null = target.files?.[0] || null;
        if (file) {
          this.collectionService.postCollectionImage(this.collectionId!!, file).subscribe({
            next: () => {
              this.snackBar.open("Successfully uploaded collection image.", "Dismiss", {duration: 2000} as MatSnackBarConfig)
              this.reload(this.collectionId!!)
            },
            error: (err) => this.snackBar.open(`Error occurred while trying to upload image: ${err?.error?.description}.`, "Dismiss", {duration: 2000} as MatSnackBarConfig)
          });
        }
      });
      fileInput.click();
    }
  }

  /**
   * Reloads the current collection.
   *
   * @param id The ID of the collection to reload.
   */
  private reload(id: number) {
    combineLatest([this.collectionService.getCollection(id), this.institutions]).subscribe({
      next: ([collection, institutions]) => {
        /* Update form control. */
        this.formControl.get('name')?.setValue(collection.name)
        this.formControl.get('displayName')?.setValue(collection.displayName)
        this.formControl.get('description')?.setValue(collection.description)
        this.formControl.get('institution')?.setValue(institutions.find(i => i.id === collection.institution?.id))
        this.formControl.get('publish')?.setValue(collection.publish)

        /* Update filters. */
        const filters = (this.formControl.get('filters') as FormArray<FormControl>)
        filters.clear()
        for (const filter of (collection.filters ?? [])) {
          filters.push(new FormControl(filter, [Validators.required, Validators.minLength(5)]));
        }
        if (filters.length == 0) {
          filters.push(new FormControl(null, [Validators.required, Validators.minLength(5)]));
        }

        /* Update images. */
        this.images = collection.images
      },
      error: (err) => this.snackBar.open(`Error occurred while trying to create institution: ${err?.error?.description}.`, "Dismiss", {duration: 2000} as MatSnackBarConfig)
    })
  }
}