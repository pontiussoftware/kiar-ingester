import {enableProdMode} from '@angular/core';
import {platformBrowserDynamic} from '@angular/platform-browser-dynamic';

import {AppModule} from './app/app.module';
import {environment} from './environments/environment';

if (environment.production) {
  enableProdMode();
}

/* Zoneless change detection is the default in Angular 21; no zone.js and no zone provider required. */
platformBrowserDynamic().bootstrapModule(AppModule)
  .catch(err => console.error(err));
