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
import { ValueParser } from './valueParser';


export interface AttributeMapping { 
    source: string;
    destination: string;
    parser: ValueParser;
    required: boolean;
    multiValued: boolean;
    parameters: { [key: string]: string; };
}
export namespace AttributeMapping {
}


