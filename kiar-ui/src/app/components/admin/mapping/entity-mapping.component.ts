import {AfterViewInit, Component, inject} from "@angular/core";
import {toSignal} from "@angular/core/rxjs-interop";
import {
  AttributeMapping,
  EntityMapping,
  EntityMappingService,
  MappingFormat,
  ValueParser
} from "../../../../../openapi";
import {ActivatedRoute, Router} from "@angular/router";
import {catchError, map, mergeMap, Observable, of} from "rxjs";
import {MatSnackBar, MatSnackBarConfig} from "@angular/material/snack-bar";
import {FormArray, FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators} from "@angular/forms";
import {MatDialog} from "@angular/material/dialog";
import {AttributeMappingData, AttributeMappingDialogComponent} from "./attribute-mapping-dialog.component";
import {MatFormField, MatInput, MatLabel} from "@angular/material/input";
import {MatOption, MatSelect} from "@angular/material/select";
import {MatButton, MatIconButton, MatMiniFabButton} from "@angular/material/button";
import {MatTooltip} from "@angular/material/tooltip";
import {MatIcon} from "@angular/material/icon";
import {MatCheckbox} from "@angular/material/checkbox";

@Component({
    selector: 'kiar-entity-mapping-admin',
    templateUrl: './entity-mapping.component.html',
    styleUrls: ['./entity-mapping.component.scss'],
    imports: [FormsModule, ReactiveFormsModule, MatFormField, MatLabel, MatInput, MatSelect, MatOption, MatButton, MatTooltip, MatMiniFabButton, MatIcon, MatIconButton, MatCheckbox]
})
export class EntityMappingComponent implements AfterViewInit {
  /** The {@link EntityMappingService} used to access entity mappings, parsers and mapping formats. */
  private service = inject(EntityMappingService);

  /** The {@link Router} used for navigation. */
  private router = inject(Router);

  /** The {@link ActivatedRoute} used to read route parameters. */
  private route = inject(ActivatedRoute);

  /** The {@link MatSnackBar} used to display notifications. */
  private snackBar = inject(MatSnackBar);

  /** The {@link MatDialog} service used to open dialogs. */
  dialog = inject(MatDialog);

  /** {@link Observable} backing {@link mappingId}. */
  private readonly mappingId$ = this.route.paramMap.pipe(map(params => Number(params.get('id')!!)))

  /** A signal of the current mappingId. */
  public readonly mappingId = toSignal(this.mappingId$)

  /** A signal of the available {@link ValueParser}s. */
  public readonly parsers = toSignal(this.service.getListParsers(), {initialValue: [] as Array<ValueParser>})

  /** A signal of the available {@link MappingFormat}s. */
  public readonly mappingFormats = toSignal(this.service.getListMappingFormats(), {initialValue: [] as Array<MappingFormat>})
  /** List of attribute {@link FormGroup}s. */
  public readonly attributes: FormArray<FormGroup> = new FormArray<FormGroup>([])

  /** The {@link FormControl} that backs this {@link EntityMappingComponent}. */
  public readonly formControl = new FormGroup({
    name: new FormControl('', [Validators.required]),
    description: new FormControl(''),
    type: new FormControl('', [Validators.required]),
    attributes: this.attributes
  })

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
    if (newIndex < 0) {
      this.attributes.removeAt(index)
      this.attributes.push(entry)
    } else if (newIndex > this.attributes.length - 1) {
      this.attributes.removeAt(index)
      this.attributes.insert(0, entry)
    } else {
      this.attributes.removeAt(index)
      this.attributes.insert(newIndex, entry)
    }
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
    this.mappingId$.pipe(
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
      this.mappingId$.pipe(
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
    this.mappingId$.pipe(
        mergeMap(id => this.service.getEntityMapping(id)),
        catchError((err) => {
          this.snackBar.open(`Error occurred while trying to load entity mapping: ${err?.error?.description}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig);
          return of(null)
        })
    ).subscribe(m => this.updateForm(m))
  }

  /**
   * Downloads the current {@link EntityMapping} as a file.
   */
  public download() {
    this.mappingId$.pipe(
        mergeMap(id => this.service.getEntityMapping(id)),
        catchError((err) => {
          this.snackBar.open(`Error occurred while trying to load entity mapping: ${err?.error?.description}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig);
          return of(null)
        })
    ).subscribe(data => {
      if (data != null) {
        const fileName = 'mapping.json';
        const fileToSave = new Blob([JSON.stringify(data, null, 2)], {type: 'application/json'});

        /* Create download */
        const a = document.createElement("a");
        a.href = URL.createObjectURL(fileToSave);
        a.download = fileName;
        a.click();
      }
    })
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
  private formToEntityMapping(id: number): EntityMapping {
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