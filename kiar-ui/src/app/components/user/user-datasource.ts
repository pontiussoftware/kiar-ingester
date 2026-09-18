import {signal} from "@angular/core";
import {User, UserService} from "../../../../openapi";

/**
 * Holds a page of {@link User} objects loaded through the backend API, exposed as signals.
 *
 * Bind {@link data} to a table's `[dataSource]` and {@link total} to a paginator's `[length]`.
 */
export class UserDataSource {
  /** The currently loaded page of {@link User} objects. */
  public readonly data = signal<Array<User>>([])

  /** The total number of {@link User} objects available on the server. */
  public readonly total = signal(0)

  constructor(private service: UserService) {}

  /**
   * Reloads the data using the provided paging parameters.
   * @param page
   * @param pageSize
   * @param order
   * @param orderDir
   */
  public load(page: number, pageSize: number, order: string, orderDir: string = 'asc') {
    this.service.getUsers(page, pageSize, order, orderDir).subscribe(next => {
      this.total.set(next.total)
      this.data.set(next.results)
    })
  }
}
