import {NgModule} from "@angular/core";
import {BrowserModule} from "@angular/platform-browser";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {MatButtonModule} from "@angular/material/button";
import {MatCardModule} from "@angular/material/card";
import {MatInputModule} from "@angular/material/input";
import {MatSnackBarModule} from "@angular/material/snack-bar";
import {AdminDashboardComponent} from "./dashboard/admin-dashboard.component";
import {MatListModule} from "@angular/material/list";
import {CommonModule} from "@angular/common";
import {MatIconModule} from "@angular/material/icon";

@NgModule({
  declarations: [
      AdminDashboardComponent
  ],
  imports: [
    CommonModule,
    BrowserModule,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatInputModule,
    MatSnackBarModule,
    ReactiveFormsModule,
    MatListModule,
    MatIconModule,
  ],
  exports: [
    AdminDashboardComponent
  ]
})
export class AdminModule {}