import {AfterViewInit, Component, OnInit, ViewChild} from "@angular/core";
import {InstitutionService} from "../../../../openapi";
import {Observable, tap} from "rxjs";
import {MatPaginator} from "@angular/material/paginator";
import {InstitutionDatasource} from "./institution-datasource";
import {MatSort} from "@angular/material/sort";

@Component({
  selector: 'kiar-institution-list',
  templateUrl: './institution-list.component.html',
  styleUrls: ['./institution-list.component.scss']
})
export class InstitutionListComponent implements AfterViewInit, OnInit  {

  /** {@link Observable} of all available participants. */
  public readonly dataSource: InstitutionDatasource

  /** The columns that should be displayed in the data table. */
  public readonly displayedColumns: string[] = ['name', 'displayName', 'participant', 'city', 'canton', 'email'];

  /** Reference to the {@link MatPaginator}*/
  @ViewChild(MatPaginator) paginator: MatPaginator;

  /** Reference to the {@link MatSort}*/
  @ViewChild(MatSort) sort: MatSort;

  constructor(service: InstitutionService) {
    this.dataSource = new InstitutionDatasource(service)
  }

  /**
   * Initializes the data source and load the data.
   */
  public ngOnInit() {
    this.dataSource.load(0, 15, 'name', 'asc');
  }

  /**
   * Registers an observable for page change.
   */
  public ngAfterViewInit() {
    this.paginator.page.pipe(tap(() => this.dataSource.load(this.paginator.pageIndex, this.paginator.pageSize, this.sort.active, this.sort.direction))).subscribe();
    this.sort.sortChange.pipe(tap(() => this.dataSource.load(this.paginator.pageIndex, this.paginator.pageSize, this.sort.active, this.sort.direction))).subscribe();
  }

  /**
   *
   */
  public createInstitution() {

  }
}