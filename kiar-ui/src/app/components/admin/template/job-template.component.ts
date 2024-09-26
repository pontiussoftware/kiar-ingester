import {AfterViewInit, Component} from "@angular/core";
import {catchError, map, mergeMap, Observable, of, shareReplay} from "rxjs";
import {FormArray, FormControl, FormGroup, Validators} from "@angular/forms";
import {ApacheSolrConfig, ConfigService, EntityMapping, JobTemplate, JobType, TransformerConfig, TransformerType} from "../../../../../openapi";
import {ActivatedRoute, Router} from "@angular/router";
import {MatSnackBar, MatSnackBarConfig} from "@angular/material/snack-bar";
import {TransformerDialogComponent} from "./transformer-dialog.component";
import {MatDialog} from "@angular/material/dialog";

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

  /** An {@link Observable} of available {@link TransformerType}. */
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
      private snackBar: MatSnackBar,
      private dialog: MatDialog
  ) {
    this.templateId = this.route.paramMap.pipe(map(params => params.get('id')!!));
    this.mappings = this.service.getListEntityMappings().pipe(shareReplay(1))
    this.solr = this.service.getListSolrConfiguration().pipe(shareReplay(1))
    this.jobTypes = this.service.getListJobTemplateTypes().pipe(shareReplay(1))
    this.transformerTypes = this.service.getListTransformerTypes().pipe(shareReplay(1))
    this.participants = this.service.getListParticipants().pipe(shareReplay(1))
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
   * Tries to save the current state of the {@link JobTemplate} represented by the local {@link FormControl}
   */
  public save() {
    this.templateId.pipe(
        mergeMap((id) => this.service.updateJobTemplate(id, this.formToJobTemplate(id)))
    ).subscribe({
      next: () => this.snackBar.open(`Successfully updated job template.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig),
      error: (err) => this.snackBar.open(`Error occurred while trying to update job template: ${err?.error?.description}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig)
    })
  }

  /**
   * Deletes this {@link JobTemplate}.
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
   * Downloads the current {@link JobTemplate} as a file.
   */
  public download() {
    this.templateId.pipe(
        mergeMap(id => this.service.getJobTemplate(id)),
        catchError((err) => {
          this.snackBar.open(`Error occurred while trying to load job template: ${err?.error?.description}.`, "Dismiss", {duration: 2000} as MatSnackBarConfig);
          return of(null)
        })
    ).subscribe(data => {
      if (data != null) {
        const fileName = 'template.json';
        const fileToSave = new Blob([JSON.stringify(data, null, 2)], {type: 'application/json'});

        /* Create download */
        const a = document.createElement("a");
        a.href = URL.createObjectURL(fileToSave);
        a.download = fileName;
        a.click();
      }
    })
  }

  /**
   * Adds a {@link ApacheSolrCollection} to edit an existing {@link AttributeMapping}.
   */
  public addTransformer() {
    this.transformers.push(new FormGroup({
      type: new FormControl('', [Validators.required]),
      parameters: new FormArray([])
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
   * Opens a {@link AttributeMappingDialogComponent} to edit an existing {@link AttributeMapping}.
   *
   * @param index The index of the {@link AttributeMapping} to edit.
   */
  public editTransformer(index: number) {
    this.dialog.open(TransformerDialogComponent, {
      data: this.transformers.at(index),
      width: '750px'
    })
  }

  /**
   * Moves a transformer in the list.
   *
   * @param index The index of the transformer to move.
   * @param newIndex The new index to move.
   */
  public moveTransformer(index: number, newIndex: number): void {
    const entry = this.transformers.at(index)
    this.transformers.removeAt(index)
    this.transformers.insert(newIndex, entry)
  }

  /**
   * Generates a string representations of the attributes stored with a transformer.
   *
   * @param transformer {@link FormGroup} to generate the representation for.
   * @return The string representation.
   */
  public stringForAttributes(transformer: FormGroup): string {
    const controls = (transformer.get('parameters') as FormArray)?.controls
    if (controls && controls!!.length > 0) {
      return controls.map(param => {
        const key = param.get('key')?.value
        const value = param.get('value')?.value
        return `${key}: ${value}`
      }).reduce((p, c) => `${p}, ${c}`)
    } else {
      return ""
    }
  }

  /**
   * Converts this {@link FormGroup} to an {@link JobTemplate}.
   *
   * @param id The ID of the {@link JobTemplate}
   * @return {@link JobTemplate}
   */
  private formToJobTemplate(id: string): JobTemplate {
    return {
      id: id,
      name: this.formControl.get('name')?.value,
      description: this.formControl.get('description')?.value,
      type: this.formControl.get('type')?.value as JobType,
      startAutomatically: this.formControl.get('startAutomatically')?.value,
      participantName: this.formControl.get('participantName')?.value,
      solrConfigName: this.formControl.get('solrConfigName')?.value,
      entityMappingName:this.formControl.get('entityMappingName')?.value,
      transformers: this.transformers.controls.map(transformer => {
        let map = new Map<string,string>;
        (transformer.get('parameters') as FormArray)?.controls?.forEach(param => {
          const key = param.get('key')?.value
          const value = param.get('value')?.value
          map.set(key, value)
        })
        return {
          type: transformer.get('type')?.value,
          parameters: Object.fromEntries(map)
        } as TransformerConfig
      }),
    } as JobTemplate
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
        type: new FormControl(transformer.type , [Validators.required]),
        parameters: new FormArray(Object.entries(transformer.parameters).map(p => new FormGroup({
          key: new FormControl(p[0] || ''),
          value: new FormControl(p[1] || '')
        })))
      }))
    }
  }
}