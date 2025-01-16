import {Component, Input, OnInit} from "@angular/core";
import {CollectionService} from "../../../../openapi";
import { catchError, of } from "rxjs";

@Component({
  selector: 'kiar-collection-image',
  template: '<img *ngIf="imageUrl" [src]="imageUrl" [width]="width" [height]="height" [style.object-fit]="\'contain\'" />'
})
export class CollectionImageComponent implements OnInit {
  /** The ID to fetch image for. */
  @Input() collectionId: string;

  /** The name of the image. */
  @Input() name: string;

  /** The width of th image. */
  @Input() width: number = 100;

  /** The height of th image. */
  @Input() height: number = 100;

  /** The generate image URL. */
  public imageUrl: string | null = null;

  constructor(private collectionService: CollectionService) { }

  public ngOnInit() {
    this.collectionService.getImage(this.collectionId, this.name).pipe(
        catchError(err => of(null))
    ).subscribe({
      next: (imageData) => {
        if (imageData) {
          this.imageUrl = URL.createObjectURL(imageData)
        }
      }
    });
  }
}