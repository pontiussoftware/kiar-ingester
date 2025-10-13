import {AfterViewInit, Component} from "@angular/core";
import {ApacheSolrConfig, ConfigService, EntityMapping, JobTemplate} from "../../../../../openapi";
import {mergeMap, Observable, Observer, shareReplay, Subject} from "rxjs";
import {MatDialog} from "@angular/material/dialog";
import {AddEntityMappingDialogComponent} from "./add-entity-mapping-dialog.component";
import {MatSnackBar, MatSnackBarConfig} from "@angular/material/snack-bar";
import {AddSolrConfigDialogComponent} from "./add-solr-config.dialog.component";
import {AddJobTemplateDialogComponent} from "./add-job-template-dialog.component";
import {AddParticipantDialogComponent} from "./add-participant-dialog.component";

@Component({
    selector: 'kiar-admin-dashboard',
    templateUrl: './admin-dashboard.component.html',
    styleUrls: ['./admin-dashboard.component.scss'],
    standalone: false
})
export class AdminDashboardComponent implements AfterViewInit {


  /** {@link Observable} of all available {@link JobTemplate}s. */
  public readonly templates: Observable<Array<JobTemplate>>

  /** {@link Observable} of all available {@link EntityMapping}s. */
  public readonly mappings: Observable<Array<EntityMapping>>

  /** {@link Observable} of all available {@link ApacheSolrConfig}s. */
  public readonly solr: Observable<Array<ApacheSolrConfig>>

  /** {@link Observable} of all available participants. */
  public readonly participant: Observable<Array<String>>

  /** A {@link Subject} that can be used to trigger a data reload. */
  private reload= new Subject<void>()

  constructor(private config: ConfigService, private _dialog: MatDialog, private _snackBar: MatSnackBar) {
    this.templates = this.reload.pipe(
        mergeMap(m => this.config.getListJobTemplates()),
        shareReplay(1)
    );

    this.mappings = this.reload.pipe(
        mergeMap(m => this.config.getListEntityMappings()),
        shareReplay(1)
    );

    this.solr = this.reload.pipe(
        mergeMap(m => this.config.getListSolrConfiguration()),
        shareReplay(1)
    );

    this.participant = this.reload.pipe(
        mergeMap(m => this.config.getListParticipants()),
        shareReplay(1)
    );
  }

  /**
   * Reloads the data once the view has been loaded.
   */
  public ngAfterViewInit() {
    this.reload.next()
  }

  /**
   * Opens a dialog to create a new {@link JobTemplate}.
   */
  public addJobTemplate() {
    this._dialog.open(AddJobTemplateDialogComponent).afterClosed().subscribe(config => {
      if (config != null) {
        this.config.postCreateJobTemplate(config).subscribe({
          next: value => {
            this._snackBar.open(`Successfully created job template.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig);
            this.reload.next()
          },
          error: err => this._snackBar.open(`Error occurred while trying to create job template: ${err?.error?.description}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig),
          complete: () => {}
        } as Observer<JobTemplate>)
      }
    })
  }

  /**
   * Opens a dialog to create a new {@link EntityMapping}.
   */
  public addEntityMapping() {
    this._dialog.open(AddEntityMappingDialogComponent).afterClosed().subscribe(config => {
      if (config != null) {
        this.config.postCreateEntityMapping(config).subscribe({
          next: value => {
            this._snackBar.open(`Successfully created entity mapping.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig);
            this.reload.next()
          },
          error: err => this._snackBar.open(`Error occurred while trying to create entity mapping: ${err?.error?.description}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig),
          complete: () => {}
        } as Observer<EntityMapping>)
      }
    })
  }

  /**
   * Opens a dialog to create a new {@link EntityMapping}.
   */
  public addSolrConfig() {
    this._dialog.open(AddSolrConfigDialogComponent).afterClosed().subscribe(config => {
      if (config != null) {
        this.config.postCreateSolrConfig(config).subscribe({
          next: value => {
            this._snackBar.open(`Successfully created Apache Solr configuration.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig);
            this.reload.next()
          },
          error: err => this._snackBar.open(`Error occurred while trying to create Apache Solr config: ${err?.error?.description}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig),
          complete: () => {}
        } as Observer<ApacheSolrConfig>)
      }
    })
  }

  /**
   * Opens a dialog to create a new {@link EntityMapping}.
   */
  public addParticipant() {
    this._dialog.open(AddParticipantDialogComponent).afterClosed().subscribe(participant => {
      if (participant != null) {
        this.config.postCreateParticipant(participant).subscribe({
          next: value => {
            this._snackBar.open(`Successfully created participant.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig);
            this.reload.next()
          },
          error: err => this._snackBar.open(`Error occurred while trying to create participant: ${err?.error?.description}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig),
          complete: () => {}
        })
      }
    })
  }
}