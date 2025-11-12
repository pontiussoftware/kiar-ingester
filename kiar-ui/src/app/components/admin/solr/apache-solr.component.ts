import {AfterViewInit, Component} from "@angular/core";
import {catchError, map, mergeMap, Observable, of, shareReplay} from "rxjs";
import {
  ApacheSolrCollection,
  ApacheSolrConfig,
  ApacheSolrService,
  AttributeMapping,
  ImageDeployment,
  ImageFormat
} from "../../../../../openapi";
import {ActivatedRoute, Router} from "@angular/router";
import {FormArray, FormControl, FormGroup, Validators} from "@angular/forms";
import {MatSnackBar, MatSnackBarConfig} from "@angular/material/snack-bar";

@Component({
    selector: 'kiar-apache-solr-admin',
    templateUrl: './apache-solr.component.html',
    styleUrls: ['./apache-solr.component.scss'],
    standalone: false
})
export class ApacheSolrComponent implements AfterViewInit{
  /** An {@link Observable} of the mapping ID that is being inspected by this {@link EntityMappingComponent}. */
  public readonly solrId: Observable<number>

  /** List of attribute {@link FormGroup}s. */
  public readonly collections: Array<FormGroup> = []

  /** List of attribute {@link FormGroup}s. */
  public readonly deployments: Array<FormGroup> = []

  /** An {@link Observable} of available {@link ImageFormat}. */
  public readonly imageFormats: Observable<Array<ImageFormat>>

  /** The {@link FormControl} that backs this {@link EntityMappingComponent}. */
  public formControl = new FormGroup({
    name: new FormControl('', [Validators.required]),
    description: new FormControl(''),
    server: new FormControl('', [Validators.required]),
    publicServer: new FormControl('',),
    username: new FormControl(''),
    password: new FormControl(''),
    collections: new FormArray(this.collections),
    deployments: new FormArray(this.deployments)
  })

  constructor(
      private service: ApacheSolrService,
      private router: Router,
      private route: ActivatedRoute,
      private snackBar: MatSnackBar
  ) {
    this.solrId = this.route.paramMap.pipe(map(params => Number(params.get('id')!!)));
    this.imageFormats = this.service.getListImageFormats().pipe(shareReplay(1))
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
    this.solrId.pipe(
        mergeMap(id => this.service.getSolrConfig(id)),
    ).subscribe({
      next: (c) => this.updateForm(c),
      error: (err) => this.snackBar.open(`Error occurred while trying to Apache Solr configuration: ${err?.error?.description}.`, "Dismiss", {duration: 2000} as MatSnackBarConfig)
    })
  }

