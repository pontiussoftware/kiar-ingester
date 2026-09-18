import {AfterViewInit, Component, inject} from "@angular/core";
import {TranslatePipe, TranslateService} from "@ngx-translate/core";
import {toSignal} from "@angular/core/rxjs-interop";
import {catchError, firstValueFrom, map, mergeMap, Observable, of, shareReplay} from "rxjs";
import {FormArray, FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators} from "@angular/forms";
import {
  ApacheSolrConfig,
  ConfigService,
  EntityMapping,
  JobTemplate,
  JobType,
  TransformerConfig,
  TransformerType
} from "../../../../../openapi";
import {ActivatedRoute, Router} from "@angular/router";
import {MatSnackBar, MatSnackBarConfig} from "@angular/material/snack-bar";
import {TransformerDialogComponent} from "./transformer-dialog.component";
import {MatDialog} from "@angular/material/dialog";
import {MatFormField, MatInput, MatLabel} from "@angular/material/input";
import {MatOption, MatSelect} from "@angular/material/select";
import {MatCheckbox} from "@angular/material/checkbox";
import {MatButton, MatIconButton, MatMiniFabButton} from "@angular/material/button";
import {MatTooltip} from "@angular/material/tooltip";
import {MatIcon} from "@angular/material/icon";

@Component({
    selector: 'kiar-job-template-admin',
    templateUrl: './job-template.component.html',
    styleUrls: ['./job-template.component.scss'],
    imports: [FormsModule, ReactiveFormsModule, MatFormField, MatLabel, MatInput, MatSelect, MatOption, MatCheckbox, MatButton, MatTooltip, MatMiniFabButton, MatIcon, MatIconButton, TranslatePipe]
})
export class JobTemplateComponent implements AfterViewInit {
  /** The {@link ConfigService} used to access application configuration (templates, mappings, Solr configurations, participants). */
  private service = inject(ConfigService);

  /** The {@link Router} used for navigation. */
  private router = inject(Router);

  /** The {@link ActivatedRoute} used to read route parameters. */
  private route = inject(ActivatedRoute);

  /** The {@link MatSnackBar} used to display notifications. */
  private snackBar = inject(MatSnackBar);

  /** The {@link MatDialog} service used to open dialogs. */
  private dialog = inject(MatDialog);

  /** The {@link TranslateService} used to resolve user-facing messages. */
  private translate = inject(TranslateService);

  /** {@link Observable} backing {@link templateId}. */
  private readonly templateId$ = this.route.paramMap.pipe(map(params => Number(params.get('id')!!)))

  /** A signal of the current templateId. */
  public readonly templateId = toSignal(this.templateId$)
  /** List of transformers {@link FormGroup}s. */
  public readonly transformers: FormArray<FormGroup> = new FormArray<FormGroup>([])

  /** {@link Observable} backing {@link mappings}. */
  private readonly mappings$ = this.service.getListEntityMappings().pipe(shareReplay(1))

  /** A signal of the available {@link EntityMapping}s. */
  public readonly mappings = toSignal(this.mappings$, {initialValue: [] as Array<EntityMapping>})

  /** {@link Observable} backing {@link solr}. */
  private readonly solr$ = this.service.getListSolrConfiguration().pipe(shareReplay(1))

  /** A signal of the available {@link ApacheSolrConfig}s. */
  public readonly solr = toSignal(this.solr$, {initialValue: [] as Array<ApacheSolrConfig>})

  /** A signal of the available {@link JobType}s. */
  public readonly jobTypes = toSignal(this.service.getListJobTemplateTypes(), {initialValue: [] as Array<JobType>})

  /** A signal of the available {@link TransformerType}s. */
  public readonly transformerTypes = toSignal(this.service.getListTransformerTypes(), {initialValue: [] as Array<TransformerType>})

