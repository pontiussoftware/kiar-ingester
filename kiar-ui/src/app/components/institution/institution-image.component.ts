import {Component, inject, input} from "@angular/core";
import {toObservable, toSignal} from "@angular/core/rxjs-interop";
import {InstitutionService} from "../../../../openapi";
import {catchError, map, of, switchMap} from "rxjs";

@Component({
    selector: 'app-institution-image',
    template: '@if (imageUrl()) {<img [src]="imageUrl()" [width]="width()" [height]="height()" [style.object-fit]="\'contain\'" />}',
    standalone: false
})
export class InstitutionImageComponent {
  /** The {@link InstitutionService} used to access institution data. */
  private institutionService = inject(InstitutionService);

  /** The ID to fetch image for. */
  readonly institutionId = input.required<number>();

  /** The width of th image. */
  readonly width = input<number>(100);

  /** The height of th image. */
  readonly height = input<number>(100);

  /** The generated image URL; re-fetched whenever {@link institutionId} changes. */
  public readonly imageUrl = toSignal(
      toObservable(this.institutionId).pipe(
          switchMap(id => this.institutionService.getInstitutionImage(id).pipe(catchError(() => of(null)))),
          map(imageData => imageData ? URL.createObjectURL(imageData) : null)
      ),
      {initialValue: null}
  );
}
