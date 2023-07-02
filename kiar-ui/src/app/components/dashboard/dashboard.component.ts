import {AfterViewInit, Component} from "@angular/core";
import {MatDialog} from "@angular/material/dialog";
import {Job, JobService} from "../../../../openapi";
import {mergeMap, Observable, shareReplay, Subject} from "rxjs";

@Component({
  selector: 'kiar-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements AfterViewInit {

  /** Name of the columns being displayed by the data table. */
  public readonly displayedColumns: string[] = ['name', 'status', 'source', 'template', 'createdAt', 'createdBy'];

  /** {@link Observable} of all available participants. */
  public readonly activeJobs: Observable<Array<Job>>

  /** {@link Observable} of all available participants. */
  public readonly jobHistory: Observable<Array<Job>>

  /** A {@link Subject} that can be used to trigger a data reload. */
  private reload = new Subject<void>()

  constructor(private _dialog: MatDialog, private service: JobService) {
    this.activeJobs = this.reload.pipe(
        mergeMap(m => this.service.getActiveJobs()),
        shareReplay(1)
    );

    this.jobHistory = this.reload.pipe(
        mergeMap(m => this.service.getInactiveJobs()),
        shareReplay(1)
    );
  }

  ngAfterViewInit(): void {
    this.reload.next()
  }

  /**
   *
   */
  public viewLog() {

  }
}