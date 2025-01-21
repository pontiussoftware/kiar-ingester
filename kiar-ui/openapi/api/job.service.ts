/**
 * KIAR Dashboard API
 *
 * Contact: support@kimnet.ch
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
/* tslint:disable:no-unused-variable member-ordering */

import { Inject, Injectable, Optional }                      from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams,
         HttpResponse, HttpEvent, HttpParameterCodec, HttpContext 
        }       from '@angular/common/http';
import { CustomHttpParameterCodec }                          from '../encoder';
import { Observable }                                        from 'rxjs';

// @ts-ignore
import { CreateJobRequest } from '../model/createJobRequest';
// @ts-ignore
import { ErrorStatus } from '../model/errorStatus';
// @ts-ignore
import { Job } from '../model/job';
// @ts-ignore
import { JobTemplate } from '../model/jobTemplate';
// @ts-ignore
import { PaginatedJobLogResult } from '../model/paginatedJobLogResult';
// @ts-ignore
import { PaginatedJobResult } from '../model/paginatedJobResult';
// @ts-ignore
import { SuccessStatus } from '../model/successStatus';

// @ts-ignore
import { BASE_PATH, COLLECTION_FORMATS }                     from '../variables';
import { Configuration }                                     from '../configuration';



@Injectable({
  providedIn: 'root'
})
export class JobService {

    protected basePath = 'http://localhost';
    public defaultHeaders = new HttpHeaders();
    public configuration = new Configuration();
    public encoder: HttpParameterCodec;

    constructor(protected httpClient: HttpClient, @Optional()@Inject(BASE_PATH) basePath: string|string[], @Optional() configuration: Configuration) {
        if (configuration) {
            this.configuration = configuration;
        }
        if (typeof this.configuration.basePath !== 'string') {
            const firstBasePath = Array.isArray(basePath) ? basePath[0] : undefined;
            if (firstBasePath != undefined) {
                basePath = firstBasePath;
            }

            if (typeof basePath !== 'string') {
                basePath = this.basePath;
            }
            this.configuration.basePath = basePath;
        }
        this.encoder = this.configuration.encoder || new CustomHttpParameterCodec();
    }

    /**
     * @param consumes string[] mime-types
     * @return true: consumes contains 'multipart/form-data', false: otherwise
     */
    private canConsumeForm(consumes: string[]): boolean {
        const form = 'multipart/form-data';
        for (const consume of consumes) {
            if (form === consume) {
                return true;
            }
        }
        return false;
    }

    // @ts-ignore
    private addToHttpParams(httpParams: HttpParams, value: any, key?: string): HttpParams {
        if (typeof value === "object" && value instanceof Date === false) {
            httpParams = this.addToHttpParamsRecursive(httpParams, value);
        } else {
            httpParams = this.addToHttpParamsRecursive(httpParams, value, key);
        }
        return httpParams;
    }

    private addToHttpParamsRecursive(httpParams: HttpParams, value?: any, key?: string): HttpParams {
        if (value == null) {
            return httpParams;
        }

        if (typeof value === "object") {
            if (Array.isArray(value)) {
                (value as any[]).forEach( elem => httpParams = this.addToHttpParamsRecursive(httpParams, elem, key));
            } else if (value instanceof Date) {
                if (key != null) {
                    httpParams = httpParams.append(key, (value as Date).toISOString().substring(0, 10));
                } else {
                   throw Error("key may not be null if value is Date");
                }
            } else {
                Object.keys(value).forEach( k => httpParams = this.addToHttpParamsRecursive(
                    httpParams, value[k], key != null ? `${key}.${k}` : k));
            }
        } else if (key != null) {
            httpParams = httpParams.append(key, value);
        } else {
            throw Error("key may not be null if value is not object or array");
        }
        return httpParams;
    }

