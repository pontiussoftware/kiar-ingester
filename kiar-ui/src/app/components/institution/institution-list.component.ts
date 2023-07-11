import {AfterViewInit, Component} from "@angular/core";
import {Institution, InstitutionService} from "../../../../openapi";
import {mergeMap, Observable, Subject} from "rxjs";

@Component({
  selector: 'kiar-institution-list',
  templateUrl: './institution-list.component.html',
  styleUrls: ['./institution-list.component.scss']
})
export class InstitutionListComponent implements AfterViewInit {

  /** {@link Observable} of all available participants. */
  public readonly institutions: Observable<Array<Institution>>

  /** The columns that should be displayed in the data table. */
  public readonly displayedColumns: string[] = ['name', 'displayName', 'participant', 'canton', 'email'];

  /** A {@link Subject} that can be used to trigger a data reload. */
  private reload= new Subject<void>()

  constructor(private service: InstitutionService) {
    this.institutions = this.reload.pipe(
        mergeMap(r => this.service.getInstitutions())
    )
  }

  ngAfterViewInit(): void {
    this.reload.next()
  }
}