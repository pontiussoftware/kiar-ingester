import {AfterViewInit, Component, ViewChild} from "@angular/core";
import {ConfigService, User, UserService} from "../../../../openapi";
import {Observable, tap} from "rxjs";
import {MatPaginator} from "@angular/material/paginator";
import {MatSort} from "@angular/material/sort";
import {MatSnackBar, MatSnackBarConfig} from "@angular/material/snack-bar";
import {MatDialog} from "@angular/material/dialog";
import {UserDataSource} from "./user-datasource";
import {UserDialogComponent} from "./user-dialog.component";

@Component({
    selector: 'kiar-user-list',
    templateUrl: './user-list.component.html',
    styleUrls: ['./user-list.component.scss'],
    standalone: false
})
export class UserListComponent implements AfterViewInit  {

  /** {@link Observable} of all available participants. */
  public readonly dataSource: UserDataSource

  /** An {@link Observable} of available participants. */
  public readonly collections: Observable<Array<string[]>>

  /** The columns that should be displayed in the data table. */
  public readonly displayedColumns: string[] = ['username', 'email', 'role', 'institution', 'active', 'action'];

  /** Reference to the {@link MatPaginator}*/
  @ViewChild(MatPaginator) paginator: MatPaginator;

  /** Reference to the {@link MatSort}*/
  @ViewChild(MatSort) sort: MatSort;

  constructor(private user: UserService, private config: ConfigService, private dialog: MatDialog, private snackBar: MatSnackBar) {
    this.dataSource = new UserDataSource(this.user)
  }

  /**
   * Registers an observable for page change.
   */
  public ngAfterViewInit() {
    this.sort.direction = 'asc'
    this.dataSource.load(0, 15, this.sort.active, this.sort.direction);
    this.paginator.page.pipe(tap(() => this.dataSource.load(this.paginator.pageIndex, this.paginator.pageSize, this.sort.active, this.sort.direction))).subscribe();
    this.sort.sortChange.pipe(tap(() => this.dataSource.load(this.paginator.pageIndex, this.paginator.pageSize, this.sort.active, this.sort.direction))).subscribe();
  }

  /**
   * Opens a dialog to add a new {@link User} to the collection and persists it through the API upon saving.
   */
  public add() {
    this.dialog.open(UserDialogComponent).afterClosed().subscribe(user => {
      if (user != null) {
        this.user.postCreateUser(user).subscribe({
          next: (value) => {
            this.snackBar.open(value.description, "Dismiss", { duration: 2000 } as MatSnackBarConfig);
            this.dataSource.load(this.paginator.pageIndex, this.paginator.pageSize, this.sort.active, this.sort.direction);
          },
          error: (err) => this.snackBar.open(`Error occurred while trying to create user: ${err?.error?.description}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig),
        })
      }
    })
  }

  /**
   * Opens a dialog to edit an existing {@link User} to the collection and persists it through the API upon saving.
   */
  public edit(user: User) {
    this.dialog.open(UserDialogComponent, {data: user}).afterClosed().subscribe(ret => {
      if (ret != null) {
        this.user.putUpdateUser(ret.id!!, ret).subscribe({
          next: (value) => {
            this.snackBar.open(value.description, "Dismiss", { duration: 2000 } as MatSnackBarConfig);
            this.dataSource.load(this.paginator.pageIndex, this.paginator.pageSize, this.sort.active, this.sort.direction);
          },
          error: (err) => this.snackBar.open(`Error occurred while trying to update user: ${err?.error?.description}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig),
        })
      }
    })
  }

  /**
   * Opens a dialog to add a new {@link User} to the collection and persists it through the API upon saving.
   */
  public delete(user: User) {
    if (confirm(`Are you sure that you want to delete user '${user.id}'?\nAfter deletion, it can no longer be retrieved.`)) {
      this.user.deleteUser(user.id!!).subscribe({
        next: (value) => {
          this.snackBar.open(value.description, "Dismiss", { duration: 2000 } as MatSnackBarConfig);
          this.dataSource.load(this.paginator.pageIndex, this.paginator.pageSize, this.sort.active, this.sort.direction);
        },
        error: (err) => this.snackBar.open(`Error occurred while trying to delete user '${user.username}': ${err?.error?.description}.`, "Dismiss", { duration: 2000 } as MatSnackBarConfig),
      })
    }
  }
}