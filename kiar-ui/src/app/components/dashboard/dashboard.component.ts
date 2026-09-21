import {AfterViewInit, Component, inject, OnDestroy, signal, viewChild} from "@angular/core";
import {TranslatePipe, TranslateService} from "@ngx-translate/core";
import {LanguageService} from "../../services/language.service";
import {MatDialog} from "@angular/material/dialog";
import {Job, JobService} from "../../../../openapi";
import {firstValueFrom, interval, Subscription} from "rxjs";
import {CreateJobDialogComponent} from "./job/create-job-dialog.component";
import {MatSnackBar, MatSnackBarConfig} from "@angular/material/snack-bar";
import {MatPaginator} from "@angular/material/paginator";
import {JobHistoryDatasource} from "./job-history-datasource";
import {JobCurrentDatasource} from "./job-current-datasource";
import {MatIconButton, MatMiniFabButton} from "@angular/material/button";
import {MatTooltip} from "@angular/material/tooltip";
import {MatIcon} from "@angular/material/icon";
import {MatTab, MatTabGroup} from "@angular/material/tabs";
import {
  MatCell,
  MatCellDef,
  MatColumnDef,
  MatHeaderCell,
  MatHeaderCellDef,
  MatHeaderRow,
  MatHeaderRowDef,
  MatNoDataRow,
  MatRow,
  MatRowDef,
  MatTable
} from "@angular/material/table";
import {RouterLink} from "@angular/router";
import {MatProgressBar} from "@angular/material/progress-bar";
import {DatePipe} from "@angular/common";

@Component({
    selector: 'kiar-dashboard',
    templateUrl: './dashboard.component.html',
    styleUrls: ['./dashboard.component.scss'],
    imports: [MatMiniFabButton, MatTooltip, MatIcon, MatTabGroup, MatTab, MatTable, MatColumnDef, MatHeaderCellDef, MatHeaderCell, MatCellDef, MatCell, MatIconButton, RouterLink, MatProgressBar, MatHeaderRowDef, MatHeaderRow, MatRowDef, MatRow, MatNoDataRow, MatPaginator, DatePipe, TranslatePipe]
})
export class DashboardComponent implements AfterViewInit, OnDestroy {
  /** The {@link MatDialog} service used to open dialogs. */
  private dialog = inject(MatDialog);

  /** The {@link MatSnackBar} used to display notifications. */
  private snackBar = inject(MatSnackBar);

  /** The {@link JobService} used to access and manage jobs. */
  private service = inject(JobService);

  /** The {@link LanguageService} providing the locale used for date formatting. */
  protected readonly language = inject(LanguageService);

  /** The {@link TranslateService} used to resolve user-facing messages. */
  private translate = inject(TranslateService);

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
  private readonly activeJobPaginator = viewChild.required<MatPaginator>('activeJobPaginator');

  /** Reference to the {@link MatPaginator}*/
  private readonly jobHistoryPaginator = viewChild.required<MatPaginator>('jobHistoryPaginator');

  /** The upload progress for a specific job. */
  private readonly uploadProgress = signal(new Map<number, number>())

