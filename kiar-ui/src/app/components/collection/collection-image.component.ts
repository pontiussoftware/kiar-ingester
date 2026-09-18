import {Component, computed, inject, input, linkedSignal, signal} from "@angular/core";
import {TranslatePipe} from "@ngx-translate/core";
import {toObservable, toSignal} from "@angular/core/rxjs-interop";
import {CollectionService} from "../../../../openapi";
import {catchError, map, of, switchMap} from "rxjs";
import {MatIcon} from "@angular/material/icon";

@Component({
    selector: 'kiar-collection-image',
    templateUrl: 'collection-image.component.html',
    styleUrls: ['collection-image.component.scss'],
    imports: [MatIcon, TranslatePipe]
})
export class CollectionImageComponent {
  /** The {@link CollectionService} used to access collection data. */
  private collectionService = inject(CollectionService);

  /** The ID to fetch image for. */
  readonly collectionId = input.required<number>();

  /** The name of the image. */
  readonly name = input.required<string>();

  /** The edit status of the image. */
  readonly edit = input<boolean>(false);

  /** The width of th image. */
  readonly width = input<number>(100);

  /** The height of th image. */
  readonly height = input<number>(100);

  /** The overlay state. */
  public readonly showOverlay = signal(false);

  /** The image URL as loaded from the backend; re-fetched whenever {@link collectionId} or {@link name} changes. */
  private readonly loadedUrl = toSignal(
      toObservable(computed(() => ({id: this.collectionId(), name: this.name()}))).pipe(
          switchMap(({id, name}) => this.collectionService.getCollectionImage(id, name).pipe(
              catchError(err => {
                console.log('Failed to load image.', err)
                return of(null)
              })
          )),
          map(imageData => imageData ? URL.createObjectURL(imageData) : null)
      ),
      {initialValue: null}
  );

  /** The displayed image URL. Follows {@link loadedUrl} but can be cleared locally after a delete. */
  public readonly imageUrl = linkedSignal(() => this.loadedUrl());

  /**
   * Deletes the image.
   */
  public delete() {
    this.collectionService.deleteCollectionImage(this.collectionId(), this.name()).pipe(
        catchError(err => {
          console.log('Failed to delete image', err)
          return of(null)
        })
    ).subscribe({
      next: () => {
        this.imageUrl.set(null);
      }
    })
  }
}
