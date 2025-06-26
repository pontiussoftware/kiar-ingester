import {AfterViewInit, Component, OnDestroy, ViewChild} from "@angular/core";
import {MatDialog} from "@angular/material/dialog";
import {Job, JobService} from "../../../../openapi";
import {firstValueFrom, interval, Subscription} from "rxjs";
import {CreateJobDialogComponent} from "./job/create-job-dialog.component";
import {MatSnackBar, MatSnackBarConfig} from "@angular/material/snack-bar";
import {MatPaginator} from "@angular/material/paginator";
import {JobHistoryDatasource} from "./job-history-datasource";
import {JobCurrentDatasource} from "./job-current-datasource";

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

  /**  A {@link Subscription} to a timer that updates list of active jobs at a regular invterval. */
  private activeJobPaginatorSubscription: (Subscription | null) = null

  /**  A {@link Subscription} to a timer that updates list of active jobs at a regular invterval. */
  private jobHistoryPaginatorSubscription: (Subscription | null) = null

  /** Reference to the {@link MatPaginator}*/
  @ViewChild('activeJobPaginator')
  private activeJobPaginator: MatPaginator;

  /** Reference to the {@link MatPaginator}*/
  @ViewChild('jobHistoryPaginator')
  private jobHistoryPaginator: MatPaginator;

  /** The upload progress for a specific job. */
  private uploadProgress = new Map<string, number>()

  constructor(private dialog: MatDialog, private snackBar: MatSnackBar, private service: JobService) {
    this.activeJobs = new JobCurrentDatasource(this.service)
    this.jobHistory = new JobHistoryDatasource(this.service)
  }
  /**
   * Registers an observable for page change and load data initially.
   */
  public ngAfterViewInit(): void {
    this.reload()
    this.activeJobPaginatorSubscription = this.activeJobPaginator.page.subscribe((s) => this.activeJobs.load(s.pageIndex, s.pageSize));
    this.jobHistoryPaginatorSubscription = this.jobHistoryPaginator.page.subscribe((s) => this.jobHistory.load(s.pageIndex, s.pageSize));
    this.timerSubscription = interval(5000).subscribe(s => this.activeJobs.load(this.activeJobPaginator.pageIndex, this.activeJobPaginator.pageSize))
  }

  /**
   * Unsubscribes from timer.
   */
  public ngOnDestroy() {
    this.activeJobPaginatorSubscription?.unsubscribe()
    this.activeJobPaginatorSubscription = null
    this.jobHistoryPaginatorSubscription?.unsubscribe()
    this.jobHistoryPaginatorSubscription = null
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
   * @param job {@link Job} to initiate KIAR file upload for.
   */
  public upload(job: Job) {
      const fileInput: HTMLInputElement = document.createElement('input');
      fileInput.type = 'file';
      fileInput.addEventListener('change', async (event: Event) => {
        const target = event.target as HTMLInputElement;
        const file: File | null = target.files?.[0] || null;
        if (file) {
          const formData: FormData = new FormData();
          if (job.id == null) throw new Error("Undefined job ID.")
          formData.append('file', file);
          this.uploadProgress.set(job.id, 0);

          /* Slice file and upload it. */
          const sliceSize = 1e8
          const slices = Math.floor(file.size / sliceSize) + 1
          for (let i = 0; i < slices; i++) {
            const slice = file.slice(i * sliceSize, Math.min((i + 1) * sliceSize, file.size), file.type)
            try {
              await firstValueFrom(this.service.putUpload(job.id!!, i == 0, i == (slices - 1), slice, 'body'));
              this.uploadProgress.set(job.id, (i / slices) * 100)
            } catch (err) {
              this.snackBar.open(`Error while uploading ${job.template?.type} file for job ${job.id}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig)
              break
            }
          }

          this.snackBar.open(`${job.template?.type} uploaded successfully. Ready for harvesting!`, "Dismiss", { duration: 2000 } as MatSnackBarConfig)
          this.uploadProgress.delete(job.id);
        }
      });
      fileInput.click();
  }

  /**
   * Starts the data ingest for the selected {@link Job}.
   *
   * @param job {@link Job} to start data ingest for.
   * @param test Whether to run the ingest in test mode.
   */
  public startIngest(job: Job, test: boolean) {
    this.service.putScheduleJob(job.id!!, test).subscribe({
      next: (next) => {
        this.snackBar.open(`Successfully scheduled job ${job.id}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig)
        this.reload()
      },
      error: (err) => this.snackBar.open(`Error occurred while scheduling job ${job.id}: ${err?.error?.description}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig)
    })
  }

  /**
   * The {@link Job} to abort.
   *
   * @param job
   */
  public abortJob(job: Job) {
    this.service.deleteAbortJob(job.id!!).subscribe({
        next: (next) => {
          this.snackBar.open(`Successfully aborted job ${job.id}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig)
          this.reload()
        },
        error: (err) => this.snackBar.open(`Error occurred while aborting job ${job.id}: ${err?.error?.description}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig)
    })
  }

  /**
   * Purges the {@link Job} log.
   *
   * @param job
   */
  public purgeLog(job: Job) {
    this.service.deletePurgeJobLog(job.id!!).subscribe({
      next: (next) => {
        this.snackBar.open(`Successfully purged job ${job.id} log.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig)
        this.reload()
      },
      error: (err) => this.snackBar.open(`Error occurred while purging job ${job.id} log: ${err?.error?.description}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig)
    })
  }

  /**
   * Checks if @{link Job} is uploading.
   *
   * @param job The @{link Job} to check for.
   */
  public isUploading(job: Job): boolean {
    return this.uploadProgress.has(job.id || "");
  }

  /**
   * Checks if @{link Job} uploading.
   *
   * @param job The @{link Job} to check for.
   */
  public progressForJob(job: Job): number {
    return this.uploadProgress.get(job.id || "") || 0;
  }
}