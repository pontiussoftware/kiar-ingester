import {Component, inject} from "@angular/core";
import {TranslatePipe, TranslateService} from "@ngx-translate/core";
import {toSignal} from "@angular/core/rxjs-interop";
import {FormControl, FormGroup, FormsModule, ReactiveFormsModule} from "@angular/forms";
import {CreateJobRequest, Job, JobService, JobTemplate} from "../../../../../openapi";
import {Observable, Observer} from "rxjs";
import {MatSnackBar, MatSnackBarConfig} from "@angular/material/snack-bar";
import {MatDialogActions, MatDialogContent, MatDialogRef, MatDialogTitle} from "@angular/material/dialog";
import {MatFormField, MatInput, MatLabel} from "@angular/material/input";
import {MatOption, MatSelect} from "@angular/material/select";
import {MatButton} from "@angular/material/button";

@Component({
    selector: 'create-job-dialog',
    templateUrl: 'create-job-dialog.component.html',
  imports: [MatDialogTitle, MatDialogContent, FormsModule, ReactiveFormsModule, MatFormField, MatLabel, MatInput, MatSelect, MatOption, MatDialogActions, MatButton, TranslatePipe]
})
export class CreateJobDialogComponent {
  /** The {@link JobService} used to access and manage jobs. */
  private service = inject(JobService);

  /** The {@link MatSnackBar} used to display notifications. */
  private snackBar = inject(MatSnackBar);

  /** The {@link MatDialogRef} used to interact with and close this dialog. */
  private dialogRef = inject<MatDialogRef<CreateJobDialogComponent>>(MatDialogRef);

  /** The {@link TranslateService} used to resolve user-facing messages. */
  private translate = inject(TranslateService);

  /** The {@link FormControl} that backs this {@link AddJobTemplateDialogComponent}. */
  public formControl: FormGroup =  new FormGroup({
    name: new FormControl(''),
    template: new FormControl('')
  })

  /** A signal of the available {@link JobTemplate}s. */
  public readonly templates = toSignal(this.service.getListJobTemplates(), {initialValue: [] as Array<JobTemplate>})
  /**
   * Initializes the {@link Observable} of available {@link JobTemplate}s.
   *
   * @param service
   * @param snackBar
   * @param dialogRef
   */

  /**
   * Tries to create a new Job based on the entries made by the user.
   */
  public create() {
    if (this.formControl.valid) {
      /* Prepare observer. */
      const observer = {
        next: (job) => {
          this.snackBar.open(this.translate.instant('createJob.messages.created', {id: job.id}), this.translate.instant('common.action.dismiss'), { duration: 2000 } as MatSnackBarConfig)
          this.dialogRef.close()
        },
        error: (err) => {
          this.snackBar.open(this.translate.instant('createJob.messages.createError', {error: err?.error?.description}), this.translate.instant('common.action.dismiss'), { duration: 2000 } as MatSnackBarConfig)
        }
      } as Observer<Job>

      /* Post job. */
      this.service.postCreateJob({
        jobName: this.formControl.get('name')?.value,
        templateId: this.formControl.get('template')?.value?.id
      } as CreateJobRequest).subscribe(observer)
    }
  }

  /**
   * Closes this dialog.
   */
  public close() {
    this.dialogRef.close()
  }
}