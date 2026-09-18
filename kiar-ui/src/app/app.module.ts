import {inject, NgModule, provideAppInitializer, provideCheckNoChangesConfig} from '@angular/core';
import {environment} from '../environments/environment';
import {BrowserModule} from '@angular/platform-browser';
import {CommonModule, registerLocaleData} from "@angular/common";
import localeDeCH from '@angular/common/locales/de-CH';
import localeEnGB from '@angular/common/locales/en-GB';
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
import {MatPaginatorIntl} from "@angular/material/paginator";
import {provideTranslateService, TranslatePipe} from "@ngx-translate/core";
import {provideTranslateHttpLoader} from "@ngx-translate/http-loader";
import {LanguageService} from "./services/language.service";
import {TranslatedPaginatorIntl} from "./services/translated-paginator-intl";

/* Locale data for date and number formatting in the supported languages. */
registerLocaleData(localeDeCH);
registerLocaleData(localeEnGB);

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
    TranslatePipe,
    /* Own modules. */
    AdminModule,
    ServiceModule], providers: [
        provideHttpClient(withInterceptorsFromDi()),
        /* Translations are loaded from assets/i18n/<lang>.json; English is the fallback for missing keys. */
        provideTranslateService({
            loader: provideTranslateHttpLoader({prefix: './assets/i18n/', suffix: '.json'}),
            fallbackLang: 'en'
        }),
        /* Load the translation file for the detected language before the first component renders. */
        provideAppInitializer(() => inject(LanguageService).initialize()),
        {provide: MatPaginatorIntl, useClass: TranslatedPaginatorIntl},
        /* Development only: periodically verify that no template expression changed without Angular being notified (zoneless safety net). */
        ...(environment.production ? [] : [provideCheckNoChangesConfig({exhaustive: true, interval: 1000})])
    ] })
export class AppModule { }
