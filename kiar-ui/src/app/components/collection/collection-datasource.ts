import {CollectionViewer, DataSource} from "@angular/cdk/collections";
import {CollectionService, Institution, InstitutionService, ObjectCollection} from "../../../../openapi";
import {BehaviorSubject, catchError, map, Observable, of} from "rxjs";

/**
 * A {@link DataSource} for {@link ObjectCollection} object loaded through the backend API.
 *
 * Can be used as a data source for table.
 */
export class CollectionDatasource implements DataSource<ObjectCollection> {
  /** The {@link BehaviorSubject} that acts as a data source. */
  private data = new BehaviorSubject<ObjectCollection[]>([])

  /** The {@link BehaviorSubject} that acts as a data source. */
  private total = new BehaviorSubject<number>(0)

  constructor(private service: CollectionService) {}

  /**
   * Connects this {@link DataSource} to a {@link CollectionViewer}.
   *
   * @param collectionViewer
   */
  public connect(collectionViewer: CollectionViewer): Observable<ObjectCollection[]> {
    return this.data.asObservable();
  }

  /**
   * Disconnects this {@link DataSource} from a {@link CollectionViewer}.
   *
   * @param collectionViewer
   */
  public disconnect(collectionViewer: CollectionViewer): void {
    this.data.complete()
  }

  /**
   * Returns the total size for this {@link JobLogDatasource} as {@link Observable}.
   */
  get totalSize(): number {
    return this.total.value
  }

  /**
   * Reloads the data using the provided page index and page size, order field and order direction
   * @param page The requested page index.
   * @param pageSize The requested page size.
   * @param filter The filter to apply.
   */
  public load(page: number, pageSize: number, filter: string | undefined = undefined) {
    this.service.getCollections(filter, page, pageSize).subscribe(
        (next) => {
          this.total.next(next.total)
          this.data.next(next.results)
        }
    )
  }
}