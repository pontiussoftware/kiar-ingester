import {Component} from "@angular/core";
import {FormControl, FormGroup, Validators} from "@angular/forms";
import {JobType} from "../../../../../openapi";

@Component({
  selector: 'app-user-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent {
  /** The {@link FormControl} that backs this {@link AddJobTemplateDialogComponent}. */
  public formControl: FormGroup =  new FormGroup({
    name: new FormControl('', [Validators.required, Validators.minLength(3)]),
    description: new FormControl(''),
    type: new FormControl(JobType.KIAR, [Validators.required]),
    startAutomatically: new FormControl(false),
    participantName: new FormControl('test', [Validators.required]),
    entityMappingName: new FormControl('', [Validators.required]),
    solrConfigName: new FormControl('', [Validators.required]),
  })
}