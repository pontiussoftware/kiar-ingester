import {AfterViewInit, Component, OnDestroy, ViewChild} from "@angular/core";
import {MatDialog} from "@angular/material/dialog";
import {Job, JobService, SuccessStatus} from "../../../../openapi";
import {Observer, Subscription, tap, timer} from "rxjs";
import {CreateJobDialogComponent} from "./job/create-job-dialog.component";
import {MatSnackBar, MatSnackBarConfig} from "@angular/material/snack-bar";
import {MatPaginator} from "@angular/material/paginator";
import {JobHistoryDatasource} from "./job-history-datasource";
import {JobCurrentDatasource} from "./job-current-datasource";


/**
 * Internal interface for a Job that is currently active.
 */
interface ActiveJob extends Job {
  harvesting: boolean
}

@Component({
  selector: 'kiar-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements AfterViewInit, OnDestroy {

  /** Name of the columns being displayed by the data table. */
  public readonly displayedColumns: string[] = ['name', 'status', 'source', 'template', 'statistics', 'changedAt', 'createdAt', 'createdBy',  'action'];

  /** The {@link JobHistoryDatasource} backing this {@link DashboardComponent}. */
  public readonly activeJobs: JobCurrentDatasource

  /** The {@link JobHistoryDatasource} backing this {@link DashboardComponent}. */
  public readonly jobHistory: JobHistoryDatasource

  /**  A {@link Subscription} to a timer that updates list of active jobs at a regular invterval. */
  private timerSubscription: (Subscription | null) = null

  /** Reference to the {@link MatPaginator}*/
  @ViewChild('activeJobPaginator') activeJobPaginator: MatPaginator;

  /** Reference to the {@link MatPaginator}*/
  @ViewChild('jobHistoryPaginator') jobHistoryPaginator: MatPaginator;

  constructor(private dialog: MatDialog, private snackBar: MatSnackBar, private service: JobService) {
    this.activeJobs = new JobCurrentDatasource(this.service)
    this.jobHistory = new JobHistoryDatasource(this.service)
  }
  /**
   * Registers an observable for page change and load data initially.
   */
  public ngAfterViewInit(): void {
    this.timerSubscription = timer(0, 5000).subscribe(t =>     this.activeJobPaginator.page.pipe(tap(() => this.jobHistory.load(this.activeJobPaginator.pageIndex, this.activeJobPaginator.pageSize))).subscribe())
    this.activeJobPaginator.page.pipe(tap(() => this.jobHistory.load(this.activeJobPaginator.pageIndex, this.activeJobPaginator.pageSize))).subscribe();
    this.jobHistoryPaginator.page.pipe(tap(() => this.jobHistory.load(this.jobHistoryPaginator.pageIndex, this.jobHistoryPaginator.pageSize))).subscribe();
  }

  /**
   * Unsubscribes from timer.
   */
  public ngOnDestroy() {
    this.timerSubscription?.unsubscribe()
    this.timerSubscription = null
  }

  /**
   * Opens the dialog to create a new job. Causes the list of jobs to be reloaded when dialog closes.
   */
  public createJob() {
    this.dialog.open(CreateJobDialogComponent).afterClosed().subscribe(c => {
      this.reload()
    })
  }

  /**
   * Reloads both the list of active jobs and the job history.
   */
  public reload() {
    this.activeJobs.load(this.activeJobPaginator.pageIndex, this.activeJobPaginator.pageSize);
    this.jobHistory.load(this.jobHistoryPaginator.pageIndex, this.jobHistoryPaginator.pageSize);
  }

  /**
   * Opens the 'file open' dialog and starts a KIAR file upload.
   *
   * @param job {@link ActiveJob} to initiate KIAR file upload for.
   */
  public uploadKiar(job: ActiveJob) {
      const fileInput: HTMLInputElement = document.createElement('input');
      fileInput.type = 'file';
      fileInput.addEventListener('change', (event: Event) => {
        const target = event.target as HTMLInputElement;
        const file: File | null = target.files?.[0] || null;
        if (file) {
          const formData: FormData = new FormData();
          formData.append('kiar', file);
          job.harvesting = true
          this.service.putUploadKiar(job.id!!, file, 'body').subscribe({
            next: (v) => {
              this.snackBar.open(`Successfully uploaded KIAR file to job ${job.id}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig)
              job.harvesting = false
              this.reload()
            },
            error: (err) => {
              this.snackBar.open(`Error while uploading KIAR file for job ${job.id}: ${err?.error?.description}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig)
              job.harvesting = false
              this.reload()
            }
          } as Observer<SuccessStatus>)
        }
      });
      fileInput.click();
  }

  /**
   * Starts the data ingest for the selected {@link ActiveJob}.
   *
   * @param job {@link ActiveJob} to start data ingest for.
   */
  public startIngest(job: ActiveJob) {
    this.service.putScheduleJob(job.id!!).subscribe({
      next: (next) => {
        this.snackBar.open(`Successfully scheduled job ${job.id}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig)
        this.reload()
      },
      error: (err) => this.snackBar.open(`Error occurred while scheduling job ${job.id}: ${err?.error?.description}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig)
    })
  }

  /**
   * The {@link ActiveJob} to abort.
   *
   * @param job
   */
  public abortJob(job: ActiveJob) {
    this.service.deleteAbortJob(job.id!!).subscribe({
        next: (next) => {
          this.snackBar.open(`Successfully aborted job ${job.id}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig)
          this.reload()
        },
        error: (err) => this.snackBar.open(`Error occurred while aborting job ${job.id}: ${err?.error?.description}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig)
    })
  }
}