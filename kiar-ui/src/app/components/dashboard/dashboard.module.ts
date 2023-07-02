import {NgModule} from "@angular/core";
import {BrowserModule} from "@angular/platform-browser";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {MatButtonModule} from "@angular/material/button";
import {MatCardModule} from "@angular/material/card";
import {MatInputModule} from "@angular/material/input";
import {MatSnackBarModule} from "@angular/material/snack-bar";
import {DashboardComponent} from "./dashboard.component";
import {MatTabsModule} from "@angular/material/tabs";
import {MatTableModule} from "@angular/material/table";

@NgModule({
  declarations: [
    DashboardComponent
  ],
  imports: [
    BrowserModule,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatInputModule,
    MatSnackBarModule,
    ReactiveFormsModule,
    MatTabsModule,
    MatTableModule,
  ],
  exports: [
    DashboardComponent
  ]
})
export class DashboardModule {}