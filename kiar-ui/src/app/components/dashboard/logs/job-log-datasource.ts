import {signal} from "@angular/core";
import {JobLog, JobService} from "../../../../../openapi";

/**
 * Holds a page of {@link JobLog} objects loaded through the backend API, exposed as signals.
 *
 * Bind {@link data} to a table's `[dataSource]` and {@link total} to a paginator's `[length]`.
 */
export class JobLogDatasource {
  /** The currently loaded page of {@link JobLog} objects. */
  public readonly data = signal<Array<JobLog>>([])

  /** The total number of {@link JobLog} objects available on the server. */
  public readonly total = signal(0)

  constructor(private service: JobService, private jobId: number) {}

  /**
   * Reloads the data using the provided paging parameters.
   * @param page The requested page index.
   * @param pageSize The requested page size.
   * @param level The log level to filter for.
   * @param context The context to filter for.
   */
  public load(page: number, pageSize: number, level: string | undefined, context: string | undefined) {
    this.service.getJobLog(this.jobId, page, pageSize, level, context).subscribe(next => {
      this.total.set(next.total)
      this.data.set(next.results)
    })
  }
}
