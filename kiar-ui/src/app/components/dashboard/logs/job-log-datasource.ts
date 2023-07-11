import {CollectionViewer, DataSource} from "@angular/cdk/collections";
import {JobLog, JobService} from "../../../../../openapi";
import {BehaviorSubject, Observable} from "rxjs";

export class JobLogDatasource implements DataSource<JobLog> {

  /** The {@link BehaviorSubject} that acts as a data source. */
  private data = new BehaviorSubject<Array<JobLog>>([])

  /** The {@link BehaviorSubject} that acts as a data source. */
  private total = new BehaviorSubject<number>(0)

  constructor(private service: JobService, private jobId: string) {
  }

  connect(collectionViewer: CollectionViewer): Observable<JobLog[]> {
    return this.data.asObservable();
  }

  disconnect(collectionViewer: CollectionViewer): void {
    this.data.complete()
  }

  /**
   * Reloads the data using the provided page index and page size.
   * @param page The requested page index.
   * @param pageSize The requested page size.
   */
  public load(page: number, pageSize: number) {
    this.service.getJobLog(this.jobId, page, pageSize).subscribe(
        (next) => {
          this.total.next(next.total)
          this.data.next(next.logs)
        }
    )
  }
}