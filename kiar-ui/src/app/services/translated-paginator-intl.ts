import {inject, Injectable} from "@angular/core";
import {MatPaginatorIntl} from "@angular/material/paginator";
import {TranslateService} from "@ngx-translate/core";

/**
 * A {@link MatPaginatorIntl} whose labels come from the translation files and follow language changes.
 */
@Injectable()
export class TranslatedPaginatorIntl extends MatPaginatorIntl {
  /** The {@link TranslateService} used to resolve the paginator labels. */
  private translate = inject(TranslateService);

  constructor() {
    super();
    this.translate.onLangChange.subscribe(() => this.update());
    this.update();
  }

  /**
   * Re-reads all labels from the current translation and notifies the paginators.
   */
  private update() {
    this.itemsPerPageLabel = this.translate.instant('paginator.itemsPerPage');
    this.nextPageLabel = this.translate.instant('paginator.nextPage');
    this.previousPageLabel = this.translate.instant('paginator.previousPage');
    this.firstPageLabel = this.translate.instant('paginator.firstPage');
    this.lastPageLabel = this.translate.instant('paginator.lastPage');
    this.getRangeLabel = (page: number, pageSize: number, length: number) => {
      if (length === 0 || pageSize === 0) {
        return this.translate.instant('paginator.rangeEmpty', {length});
      }
      const start = page * pageSize;
      const end = Math.min(start + pageSize, length);
      return this.translate.instant('paginator.range', {start: start + 1, end, length});
    };
    this.changes.next();
  }
}
