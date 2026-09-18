import {AfterViewInit, Component, inject} from "@angular/core";
import {toSignal} from "@angular/core/rxjs-interop";
import {ApacheSolrConfig, ConfigService, EntityMapping, JobTemplate} from "../../../../../openapi";
import {mergeMap, Observer, Subject} from "rxjs";
import {MatDialog} from "@angular/material/dialog";
import {AddEntityMappingDialogComponent} from "./add-entity-mapping-dialog.component";
import {MatSnackBar, MatSnackBarConfig} from "@angular/material/snack-bar";
import {AddSolrConfigDialogComponent} from "./add-solr-config.dialog.component";
import {AddJobTemplateDialogComponent} from "./add-job-template-dialog.component";
import {AddParticipantDialogComponent} from "./add-participant-dialog.component";
import {MatCard, MatCardContent, MatCardHeader, MatCardTitle} from "@angular/material/card";
import {MatMiniFabButton} from "@angular/material/button";
import {MatTooltip} from "@angular/material/tooltip";
import {MatIcon} from "@angular/material/icon";
import {MatList, MatListItem, MatListItemLine, MatListItemTitle} from "@angular/material/list";
import {RouterLink} from "@angular/router";

@Component({
    selector: 'kiar-admin-dashboard',
    templateUrl: './admin-dashboard.component.html',
    styleUrls: ['./admin-dashboard.component.scss'],
    imports: [MatCard, MatCardHeader, MatCardTitle, MatMiniFabButton, MatTooltip, MatIcon, MatCardContent, MatList, MatListItem, RouterLink, MatListItemTitle, MatListItemLine]
})
export class AdminDashboardComponent implements AfterViewInit {
  /** The {@link ConfigService} used to access application configuration (templates, mappings, Solr configurations, participants). */
  private config = inject(ConfigService);

  /** The {@link MatDialog} service used to open dialogs. */
  private _dialog = inject(MatDialog);

  /** The {@link MatSnackBar} used to display notifications. */
  private _snackBar = inject(MatSnackBar);

  /** A {@link Subject} that can be used to trigger a data reload. */
  private reload = new Subject<void>()

  /** A signal of the available {@link JobTemplate}s. */
  public readonly templates = toSignal(this.reload.pipe(mergeMap(() => this.config.getListJobTemplates())), {initialValue: [] as Array<JobTemplate>})

  /** A signal of the available {@link EntityMapping}s. */
  public readonly mappings = toSignal(this.reload.pipe(mergeMap(() => this.config.getListEntityMappings())), {initialValue: [] as Array<EntityMapping>})

  /** A signal of the available {@link ApacheSolrConfig}s. */
  public readonly solr = toSignal(this.reload.pipe(mergeMap(() => this.config.getListSolrConfiguration())), {initialValue: [] as Array<ApacheSolrConfig>})

  /** A signal of the available participant names. */
  public readonly participant = toSignal(this.reload.pipe(mergeMap(() => this.config.getListParticipants())), {initialValue: [] as Array<string>})

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