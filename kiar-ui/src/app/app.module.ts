import {NgModule, provideCheckNoChangesConfig} from '@angular/core';
import {environment} from '../environments/environment';
import {BrowserModule} from '@angular/platform-browser';
import {CommonModule} from "@angular/common";
import {AppRoutingModule} from './app-routing.module';
import {AppComponent} from './app.component';
import {ApiModule, Configuration} from "../../openapi";
import {MatButtonModule} from "@angular/material/button";

import {provideHttpClient, withInterceptorsFromDi} from "@angular/common/http";
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {MatToolbarModule} from "@angular/material/toolbar";
import {MatIconModule} from "@angular/material/icon";
import {ServiceModule} from "./services/service.module";
import {MatMenuModule} from "@angular/material/menu";

import {AdminModule} from "./components/admin/admin.module";

import {MatTooltipModule} from "@angular/material/tooltip";


/**
 * Provides the {@link AppConfig} reference.
 *
 * @param appConfig Reference (provided by DI).
 */
export function initializeApiConfig() {
  return new Configuration({ basePath: window.location.origin, withCredentials: true }); /* TODO: Change. */
}

@NgModule({ declarations: [
        AppComponent
    ],
    bootstrap: [AppComponent], imports: [{
        ngModule: ApiModule,
        providers: [{ provide: Configuration, useFactory: initializeApiConfig }],
    },
    AppRoutingModule,
    BrowserAnimationsModule,
    BrowserModule,
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatToolbarModule,
    MatTooltipModule,
    /* Own modules. */
    AdminModule,
    ServiceModule], providers: [
        provideHttpClient(withInterceptorsFromDi()),
        /* Development only: periodically verify that no template expression changed without Angular being notified (zoneless safety net). */
        ...(environment.production ? [] : [provideCheckNoChangesConfig({exhaustive: true, interval: 1000})])
    ] })
export class AppModule { }
