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
    id?: string;
    username: string;
    password?: string;
    email?: string;
    active: boolean;
    role: Role;
    institution?: string;
    createdAt?: number;
    changedAt?: number;
}
export namespace User {
}


