import {AfterViewInit, Component, ElementRef, ViewChild} from "@angular/core";
import {
  ApacheSolrCollection,
  CollectionService,
  ConfigService,
  Institution,
  ObjectCollection
} from "../../../../openapi";
import {map, Observable, shareReplay, tap} from "rxjs";
import {MatPaginator} from "@angular/material/paginator";
import {CollectionDatasource} from "./collection-datasource";
import {MatSnackBar, MatSnackBarConfig} from "@angular/material/snack-bar";
import {MatDialog} from "@angular/material/dialog";
import {CollectionDialogComponent} from "./collection-dialog.component";

@Component({
    selector: 'kiar-collection-list',
    templateUrl: './collection-list.component.html',
    styleUrls: ['./collection-list.component.scss'],
    standalone: false
})
export class CollectionListComponent implements AfterViewInit  {

  /** {@link Observable} of all available participants. */
  public readonly dataSource: CollectionDatasource

  /** An {@link Observable} of available participants. */
  public readonly solrCollections: Observable<Array<ApacheSolrCollection>>

  /** The columns that should be displayed in the data table. */
  public readonly displayedColumns: string[] = ['image', 'name', 'displayName', 'institutionName', 'publish', 'action'];

  /** Reference to the {@link MatPaginator}*/
  @ViewChild(MatPaginator) paginator: MatPaginator;

  /** Reference to the filter field. */
  @ViewChild('filterField') filterField: ElementRef;


  constructor(private collectionService: CollectionService, private configService: ConfigService, private dialog: MatDialog, private snackBar: MatSnackBar) {
    this.dataSource = new CollectionDatasource(this.collectionService)
    this.solrCollections = this.configService.getListSolrCollections().pipe(
        map((collections) => {
          return collections.filter(c => c.type === "COLLECTION")
        }),
        shareReplay(1)
    )
  }

  /**
   * Registers an observable for page change.
   */
  public ngAfterViewInit() {
    this.dataSource.load(0, 15);
    this.paginator.page.pipe(tap(() => this.dataSource.load(this.paginator.pageIndex, this.paginator.pageSize, this.filterField.nativeElement.value))).subscribe();
  }

  /**
   * Opens a dialog to add a new {@link Institution} to the collection and persists it through the API upon saving.
   */
  public add() {
    this.dialog.open(CollectionDialogComponent).afterClosed().subscribe(ret => {
      if (ret != null) {
        this.dataSource.load(this.paginator.pageIndex, this.paginator.pageSize, this.filterField.nativeElement.value);
      }
    })
  }

  /**
   * Opens a dialog to edit an existing {@link Institution} to the collection and persists it through the API upon saving.
   */
  public edit(collection: ObjectCollection) {
    this.dialog.open(CollectionDialogComponent, {data: collection.id}).afterClosed().subscribe(ret => {
      if (ret != null) {
        this.dataSource.load(this.paginator.pageIndex, this.paginator.pageSize, this.filterField.nativeElement.value);
      }
    })
  }

  /**
   * Deletes the given {@link ObjectCollection} from the backend.
   */
  public delete(collection: ObjectCollection) {
    if (confirm(`Are you sure that you want to delete institution '${collection.id}'?\nAfter deletion, it can no longer be retrieved.`)) {
      this.collectionService.deleteCollection(collection.id!!).subscribe({
        next: (value) => {
          this.snackBar.open(value.description, "Dismiss", { duration: 2000 } as MatSnackBarConfig);
          this.dataSource.load(this.paginator.pageIndex, this.paginator.pageSize, this.filterField.nativeElement.value);
        },
        error: (err) => this.snackBar.open(`Error occurred while trying to delete institution '${collection.name}': ${err?.error?.description}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig),
      })
    }
  }

  /**
   * Filters the data table based on the user input.
   */
  public onFilterChange() {
    this.dataSource.load(this.paginator.pageIndex, this.paginator.pageSize, this.filterField.nativeElement.value)
  }

  /**
   * Uses the API to trigger synchronisation of institution master data with the Apache Solt backend.
   *
   * @param collection The {@link ApacheSolrCollection} to synchronise with.
   */
  public synchronize(collection: ApacheSolrCollection) {
    if (!collection.id) return;
    this.collectionService.postSynchronizeCollections(collection.id).subscribe({
      next: (value) =>  this.snackBar.open(`Successfully synchronised collections with Apache Solr backend (${collection.name}).`, "Dismiss", { duration: 2000 } as MatSnackBarConfig),
      error: (err) => this.snackBar.open(`Error occurred while synchronising collections with Apache Solr backend (${collection.name}): ${err?.error?.description}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig),
    })
  }
}