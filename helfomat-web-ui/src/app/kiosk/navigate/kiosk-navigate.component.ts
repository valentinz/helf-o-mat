import {Component} from "@angular/core";
import {Location} from "@angular/common";
import {ActivationEnd, NavigationEnd, Router} from "@angular/router";
import {BehaviorSubject, Observable, Subject, timer} from "rxjs";
import {switchMap} from "rxjs/operators";
import {environment} from "../../../environments/environment";
import {TranslateService} from "@ngx-translate/core";

@Component({
    selector: 'kiosk-navigate',
    templateUrl: './kiosk-navigate.component.html',
    styleUrls: [
        './kiosk-navigate.component.scss'
    ]
})
export class KioskNavigateComponent {

    private history: string[] = []

    private countdown: Subject<number> = new BehaviorSubject(0);
    public currentCountdown: Observable<number>;
    public countdownVisible: boolean;

    constructor(
        private router: Router,
        private location: Location,
        private translateService: TranslateService
    ) {

        this.currentCountdown = this.countdown
            .pipe(
                switchMap(() => timer(0, 1000)),
            );

        this.currentCountdown.subscribe(current => {
            this.countdownVisible = this.isCountdownVisible(current);
            if (current > environment.defaults.kiosk.navigateToHomeAfter && this.router.url !== '/') {
                this.router.navigateByUrl('/');
            }
        })

        this.router.events.subscribe((event) => {
            if (event instanceof ActivationEnd) {
                this.countdown.next();
            } else if (event instanceof NavigationEnd) {
                if (event.urlAfterRedirects == '/') {
                    this.history.length = 0;
                }
                this.history.push(event.toString());
            }
        });
    }

    public getCountdownTime(currentTime: number): string {
        if (!this.isCountdownVisible(currentTime)) {
            return "";
        }
        const timeRemaining = Math.max(environment.defaults.kiosk.navigateToHomeAfter - currentTime, 0);
        return this.translateService.instant('kiosk.header.countdownTime', {timeRemaining: timeRemaining});
    }

    public isCountdownVisible(currentTime: number) {
        return currentTime >= environment.defaults.kiosk.showCountdownAfter && this.router.url !== '/';
    }

    public abortCountdown(): void {
        this.countdown.next();
    }

    public hasHistory() {
        return this.history.length > 1 && this.router.url.startsWith('/helf-o-mat/volunteer/organization');
    }

    public home() {
        this.router.navigateByUrl('/');
    }

    public back(): void {
        const lastItem = this.history.pop();
        if (this.history.length > 0) {
            this.location.back();
        } else {
            this.router.navigateByUrl('/')
        }
    }
}