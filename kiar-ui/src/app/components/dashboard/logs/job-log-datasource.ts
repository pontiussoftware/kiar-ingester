import {CollectionViewer, DataSource} from "@angular/cdk/collections";
import {JobLog, JobService} from "../../../../../openapi";
import {BehaviorSubject, Observable} from "rxjs";

/**
 * A {@link DataSource} for {@link JobLog} object loaded through the backend API.
 *
 * Can be used as a data source for table.
 */
export class JobLogDatasource implements DataSource<JobLog> {

  /** The {@link BehaviorSubject} that acts as a data source. */
  private data = new BehaviorSubject<Array<JobLog>>([])

  /** The {@link BehaviorSubject} that acts as a data source. */
  private total = new BehaviorSubject<number>(0)

  constructor(private service: JobService, private jobId: number) {
  }

  connect(collectionViewer: CollectionViewer): Observable<JobLog[]> {
    return this.data.asObservable();
  }

  disconnect(collectionViewer: CollectionViewer): void {
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
   * @param level The log level to filter for.
   * @param context The context to filter for.
   */
  public load(page: number, pageSize: number, level: string | undefined, context: string | undefined) {
    this.service.getJobLog(this.jobId, page, pageSize, level, context).subscribe(
        (next) => {
          this.total.next(next.total)
          this.data.next(next.results)
        }
    )
  }
}