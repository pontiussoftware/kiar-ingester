import {AfterViewInit, Component, inject, viewChild} from "@angular/core";
import {TranslatePipe, TranslateService} from "@ngx-translate/core";
import {ConfigService, User, UserService} from "../../../../openapi";
import {tap} from "rxjs";
import {MatPaginator} from "@angular/material/paginator";
import {MatSort, MatSortHeader} from "@angular/material/sort";
import {MatSnackBar, MatSnackBarConfig} from "@angular/material/snack-bar";
import {MatDialog} from "@angular/material/dialog";
import {UserDataSource} from "./user-datasource";
import {UserDialogComponent} from "./user-dialog.component";
import {MatIconButton, MatMiniFabButton} from "@angular/material/button";
import {MatTooltip} from "@angular/material/tooltip";
import {MatIcon} from "@angular/material/icon";
import {
  MatCell,
  MatCellDef,
  MatColumnDef,
  MatHeaderCell,
  MatHeaderCellDef,
  MatHeaderRow,
  MatHeaderRowDef,
  MatRow,
  MatRowDef,
  MatTable
} from "@angular/material/table";

@Component({
    selector: 'kiar-user-list',
    templateUrl: './user-list.component.html',
    styleUrls: ['./user-list.component.scss'],
    imports: [MatMiniFabButton, MatTooltip, MatIcon, MatTable, MatSort, MatColumnDef, MatHeaderCellDef, MatHeaderCell, MatSortHeader, MatCellDef, MatCell, MatIconButton, MatHeaderRowDef, MatHeaderRow, MatRowDef, MatRow, MatPaginator, TranslatePipe]
})
export class UserListComponent implements AfterViewInit  {
  /** The {@link UserService} used to access user data. */
  private user = inject(UserService);

  /** The {@link ConfigService} used to access application configuration (templates, mappings, Solr configurations, participants). */
  private config = inject(ConfigService);

  /** The {@link MatDialog} service used to open dialogs. */
  private dialog = inject(MatDialog);

  /** The {@link MatSnackBar} used to display notifications. */
  private snackBar = inject(MatSnackBar);

  /** The {@link TranslateService} used to resolve user-facing messages. */
  private translate = inject(TranslateService);

  /** {@link Observable} of all available participants. */
  public readonly dataSource: UserDataSource

  /** The columns that should be displayed in the data table. */
  public readonly displayedColumns: string[] = ['username', 'email', 'role', 'institution', 'active', 'action'];

  /** Reference to the {@link MatPaginator}*/
  public readonly paginator = viewChild.required(MatPaginator);

  /** Reference to the {@link MatSort}*/
  public readonly sort = viewChild.required(MatSort);

  constructor() {
    this.dataSource = new UserDataSource(this.user)
  }

  /**
   * Registers an observable for page change.
   */
  public ngAfterViewInit() {
    const sort = this.sort()
    sort.direction = 'asc'
    this.dataSource.load(0, 15, sort.active, sort.direction);
    this.paginator().page.pipe(tap(() => this.dataSource.load(this.paginator().pageIndex, this.paginator().pageSize, this.sort().active, this.sort().direction))).subscribe();
    sort.sortChange.pipe(tap(() => this.dataSource.load(this.paginator().pageIndex, this.paginator().pageSize, this.sort().active, this.sort().direction))).subscribe();
  }

  /**
   * Opens a dialog to add a new {@link User} to the collection and persists it through the API upon saving.
   */
  public add() {
    this.dialog.open(UserDialogComponent).afterClosed().subscribe(user => {
      if (user != null) {
        this.user.postCreateUser(user).subscribe({
          next: (value) => {
            this.snackBar.open(value.description, this.translate.instant('common.action.dismiss'), { duration: 2000 } as MatSnackBarConfig);
            this.dataSource.load(this.paginator().pageIndex, this.paginator().pageSize, this.sort().active, this.sort().direction);
          },
          error: (err) => this.snackBar.open(this.translate.instant('user.list.messages.createFailed', {error: err?.error?.description}), this.translate.instant('common.action.dismiss'), { duration: 2000 } as MatSnackBarConfig),
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
            this.snackBar.open(value.description, this.translate.instant('common.action.dismiss'), { duration: 2000 } as MatSnackBarConfig);
            this.dataSource.load(this.paginator().pageIndex, this.paginator().pageSize, this.sort().active, this.sort().direction);
          },
          error: (err) => this.snackBar.open(this.translate.instant('user.list.messages.updateFailed', {error: err?.error?.description}), this.translate.instant('common.action.dismiss'), { duration: 2000 } as MatSnackBarConfig),
        })
      }
    })
  }

  /**
   * Opens a dialog to add a new {@link User} to the collection and persists it through the API upon saving.
   */
  public delete(user: User) {
    if (confirm(this.translate.instant('user.list.confirmDelete', {id: user.id}) + '\n' + this.translate.instant('common.confirmDeleteSuffix'))) {
      this.user.deleteUser(user.id!!).subscribe({
        next: (value) => {
          this.snackBar.open(value.description, this.translate.instant('common.action.dismiss'), { duration: 2000 } as MatSnackBarConfig);
          this.dataSource.load(this.paginator().pageIndex, this.paginator().pageSize, this.sort().active, this.sort().direction);
        },
        error: (err) => this.snackBar.open(this.translate.instant('user.list.messages.deleteFailed', {username: user.username, error: err?.error?.description}), this.translate.instant('common.action.dismiss'), { duration: 2000 } as MatSnackBarConfig),
      })
    }
  }
}