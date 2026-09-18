import {Component, inject} from "@angular/core";
import {toSignal} from "@angular/core/rxjs-interop";
import {FormControl, FormGroup, Validators} from "@angular/forms";
import {MatDialogRef} from "@angular/material/dialog";
import {ApacheSolrConfig, ConfigService, EntityMapping, JobTemplate, JobType} from "../../../../../openapi";

@Component({
    selector: 'kiar-add-job-template-dialog',
    templateUrl: './add-job-template.dialog.component.html',
    standalone: false
})
export class AddJobTemplateDialogComponent {
  /** The {@link ConfigService} used to access application configuration (templates, mappings, Solr configurations, participants). */
  private config = inject(ConfigService);

  /** The {@link MatDialogRef} used to interact with and close this dialog. */
  private dialogRef = inject<MatDialogRef<AddJobTemplateDialogComponent>>(MatDialogRef);

  /** The {@link FormControl} that backs this {@link AddJobTemplateDialogComponent}. */
  public formControl: FormGroup =  new FormGroup({
      name: new FormControl('', [Validators.required, Validators.minLength(3)]),
      description: new FormControl(''),
      type: new FormControl(JobType.KIAR, [Validators.required]),
      startAutomatically: new FormControl(false),
      participantName: new FormControl('', [Validators.required]),
      mapping: new FormControl<EntityMapping | null>(null, [Validators.required]),
      config: new FormControl<ApacheSolrConfig | null>(null, [Validators.required]),
  })

  /** A signal of the available {@link EntityMapping}s. */
  public readonly mappings = toSignal(this.config.getListEntityMappings(), {initialValue: [] as Array<EntityMapping>})

  /** A signal of the available {@link ApacheSolrConfig}s. */
  public readonly solr = toSignal(this.config.getListSolrConfiguration(), {initialValue: [] as Array<ApacheSolrConfig>})

  /** A signal of the available {@link JobType}s. */
  public readonly types = toSignal(this.config.getListJobTemplateTypes(), {initialValue: [] as Array<JobType>})

  /** A signal of the available participant names. */
  public readonly participants = toSignal(this.config.getListParticipants(), {initialValue: [] as Array<string>})

  /**
   * Saves the data in this {@link AddJobTemplateDialogComponent}.
   */
  public save() {
    if (this.formControl.valid) {
      let object = {
        name: this.formControl.get('name')?.value,
        description: this.formControl.get('description')?.value,
        type: this.formControl.get('type')?.value as JobType,
        startAutomatically: this.formControl.get('startAutomatically')?.value,
        participantName: this.formControl.get('participantName')?.value,
        mapping: this.formControl.get('mapping')?.value,
        config: this.formControl.get('config')?.value,
        transformers: [],
          createdAt: -1,
          changedAt: -1
      } as JobTemplate
      this.dialogRef.close(object)
    }
  }
}