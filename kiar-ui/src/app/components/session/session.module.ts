import {NgModule} from "@angular/core";
import {MatCardModule} from "@angular/material/card";
import {MatInputModule} from "@angular/material/input";
import {MatButtonModule} from "@angular/material/button";
import {LoginComponent} from "./login/login.component";
import {BrowserModule} from "@angular/platform-browser";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {MatSnackBarModule} from "@angular/material/snack-bar";
import {ForbiddenComponent} from "./forbidden/forbidden.component";

@NgModule({
  declarations: [
      LoginComponent,
    ForbiddenComponent
  ],
  imports: [
      BrowserModule,
      FormsModule,
      MatButtonModule,
      MatCardModule,
      MatInputModule,
      MatSnackBarModule,
      ReactiveFormsModule,
  ],
  exports: [
      LoginComponent,
      ForbiddenComponent
  ]
})
export class SessionModule {}