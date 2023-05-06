import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import {ApiModule, Configuration} from "../../openapi";


/**
 * Provides the {@link AppConfig} reference.
 *
 * @param appConfig Reference (provided by DI).
 */
export function initializeApiConfig() {
  return new Configuration({ basePath: "http://localhost:7070", withCredentials: true }); /* TODO: Change. */
}

@NgModule({
  imports: [
    {
      ngModule: ApiModule,
      providers: [{ provide: Configuration, useFactory: initializeApiConfig }],
    },
    BrowserModule,
    AppRoutingModule
  ],
  declarations: [
    AppComponent
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
