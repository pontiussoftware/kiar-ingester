import {ActivatedRouteSnapshot, CanActivateFn, RouterStateSnapshot} from "@angular/router";
import {inject} from "@angular/core";
import {AuthenticationService} from "./authentication.service";
import {Role} from "../../../openapi";


/**
 * Guard used to determine if the dashboard can be activaed.
 */
export const canActivateDashboard: CanActivateFn = (route: ActivatedRouteSnapshot, state: RouterStateSnapshot) => {
  return inject(AuthenticationService).canActivate([Role.ADMINISTRATOR, Role.MANAGER, Role.VIEWER], route, state);
};