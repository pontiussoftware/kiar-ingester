import {signal} from "@angular/core";
import {Institution, InstitutionService} from "../../../../openapi";

/**
 * Holds a page of {@link Institution} objects loaded through the backend API, exposed as signals.
 *
 * Bind {@link data} to a table's `[dataSource]` and {@link total} to a paginator's `[length]`.
 */
export class InstitutionDatasource {
  /** The currently loaded page of {@link Institution} objects. */
  public readonly data = signal<Array<Institution>>([])

  /** The total number of {@link Institution} objects available on the server. */
  public readonly total = signal(0)

  constructor(private service: InstitutionService) {}

  /**
   * Reloads the data using the provided paging parameters.
   *
   * @param page The requested page index.
   * @param pageSize The requested page size.
   * @param order The field / attribute to order by.
   * @param orderDir The sort order direction.
   * @param filter The filter to apply.
   */
  public load(page: number, pageSize: number, order: string, orderDir: string = 'asc', filter: string | undefined = undefined) {
    this.service.getInstitutions(page, pageSize, order, orderDir, filter).subscribe(next => {
      this.total.set(next.total)
      this.data.set(next.results)
    })
  }
}
