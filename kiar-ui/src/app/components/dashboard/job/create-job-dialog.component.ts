import {Component} from "@angular/core";
import {FormControl, FormGroup} from "@angular/forms";
import {CreateJobRequest, Job, JobService, JobTemplate, Role, SessionStatus, SuccessStatus, User} from "../../../../../openapi";
import {Observable, Observer, shareReplay} from "rxjs";
import {MatSnackBar, MatSnackBarConfig} from "@angular/material/snack-bar";
import {MatDialogRef} from "@angular/material/dialog";

@Component({
    selector: 'create-job-dialog',
    templateUrl: 'create-job-dialog.component.html',
    standalone: false
})
export class CreateJobDialogComponent {
  /** The {@link FormControl} that backs this {@link AddJobTemplateDialogComponent}. */
  public formControl: FormGroup =  new FormGroup({
    name: new FormControl(''),
    template: new FormControl('')
  })

  /** An {@link Observable} of available {@link JobTemplate}s. */
  public readonly templates: Observable<Array<JobTemplate>>

  /**
   * Initializes the {@link Observable} of available {@link JobTemplate}s.
   *
   * @param service
   * @param snackBar
   * @param dialogRef
   */
  constructor(private service: JobService, private snackBar: MatSnackBar, private dialogRef: MatDialogRef<CreateJobDialogComponent>,) {
    this.templates = this.service.getListJobTemplates().pipe(shareReplay(1, 30000))
  }

  /**
   * Tries to create a new Job based on the entries made by the user.
   */
  public create() {
    if (this.formControl.valid) {
      /* Prepare observer. */
      const observer = {
        next: (job) => {
          this.snackBar.open(`Successfully created job ${job.id}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig)
          this.dialogRef.close()
        },
        error: (err) => {
          this.snackBar.open(`Error occurred while creating job: ${err?.error?.description}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig)
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