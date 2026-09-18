import {AfterViewInit, Component, ElementRef, inject, viewChild} from "@angular/core";
import {toSignal} from "@angular/core/rxjs-interop";
import {ApacheSolrCollection, ConfigService, Institution, InstitutionService} from "../../../../openapi";
import {map, Observable, tap} from "rxjs";
import {MatPaginator} from "@angular/material/paginator";
import {InstitutionDatasource} from "./institution-datasource";
import {MatSort} from "@angular/material/sort";
import {MatSnackBar, MatSnackBarConfig} from "@angular/material/snack-bar";
import {MatDialog} from "@angular/material/dialog";
import {InstitutionDialogComponent} from "./institution-dialog.component";

@Component({
    selector: 'kiar-institution-list',
    templateUrl: './institution-list.component.html',
    styleUrls: ['./institution-list.component.scss'],
    standalone: false
})
export class InstitutionListComponent implements AfterViewInit  {
  /** The {@link InstitutionService} used to access institution data. */
  private institutionService = inject(InstitutionService);

  /** The {@link ConfigService} used to access application configuration (templates, mappings, Solr configurations, participants). */
  private configService = inject(ConfigService);

  /** The {@link MatDialog} service used to open dialogs. */
  private dialog = inject(MatDialog);

  /** The {@link MatSnackBar} used to display notifications. */
  private snackBar = inject(MatSnackBar);

  /** {@link Observable} of all available participants. */
  public readonly dataSource: InstitutionDatasource

  /** A signal of the available {@link ApacheSolrCollection}s. */
  public readonly collections = toSignal(this.configService.getListSolrCollections().pipe(
        map((collections) => {
          return collections.filter(c => c.type === "MUSEUM")
        })), {initialValue: [] as Array<ApacheSolrCollection>})
  /** The columns that should be displayed in the data table. */
  public readonly displayedColumns: string[] = ['image', 'name', 'displayName', 'participant', 'street', 'city', 'zip', 'canton', 'email', 'publish', 'action'];

  /** Reference to the {@link MatPaginator}*/
  protected readonly paginator = viewChild.required(MatPaginator);

  /** Reference to the {@link MatSort}*/
  protected readonly sort = viewChild.required(MatSort);

  /** Reference to the filter field. */
  protected readonly filterField = viewChild.required<ElementRef>('filterField');

  constructor() {
    this.dataSource = new InstitutionDatasource(this.institutionService)
  }

  /**
   * Registers an observable for page change.
   */
  public ngAfterViewInit() {
    const sort = this.sort()
    sort.direction = 'asc'
    this.dataSource.load(0, 15, sort.active, sort.direction);
    this.paginator().page.pipe(tap(() => this.dataSource.load(this.paginator().pageIndex, this.paginator().pageSize, this.sort().active, this.sort().direction, this.filterField().nativeElement.value))).subscribe();
    sort.sortChange.pipe(tap(() => this.dataSource.load(this.paginator().pageIndex, this.paginator().pageSize, this.sort().active, this.sort().direction, this.filterField().nativeElement.value))).subscribe();
  }

  /**
   * Opens a dialog to add a new {@link Institution} to the collection and persists it through the API upon saving.
   */
  public add() {
    this.dialog.open(InstitutionDialogComponent).afterClosed().subscribe(ret => {
      if (ret != null) {
        this.dataSource.load(this.paginator().pageIndex, this.paginator().pageSize, this.sort().active, this.sort().direction, this.filterField().nativeElement.value);
      }
    })
  }

  /**
   * Opens a dialog to edit an existing {@link Institution} to the collection and persists it through the API upon saving.
   */
  public edit(institution: Institution) {
    this.dialog.open(InstitutionDialogComponent, {data: institution.id}).afterClosed().subscribe(ret => {
      if (ret != null) {
        this.dataSource.load(this.paginator().pageIndex, this.paginator().pageSize, this.sort().active, this.sort().direction, this.filterField().nativeElement.value);
      }
    })
  }

  /**
   * Opens a dialog to add a new {@link Institution} to the collection and persists it through the API upon saving.
   */
  public delete(institution: Institution) {
    if (confirm(`Are you sure that you want to delete institution '${institution.id}'?\nAfter deletion, it can no longer be retrieved.`)) {
      this.institutionService.deleteInstitution(institution.id!!).subscribe({
        next: (value) => {
          this.snackBar.open(value.description, "Dismiss", { duration: 2000 } as MatSnackBarConfig);
          this.dataSource.load(this.paginator().pageIndex, this.paginator().pageSize, this.sort().active, this.sort().direction, this.filterField().nativeElement.value);
        },
        error: (err) => this.snackBar.open(`Error occurred while trying to delete institution '${institution.name}': ${err?.error?.description}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig),
      })
    }
  }

  /**
   * Filters the data table based on the user input.
   */
  public onFilterChange() {
    this.dataSource.load(this.paginator().pageIndex, this.paginator().pageSize, this.sort().active, this.sort().direction, this.filterField().nativeElement.value)
  }

  /**
   * Uses the API to trigger synchronisation of institution master data with the Apache Solt backend.
   *
   * @param collection The name of the collection to use.
   */
  public synchronize(collection: ApacheSolrCollection) {
    if (!collection.id) return;
    this.institutionService.postSynchronizeInstitutions(collection.id).subscribe({
      next: (value) =>  this.snackBar.open(`Successfully synchronised institutions with Apache Solr backend (${collection.name}).`, "Dismiss", { duration: 2000 } as MatSnackBarConfig),
      error: (err) => this.snackBar.open(`Error occurred while synchronising institutions with Apache Solr backend (${collection.name}): ${err?.error?.description}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig),
    })
  }
}