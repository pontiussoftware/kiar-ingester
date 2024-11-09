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
import { JobType } from './jobType';
import { TransformerConfig } from './transformerConfig';


export interface JobTemplate { 
    id?: string | null;
    name: string;
    description?: string | null;
    type: JobType;
    startAutomatically: boolean;
    participantName: string;
    solrConfigName: string;
    entityMappingName: string;
    createdAt?: number | null;
    changedAt?: number | null;
    transformers: Array<TransformerConfig>;
}
export namespace JobTemplate {
}