  /**
   * Tries to save the current state of the {@link ApacheSolrConfig} represented by the local {@link FormControl}
   */
  public save() {
    this.solrId.pipe(
        mergeMap((id) => this.service.updateSolrConfig(id, this.formToApacheSolrConfig(id)))
    ).subscribe({
      next: (c) => {
        this.snackBar.open(`Successfully updated  Apache Solr configuration.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig)
        this.updateForm(c)
      },
      error: (err) => this.snackBar.open(`Error occurred while trying to update entity mapping: ${err?.error?.description}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig)
    })
  }

  /**
   * Deletes this {@link ApacheSolrConfig}.
   */
  public delete() {
    if (confirm("Are you sure that you want to delete this Apache Solr configuration?\nAfter deletion, it can no longer be retrieved.")) {
      this.solrId.pipe(
          mergeMap((id) =>  this.service.deleteSolrConfig(id))
      ).subscribe({
        next: () => {
          this.snackBar.open(`Successfully deleted Apache Solr configuration.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig);
          this.router.navigate(['admin', 'dashboard']).then(() => {})
        },
        error: (err) => this.snackBar.open(`Error occurred while trying to delete Apache Solr configuration: ${err?.error?.description}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig)
      })
    }
  }

  /**
   * Downloads the current {@link ApacheSolrConfig} as a file.
   */
  public download() {
    this.solrId.pipe(
        mergeMap(id => this.service.getSolrConfig(id)),
        catchError((err) => {
          this.snackBar.open(`Error occurred while trying to load Apache Solr configuration: ${err?.error?.description}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig);
          return of(null)
        })
    ).subscribe(data => {
      if (data != null) {
        const fileName = 'solr.json';
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
  public addCollection() {
    this.collections.push(new FormGroup({
      id: new FormControl<number | null>(null, [Validators.required]),
      displayName: new FormControl('', [Validators.required]),
      name: new FormControl('', [Validators.required]),
      type: new FormControl('', [Validators.required]),
      selector: new FormControl(''),
      oai: new FormControl(false),
      sru: new FormControl(false)
    }))
  }

  /**
   * Adds a {@link ApacheSolrCollection} to edit an existing {@link AttributeMapping}.
   */
  public addDeployment() {
    this.deployments.push(new FormGroup({
      name: new FormControl('', [Validators.required]),
      format: new FormControl('', [Validators.required]),
      source: new FormControl('', [Validators.required]),
      server: new FormControl(''),
      path: new FormControl('', [Validators.required]),
      maxSize: new FormControl(100, [Validators.required, Validators.min(100)])
    }))
  }

  /**
   * Removes an existing {@link ApacheSolrCollection}.
   *
   * @param index The index of the {@link ApacheSolrCollection} to remove.
   */
  public removeCollection(index: number) {
    this.collections.splice(index, 1)
  }

  /**
   * Removes an existing {@link ImageDeployment}.
   *
   * @param index The index of the {@link ImageDeployment} to remove.
   */
  public removeDeployment(index: number) {
    this.deployments.splice(index, 1)
  }

  /**
   * Views a collection {@link ApacheSolrCollection}.
   *
   * @param index The index of the {@link ApacheSolrCollection} to view.
   */
  public viewCollection(index: number) {
    let server = this.formControl.controls['publicServer'].value || this.formControl.controls['server'].value
    let collection = this.collections[index]?.get('name')?.value
    if (server != null && collection != null) {
      if (server.endsWith('/')) {
        window.open(`${server}${collection}/select?q=*:*`, "_blank");
      } else {
        window.open(`${server}/${collection}/select?q=*:*`, "_blank");
      }
    }
  }

  /**
   * Updates the {@link FormControl} backing this view with a new {@link ApacheSolrConfig}.
   *
   * @param solr The {@link ApacheSolrConfig} to apply.
   */
  private updateForm(solr: ApacheSolrConfig | null) {
    this.formControl.controls['name'].setValue(solr?.name ?? '');
    this.formControl.controls['description'].setValue(solr?.description ?? '');
    this.formControl.controls['server'].setValue(solr?.server ?? '');
    this.formControl.controls['publicServer'].setValue(solr?.publicServer ?? '');
    this.formControl.controls['username'].setValue(solr?.username ?? '');
    this.formControl.controls['password'].setValue(solr?.password ?? '');

    this.collections.length = 0
    for (let collection of (solr?.collections || [])) {
      this.collections.push(new FormGroup({
        id: new FormControl(collection.id ?? null, [Validators.required]),
        displayName: new FormControl(collection.displayName ?? '', [Validators.required]),
        name: new FormControl(collection.name ?? '', [Validators.required]),
        type: new FormControl(collection.type ?? '', [Validators.required]),
        selector: new FormControl(collection.selector ?? ''),
        oai: new FormControl({ value: collection.oai ?? false, disabled: collection.type !== 'OBJECT' }),
        sru: new FormControl({ value: collection.sru ?? false, disabled: collection.type !== 'OBJECT' }),
      }))
    }

    this.deployments.length = 0
    for (let deployment of (solr?.deployments || [])) {
      this.deployments.push(new FormGroup({
        name: new FormControl(deployment.name ?? '', [Validators.required]),
        format: new FormControl(deployment.format ?? '', [Validators.required]),
        source: new FormControl(deployment.source ?? '', [Validators.required]),
        server: new FormControl(deployment.server ?? ''),
        path: new FormControl(deployment.path ?? '', [Validators.required]),
        maxSize: new FormControl(deployment.maxSize ?? 100, [Validators.required, Validators.min(100)])
      }))
    }
  }

  /**
   * Converts this {@link FormGroup} to an {@link ApacheSolrConfig}.
   *
   * @param id The ID of the {@link ApacheSolrConfig}
   * @return {@link ApacheSolrConfig}
   */
  private formToApacheSolrConfig(id: number): ApacheSolrConfig {
    return {
      id: id,
      name: this.formControl.get('name')?.value,
      description: this.formControl.get('description')?.value,
      server: this.formControl.get('server')?.value,
      publicServer: this.formControl.get('publicServer')?.value,
      username: this.formControl.get('username')?.value,
      password: this.formControl.get('password')?.value,
      collections: this.collections.map((collection) => {
        return {
          id: collection.get('id')?.value,
          displayName: collection.get('displayName')?.value,
          name: collection.get('name')?.value,
          type: collection.get('type')?.value,
          selector: collection.get('selector')?.value,
          oai: collection.get('oai')?.value,
          sru: collection.get('sru')?.value
        } as ApacheSolrCollection
      }),
      deployments: this.deployments.map((deployment) => {
        return {
          name: deployment.get('name')?.value,
          format: deployment.get('format')?.value,
          source: deployment.get('source')?.value,
          server: deployment.get('server')?.value,
          path: deployment.get('path')?.value,
          maxSize: deployment.get('maxSize')?.value
        } as ImageDeployment
      }),
    } as ApacheSolrConfig
  }
}