import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {LoginComponent} from "./components/session/login/login.component";
import {canActivateAdministrator, canActivateViewer} from "./services/guards";
import {DashboardComponent} from "./components/dashboard/dashboard.component";
import {ForbiddenComponent} from "./components/session/forbidden/forbidden.component";
import {AdminDashboardComponent} from "./components/admin/dashboard/admin-dashboard.component";
import {EntityMappingComponent} from "./components/admin/mapping/entity-mapping.component";

const routes: Routes = [

  /* Login & forbidden page are the only page that don't need authentication. */
  { path: 'login', component: LoginComponent },
  { path: 'forbidden', component: ForbiddenComponent },

  /* Now come all the routes that require authentication. */
  { path: 'admin/dashboard', component: AdminDashboardComponent, canActivate: [canActivateAdministrator] },
  { path: 'admin/mapping/:id', component: EntityMappingComponent, canActivate: [canActivateAdministrator] },

  /* Now come all the routes that require authentication. */
  { path: 'manager/dashboard', component: DashboardComponent, canActivate: [canActivateViewer] },

  // otherwise redirect to home
  { path: '', redirectTo: 'manager/dashboard', pathMatch: 'full' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
