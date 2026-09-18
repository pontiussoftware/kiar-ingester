import {AfterViewInit, Component, ElementRef, inject, viewChild} from "@angular/core";
import {TranslatePipe, TranslateService} from "@ngx-translate/core";
import {toSignal} from "@angular/core/rxjs-interop";
import {
  ApacheSolrCollection,
  CollectionService,
  ConfigService,
  Institution,
  ObjectCollection
} from "../../../../openapi";
import {map, Observable, tap} from "rxjs";
import {MatPaginator} from "@angular/material/paginator";
import {CollectionDatasource} from "./collection-datasource";
import {MatSnackBar, MatSnackBarConfig} from "@angular/material/snack-bar";
import {MatDialog} from "@angular/material/dialog";
import {CollectionDialogComponent} from "./collection-dialog.component";
import {MatIconButton, MatMiniFabButton} from "@angular/material/button";
import {MatTooltip} from "@angular/material/tooltip";
import {MatIcon} from "@angular/material/icon";
import {MatMenu, MatMenuItem, MatMenuTrigger} from "@angular/material/menu";
import {MatFormField, MatInput, MatLabel} from "@angular/material/input";
import {
  MatCell,
  MatCellDef,
  MatColumnDef,
  MatHeaderCell,
  MatHeaderCellDef,
  MatHeaderRow,
  MatHeaderRowDef,
  MatRow,
  MatRowDef,
  MatTable
} from "@angular/material/table";
import {MatSort, MatSortHeader} from "@angular/material/sort";
import {CollectionImageComponent} from "./collection-image.component";

@Component({
    selector: 'kiar-collection-list',
    templateUrl: './collection-list.component.html',
    styleUrls: ['./collection-list.component.scss'],
    imports: [MatMiniFabButton, MatTooltip, MatIcon, MatMenuTrigger, MatMenu, MatMenuItem, MatFormField, MatLabel, MatInput, MatTable, MatSort, MatColumnDef, MatHeaderCellDef, MatHeaderCell, MatCellDef, MatCell, CollectionImageComponent, MatSortHeader, MatIconButton, MatHeaderRowDef, MatHeaderRow, MatRowDef, MatRow, MatPaginator, TranslatePipe]
})
export class CollectionListComponent implements AfterViewInit  {
  /** The {@link CollectionService} used to access collection data. */
  private collectionService = inject(CollectionService);

  /** The {@link ConfigService} used to access application configuration (templates, mappings, Solr configurations, participants). */
  private configService = inject(ConfigService);

  /** The {@link MatDialog} service used to open dialogs. */
  private dialog = inject(MatDialog);

  /** The {@link MatSnackBar} used to display notifications. */
  private snackBar = inject(MatSnackBar);

  /** The {@link TranslateService} used to resolve user-facing messages. */
  private translate = inject(TranslateService);

  /** {@link Observable} of all available participants. */
  public readonly dataSource: CollectionDatasource

  /** A signal of the available {@link ApacheSolrCollection}s. */
  public readonly solrCollections = toSignal(this.configService.getListSolrCollections().pipe(
        map((collections) => {
          return collections.filter(c => c.type === "COLLECTION")
        })), {initialValue: [] as Array<ApacheSolrCollection>})
  /** The columns that should be displayed in the data table. */
  public readonly displayedColumns: string[] = ['image', 'name', 'displayName', 'institutionName', 'publish', 'action'];

  /** Reference to the {@link MatPaginator}*/
  readonly paginator = viewChild.required(MatPaginator);

  /** Reference to the filter field. */
  readonly filterField = viewChild.required<ElementRef>('filterField');

  constructor() {
    this.dataSource = new CollectionDatasource(this.collectionService)
  }

  /**
   * Registers an observable for page change.
   */
  public ngAfterViewInit() {
    this.dataSource.load(0, 15);
    this.paginator().page.pipe(tap(() => this.dataSource.load(this.paginator().pageIndex, this.paginator().pageSize, this.filterField().nativeElement.value))).subscribe();
  }

  /**
   * Opens a dialog to add a new {@link Institution} to the collection and persists it through the API upon saving.
   */
  public add() {
    this.dialog.open(CollectionDialogComponent).afterClosed().subscribe(ret => {
      if (ret != null) {
        this.dataSource.load(this.paginator().pageIndex, this.paginator().pageSize, this.filterField().nativeElement.value);
      }
    })
  }

  /**
   * Opens a dialog to edit an existing {@link Institution} to the collection and persists it through the API upon saving.
   */
  public edit(collection: ObjectCollection) {
    this.dialog.open(CollectionDialogComponent, {data: collection.id}).afterClosed().subscribe(ret => {
      if (ret != null) {
        this.dataSource.load(this.paginator().pageIndex, this.paginator().pageSize, this.filterField().nativeElement.value);
      }
    })
  }

  /**
   * Deletes the given {@link ObjectCollection} from the backend.
   */
  public delete(collection: ObjectCollection) {
    if (confirm(this.translate.instant('collection.list.confirmDelete', {id: collection.id}) + '\n' + this.translate.instant('common.confirmDeleteSuffix'))) {
      this.collectionService.deleteCollection(collection.id!!).subscribe({
        next: (value) => {
          this.snackBar.open(value.description, this.translate.instant('common.action.dismiss'), { duration: 2000 } as MatSnackBarConfig);
          this.dataSource.load(this.paginator().pageIndex, this.paginator().pageSize, this.filterField().nativeElement.value);
        },
        error: (err) => this.snackBar.open(this.translate.instant('collection.list.messages.deleteError', {name: collection.name, error: err?.error?.description}), this.translate.instant('common.action.dismiss'), { duration: 2000 } as MatSnackBarConfig),
      })
    }
  }

  /**
   * Filters the data table based on the user input.
   */
  public onFilterChange() {
    this.dataSource.load(this.paginator().pageIndex, this.paginator().pageSize, this.filterField().nativeElement.value)
  }

  /**
   * Uses the API to trigger synchronisation of institution master data with the Apache Solt backend.
   *
   * @param collection The {@link ApacheSolrCollection} to synchronise with.
   */
  public synchronize(collection: ApacheSolrCollection) {
    if (!collection.id) return;
    this.collectionService.postSynchronizeCollections(collection.id).subscribe({
      next: (value) =>  this.snackBar.open(this.translate.instant('collection.list.messages.synchronized', {name: collection.name}), this.translate.instant('common.action.dismiss'), { duration: 2000 } as MatSnackBarConfig),
      error: (err) => this.snackBar.open(this.translate.instant('collection.list.messages.synchronizeError', {name: collection.name, error: err?.error?.description}), this.translate.instant('common.action.dismiss'), { duration: 2000 } as MatSnackBarConfig),
    })
  }
}