import {Component} from "@angular/core";
import {MatCard, MatCardContent, MatCardTitle} from "@angular/material/card";

@Component({
    selector: 'app-forbidden',
    templateUrl: './forbidden.component.html',
    styleUrls: ['./forbidden.component.scss'],
    imports: [MatCard, MatCardTitle, MatCardContent]
})
export class ForbiddenComponent {

}