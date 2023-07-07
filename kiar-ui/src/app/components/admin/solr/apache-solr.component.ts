import {AfterViewInit, Component} from "@angular/core";
import {map, mergeMap, Observable} from "rxjs";
import {ApacheSolrCollection, ApacheSolrConfig, ApacheSolrService, AttributeMapping} from "../../../../../openapi";
import {ActivatedRoute, Router} from "@angular/router";
import {FormArray, FormControl, FormGroup, Validators} from "@angular/forms";
import {MatSnackBar, MatSnackBarConfig} from "@angular/material/snack-bar";
@Component({
  selector: 'kiar-apache-solr-admin',
  templateUrl: './apache-solr.component.html',
  styleUrls: ['./apache-solr.component.scss']
})
export class ApacheSolrComponent implements AfterViewInit{

  /** An {@link Observable} of the mapping ID that is being inspected by this {@link EntityMappingComponent}. */
  public readonly solrId: Observable<string>

  /** List of attribute {@link FormGroup}s. */
  public readonly collections: Array<FormGroup> = []

  /** The {@link FormControl} that backs this {@link EntityMappingComponent}. */
  public formControl = new FormGroup({
    name: new FormControl('', [Validators.required]),
    description: new FormControl(''),
    server: new FormControl('', [Validators.required, Validators.pattern('(https?://)?([\\da-z.-]+)\\.([a-z.]{2,6})[/\\w .-]*/?')]),
    username: new FormControl(''),
    password: new FormControl(''),
    collections: new FormArray(this.collections)
  })

  constructor(
      private service: ApacheSolrService,
      private router: Router,
      private route: ActivatedRoute,
      private snackBar: MatSnackBar
  ) {
    this.solrId = this.route.paramMap.pipe(map(params => params.get('id')!!));
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
   * Adds a {@link ApacheSolrCollection} to edit an existing {@link AttributeMapping}.
   */
  public addCollection() {
    this.collections.push(new FormGroup({
      name: new FormControl('', [Validators.required]),
      type: new FormControl('', [Validators.required]),
      selector: new FormControl('')
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
   * Updates the {@link FormControl} backing this view with a new {@link ApacheSolrConfig}.
   *
   * @param solr The {@link ApacheSolrConfig} to apply.
   */
  private updateForm(solr: ApacheSolrConfig | null) {
    this.formControl.controls['name'].setValue(solr?.name || '');
    this.formControl.controls['description'].setValue(solr?.description || '');
    this.formControl.controls['server'].setValue(solr?.server || '');
    this.formControl.controls['username'].setValue(solr?.username || '');
    this.formControl.controls['password'].setValue(solr?.password || '');

    this.collections.length = 0
    for (let collection of (solr?.collections || [])) {
        this.collections.push(new FormGroup({
            name: new FormControl(collection.name || '', [Validators.required]),
            type: new FormControl(collection.type || '', [Validators.required]),
            selector: new FormControl(collection.selector || '')
        }))
    }
  }

  /**
   * Converts this {@link FormGroup} to an {@link ApacheSolrConfig}.
   *
   * @param id The ID of the {@link ApacheSolrConfig}
   * @return {@link ApacheSolrConfig}
   */
  private formToApacheSolrConfig(id: string): ApacheSolrConfig {
    return {
      id: id,
      name: this.formControl.get('name')?.value,
      description: this.formControl.get('description')?.value,
      server: this.formControl.get('server')?.value,
      username: this.formControl.get('username')?.value,
      password: this.formControl.get('password')?.value,
      collections: this.collections.map((collection) => {
        return {
          name: collection.get('name')?.value,
          type: collection.get('type')?.value,
          selector: collection.get('selector')?.value
        } as ApacheSolrCollection
      }),
    } as ApacheSolrConfig
  }
}