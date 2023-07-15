import {Component} from "@angular/core";
import {FormControl, FormGroup, Validators} from "@angular/forms";
import {MatDialogRef} from "@angular/material/dialog";
import {ApacheSolrConfig, ConfigService, EntityMapping, JobTemplate, JobType} from "../../../../../openapi";
import {Observable, shareReplay} from "rxjs";

@Component({
  selector: 'kiar-add-job-template-dialog',
  templateUrl: './add-job-template.dialog.component.html'
})
export class AddJobTemplateDialogComponent {

  /** The {@link FormControl} that backs this {@link AddJobTemplateDialogComponent}. */
  public formControl: FormGroup =  new FormGroup({
      name: new FormControl('', [Validators.required, Validators.minLength(3)]),
      description: new FormControl(''),
      type: new FormControl(JobType.KIAR, [Validators.required]),
      startAutomatically: new FormControl(false),
      participantName: new FormControl('', [Validators.required]),
      entityMappingName: new FormControl('', [Validators.required]),
      solrConfigName: new FormControl('', [Validators.required]),
  })

  /** An {@link Observable} of available {@link JobTemplate}. */
  public readonly mappings: Observable<Array<EntityMapping>>

  /** An {@link Observable} of available {@link SolrConfig}. */
  public readonly solr: Observable<Array<ApacheSolrConfig>>

  /** An {@link Observable} of available {@link SolrConfig}. */
  public readonly types: Observable<Array<JobType>>

  /** An {@link Observable} of available participants. */
  public readonly participants: Observable<Array<String>>

  constructor(private config: ConfigService, private dialogRef: MatDialogRef<AddJobTemplateDialogComponent>) {
      this.mappings = this.config.getListEntityMappings().pipe(shareReplay(1))
    this.solr = this.config.getListSolrConfiguration().pipe(shareReplay(1))
    this.types = this.config.getListJobTemplateTypes().pipe(shareReplay(1))
    this.participants = this.config.getListParticipants().pipe(shareReplay(1))
  }

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
        entityMappingName: this.formControl.get('entityMappingName')?.value,
        solrConfigName: this.formControl.get('solrConfigName')?.value,
        transformers: []
      } as JobTemplate
      this.dialogRef.close(object)
    }
  }
}