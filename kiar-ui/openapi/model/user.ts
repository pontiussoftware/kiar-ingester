/**
 * KIAR Dashboard API
 * API for the KIAR Dashboard.
 *
 * The version of the OpenAPI document: 1.0.0
 * Contact: support@kimnet.ch
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
import { Role } from './role';


export interface User { 
    id?: string | null;
    username: string;
    password?: string | null;
    email?: string | null;
    active: boolean;
    role: Role;
    institution?: string | null;
    createdAt?: number | null;
    changedAt?: number | null;
}
export namespace User {
}


