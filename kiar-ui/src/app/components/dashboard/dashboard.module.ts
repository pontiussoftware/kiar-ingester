import {NgModule} from "@angular/core";
import {BrowserModule} from "@angular/platform-browser";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {MatButtonModule} from "@angular/material/button";
import {MatCardModule} from "@angular/material/card";
import {MatInputModule} from "@angular/material/input";
import {MatSnackBarModule} from "@angular/material/snack-bar";
import {DashboardComponent} from "./dashboard.component";
import {KiarUploadComponent} from "./job/kiar-upload.component";
import {MatTabsModule} from "@angular/material/tabs";

@NgModule({
  declarations: [
    DashboardComponent,
    KiarUploadComponent
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
  ],
  exports: [
    DashboardComponent
  ]
})
export class DashboardModule {}