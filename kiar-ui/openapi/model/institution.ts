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
import { License } from './license';
import { ApacheSolrCollection } from './apacheSolrCollection';


export interface Institution { 
    id?: string;
    name: string;
    displayName: string;
    participantName: string;
    description?: string;
    isil?: string;
    street?: string;
    zip: number;
    city: string;
    canton: string;
    email: string;
    homepage?: string;
    publish: boolean;
    availableCollections: Array<ApacheSolrCollection>;
    selectedCollections: Array<ApacheSolrCollection>;
    defaultLicense?: License;
    defaultCopyright?: string;
    createdAt?: number;
    changedAt?: number;
}

