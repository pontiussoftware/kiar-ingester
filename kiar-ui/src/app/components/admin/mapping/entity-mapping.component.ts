import {AfterViewInit, Component} from "@angular/core";
import {AttributeMapping, EntityMapping, EntityMappingService, MappingFormat, ValueParser} from "../../../../../openapi";
import {ActivatedRoute, Router} from "@angular/router";
import {catchError, map, mergeMap, Observable, of, shareReplay} from "rxjs";
import {MatSnackBar, MatSnackBarConfig} from "@angular/material/snack-bar";
import {FormArray, FormControl, FormGroup, Validators} from "@angular/forms";
import {MatDialog} from "@angular/material/dialog";
import {AttributeMappingData, AttributeMappingDialogComponent} from "./attribute-mapping-dialog.component";

@Component({
  selector: 'kiar-entity-mapping-admin',
  templateUrl: './entity-mapping.component.html',
  styleUrls: ['./entity-mapping.component.scss']
})
export class EntityMappingComponent implements AfterViewInit {

  /** An {@link Observable} of the mapping ID that is being inspected by this {@link EntityMappingComponent}. */
  public readonly mappingId: Observable<string>

  /** An {@link Observable} of the list of available {@link ValueParser}s. */
  public readonly parsers: Observable<Array<ValueParser>>

  /** An {@link Observable} of available {@link MappingFormat}. */
  public readonly mappingFormats: Observable<Array<MappingFormat>>

  /** List of attribute {@link FormGroup}s. */
  public readonly attributes: FormArray<any> = new FormArray<any>([])

  /** The {@link FormControl} that backs this {@link EntityMappingComponent}. */
  public readonly formControl = new FormGroup({
    name: new FormControl('', [Validators.required]),
    description: new FormControl(''),
    type: new FormControl('', [Validators.required]),
    attributes: this.attributes
  })

  constructor(
      private service: EntityMappingService,
      private router: Router,
      private route: ActivatedRoute,
      private snackBar: MatSnackBar,
      public dialog: MatDialog
  ) {
    this.mappingId = this.route.paramMap.pipe(map(params => params.get('id')!!));
    this.parsers = this.service.getListParsers().pipe(shareReplay(1))
    this.mappingFormats = this.service.getListMappingFormats().pipe(shareReplay(1))
  }

  /**
   * Refreshes the data after view has been setup.
   */
  public ngAfterViewInit() {
    this.refresh()
  }

  /**
   * Opens a {@link AttributeMappingDialogComponent} to add an existing  {@link AttributeMapping}.
   */
  public addAttributeMapping() {
    this.attributes.insert(0, this.newAttributeMappingFormGroup(null))
  }

  /**
   * Removes an existing {@link AttributeMapping}.
   *
   * @param index The index of the {@link AttributeMapping} to remove.
   */
  public removeAttributeMapping(index: number) {
    this.attributes.removeAt(index)
  }

  /**
   * Moves a {@link AttributeMapping} in the list.
   *
   * @param index The index of the transformer to move.
   * @param newIndex The new index to move.
   */
  public moveAttributeMapping(index: number, newIndex: number): void {
    const entry = this.attributes.at(index)
    this.attributes.removeAt(index)
    this.attributes.insert(newIndex, entry)
  }

  /**
   * Opens a {@link AttributeMappingDialogComponent} to edit an existing {@link AttributeMapping}.
   *
   * @param index The index of the {@link AttributeMapping} to edit.
   */
  public editAttributeMapping(index: number) {
    this.dialog.open(AttributeMappingDialogComponent, {
      data: { form: this.attributes.at(index), new: false } as AttributeMappingData,
      width: '750px'
    })
  }


