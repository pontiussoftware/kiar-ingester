import {CollectionViewer, DataSource} from "@angular/cdk/collections";
import {Institution, InstitutionService} from "../../../../openapi";
import {BehaviorSubject, catchError, map, Observable, of} from "rxjs";

/**
 * 
 */
interface InstitutionWithImage extends Institution {
  image: Observable<string | null>
}

/**
 * A {@link DataSource} for {@link Institution} object loaded through the backend API.
 *
 * Can be used as a data source for table.
 */
export class InstitutionDatasource implements DataSource<Institution> {
  /** The {@link BehaviorSubject} that acts as a data source. */
  private data = new BehaviorSubject<Institution[]>([])

  /** The {@link BehaviorSubject} that acts as a data source. */
  private total = new BehaviorSubject<number>(0)

  constructor(private service: InstitutionService) {
  }

  /**
   * Connects this {@link DataSource} to a {@link CollectionViewer}.
   *
   * @param collectionViewer
   */
  public connect(collectionViewer: CollectionViewer): Observable<Institution[]> {
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
   * @param order The field / attribute to order by.
   * @param orderDir The sort order direction.
   */
  public load(page: number, pageSize: number, order: string, orderDir: string = 'asc') {
    this.service.getInstitutions(page, pageSize, order, orderDir).subscribe(
        (next) => {
          this.total.next(next.total)
          const institutions = next.results.map(institution => {
             ((institution as any)['image'] = this.service.getImage(institution.id!!).pipe(
                 map(image => URL.createObjectURL(image)),
                 catchError(() => of(null))
             ))
             return institution as InstitutionWithImage
          })
          this.data.next(institutions)
        }
    )
  }
}