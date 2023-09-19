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
import { ImageFormat } from './imageFormat';


export interface ImageDeployment { 
    name: string;
    format: ImageFormat;
    source: string;
    path: string;
    server?: string;
    maxSize: number;
}
export namespace ImageDeployment {
}


