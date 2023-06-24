import {Component, ViewChild} from "@angular/core";
import {AttributeMapping, EntityMapping, EntityMappingService, MappingType, ValueParser} from "../../../../../openapi";
import {ActivatedRoute, Router} from "@angular/router";
import {catchError, map, mergeMap, Observable, of} from "rxjs";
import {MatSnackBar, MatSnackBarConfig} from "@angular/material/snack-bar";
import {FormArray, FormControl, FormGroup, Validators} from "@angular/forms";
import {MatTable, MatTableDataSource} from "@angular/material/table";
import {MatDialog} from "@angular/material/dialog";
import {AttributeMappingData, AttributeMappingDialogComponent} from "./attribute-mapping-dialog.component";

@Component({
  selector: 'kiar-admin-dashboard',
  templateUrl: './entity-mapping.component.html',
  styleUrls: ['./entity-mapping.component.scss']
})
export class EntityMappingComponent {

  /** An {@link Observable} of the mapping ID that is being inspected by this {@link EntityMappingComponent}. */
  public mappingId: Observable<string>

  /** The list of columns displayed by the table. */
  public columns = ['source', 'destination', 'parser', 'required', 'multiValued', 'parameters', 'action']

  /** List of attribute {@link FormGroup}s. */
  public attributes: Array<FormGroup> = []

  /** A {@link MatTableDataSource} for the table. */
  public tableDataSource = new MatTableDataSource(this.attributes)

  /** The {@link FormControl} that backs this {@link EntityMappingComponent}. */
  public formControl = new FormGroup({
    name: new FormControl('', [Validators.required]),
    description: new FormControl(''),
    type: new FormControl('', [Validators.required]),
    attributes: new FormArray(this.attributes)
  })

  /** */
  @ViewChild(MatTable) table: MatTable<FormGroup>;

  constructor(
      private service: EntityMappingService,
      private router: Router,
      private route: ActivatedRoute,
      private snackBar: MatSnackBar,
      public dialog: MatDialog
  ) {
    this.mappingId = this.route.paramMap.pipe(
        map(params => params.get('id')!!)
    );
    this.refresh()

  }

  /**
   * Opens a {@link AttributeMappingDialogComponent} to add an existing  {@link AttributeMapping}.
   */
  public addAttributeMapping() {
    let mapping = {form: this.newAttributeMappingFormGroup(null), new: true} as AttributeMappingData
    this.dialog.open(AttributeMappingDialogComponent, {data: mapping, width: '750px'}).afterClosed().subscribe(data => {
      if (data != null && data.new) {
        (this.formControl.get('attributes') as FormArray<FormGroup>).push(data.form)
        this.table.renderRows()
      }
    })
  }

  /**
   * Opens a {@link AttributeMappingDialogComponent} to edit an existing {@link AttributeMapping}.
   *
   * @param index The index of the {@link AttributeMapping} to edit.
   */
  public editAttributeMapping(index: number) {
    this.dialog.open(AttributeMappingDialogComponent, {
      data: {form: this.attributes[index], new: false} as AttributeMappingData,
      width: '750px'
    })
  }

  /**
   * Removes an existing {@link AttributeMapping}.
   *
   * @param index The index of the {@link AttributeMapping} to remove.
   */
  public removeAttribute(index: number) {
    (this.formControl.get('attributes') as FormArray<FormGroup>).removeAt(index)
    this.table.renderRows()
  }

  /**
   * Tries to save the current state of the {@link EntityMapping} represented by the local {@link FormControl}
   */
  public save() {
    this.mappingId.pipe(
        mergeMap(s => {
          let mapping = this.fromToEntityMapping(s)
          return this.service.updateEntityMapping(s, mapping).pipe(
              catchError((err) => {
                this.snackBar.open(`Error occurred while trying to update entity mapping: ${err?.error?.description}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig);
                return of(null)
              })
          )
        })
    ).subscribe(m => {
      this.snackBar.open(`Successfully saved updated entity mapping.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig);
      this.updateForm(m)
    })
  }

  /**
   * Opens a {@link AttributeMappingDialogComponent} to add an existing  {@link AttributeMapping}.
   */
  public delete() {
    if (confirm("Are you sure that you want to delete this entity mapping?\nAfter deletion, it can no longer be retrieved.")) {
      this.mappingId.pipe(
          mergeMap(s => {
            return this.service.deleteEntityMapping(s).pipe(
                catchError((err) => {
                  this.snackBar.open(`Error occurred while trying to delete entity mapping: ${err?.error?.description}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig);
                  return of(null)
                })
            )
          })
      ).subscribe(m => {
        this.snackBar.open(`Successfully deleted entity mapping.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig);
        this.router.navigate(['admin', 'dashboard']).then(r => {})
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
    this.attributes.length = 0
    for (let a of (mapping?.attributes || [])) {
      this.attributes.push(this.newAttributeMappingFormGroup(a))
    }

    /* Re-render table. */
    this.table.renderRows();
  }

  /**
   * Converts this {@link FormGroup} to an {@link EntityMapping}.
   *
   * @param id The ID of the {@link EntityMapping}
   * @return {@link EntityMapping}
   */
  private fromToEntityMapping(id: string): EntityMapping {
    return {
      id: id,
      name: this.formControl.get('name')?.value,
      description: this.formControl.get('description')?.value,
      type: this.formControl.get('type')?.value as MappingType,
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