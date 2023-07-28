import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';
import {CommonModule} from "@angular/common";
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import {ApiModule, Configuration} from "../../openapi";
import {MatButtonModule} from "@angular/material/button";
import {SessionModule} from "./components/session/session.module";
import {HttpClientModule} from "@angular/common/http";
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {MatToolbarModule} from "@angular/material/toolbar";
import {MatIconModule} from "@angular/material/icon";
import {ServiceModule} from "./services/service.module";
import {MatMenuModule} from "@angular/material/menu";
import {DashboardModule} from "./components/dashboard/dashboard.module";
import {AdminModule} from "./components/admin/admin.module";
import {InstitutionModule} from "./components/institution/institution.module";
import {MatLegacyTooltipModule} from "@angular/material/legacy-tooltip";


/**
 * Provides the {@link AppConfig} reference.
 *
 * @param appConfig Reference (provided by DI).
 */
export function initializeApiConfig() {
  return new Configuration({ basePath: window.location.origin, withCredentials: true }); /* TODO: Change. */
}

@NgModule({
  imports: [
    {
      ngModule: ApiModule,
      providers: [{provide: Configuration, useFactory: initializeApiConfig}],
    },
    AppRoutingModule,
    BrowserAnimationsModule,
    BrowserModule,
    CommonModule,
    HttpClientModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatToolbarModule,

    /* Own modules. */
    AdminModule,
    InstitutionModule,
    DashboardModule,
    SessionModule,
    ServiceModule,
    MatLegacyTooltipModule
  ],
  declarations: [
    AppComponent
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
