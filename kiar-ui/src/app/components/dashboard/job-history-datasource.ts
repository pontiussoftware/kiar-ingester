import {signal} from "@angular/core";
import {Job, JobService} from "../../../../openapi";

/**
 * Holds a page of {@link Job} objects loaded through the backend API, exposed as signals.
 *
 * Bind {@link data} to a table's `[dataSource]` and {@link total} to a paginator's `[length]`.
 */
export class JobHistoryDatasource {
  /** The currently loaded page of {@link Job} objects. */
  public readonly data = signal<Array<Job>>([])

  /** The total number of {@link Job} objects available on the server. */
  public readonly total = signal(0)

  constructor(private service: JobService) {}

  /**
   * Reloads the data using the provided paging parameters.
   * @param page The requested page index.
   * @param pageSize The requested page size.
   */
  public load(page: number, pageSize: number) {
    this.service.getInactiveJobs(page, pageSize).subscribe(next => {
      this.total.set(next.total)
      this.data.set(next.results)
    })
  }
}
