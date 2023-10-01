import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {LoginComponent} from "./components/session/login/login.component";
import {canActivateAdministrator, canActivateEverybody, canActivateViewer} from "./services/guards";
import {DashboardComponent} from "./components/dashboard/dashboard.component";
import {ForbiddenComponent} from "./components/session/forbidden/forbidden.component";
import {AdminDashboardComponent} from "./components/admin/dashboard/admin-dashboard.component";
import {EntityMappingComponent} from "./components/admin/mapping/entity-mapping.component";
import {ProfileComponent} from "./components/session/user/profile.component";
import {ApacheSolrComponent} from "./components/admin/solr/apache-solr.component";
import {JobTemplateComponent} from "./components/admin/template/job-template.component";
import {InstitutionListComponent} from "./components/institution/institution-list.component";
import {JobLogComponent} from "./components/dashboard/logs/job-log.component";
import {UserListComponent} from "./components/user/user-list.component";

const routes: Routes = [

  /* Login & forbidden page are the only page that don't need authentication. */
  { path: 'login', component: LoginComponent },
  { path: 'forbidden', component: ForbiddenComponent },

  /* Some pages can be activated by everybody that is logged in. */
  { path: 'profile', component: ProfileComponent, canActivate: [canActivateEverybody] },

  /* Some pages can be activated by everybody that is logged in. */
  { path: 'institutions', component: InstitutionListComponent, canActivate: [canActivateAdministrator] },

  /* Some pages can be activated by everybody that is logged in. */
  { path: 'users', component: UserListComponent, canActivate: [canActivateAdministrator] },

  /* Now come all the routes that require authentication as an ADMINISTRATOR */
  { path: 'admin/dashboard', component: AdminDashboardComponent, canActivate: [canActivateAdministrator] },
  { path: 'admin/mapping/:id', component: EntityMappingComponent, canActivate: [canActivateAdministrator] },
  { path: 'admin/solr/:id', component: ApacheSolrComponent, canActivate: [canActivateAdministrator] },
  { path: 'admin/template/:id', component: JobTemplateComponent, canActivate: [canActivateAdministrator] },

  /* Now come all the routes that require authentication as a VIEWER, MANAGER or ADMINISTRATOR */
  { path: 'manager/dashboard', component: DashboardComponent, canActivate: [canActivateViewer] },
  { path: 'manager/logs/:id', component: JobLogComponent, canActivate: [canActivateViewer] },

  // otherwise redirect to home
  { path: '', redirectTo: 'manager/dashboard', pathMatch: 'full' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