    /**
     * Aborts a running job.
     * @param id The ID of the Job that should be aborted.
     * @param observe set whether or not to return the data Observable as the body, response or events. defaults to returning the body.
     * @param reportProgress flag to report request and response progress.
     */
    public deleteAbortJob(id: string, observe?: 'body', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext}): Observable<SuccessStatus>;
    public deleteAbortJob(id: string, observe?: 'response', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext}): Observable<HttpResponse<SuccessStatus>>;
    public deleteAbortJob(id: string, observe?: 'events', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext}): Observable<HttpEvent<SuccessStatus>>;
    public deleteAbortJob(id: string, observe: any = 'body', reportProgress: boolean = false, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext}): Observable<any> {
        if (id === null || id === undefined) {
            throw new Error('Required parameter id was null or undefined when calling deleteAbortJob.');
        }

        let localVarHeaders = this.defaultHeaders;

        let localVarHttpHeaderAcceptSelected: string | undefined = options && options.httpHeaderAccept;
        if (localVarHttpHeaderAcceptSelected === undefined) {
            // to determine the Accept header
            const httpHeaderAccepts: string[] = [
                'application/json'
            ];
            localVarHttpHeaderAcceptSelected = this.configuration.selectHeaderAccept(httpHeaderAccepts);
        }
        if (localVarHttpHeaderAcceptSelected !== undefined) {
            localVarHeaders = localVarHeaders.set('Accept', localVarHttpHeaderAcceptSelected);
        }

        let localVarHttpContext: HttpContext | undefined = options && options.context;
        if (localVarHttpContext === undefined) {
            localVarHttpContext = new HttpContext();
        }


        let responseType_: 'text' | 'json' | 'blob' = 'json';
        if (localVarHttpHeaderAcceptSelected) {
            if (localVarHttpHeaderAcceptSelected.startsWith('text')) {
                responseType_ = 'text';
            } else if (this.configuration.isJsonMime(localVarHttpHeaderAcceptSelected)) {
                responseType_ = 'json';
            } else {
                responseType_ = 'blob';
            }
        }

        let localVarPath = `/api/jobs/${this.configuration.encodeParam({name: "id", value: id, in: "path", style: "simple", explode: false, dataType: "string", dataFormat: undefined})}`;
        return this.httpClient.request<SuccessStatus>('delete', `${this.configuration.basePath}${localVarPath}`,
            {
                context: localVarHttpContext,
                responseType: <any>responseType_,
                withCredentials: this.configuration.withCredentials,
                headers: localVarHeaders,
                observe: observe,
                reportProgress: reportProgress
            }
        );
    }

    /**
     * Purges the logs for the job with the provided ID.
     * @param id The ID of the Job for which the logs should be pruged.
     * @param observe set whether or not to return the data Observable as the body, response or events. defaults to returning the body.
     * @param reportProgress flag to report request and response progress.
     */
    public deletePurgeJobLog(id: string, observe?: 'body', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext}): Observable<PaginatedJobLogResult>;
    public deletePurgeJobLog(id: string, observe?: 'response', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext}): Observable<HttpResponse<PaginatedJobLogResult>>;
    public deletePurgeJobLog(id: string, observe?: 'events', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext}): Observable<HttpEvent<PaginatedJobLogResult>>;
    public deletePurgeJobLog(id: string, observe: any = 'body', reportProgress: boolean = false, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext}): Observable<any> {
        if (id === null || id === undefined) {
            throw new Error('Required parameter id was null or undefined when calling deletePurgeJobLog.');
        }

        let localVarHeaders = this.defaultHeaders;

        let localVarHttpHeaderAcceptSelected: string | undefined = options && options.httpHeaderAccept;
        if (localVarHttpHeaderAcceptSelected === undefined) {
            // to determine the Accept header
            const httpHeaderAccepts: string[] = [
                'application/json'
            ];
            localVarHttpHeaderAcceptSelected = this.configuration.selectHeaderAccept(httpHeaderAccepts);
        }
        if (localVarHttpHeaderAcceptSelected !== undefined) {
            localVarHeaders = localVarHeaders.set('Accept', localVarHttpHeaderAcceptSelected);
        }

        let localVarHttpContext: HttpContext | undefined = options && options.context;
        if (localVarHttpContext === undefined) {
            localVarHttpContext = new HttpContext();
        }


        let responseType_: 'text' | 'json' | 'blob' = 'json';
        if (localVarHttpHeaderAcceptSelected) {
            if (localVarHttpHeaderAcceptSelected.startsWith('text')) {
                responseType_ = 'text';
            } else if (this.configuration.isJsonMime(localVarHttpHeaderAcceptSelected)) {
                responseType_ = 'json';
            } else {
                responseType_ = 'blob';
            }
        }

        let localVarPath = `/api/jobs/${this.configuration.encodeParam({name: "id", value: id, in: "path", style: "simple", explode: false, dataType: "string", dataFormat: undefined})}/logs`;
        return this.httpClient.request<PaginatedJobLogResult>('delete', `${this.configuration.basePath}${localVarPath}`,
            {
                context: localVarHttpContext,
                responseType: <any>responseType_,
                withCredentials: this.configuration.withCredentials,
                headers: localVarHeaders,
                observe: observe,
                reportProgress: reportProgress
            }
        );
    }

    /**
     * Retrieves all jobs that are currently active. Non-administrator users can only see Jobs that belong to them.
     * @param page The page index (zero-based) for pagination.
     * @param pageSize The page size for pagination.
     * @param observe set whether or not to return the data Observable as the body, response or events. defaults to returning the body.
     * @param reportProgress flag to report request and response progress.
     */
    public getActiveJobs(page?: number, pageSize?: number, observe?: 'body', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext}): Observable<PaginatedJobResult>;
    public getActiveJobs(page?: number, pageSize?: number, observe?: 'response', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext}): Observable<HttpResponse<PaginatedJobResult>>;
    public getActiveJobs(page?: number, pageSize?: number, observe?: 'events', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext}): Observable<HttpEvent<PaginatedJobResult>>;
    public getActiveJobs(page?: number, pageSize?: number, observe: any = 'body', reportProgress: boolean = false, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext}): Observable<any> {

        let localVarQueryParameters = new HttpParams({encoder: this.encoder});
        if (page !== undefined && page !== null) {
          localVarQueryParameters = this.addToHttpParams(localVarQueryParameters,
            <any>page, 'page');
        }
        if (pageSize !== undefined && pageSize !== null) {
          localVarQueryParameters = this.addToHttpParams(localVarQueryParameters,
            <any>pageSize, 'pageSize');
        }

        let localVarHeaders = this.defaultHeaders;

        let localVarHttpHeaderAcceptSelected: string | undefined = options && options.httpHeaderAccept;
        if (localVarHttpHeaderAcceptSelected === undefined) {
            // to determine the Accept header
            const httpHeaderAccepts: string[] = [
                'application/json'
            ];
            localVarHttpHeaderAcceptSelected = this.configuration.selectHeaderAccept(httpHeaderAccepts);
        }
        if (localVarHttpHeaderAcceptSelected !== undefined) {
            localVarHeaders = localVarHeaders.set('Accept', localVarHttpHeaderAcceptSelected);
        }

        let localVarHttpContext: HttpContext | undefined = options && options.context;
        if (localVarHttpContext === undefined) {
            localVarHttpContext = new HttpContext();
        }


        let responseType_: 'text' | 'json' | 'blob' = 'json';
        if (localVarHttpHeaderAcceptSelected) {
            if (localVarHttpHeaderAcceptSelected.startsWith('text')) {
                responseType_ = 'text';
            } else if (this.configuration.isJsonMime(localVarHttpHeaderAcceptSelected)) {
                responseType_ = 'json';
            } else {
                responseType_ = 'blob';
            }
        }

        let localVarPath = `/api/jobs/active`;
        return this.httpClient.request<PaginatedJobResult>('get', `${this.configuration.basePath}${localVarPath}`,
            {
                context: localVarHttpContext,
                params: localVarQueryParameters,
                responseType: <any>responseType_,
                withCredentials: this.configuration.withCredentials,
                headers: localVarHeaders,
                observe: observe,
                reportProgress: reportProgress
            }
        );
    }

    /**
     * Retrieves all jobs that are currently inactive (job history). Non-administrator users can only see Jobs that belong to them.
     * @param page The page index (zero-based) for pagination.
     * @param pageSize The page size for pagination.
     * @param observe set whether or not to return the data Observable as the body, response or events. defaults to returning the body.
     * @param reportProgress flag to report request and response progress.
     */
    public getInactiveJobs(page?: number, pageSize?: number, observe?: 'body', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext}): Observable<PaginatedJobResult>;
    public getInactiveJobs(page?: number, pageSize?: number, observe?: 'response', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext}): Observable<HttpResponse<PaginatedJobResult>>;
    public getInactiveJobs(page?: number, pageSize?: number, observe?: 'events', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext}): Observable<HttpEvent<PaginatedJobResult>>;
    public getInactiveJobs(page?: number, pageSize?: number, observe: any = 'body', reportProgress: boolean = false, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext}): Observable<any> {

        let localVarQueryParameters = new HttpParams({encoder: this.encoder});
        if (page !== undefined && page !== null) {
          localVarQueryParameters = this.addToHttpParams(localVarQueryParameters,
            <any>page, 'page');
        }
        if (pageSize !== undefined && pageSize !== null) {
          localVarQueryParameters = this.addToHttpParams(localVarQueryParameters,
            <any>pageSize, 'pageSize');
        }

        let localVarHeaders = this.defaultHeaders;

        let localVarHttpHeaderAcceptSelected: string | undefined = options && options.httpHeaderAccept;
        if (localVarHttpHeaderAcceptSelected === undefined) {
            // to determine the Accept header
            const httpHeaderAccepts: string[] = [
                'application/json'
            ];
            localVarHttpHeaderAcceptSelected = this.configuration.selectHeaderAccept(httpHeaderAccepts);
        }
        if (localVarHttpHeaderAcceptSelected !== undefined) {
            localVarHeaders = localVarHeaders.set('Accept', localVarHttpHeaderAcceptSelected);
        }

        let localVarHttpContext: HttpContext | undefined = options && options.context;
        if (localVarHttpContext === undefined) {
            localVarHttpContext = new HttpContext();
        }


        let responseType_: 'text' | 'json' | 'blob' = 'json';
        if (localVarHttpHeaderAcceptSelected) {
            if (localVarHttpHeaderAcceptSelected.startsWith('text')) {
                responseType_ = 'text';
            } else if (this.configuration.isJsonMime(localVarHttpHeaderAcceptSelected)) {
                responseType_ = 'json';
            } else {
                responseType_ = 'blob';
            }
        }

        let localVarPath = `/api/jobs/inactive`;
        return this.httpClient.request<PaginatedJobResult>('get', `${this.configuration.basePath}${localVarPath}`,
            {
                context: localVarHttpContext,
                params: localVarQueryParameters,
                responseType: <any>responseType_,
                withCredentials: this.configuration.withCredentials,
                headers: localVarHeaders,
                observe: observe,
                reportProgress: reportProgress
            }
        );
    }

    /**
     * Retrieves the job log for the provided job ID.
     * @param id The ID of the Job for which the logs should be retrieved.
     * @param page The page index (zero-based) for pagination.
     * @param pageSize The page size  for pagination.
     * @param level A filter for the \&#39;level\&#39; field.
     * @param context A filter for the \&#39;context\&#39; field.
     * @param observe set whether or not to return the data Observable as the body, response or events. defaults to returning the body.
     * @param reportProgress flag to report request and response progress.
     */
    public getJobLog(id: string, page?: number, pageSize?: number, level?: string, context?: string, observe?: 'body', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext}): Observable<PaginatedJobLogResult>;
    public getJobLog(id: string, page?: number, pageSize?: number, level?: string, context?: string, observe?: 'response', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext}): Observable<HttpResponse<PaginatedJobLogResult>>;
    public getJobLog(id: string, page?: number, pageSize?: number, level?: string, context?: string, observe?: 'events', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext}): Observable<HttpEvent<PaginatedJobLogResult>>;
    public getJobLog(id: string, page?: number, pageSize?: number, level?: string, context?: string, observe: any = 'body', reportProgress: boolean = false, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext}): Observable<any> {
        if (id === null || id === undefined) {
            throw new Error('Required parameter id was null or undefined when calling getJobLog.');
        }

        let localVarQueryParameters = new HttpParams({encoder: this.encoder});
        if (page !== undefined && page !== null) {
          localVarQueryParameters = this.addToHttpParams(localVarQueryParameters,
            <any>page, 'page');
        }
        if (pageSize !== undefined && pageSize !== null) {
          localVarQueryParameters = this.addToHttpParams(localVarQueryParameters,
            <any>pageSize, 'pageSize');
        }
        if (level !== undefined && level !== null) {
          localVarQueryParameters = this.addToHttpParams(localVarQueryParameters,
            <any>level, 'level');
        }
        if (context !== undefined && context !== null) {
          localVarQueryParameters = this.addToHttpParams(localVarQueryParameters,
            <any>context, 'context');
        }

        let localVarHeaders = this.defaultHeaders;

        let localVarHttpHeaderAcceptSelected: string | undefined = options && options.httpHeaderAccept;
        if (localVarHttpHeaderAcceptSelected === undefined) {
            // to determine the Accept header
            const httpHeaderAccepts: string[] = [
                'application/json'
            ];
            localVarHttpHeaderAcceptSelected = this.configuration.selectHeaderAccept(httpHeaderAccepts);
        }
        if (localVarHttpHeaderAcceptSelected !== undefined) {
            localVarHeaders = localVarHeaders.set('Accept', localVarHttpHeaderAcceptSelected);
        }

        let localVarHttpContext: HttpContext | undefined = options && options.context;
        if (localVarHttpContext === undefined) {
            localVarHttpContext = new HttpContext();
        }


        let responseType_: 'text' | 'json' | 'blob' = 'json';
        if (localVarHttpHeaderAcceptSelected) {
            if (localVarHttpHeaderAcceptSelected.startsWith('text')) {
                responseType_ = 'text';
            } else if (this.configuration.isJsonMime(localVarHttpHeaderAcceptSelected)) {
                responseType_ = 'json';
            } else {
                responseType_ = 'blob';
            }
        }

        let localVarPath = `/api/jobs/${this.configuration.encodeParam({name: "id", value: id, in: "path", style: "simple", explode: false, dataType: "string", dataFormat: undefined})}/logs`;
        return this.httpClient.request<PaginatedJobLogResult>('get', `${this.configuration.basePath}${localVarPath}`,
            {
                context: localVarHttpContext,
                params: localVarQueryParameters,
                responseType: <any>responseType_,
                withCredentials: this.configuration.withCredentials,
                headers: localVarHeaders,
                observe: observe,
                reportProgress: reportProgress
            }
        );
    }

    /**
     * Lists all available job templates.
     * @param observe set whether or not to return the data Observable as the body, response or events. defaults to returning the body.
     * @param reportProgress flag to report request and response progress.
     */
    public getListJobTemplates(observe?: 'body', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext}): Observable<Array<JobTemplate>>;
    public getListJobTemplates(observe?: 'response', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext}): Observable<HttpResponse<Array<JobTemplate>>>;
    public getListJobTemplates(observe?: 'events', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext}): Observable<HttpEvent<Array<JobTemplate>>>;
    public getListJobTemplates(observe: any = 'body', reportProgress: boolean = false, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext}): Observable<any> {

        let localVarHeaders = this.defaultHeaders;

        let localVarHttpHeaderAcceptSelected: string | undefined = options && options.httpHeaderAccept;
        if (localVarHttpHeaderAcceptSelected === undefined) {
            // to determine the Accept header
            const httpHeaderAccepts: string[] = [
                'application/json'
            ];
            localVarHttpHeaderAcceptSelected = this.configuration.selectHeaderAccept(httpHeaderAccepts);
        }
        if (localVarHttpHeaderAcceptSelected !== undefined) {
            localVarHeaders = localVarHeaders.set('Accept', localVarHttpHeaderAcceptSelected);
        }

        let localVarHttpContext: HttpContext | undefined = options && options.context;
        if (localVarHttpContext === undefined) {
            localVarHttpContext = new HttpContext();
        }


        let responseType_: 'text' | 'json' | 'blob' = 'json';
        if (localVarHttpHeaderAcceptSelected) {
            if (localVarHttpHeaderAcceptSelected.startsWith('text')) {
                responseType_ = 'text';
            } else if (this.configuration.isJsonMime(localVarHttpHeaderAcceptSelected)) {
                responseType_ = 'json';
            } else {
                responseType_ = 'blob';
            }
        }

        let localVarPath = `/api/templates`;
        return this.httpClient.request<Array<JobTemplate>>('get', `${this.configuration.basePath}${localVarPath}`,
            {
                context: localVarHttpContext,
                responseType: <any>responseType_,
                withCredentials: this.configuration.withCredentials,
                headers: localVarHeaders,
                observe: observe,
                reportProgress: reportProgress
            }
        );
    }

    /**
     * Creates a new job.
     * @param createJobRequest 
     * @param observe set whether or not to return the data Observable as the body, response or events. defaults to returning the body.
     * @param reportProgress flag to report request and response progress.
     */
    public postCreateJob(createJobRequest: CreateJobRequest, observe?: 'body', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext}): Observable<Job>;
    public postCreateJob(createJobRequest: CreateJobRequest, observe?: 'response', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext}): Observable<HttpResponse<Job>>;
    public postCreateJob(createJobRequest: CreateJobRequest, observe?: 'events', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext}): Observable<HttpEvent<Job>>;
    public postCreateJob(createJobRequest: CreateJobRequest, observe: any = 'body', reportProgress: boolean = false, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext}): Observable<any> {
        if (createJobRequest === null || createJobRequest === undefined) {
            throw new Error('Required parameter createJobRequest was null or undefined when calling postCreateJob.');
        }

        let localVarHeaders = this.defaultHeaders;

        let localVarHttpHeaderAcceptSelected: string | undefined = options && options.httpHeaderAccept;
        if (localVarHttpHeaderAcceptSelected === undefined) {
            // to determine the Accept header
            const httpHeaderAccepts: string[] = [
                'application/json'
            ];
            localVarHttpHeaderAcceptSelected = this.configuration.selectHeaderAccept(httpHeaderAccepts);
        }
        if (localVarHttpHeaderAcceptSelected !== undefined) {
            localVarHeaders = localVarHeaders.set('Accept', localVarHttpHeaderAcceptSelected);
        }

        let localVarHttpContext: HttpContext | undefined = options && options.context;
        if (localVarHttpContext === undefined) {
            localVarHttpContext = new HttpContext();
        }


        // to determine the Content-Type header
        const consumes: string[] = [
            'application/json'
        ];
        const httpContentTypeSelected: string | undefined = this.configuration.selectHeaderContentType(consumes);
        if (httpContentTypeSelected !== undefined) {
            localVarHeaders = localVarHeaders.set('Content-Type', httpContentTypeSelected);
        }

        let responseType_: 'text' | 'json' | 'blob' = 'json';
        if (localVarHttpHeaderAcceptSelected) {
            if (localVarHttpHeaderAcceptSelected.startsWith('text')) {
                responseType_ = 'text';
            } else if (this.configuration.isJsonMime(localVarHttpHeaderAcceptSelected)) {
                responseType_ = 'json';
            } else {
                responseType_ = 'blob';
            }
        }

        let localVarPath = `/api/jobs`;
        return this.httpClient.request<Job>('post', `${this.configuration.basePath}${localVarPath}`,
            {
                context: localVarHttpContext,
                body: createJobRequest,
                responseType: <any>responseType_,
                withCredentials: this.configuration.withCredentials,
                headers: localVarHeaders,
                observe: observe,
                reportProgress: reportProgress
            }
        );
    }

    /**
     * Starts execution of a job.
     * @param id The ID of the Job that should be started.
     * @param test True, if only a test-run should be executed.
     * @param observe set whether or not to return the data Observable as the body, response or events. defaults to returning the body.
     * @param reportProgress flag to report request and response progress.
     */
    public putScheduleJob(id: string, test?: boolean, observe?: 'body', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext}): Observable<SuccessStatus>;
    public putScheduleJob(id: string, test?: boolean, observe?: 'response', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext}): Observable<HttpResponse<SuccessStatus>>;
    public putScheduleJob(id: string, test?: boolean, observe?: 'events', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext}): Observable<HttpEvent<SuccessStatus>>;
    public putScheduleJob(id: string, test?: boolean, observe: any = 'body', reportProgress: boolean = false, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext}): Observable<any> {
        if (id === null || id === undefined) {
            throw new Error('Required parameter id was null or undefined when calling putScheduleJob.');
        }

        let localVarQueryParameters = new HttpParams({encoder: this.encoder});
        if (test !== undefined && test !== null) {
          localVarQueryParameters = this.addToHttpParams(localVarQueryParameters,
            <any>test, 'test');
        }

        let localVarHeaders = this.defaultHeaders;

        let localVarHttpHeaderAcceptSelected: string | undefined = options && options.httpHeaderAccept;
        if (localVarHttpHeaderAcceptSelected === undefined) {
            // to determine the Accept header
            const httpHeaderAccepts: string[] = [
                'application/json'
            ];
            localVarHttpHeaderAcceptSelected = this.configuration.selectHeaderAccept(httpHeaderAccepts);
        }
        if (localVarHttpHeaderAcceptSelected !== undefined) {
            localVarHeaders = localVarHeaders.set('Accept', localVarHttpHeaderAcceptSelected);
        }

        let localVarHttpContext: HttpContext | undefined = options && options.context;
        if (localVarHttpContext === undefined) {
            localVarHttpContext = new HttpContext();
        }


        let responseType_: 'text' | 'json' | 'blob' = 'json';
        if (localVarHttpHeaderAcceptSelected) {
            if (localVarHttpHeaderAcceptSelected.startsWith('text')) {
                responseType_ = 'text';
            } else if (this.configuration.isJsonMime(localVarHttpHeaderAcceptSelected)) {
                responseType_ = 'json';
            } else {
                responseType_ = 'blob';
            }
        }

        let localVarPath = `/api/jobs/${this.configuration.encodeParam({name: "id", value: id, in: "path", style: "simple", explode: false, dataType: "string", dataFormat: undefined})}/schedule`;
        return this.httpClient.request<SuccessStatus>('put', `${this.configuration.basePath}${localVarPath}`,
            {
                context: localVarHttpContext,
                params: localVarQueryParameters,
                responseType: <any>responseType_,
                withCredentials: this.configuration.withCredentials,
                headers: localVarHeaders,
                observe: observe,
                reportProgress: reportProgress
            }
        );
    }

    /**
     * Uploads a file for the given job.
     * @param id The ID of the Job for which a file should be uploaded.
     * @param first Set to \&#39;true\&#39; if the submitted chunk is the first one.
     * @param last Set to \&#39;true\&#39; if the submitted chunk is the last one.
     * @param file 
     * @param observe set whether or not to return the data Observable as the body, response or events. defaults to returning the body.
     * @param reportProgress flag to report request and response progress.
     */
    public putUpload(id: string, first?: boolean, last?: boolean, file?: Blob, observe?: 'body', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext}): Observable<SuccessStatus>;
    public putUpload(id: string, first?: boolean, last?: boolean, file?: Blob, observe?: 'response', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext}): Observable<HttpResponse<SuccessStatus>>;
    public putUpload(id: string, first?: boolean, last?: boolean, file?: Blob, observe?: 'events', reportProgress?: boolean, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext}): Observable<HttpEvent<SuccessStatus>>;
    public putUpload(id: string, first?: boolean, last?: boolean, file?: Blob, observe: any = 'body', reportProgress: boolean = false, options?: {httpHeaderAccept?: 'application/json', context?: HttpContext}): Observable<any> {
        if (id === null || id === undefined) {
            throw new Error('Required parameter id was null or undefined when calling putUpload.');
        }

        let localVarQueryParameters = new HttpParams({encoder: this.encoder});
        if (first !== undefined && first !== null) {
          localVarQueryParameters = this.addToHttpParams(localVarQueryParameters,
            <any>first, 'first');
        }
        if (last !== undefined && last !== null) {
          localVarQueryParameters = this.addToHttpParams(localVarQueryParameters,
            <any>last, 'last');
        }

        let localVarHeaders = this.defaultHeaders;

        let localVarHttpHeaderAcceptSelected: string | undefined = options && options.httpHeaderAccept;
        if (localVarHttpHeaderAcceptSelected === undefined) {
            // to determine the Accept header
            const httpHeaderAccepts: string[] = [
                'application/json'
            ];
            localVarHttpHeaderAcceptSelected = this.configuration.selectHeaderAccept(httpHeaderAccepts);
        }
        if (localVarHttpHeaderAcceptSelected !== undefined) {
            localVarHeaders = localVarHeaders.set('Accept', localVarHttpHeaderAcceptSelected);
        }

        let localVarHttpContext: HttpContext | undefined = options && options.context;
        if (localVarHttpContext === undefined) {
            localVarHttpContext = new HttpContext();
        }

        // to determine the Content-Type header
        const consumes: string[] = [
            'multipart/form-data'
        ];

        const canConsumeForm = this.canConsumeForm(consumes);

        let localVarFormParams: { append(param: string, value: any): any; };
        let localVarUseForm = false;
        let localVarConvertFormParamsToString = false;
        // use FormData to transmit files using content-type "multipart/form-data"
        // see https://stackoverflow.com/questions/4007969/application-x-www-form-urlencoded-or-multipart-form-data
        localVarUseForm = canConsumeForm;
        if (localVarUseForm) {
            localVarFormParams = new FormData();
        } else {
            localVarFormParams = new HttpParams({encoder: this.encoder});
        }

        if (file !== undefined) {
            localVarFormParams = localVarFormParams.append('file', <any>file) as any || localVarFormParams;
        }

        let responseType_: 'text' | 'json' | 'blob' = 'json';
        if (localVarHttpHeaderAcceptSelected) {
            if (localVarHttpHeaderAcceptSelected.startsWith('text')) {
                responseType_ = 'text';
            } else if (this.configuration.isJsonMime(localVarHttpHeaderAcceptSelected)) {
                responseType_ = 'json';
            } else {
                responseType_ = 'blob';
            }
        }

        let localVarPath = `/api/jobs/${this.configuration.encodeParam({name: "id", value: id, in: "path", style: "simple", explode: false, dataType: "string", dataFormat: undefined})}/upload`;
        return this.httpClient.request<SuccessStatus>('put', `${this.configuration.basePath}${localVarPath}`,
            {
                context: localVarHttpContext,
                body: localVarConvertFormParamsToString ? localVarFormParams.toString() : localVarFormParams,
                params: localVarQueryParameters,
                responseType: <any>responseType_,
                withCredentials: this.configuration.withCredentials,
                headers: localVarHeaders,
                observe: observe,
                reportProgress: reportProgress
            }
        );
    }

}