  /** A signal of the available participant names. */
  public readonly participants = toSignal(this.service.getListParticipants(), {initialValue: [] as Array<string>})
  /** The {@link FormControl} that backs this {@link EntityMappingComponent}. */
  public formControl = new FormGroup({
    name: new FormControl('', [Validators.required]),
    description: new FormControl(''),
    type: new FormControl('', [Validators.required]),
    participantName: new FormControl(''),
    config: new FormControl<ApacheSolrConfig | null>(null),
    mapping: new FormControl<EntityMapping | null>(null),
    startAutomatically: new FormControl(false),
    transformers: this.transformers
  })

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
    this.templateId$.pipe(
        mergeMap(id => this.service.getJobTemplate(id)),
    ).subscribe({
      next: (c) => this.updateForm(c),
      error: (err) => this.snackBar.open(this.translate.instant('admin.jobTemplate.errors.reload', {error: err?.error?.description}), this.translate.instant('common.action.dismiss'), {duration: 2000} as MatSnackBarConfig)
    })
  }

  /**
   * Tries to save the current state of the {@link JobTemplate} represented by the local {@link FormControl}
   */
  public save() {
    this.templateId$.pipe(
        mergeMap((id) => this.service.updateJobTemplate(id, this.formToJobTemplate(id)))
    ).subscribe({
      next: () => this.snackBar.open(this.translate.instant('admin.jobTemplate.messages.saved'), this.translate.instant('common.action.dismiss'), { duration: 2000 } as MatSnackBarConfig),
      error: (err) => this.snackBar.open(this.translate.instant('admin.jobTemplate.errors.save', {error: err?.error?.description}), this.translate.instant('common.action.dismiss'), { duration: 2000 } as MatSnackBarConfig)
    })
  }

  /**
   * Deletes this {@link JobTemplate}.
   */
  public delete() {
    if (confirm(this.translate.instant('admin.jobTemplate.confirmDelete') + '\n' + this.translate.instant('common.confirmDeleteSuffix'))) {
      this.templateId$.pipe(
          mergeMap((id) =>  this.service.deleteJobTemplate(id))
      ).subscribe({
        next: () => {
          this.snackBar.open(this.translate.instant('admin.jobTemplate.messages.deleted'), this.translate.instant('common.action.dismiss'), { duration: 2000 } as MatSnackBarConfig);
          this.router.navigate(['admin', 'dashboard']).then(() => {})
        },
        error: (err) => this.snackBar.open(this.translate.instant('admin.jobTemplate.errors.delete', {error: err?.error?.description}), this.translate.instant('common.action.dismiss'), { duration: 2000 } as MatSnackBarConfig)
      })
    }
  }

  /**
   * Downloads the current {@link JobTemplate} as a file.
   */
  public download() {
    this.templateId$.pipe(
        mergeMap(id => this.service.getJobTemplate(id)),
        catchError((err) => {
          this.snackBar.open(this.translate.instant('admin.jobTemplate.errors.load', {error: err?.error?.description}), this.translate.instant('common.action.dismiss'), {duration: 2000} as MatSnackBarConfig);
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
  private formToJobTemplate(id: number): JobTemplate {
    return {
      id: id,
      name: this.formControl.get('name')?.value,
      description: this.formControl.get('description')?.value,
      type: this.formControl.get('type')?.value as JobType,
      startAutomatically: this.formControl.get('startAutomatically')?.value,
      participantName: this.formControl.get('participantName')?.value,
      config: this.formControl.get('config')?.value,
      mapping: this.formControl.get('mapping')?.value,
      createdAt: -1,
      changedAt: -1,
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
  private async updateForm(template: JobTemplate) {
    this.formControl.controls['name'].setValue(template?.name ?? '');
    this.formControl.controls['description'].setValue(template?.description ?? '');
    this.formControl.controls['type'].setValue(template?.type ?? '');
    this.formControl.controls['participantName'].setValue(template?.participantName ?? '');
    this.formControl.controls['config'].setValue(await firstValueFrom(this.solr$.pipe(map( s => s.find(s => s.id === template.config?.id)))) ?? null);
    this.formControl.controls['mapping'].setValue(await firstValueFrom(this.mappings$.pipe(map( s => s.find(s => s.id === template.mapping?.id)))) ?? null);
    this.formControl.controls['startAutomatically'].setValue(template?.startAutomatically ?? false);

    this.transformers.clear()
    for (let transformer of (template?.transformers || [])) {
      this.transformers.push(new FormGroup({
        type: new FormControl(transformer.type , [Validators.required]),
        parameters: new FormArray(Object.entries(transformer.parameters).map(p => new FormGroup({
          key: new FormControl(p[0] ?? ''),
          value: new FormControl(p[1] ?? '')
        })))
      }))
    }
  }
}