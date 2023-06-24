import {ActivatedRouteSnapshot, CanActivateFn, RouterStateSnapshot} from "@angular/router";
import {inject} from "@angular/core";
import {AuthenticationService} from "./authentication.service";
import {Role} from "../../../openapi";


/**
 * Guard used to determine if the dashboard can be activaed; restricted to administrators.
 */
export const canActivateAdministrator: CanActivateFn = (route: ActivatedRouteSnapshot, state: RouterStateSnapshot) => {
  return inject(AuthenticationService).canActivate([Role.ADMINISTRATOR], route, state);
};

/**
 * Guard used to determine if the dashboard can be activated; restricted to administrators and managers.
 */
export const canActivateManager: CanActivateFn = (route: ActivatedRouteSnapshot, state: RouterStateSnapshot) => {
  return inject(AuthenticationService).canActivate([Role.ADMINISTRATOR, Role.MANAGER], route, state);
};

/**
 * Guard used to determine if the dashboard can be activated.; restricted to administrators, managers and viewers.
 */
export const canActivateViewer: CanActivateFn = (route: ActivatedRouteSnapshot, state: RouterStateSnapshot) => {
  return inject(AuthenticationService).canActivate([Role.ADMINISTRATOR, Role.MANAGER, Role.VIEWER], route, state);
};

/**
 * Guard used to determine if a view can be activated. Used for views that can be activated by everybody, as long as they are logged in.
 */
export const canActivateEverybody: CanActivateFn = (route: ActivatedRouteSnapshot, state: RouterStateSnapshot) => {
  return inject(AuthenticationService).canActivate([], route, state);
};