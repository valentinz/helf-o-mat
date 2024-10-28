import {AfterViewInit, Component, OnInit, Optional} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {Answer} from '../shared/answer.model';
import {UrlParamBuilder} from '../url-param.builder';
import {combineLatest, Observable, Subject} from 'rxjs';
import {ObservableUtil} from '../shared/observable.util';
import {
    Address,
    ContactPerson,
    Organization,
    OrganizationService,
    TravelDistance,
    TravelMode
} from '../_internal/resources/organization.service';
import {GeoPoint} from '../../_internal/geopoint';
import {filter, flatMap, map, switchMap, tap} from "rxjs/operators";
import {hasRole, Roles} from "../_internal/authentication/util";
import {OAuthService} from "angular-oauth2-oidc";
import {environment} from "../../environments/environment";
import {QrCodeService, QuestionAnswers} from "../_internal/qr-code.service";
import {NgbModal} from "@ng-bootstrap/ng-bootstrap";
import {ContactFormComponent} from "./_internal/contact-form.component";

@Component({
    selector: 'organization',
    templateUrl: './organization.component.html',
    styleUrls: ['./organization.component.scss']
})
export class OrganizationComponent implements OnInit, AfterViewInit {

    public _back$: Subject<void>;
    public organization$: Observable<Organization>;
    public userAnswers: Observable<Answer[]>;
    public organizations: Observable<Array<Organization>>;
    public position: Observable<GeoPoint>;
    public center: Observable<GeoPoint>;
    public distance: Observable<number>;
    public zoom: Observable<number>;
    public scoreNorm: Observable<number>;
    public travelDistances: Observable<Array<TravelDistance>>;
    private fragment: string;

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private organizationService: OrganizationService,
        private qrCodeService: QrCodeService,
        private modalService: NgbModal,
        @Optional() private oAuthService: OAuthService
    ) {
        if (this.showQrCode()) {
            qrCodeService.triggerUpdateLocation();
        }
        this._back$ = new Subject<void>();

        this.userAnswers = ObservableUtil.extractObjectMember(this.route.params, 'answers')
            .pipe(
                map(UrlParamBuilder.parseAnswers)
            );
        this.position = ObservableUtil.extractObjectMember(this.route.params, 'position')
            .pipe(
                map(UrlParamBuilder.parseGeoPoint)
            );
        this.distance = ObservableUtil.extractObjectMember(this.route.params, 'distance')
            .pipe(
                map(UrlParamBuilder.parseInt)
            );
        this.scoreNorm = ObservableUtil.extractObjectMember(this.route.params, 'scoreNorm')
            .pipe(
                map(UrlParamBuilder.parseInt)
            );
        this.organization$ = ObservableUtil.extractObjectMember(this.route.params, 'organization')
            .pipe(
                switchMap((organizationName: string) => this.organizationService.getOrganization(organizationName))
            );


        combineLatest([
            this.position,
            ObservableUtil.extractObjectMember(this.route.params, 'zoom').pipe(map(UrlParamBuilder.parseInt)),
            this.distance,
            this.userAnswers,
            this._back$.asObservable()
        ])
            .subscribe(([position, zoom, distance, userAnswers]: [GeoPoint, number, number, Answer[], void]) => {
                this.router.navigate(['/helf-o-mat/volunteer/result', {
                    answers: UrlParamBuilder.buildAnswers(userAnswers),
                    position: UrlParamBuilder.buildGeoPoint(position),
                    zoom: zoom,
                    distance: distance
                }]);
            });

        this.travelDistances = combineLatest([
            this.organization$,
            this.position.pipe(
                filter(position => position != null)
            )
        ])
            .pipe(
                flatMap(([organization, position]: [Organization, GeoPoint]) =>
                    this.organizationService.getTravelDistances(organization.id, position)
                        .pipe(
                            tap((travelDistances: TravelDistance[]) => {
                                    travelDistances.splice(0, 0, {
                                        travelMode: TravelMode.FLIGHT,
                                        timeInSeconds: null,
                                        distanceInMeters: GeoPoint.distanceInMeter(organization?.defaultAddress?.location, position)
                                    });
                                }
                            )
                        )
                )
            );
    }

    public ngOnInit() {
        this.route.fragment.subscribe(fragment => {
            this.fragment = fragment;
        });
    }

    public ngAfterViewInit(): void {
        if (this.fragment !== undefined) {
            this.organization$.subscribe(() => {
                window.setTimeout(() => {
                    let querySelector = document.querySelector('#' + this.fragment);
                    if (querySelector != null) {
                        querySelector.scrollIntoView();
                    }
                });
            });
        }
    }

    public areAddressesEqual(address1: Address, address2: Address): boolean {
        return Address.isEqual(address1, address2);
    }

    public showUrls(): boolean {
        return !environment.kiosk;
    }

    public showQrCode(): boolean {
        return environment.kiosk;
    }

    public useLocalImages(): boolean {
        return environment.kiosk;
    }

    public getQrCodeLink(organization: Organization, userAnswers: Answer[]) {
        const questionAnswers: QuestionAnswers[] = [];
        if (userAnswers != null) {
            for (let i = 0; i < organization.questions.length; i++) {
                questionAnswers.push({
                    questionId: organization.questions[i].questionId,
                    answer: userAnswers[i]
                });
            }
        }
        return this.qrCodeService.generateLink(organization.organizationType, questionAnswers);
    }

    public isReviewer() {
        if (this.oAuthService === null) {
            return false;
        }
        const accessToken = this.oAuthService.getAccessToken();
        return hasRole(accessToken, Roles.ADMIN) || hasRole(accessToken, Roles.REVIEWER);
    }

    public openContactForm(organization: Organization, contactPerson: ContactPerson): void {
        const modalRef = this.modalService.open(ContactFormComponent, {
            size: 'lg',
        });
        modalRef.componentInstance.contact = contactPerson;
        modalRef.componentInstance.organization = organization;
    }

}