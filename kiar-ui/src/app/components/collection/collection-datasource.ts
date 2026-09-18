import {signal} from "@angular/core";
import {CollectionService, ObjectCollection} from "../../../../openapi";

/**
 * Holds a page of {@link ObjectCollection} objects loaded through the backend API, exposed as signals.
 *
 * Bind {@link data} to a table's `[dataSource]` and {@link total} to a paginator's `[length]`.
 */
export class CollectionDatasource {
  /** The currently loaded page of {@link ObjectCollection} objects. */
  public readonly data = signal<Array<ObjectCollection>>([])

  /** The total number of {@link ObjectCollection} objects available on the server. */
  public readonly total = signal(0)

  constructor(private service: CollectionService) {}

  /**
   * Reloads the data using the provided paging parameters.
   * @param page
   * @param pageSize
   * @param filter
   */
  public load(page: number, pageSize: number, filter: string | undefined = undefined) {
    this.service.getCollections(filter, page, pageSize).subscribe(next => {
      this.total.set(next.total)
      this.data.set(next.results)
    })
  }
}