  constructor() {
    this.activeJobs = new JobCurrentDatasource(this.service)
    this.jobHistory = new JobHistoryDatasource(this.service)
  }
  /**
   * Registers an observable for page change and load data initially.
   */
  public ngAfterViewInit(): void {
    this.reload()
    this.activeJobPaginatorSubscription = this.activeJobPaginator().page.subscribe((s) => this.activeJobs.load(s.pageIndex, s.pageSize));
    this.jobHistoryPaginatorSubscription = this.jobHistoryPaginator().page.subscribe((s) => this.jobHistory.load(s.pageIndex, s.pageSize));
    this.timerSubscription = interval(5000).subscribe(s => this.activeJobs.load(this.activeJobPaginator().pageIndex, this.activeJobPaginator().pageSize))
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
    this.activeJobs.load(this.activeJobPaginator().pageIndex, this.activeJobPaginator().pageSize);
    this.jobHistory.load(this.jobHistoryPaginator().pageIndex, this.jobHistoryPaginator().pageSize);
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
          this.setProgress(job.id, 0);

          /* Slice file and upload it. */
          const sliceSize = 1e8
          const slices = Math.floor(file.size / sliceSize) + 1
          let failed = false
          for (let i = 0; i < slices; i++) {
            const slice = file.slice(i * sliceSize, Math.min((i + 1) * sliceSize, file.size), file.type)
            try {
              await firstValueFrom(this.service.putUpload(job.id!!, i == 0, i == (slices - 1), slice, 'body'));
              this.setProgress(job.id, ((i + 1) / slices) * 100)
            } catch (err) {
              this.snackBar.open(this.translate.instant('dashboard.messages.uploadError', {type: job.template?.type, id: job.id}), this.translate.instant('common.action.dismiss'), { duration: 5000 } as MatSnackBarConfig)
              failed = true
              break
            }
          }

          /* Only report success if every chunk was accepted; a partial upload leaves the job in a state that must be re-uploaded. */
          if (!failed) {
            this.snackBar.open(this.translate.instant('dashboard.messages.uploadSuccess', {type: job.template?.type}), this.translate.instant('common.action.dismiss'), { duration: 2000 } as MatSnackBarConfig)
          }
          this.clearProgress(job.id);
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
        this.snackBar.open(this.translate.instant('dashboard.messages.jobScheduled', {id: job.id}), this.translate.instant('common.action.dismiss'), { duration: 2000 } as MatSnackBarConfig)
        this.reload()
      },
      error: (err) => this.snackBar.open(this.translate.instant('dashboard.messages.jobScheduleError', {id: job.id, error: err?.error?.description}), this.translate.instant('common.action.dismiss'), { duration: 2000 } as MatSnackBarConfig)
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
          this.snackBar.open(this.translate.instant('dashboard.messages.jobAborted', {id: job.id}), this.translate.instant('common.action.dismiss'), { duration: 2000 } as MatSnackBarConfig)
          this.reload()
        },
        error: (err) => this.snackBar.open(this.translate.instant('dashboard.messages.jobAbortError', {id: job.id, error: err?.error?.description}), this.translate.instant('common.action.dismiss'), { duration: 2000 } as MatSnackBarConfig)
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
        this.snackBar.open(this.translate.instant('dashboard.messages.logPurged', {id: job.id}), this.translate.instant('common.action.dismiss'), { duration: 2000 } as MatSnackBarConfig)
        this.reload()
      },
      error: (err) => this.snackBar.open(this.translate.instant('dashboard.messages.logPurgeError', {id: job.id, error: err?.error?.description}), this.translate.instant('common.action.dismiss'), { duration: 2000 } as MatSnackBarConfig)
    })
  }

  /**
   * Checks if @{link Job} is uploading.
   *
   * @param job The @{link Job} to check for.
   */
  public isUploading(job: Job): boolean {
    if (!job.id) return false;
    return this.uploadProgress().has(job.id);
  }

  /**
   * Checks if @{link Job} uploading.
   *
   * @param job The @{link Job} to check for.
   */
  public progressForJob(job: Job): number {
    if (!job.id) return 0;
    return this.uploadProgress().get(job.id) ?? 0;
  }

  /**
   * Records the upload progress for a {@link Job}. Replaces the map so the signal notifies its consumers.
   *
   * @param jobId The ID of the {@link Job}.
   * @param progress The progress in percent.
   */
  private setProgress(jobId: number, progress: number) {
    this.uploadProgress.update(m => new Map(m).set(jobId, progress))
  }

  /**
   * Removes the upload progress entry for a {@link Job}.
   *
   * @param jobId The ID of the {@link Job}.
   */
  private clearProgress(jobId: number) {
    this.uploadProgress.update(m => {
      const copy = new Map(m)
      copy.delete(jobId)
      return copy
    })
  }
}
