import {AfterViewInit, Component} from "@angular/core";
import {MatDialog} from "@angular/material/dialog";
import {Job, JobService, SuccessStatus} from "../../../../openapi";
import {catchError, map, mergeMap, Observable, Observer, of, shareReplay, Subject} from "rxjs";
import {CreateJobDialogComponent} from "./job/create-job-dialog.component";
import {MatSnackBar, MatSnackBarConfig} from "@angular/material/snack-bar";


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
export class DashboardComponent implements AfterViewInit {

  /** Name of the columns being displayed by the data table. */
  public readonly displayedColumns: string[] = ['name', 'status', 'source', 'template', 'statistics', 'createdAt', 'createdBy',  'action'];

  /** {@link Observable} of all available participants. */
  public readonly activeJobs: Observable<Array<ActiveJob>>

  /** {@link Observable} of all available participants. */
  public readonly jobHistory: Observable<Array<Job>>

  /** A {@link Subject} that can be used to trigger a data reload. */
  private reload = new Subject<void>()

  constructor(private dialog: MatDialog, private snackBar: MatSnackBar, private service: JobService) {
    this.activeJobs = this.reload.pipe(
        mergeMap(m => this.service.getActiveJobs()),
        map(jobs => jobs.map(j => {
          (j as any)['harvesting'] = false
          return ((j as any) as ActiveJob)
        })),
        catchError(err => {
          this.snackBar.open(`Error while loading active jobs: ${err?.error?.description}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig)
          return of([])
        }),
        shareReplay(1, 10000)
    );

    this.jobHistory = this.reload.pipe(
        mergeMap(m => this.service.getInactiveJobs()),
        catchError(err => {
          this.snackBar.open(`Error while loading job history: ${err?.error?.description}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig)
          return of([])
        }),
        shareReplay(1, 60000)
    );
  }

  ngAfterViewInit(): void {
    this.reload.next()
  }

  /**
   * Opens the dialog to create a new job. Causes the list of jobs to be reloaded when dialog closes.
   */
  public createJob() {
    this.dialog.open(CreateJobDialogComponent).afterClosed().subscribe(c => {
      this.reload.next()
    })
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
              this.reload.next()
            },
            error: (err) => {
              this.snackBar.open(`Error while uploading KIAR file for job ${job.id}: ${err?.error?.description}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig)
              job.harvesting = false
              this.reload.next()
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
        this.reload.next()
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
          this.reload.next()
        },
        error: (err) => this.snackBar.open(`Error occurred while aborting job ${job.id}: ${err?.error?.description}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig)
    })
  }
}