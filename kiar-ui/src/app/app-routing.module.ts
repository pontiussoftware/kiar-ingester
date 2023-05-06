import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {LoginComponent} from "./components/session/login/login.component";
import {canActivateDashboard} from "./services/guards";
import {DashboardComponent} from "./components/dashboard/dashboard.component";
import {ForbiddenComponent} from "./components/session/forbidden/forbidden.component";

const routes: Routes = [

  /* Login & forbidden page are the only page that don't need authentication. */
  { path: 'login', component: LoginComponent },
  { path: 'forbidden', component: ForbiddenComponent },

  /* Now come all the routes that require authentication. */
  { path: 'dashboard', component: DashboardComponent, canActivate: [canActivateDashboard] },

  // otherwise redirect to home
  { path: '**', redirectTo: 'dashboard' },
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
