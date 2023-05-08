import {AfterViewInit, Component} from "@angular/core";
import {ConfigService, EntityMapping, JobTemplate, SolrConfig} from "../../../../../openapi";
import {Observable, shareReplay} from "rxjs";

@Component({
  selector: 'kiar-admin-dashboard',
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss']
})
export class AdminDashboardComponent {


  /** {@link Observable} of all available {@link JobTemplate}s. */
  public readonly templates: Observable<Array<JobTemplate>>

  /** {@link Observable} of all available {@link EntityMapping}s. */
  public readonly mappings: Observable<Array<EntityMapping>>

  /** {@link Observable} of all available {@link SolrConfig}s. */
  public readonly solr: Observable<Array<SolrConfig>>

  constructor(private config: ConfigService) {
    this.templates = this.config.getListJobTemplates().pipe(
      shareReplay(1, 120000)
    );

    this.mappings = this.config.getListEntityMappings().pipe(
        shareReplay(1, 120000)
    );

    this.solr = this.config.getListSolrConfiguration().pipe(
        shareReplay(1, 120000)
    );
  }

}