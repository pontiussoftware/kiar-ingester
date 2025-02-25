import {Component, Input, OnInit} from "@angular/core";
import {CollectionService} from "../../../../openapi";
import { catchError, of } from "rxjs";

@Component({
  selector: 'kiar-collection-image',
  templateUrl: 'collection-image.component.html',
  styleUrls: ['collection-image.component.scss']
})
export class CollectionImageComponent implements OnInit {
  /** The ID to fetch image for. */
  @Input() collectionId: string;

  /** The name of the image. */
  @Input() name: string;

  /** The edit status of the image. */
  @Input() edit: boolean = false;

  /** The width of th image. */
  @Input() width: number = 100;

  /** The height of th image. */
  @Input() height: number = 100;

  /** The generate image URL. */
  public imageUrl: string | null = null;

  /** The overlay state. */
  public showOverlay: boolean = false;

  constructor(private collectionService: CollectionService) { }

  /**
   * Loads the image and displays it.
   */
  public ngOnInit() {
    this.collectionService.getCollectionImage(this.collectionId, this.name).pipe(
        catchError(err => {
          console.log('Failed to load image.', err)
          return of(null)
        })
    ).subscribe({
      next: (imageData) => {
        if (imageData) {
          this.imageUrl = URL.createObjectURL(imageData)
        }
      }
    });
  }

  /**
   * Deletes the image.
   */
  public delete() {
    this.collectionService.deleteCollectionImage(this.collectionId, this.name).pipe(
        catchError(err => {
          console.log('Failed to delete image', err)
          return of(null)
        })
    ).subscribe({
      next: () => {
        this.imageUrl = null;
      }
    })
  }
}