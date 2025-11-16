import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';
import {CommonModule} from "@angular/common";
import {AppRoutingModule} from './app-routing.module';
import {AppComponent} from './app.component';
import {ApiModule, Configuration} from "../../openapi";
import {MatButtonModule} from "@angular/material/button";
import {SessionModule} from "./components/session/session.module";
import {provideHttpClient, withInterceptorsFromDi} from "@angular/common/http";
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {MatToolbarModule} from "@angular/material/toolbar";
import {MatIconModule} from "@angular/material/icon";
import {ServiceModule} from "./services/service.module";
import {MatMenuModule} from "@angular/material/menu";
import {DashboardModule} from "./components/dashboard/dashboard.module";
import {AdminModule} from "./components/admin/admin.module";
import {InstitutionModule} from "./components/institution/institution.module";
import {MatTooltipModule} from "@angular/material/tooltip";
import {UserModule} from "./components/user/user.module";
import {CollectionModule} from "./components/collection/collection.module";

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
        InstitutionModule,
        CollectionModule,
        UserModule,
        DashboardModule,
        SessionModule,
        ServiceModule], providers: [provideHttpClient(withInterceptorsFromDi())] })
export class AppModule { }
