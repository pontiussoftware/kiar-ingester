import {AfterViewInit, Component, OnInit, ViewChild} from "@angular/core";
import {JobService} from "../../../../../openapi";
import {JobLogDatasource} from "./job-log-datasource";
import {asapScheduler, map, Observable, tap} from "rxjs";
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
    this.dataSource.load(this.paginator.pageIndex, this.paginator.pageSize);
    this.paginator.page.pipe(tap(() => this.dataSource.load(this.paginator.pageIndex, this.paginator.pageSize))).subscribe();
  }

  protected readonly asapScheduler = asapScheduler;
}