import {Job, JobLog, JobService} from "../../../../openapi";
import {CollectionViewer, DataSource} from "@angular/cdk/collections";
import {BehaviorSubject, Observable} from "rxjs";

/**
 * A {@link DataSource} for {@link Job} object loaded through the backend API.
 *
 * Can be used as a data source for table.
 */
export class JobHistoryDatasource implements DataSource<Job> {

  /** The {@link BehaviorSubject} that acts as a data source. */
  private data = new BehaviorSubject<Array<Job>>([])

  /** The {@link BehaviorSubject} that acts as a data source. */
  private total = new BehaviorSubject<number>(0)

  constructor(private service: JobService) {
  }

  public connect(collectionViewer: CollectionViewer): Observable<Job[]> {
    return this.data.asObservable();
  }

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
   * Reloads the data using the provided page index and page size.
   * @param page The requested page index.
   * @param pageSize The requested page size.
   */
  public load(page: number, pageSize: number) {
    this.service.getInactiveJobs(page, pageSize).subscribe(
        (next) => {
          this.total.next(next.total)
          this.data.next(next.results)
        }
    )
  }
}