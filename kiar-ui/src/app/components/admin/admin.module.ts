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
import {RouterLink} from "@angular/router";
import {EntityMappingComponent} from "./mapping/entity-mapping.component";
import {MatTableModule} from "@angular/material/table";
import {MatToolbarModule} from "@angular/material/toolbar";
import {AttributeMappingDialogComponent} from "./mapping/attribute-mapping-dialog.component";
import {MatDialogModule} from "@angular/material/dialog";
import {MatSelectModule} from "@angular/material/select";
import {MatCheckboxModule} from "@angular/material/checkbox";
import {AddEntityMappingDialogComponent} from "./dashboard/add-entity-mapping-dialog.component";
import {MatTooltipModule} from "@angular/material/tooltip";
import {AddSolrConfigDialogComponent} from "./dashboard/add-solr-config.dialog.component";
import {AddJobTemplateDialogComponent} from "./dashboard/add-job-template-dialog.component";
import {AddParticipantDialogComponent} from "./dashboard/add-participant-dialog.component";

@NgModule({
  declarations: [
    AddEntityMappingDialogComponent,
    AddJobTemplateDialogComponent,
    AddParticipantDialogComponent,
    AddSolrConfigDialogComponent,
    AdminDashboardComponent,
    AttributeMappingDialogComponent,
    EntityMappingComponent,
  ],
  imports: [
    CommonModule,
    BrowserModule,
    FormsModule,
    ReactiveFormsModule,
    RouterLink,

    MatButtonModule,
    MatCardModule,
    MatDialogModule,
    MatInputModule,
    MatSnackBarModule,
    MatListModule,
    MatIconModule,
    MatTableModule,
    MatToolbarModule,
    MatSelectModule,
    MatCheckboxModule,
    MatTooltipModule
  ],
  exports: [
    AdminDashboardComponent,
    EntityMappingComponent
  ],
  providers: [
      AttributeMappingDialogComponent
  ]
})
export class AdminModule {}