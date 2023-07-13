import {AfterViewInit, Component} from "@angular/core";
import {map, mergeMap, Observable, shareReplay} from "rxjs";
import {FormArray, FormControl, FormGroup, Validators} from "@angular/forms";
import {ApacheSolrConfig, ConfigService, EntityMapping, JobTemplate, JobType, TransformerType} from "../../../../../openapi";
import {ActivatedRoute, Router} from "@angular/router";
import {MatSnackBar, MatSnackBarConfig} from "@angular/material/snack-bar";

@Component({
  selector: 'kiar-job-template-admin',
  templateUrl: './job-template.component.html',
  styleUrls: ['./job-template.component.scss']
})
export class JobTemplateComponent implements AfterViewInit {

  /** An {@link Observable} of the mapping ID that is being inspected by this {@link EntityMappingComponent}. */
  public readonly templateId: Observable<string>

  /** List of transformers {@link FormGroup}s. */
  public readonly transformers: FormArray = new FormArray<any>([])

  /** An {@link Observable} of available {@link EntityMapping}. */
  public readonly mappings: Observable<Array<EntityMapping>>

  /** An {@link Observable} of available {@link ApacheSolrConfig}. */
  public readonly solr: Observable<Array<ApacheSolrConfig>>

  /** An {@link Observable} of available {@link JobType}. */
  public readonly jobTypes: Observable<Array<JobType>>

  /** An {@link Observable} of available {@link JobType}. */
  public readonly transformerTypes: Observable<Array<TransformerType>>

  /** An {@link Observable} of available participants. */
  public readonly participants: Observable<Array<String>>

  /** The {@link FormControl} that backs this {@link EntityMappingComponent}. */
  public formControl = new FormGroup({
    name: new FormControl('', [Validators.required]),
    description: new FormControl(''),
    type: new FormControl('', [Validators.required]),
    participantName: new FormControl(''),
    solrConfigName: new FormControl(''),
    entityMappingName: new FormControl(''),
    startAutomatically: new FormControl(false),
    transformers: this.transformers
  })

  constructor(
      private service: ConfigService,
      private router: Router,
      private route: ActivatedRoute,
      private snackBar: MatSnackBar
  ) {
    this.templateId = this.route.paramMap.pipe(map(params => params.get('id')!!));
    this.mappings = this.service.getListEntityMappings().pipe(shareReplay(1, 30000))
    this.solr = this.service.getListSolrConfiguration().pipe(shareReplay(1, 30000))
    this.jobTypes = this.service.getListJobTemplateTypes().pipe(shareReplay(1, 30000))
    this.transformerTypes = this.service.getListTransformerTypes().pipe(shareReplay(1, 30000))
    this.participants = this.service.getListParticipants().pipe(shareReplay(1, 30000))
  }

  /**
   * Refreshes the data after view has been setup.
   */
  public ngAfterViewInit() {
    this.refresh()
  }

  /**
   * Reloads and refreshes the data backing this {@link EntityMappingComponent}.
   */
  public refresh() {
    this.templateId.pipe(
        mergeMap(id => this.service.getJobTemplate(id)),
    ).subscribe({
      next: (c) => this.updateForm(c),
      error: (err) => this.snackBar.open(`Error occurred while trying to reload job template: ${err?.error?.description}.`, "Dismiss", {duration: 2000} as MatSnackBarConfig)
    })
  }

  /**
   * Tries to save the current state of the {@link ApacheSolrConfig} represented by the local {@link FormControl}
   */
  public save() {

  }

  /**
   * Deletes this {@link ApacheSolrConfig}.
   */
  public delete() {
    if (confirm("Are you sure that you want to delete this Job template?\nAfter deletion, it can no longer be retrieved.")) {
      this.templateId.pipe(
          mergeMap((id) =>  this.service.deleteJobTemplate(id))
      ).subscribe({
        next: () => {
          this.snackBar.open(`Successfully deleted job template.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig);
          this.router.navigate(['admin', 'dashboard']).then(() => {})
        },
        error: (err) => this.snackBar.open(`Error occurred while trying to delete job template: ${err?.error?.description}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig)
      })
    }
  }

  /**
   * Adds a {@link ApacheSolrCollection} to edit an existing {@link AttributeMapping}.
   */
  public addTransformer() {
    this.transformers.push(new FormGroup({
      name: new FormControl('', [Validators.required]),
      attributes: new FormArray([])
    }))
  }

  /**
   * Removes a parameter {@link FormGroup} at the provided index.
   *
   * @param index
   */
  public removeTransformer(index: number) {
    this.transformers.removeAt(index)
  }

  /**
   * Updates the {@link FormControl} backing this view with a new {@link JobTemplate}.
   *
   * @param template The {@link JobTemplate} to apply.
   */
  private updateForm(template: JobTemplate) {
    this.formControl.controls['name'].setValue(template?.name || '');
    this.formControl.controls['description'].setValue(template?.description || '');
    this.formControl.controls['type'].setValue(template?.type || '');
    this.formControl.controls['participantName'].setValue(template?.participantName || '');
    this.formControl.controls['solrConfigName'].setValue(template?.solrConfigName || '');
    this.formControl.controls['entityMappingName'].setValue(template?.entityMappingName || '');
    this.formControl.controls['startAutomatically'].setValue(template?.startAutomatically || false);

    this.transformers.clear()
    for (let transformer of (template?.transformers || [])) {
      this.transformers.push(new FormGroup({
        name: new FormControl(transformer.type , [Validators.required]),
        parameters: new FormArray(Object.entries(transformer.parameters).map(p => new FormGroup({
          key: new FormControl(p[0] || ''),
          value: new FormControl(p[1] || '')
        })))
      }))
    }
  }
}