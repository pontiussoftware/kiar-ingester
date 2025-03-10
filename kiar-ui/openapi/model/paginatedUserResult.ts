/**
 * KIAR Dashboard API
 *
 * Contact: support@kimnet.ch
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
import { User } from './user';


export interface PaginatedUserResult { 
    total: number;
    page: number;
    pageSize: number;
    results: Array<User>;
}

