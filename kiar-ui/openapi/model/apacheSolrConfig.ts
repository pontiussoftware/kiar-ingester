/**
 * KIAR Dashboard API
 * API for the KIAR Dashboard.
 *
 * The version of the OpenAPI document: 1.0.0
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
import { ApacheSolrCollection } from './apacheSolrCollection';


export interface ApacheSolrConfig { 
    id?: string;
    name: string;
    description?: string;
    server: string;
    username?: string;
    password?: string;
    collections: Array<ApacheSolrCollection>;
}

