import {AfterViewInit, Component, inject, OnInit, viewChild} from "@angular/core";
import {toSignal} from "@angular/core/rxjs-interop";
import {JobService} from "../../../../../openapi";
import {JobLogDatasource} from "./job-log-datasource";
import {map, tap} from "rxjs";
import {ActivatedRoute} from "@angular/router";
import {MatPaginator} from "@angular/material/paginator";

@Component({
    selector: 'kiar-job-log',
    templateUrl: 'job-log.component.html',
    standalone: false
})
export class JobLogComponent implements AfterViewInit, OnInit {
  /** The {@link JobService} used to access and manage jobs. */
  private service = inject(JobService);

  /** The {@link ActivatedRoute} used to read route parameters. */
  private route = inject(ActivatedRoute);

  /** The {@link JobLogDatasource} backing this {@link JobLogComponent}. */
  public dataSource: JobLogDatasource

  /** The columns displayed in the data table. */
  public readonly displayedColumns= ["documentId", "collection", "level", "context", "description"];

  /** Value for the level filter. */
  public levelFilter = 'ALL'

  /** Value for the context filter. */
  public contextFilter = 'ALL'

  /** A signal of the current jobId. */
  public readonly jobId = toSignal(this.route.paramMap.pipe(map(p => p.get('id')!!)))

  /** Reference to the {@link MatPaginator}*/
  protected readonly paginator = viewChild.required(MatPaginator);

  /**
   * Initializes the data source and load the data.
   */
  public ngOnInit() {
    this.dataSource = new JobLogDatasource(this.service, Number(this.route.snapshot.paramMap.get("id")!!))
  }

  /**
   * Registers an observable for page change.
   */
  public ngAfterViewInit() {
    this.reload()
    this.paginator().page.pipe(tap(() => this.reload())).subscribe();
  }

  /**
   * Reloads the data using the current settings.
   */
  public reload() {
    const actualLevel = this.levelFilter === 'ALL' ? undefined : this.levelFilter
    const actualContext = this.contextFilter === 'ALL' ? undefined : this.contextFilter
    this.dataSource.load(this.paginator().pageIndex, this.paginator().pageSize, actualLevel, actualContext);
  }
}