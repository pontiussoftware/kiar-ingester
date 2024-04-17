import {AfterViewInit, Component, OnInit, ViewChild} from "@angular/core";
import {JobService} from "../../../../../openapi";
import {JobLogDatasource} from "./job-log-datasource";
import {map, Observable, tap} from "rxjs";
import {ActivatedRoute} from "@angular/router";
import {MatPaginator} from "@angular/material/paginator";

@Component({
  selector: 'kiar-job-log',
  templateUrl: 'job-log.component.html',
})
export class JobLogComponent implements AfterViewInit, OnInit {


  /** The {@link JobLogDatasource} backing this {@link JobLogComponent}. */
  public dataSource: JobLogDatasource

  /** The columns displayed in the data table. */
  public readonly displayedColumns= ["documentId", "collection", "level", "context", "description"];

  /** Value for the level filter. */
  public levelFilter = 'ALL'

  /** Value for the context filter. */
  public contextFilter = 'ALL'

  /** Observable holding the currently active job ID. */
  public readonly jobId: Observable<string>

  /** Reference to the {@link MatPaginator}*/
  @ViewChild(MatPaginator) paginator: MatPaginator;

  constructor(private service: JobService, private route: ActivatedRoute) {
    this.jobId = this.route.paramMap.pipe(map(p => p.get('id')!!))
  }

  /**
   * Initializes the data source and load the data.
   */
  public ngOnInit() {
    this.dataSource = new JobLogDatasource(this.service, this.route.snapshot.paramMap.get("id")!!)
  }

  /**
   * Registers an observable for page change.
   */
  public ngAfterViewInit() {
    this.reload()
    this.paginator.page.pipe(tap(() => this.reload())).subscribe();
  }

  /**
   * Reloads the data using the current settings.
   */
  public reload() {
    const actualLevel = this.levelFilter === 'ALL' ? undefined : this.levelFilter
    const actualContext = this.contextFilter === 'ALL' ? undefined : this.contextFilter
    this.dataSource.load(this.paginator.pageIndex, this.paginator.pageSize, actualLevel, actualContext);
  }
}