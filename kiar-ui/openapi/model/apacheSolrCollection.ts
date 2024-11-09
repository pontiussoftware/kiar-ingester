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
import { CollectionType } from './collectionType';


export interface ApacheSolrCollection { 
    name: string;
    displayName?: string | null;
    type: CollectionType;
    selector?: string | null;
    oai: boolean;
    deleteBeforeImport: boolean;
    acceptEmptyFilter: boolean;
}
export namespace ApacheSolrCollection {
}


