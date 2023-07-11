import {NgModule} from "@angular/core";
import {InstitutionListComponent} from "./institution-list.component";
import {CommonModule} from "@angular/common";
import {BrowserModule} from "@angular/platform-browser";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {RouterLink} from "@angular/router";
import {MatTableModule} from "@angular/material/table";
@NgModule({
  declarations: [
    InstitutionListComponent
  ],
  imports: [
    CommonModule,
    BrowserModule,
    FormsModule,
    ReactiveFormsModule,
    RouterLink,
    MatTableModule
  ],
  exports: [
    InstitutionListComponent
  ],
  providers: []
})
export class InstitutionModule {}