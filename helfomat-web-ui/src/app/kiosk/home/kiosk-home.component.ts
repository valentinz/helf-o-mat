import {Component} from "@angular/core";
import {Router} from "@angular/router";

@Component({
    selector: 'kiosk-home',
    templateUrl: './kiosk-home.component.html',
    styleUrls: [
        './kiosk-home.component.scss'
    ]
})
export class KioskHomeComponent {

    constructor(
        private router: Router
    ) {
    }

    public start() {
        this.router.navigate(['/helf-o-mat/volunteer/question', {answers: "[]"}]);
    }
}