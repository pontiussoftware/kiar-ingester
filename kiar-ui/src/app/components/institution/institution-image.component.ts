import {Component, Input, OnInit} from "@angular/core";
import {InstitutionService} from "../../../../openapi";
import {catchError, of} from "rxjs";

@Component({
    selector: 'app-institution-image',
    template: '@if (imageUrl) {<img [src]="imageUrl" [width]="width" [height]="height" [style.object-fit]="\'contain\'" />}',
    standalone: false
})
export class InstitutionImageComponent implements OnInit {
  /** The ID to fetch image for. */
  @Input() institutionId!: number;

  /** The width of th image. */
  @Input() width: number = 100;

  /** The height of th image. */
  @Input() height: number = 100;

  /** The generate image URL. */
  public imageUrl: string | null = null;

  constructor(private institutionService: InstitutionService) { }

  public ngOnInit() {
    this.institutionService.getInstitutionImage(this.institutionId).pipe(
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