  /**
   * Tries to save the current state of the {@link EntityMapping} represented by the local {@link FormControl}
   */
  public save() {
    this.mappingId.pipe(
        mergeMap((id) => this.service.updateEntityMapping(id, this.formToEntityMapping(id)))
    ).subscribe({
      next: (m) => {
        this.snackBar.open(`Successfully saved updated entity mapping.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig);
        this.updateForm(m)
      },
      error: (err) => this.snackBar.open(`Error occurred while trying to update entity mapping: ${err?.error?.description}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig)
    })
  }

  /**
   * Opens a {@link AttributeMappingDialogComponent} to add an existing  {@link AttributeMapping}.
   */
  public delete() {
    if (confirm("Are you sure that you want to delete this entity mapping?\nAfter deletion, it can no longer be retrieved.")) {
      this.mappingId.pipe(
          mergeMap((id) => this.service.deleteEntityMapping(id))
      ).subscribe({
        next: () => {
          this.snackBar.open(`Successfully deleted entity mapping.`, "Dismiss", {duration: 2000} as MatSnackBarConfig);
          this.router.navigate(['admin', 'dashboard']).then(() => {})
        },
        error: (err) => this.snackBar.open(`Error occurred while trying to delete entity mapping: ${err?.error?.description}.`, "Dismiss", {duration: 2000} as MatSnackBarConfig)
      })
    }
  }

  /**
   * Reloads and refreshes the data backing this {@link EntityMappingComponent}.
   */
  public refresh() {
    this.mappingId.pipe(
        mergeMap(id => this.service.getEntityMapping(id)),
        catchError((err) => {
          this.snackBar.open(`Error occurred while trying to load entity mapping: ${err?.error?.description}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig);
          return of(null)
        })
    ).subscribe(m => this.updateForm(m))
  }

  /**
   * Updates the {@link FormControl} backing this view with a new {@link EntityMapping}.
   *
   * @param mapping The {@link EntityMapping} to apply.
   */
  private updateForm(mapping: EntityMapping | null) {
    this.formControl.controls['name'].setValue(mapping?.name || '');
    this.formControl.controls['description'].setValue(mapping?.description || '');
    this.formControl.controls['type'].setValue(mapping?.type || '');

    /* Applies attribute mappings*/
    this.attributes.clear()
    for (let a of (mapping?.attributes || [])) {
      this.attributes.push(this.newAttributeMappingFormGroup(a))
    }
  }

  /**
   * Converts this {@link FormGroup} to an {@link EntityMapping}.
   *
   * @param id The ID of the {@link EntityMapping}
   * @return {@link EntityMapping}
   */
  private formToEntityMapping(id: string): EntityMapping {
    return {
      id: id,
      name: this.formControl.get('name')?.value,
      description: this.formControl.get('description')?.value,
      type: this.formControl.get('type')?.value as MappingFormat,
      attributes: (this.formControl.get('attributes') as FormArray).controls.map(attr => {
        let map = new Map<string,string>;
        (attr.get('parameters') as FormArray).controls.forEach(param => {
          const key = param.get('key')?.value as string
          const value = param.get('value')?.value as string
          map.set(key, value)
        })
        return {
            source: attr.get('source')?.value,
            destination: attr.get('destination')?.value,
            parser: attr.get('parser')?.value as ValueParser,
            required: attr.get('required')?.value,
            multiValued: attr.get('multiValued')?.value,
            parameters: Object.fromEntries(map)
        } as AttributeMapping
      }),
    } as EntityMapping
  }

  /**
   * Creates a new {@link FormGroup} for an {@link AttributeMapping}.
   *
   * @param a The {@link AttributeMapping} to create {@link FormGroup} for.
   * @return {@link FormGroup}
   */
  private newAttributeMappingFormGroup(a: AttributeMapping | null) {
    return new FormGroup({
      source: new FormControl(a?.source, [Validators.required]),
      destination: new FormControl(a?.destination, [Validators.required]),
      parser: new FormControl(a?.parser, [Validators.required]),
      required: new FormControl(a?.required || false),
      multiValued: new FormControl(a?.multiValued || false),
      parameters: new FormArray(Object.entries(a?.parameters || {}).map((k) => new FormGroup({
        key: new FormControl(k[0]),
        value: new FormControl(k[1])
      })))
    })
  }
}