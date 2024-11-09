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
import { JobStatus } from './jobStatus';
import { JobSource } from './jobSource';
import { JobTemplate } from './jobTemplate';


export interface Job { 
    id?: string | null;
    name: string;
    status: JobStatus;
    source: JobSource;
    template?: JobTemplate;
    processed: number;
    skipped: number;
    error: number;
    logEntries: number;
    createdAt: number;
    changedAt?: number | null;
    createdBy: string;
}
export namespace Job {
}


