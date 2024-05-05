import {ChangeDetectorRef, Component, OnInit} from '@angular/core';
import {BoundingBox, Organization, OrganizationService, UserAnswer} from '../_internal/resources/organization.service';
import {BehaviorSubject, combineLatest, from, Observable, Subject} from 'rxjs';
import {ActivatedRoute, NavigationExtras, Router} from '@angular/router';
import {UrlParamBuilder} from '../url-param.builder';
import {ObservableUtil} from '../shared/observable.util';
import {animate, state, style, transition, trigger} from '@angular/animations';
import {environment} from '../../environments/environment';
import {GeoPoint} from '../../_internal/geopoint';
import {debounceTime, distinctUntilChanged, filter, first, flatMap, map} from "rxjs/operators";
import {
    CreateOrganizationDialogService
} from "../_internal/components/create-organization-dialog/create-organization-dialog.service";
import {QrCodeService, QuestionAnswers} from "../_internal/qr-code.service";

@Component({
    selector: 'app-result',
    templateUrl: './result.component.html',
    styleUrls: ['./result.component.scss'],
    animations: [
        trigger('slide', [
            state('question', style({
                transform: 'translate3d(15px, 0, 0)'
            })),
            state('list', style({
                transform: 'translate3d(-50%, 0, 0)'
            })),
            transition('list => question', animate('400ms ease-in-out')),
            transition('question => list', animate('400ms ease-in-out'))
        ])
    ]
})
export class ResultComponent implements OnInit {

    // Inputs
    private _answers$: Subject<UserAnswer[]> = new Subject<UserAnswer[]>();
    public userAnswers: Observable<UserAnswer[]> = this._answers$.asObservable();
    public _position$: Subject<GeoPoint> = new BehaviorSubject<GeoPoint>(null);
    public _boundingBox$: Subject<BoundingBox> = new Subject<BoundingBox>();
    public _zoom$: Subject<number> = new BehaviorSubject<number>(environment.defaults.zoomLevel.withoutPosition);
    public _organization$: Subject<Organization> = new Subject<Organization>();
    public _newAnswers$: Subject<string> = new Subject<string>();
    public _organizationType$: Subject<string | null> = new BehaviorSubject<string | null>(null);
    public answers: Observable<string>;
    public position: Observable<GeoPoint>;
    public center: Observable<GeoPoint>;
    public distance = from([10]);
    public zoom: Observable<number>;
    public organizationType: Observable<string | null>;
    public _mapSize$: Subject<string> = new BehaviorSubject<string>('normal');
    public mapSize: Observable<string>;
    public hasPosition: boolean = true;

    // Outputs
    public organizations: Subject<Organization[]> = new Subject<Organization[]>();
    public clusteredOrganizations: Subject<GeoPoint[]> = new Subject<GeoPoint[]>();

    public visibleComponent: 'list' | 'question' = 'list';
    private explainScore: boolean = false;

    constructor(
        private organizationService: OrganizationService,
        private router: Router,
        private route: ActivatedRoute,
        private createOrganizationDialogService: CreateOrganizationDialogService,
        private changeDetectorRef: ChangeDetectorRef,
        private qrCodeService: QrCodeService
    ) {
        ObservableUtil.extractObjectMember(this.route.params, 'position')
            .pipe(
                map(UrlParamBuilder.parseGeoPoint)
            )
            .subscribe(position => this._position$.next(position));

        ObservableUtil.extractObjectMember(this.route.params, 'zoom')
            .pipe(map(UrlParamBuilder.parseInt))
            .pipe(
                map((zoom: number) => {
                    if (zoom == null) {
                        return environment.defaults.zoomLevel.withoutPosition;
                    }
                    return zoom;
                })
            )
            .subscribe(zoom => this._zoom$.next(zoom));

        let position: Observable<GeoPoint> = this._position$.asObservable();

        this.position = position
            .pipe(
                debounceTime(100),
                distinctUntilChanged()
            );

        position
            .pipe(
                filter(position => position !== null),
                first()
            )
            .subscribe(() => {
                this.hasPosition = true;
                this._mapSize$.next('normal');
                this._zoom$.next(environment.defaults.zoomLevel.withPosition);
                this.changeDetectorRef.detectChanges();
            });

        this.center = position
            .pipe(
                map((position) => {
                    if (position == null) {
                        return environment.defaults.mapCenter;
                    }
                    return position;
                }),
                debounceTime(100),
                distinctUntilChanged()
            );

        this.zoom = this._zoom$.asObservable()
            .pipe(
                debounceTime(100),
                distinctUntilChanged()
            );

        this.answers = ObservableUtil.extractObjectMember(this.route.params, 'answers');

        ObservableUtil.extractObjectMember(this.route.params, 'mapSize')
            .pipe(
                filter(mapSize => mapSize != null)
            )
            .subscribe((mapSize: string) => {
                if (mapSize === 'fullscreen') {
                    this.hasPosition = false;
                }
                return this._mapSize$.next(mapSize);
            });
        this.mapSize = this._mapSize$.asObservable();

        ObservableUtil.extractObjectMember(this.route.params, 'organizationType')
            .subscribe(organizationType => this._organizationType$.next(<string>organizationType || null));

        this.organizationType = this._organizationType$.asObservable()
            .pipe(
                debounceTime(100),
                distinctUntilChanged()
            );
    }

    ngOnInit() {
        combineLatest([
            this._answers$.asObservable(),
            this._position$.asObservable(),
            this.distance,
            this._boundingBox$.asObservable(),
            this._zoom$.asObservable(),
            this._organizationType$.asObservable()
        ])
            .subscribe(([userAnswers, position, distance, boundingBox, zoom, organizationType]: [UserAnswer[], GeoPoint, number, BoundingBox, number, string]) => {
                this.router.navigate(['/helf-o-mat/volunteer/result', {
                    answers: UrlParamBuilder.buildAnswersFromUserAnswer(userAnswers),
                    position: UrlParamBuilder.buildGeoPoint(position),
                    distance: distance,
                    boundingBox: UrlParamBuilder.buildBoundingBox(boundingBox),
                    zoom: zoom,
                    organizationType
                }], {
                    replaceUrl: true
                });
            });

        combineLatest([
            this._answers$.asObservable(),
            this.position,
            this.distance,
            this.organizationType
        ])
            .pipe(
                flatMap(([answers, position, distance, organizationType]: [Array<UserAnswer>, GeoPoint, number, string | null]) => {
                    let organizations;
                    if (answers.length == 0 && position == null) {
                        organizations = this.organizationService.findGlobal();
                    } else if (answers.length == 0) {
                        organizations = this.organizationService.findByPosition(position, distance);
                    } else if (position == null) {
                        organizations = this.organizationService.findGlobalByQuestionAnswers(answers);
                    } else {
                        organizations = this.organizationService.findByQuestionAnswersAndPosition(answers, position, distance);
                    }
                    return organizations
                        .pipe(map(organizations => this.filterOrganizations(organizations, organizationType)));
                })
            )
            .subscribe((organizations: Organization[]) => {
                this.organizations.next(organizations);
            });

        combineLatest([
            this.position,
            this.distance,
            this._boundingBox$.asObservable(),
            this.zoom
        ])
            .pipe(
                flatMap(([position, distance, boundingBox, zoom]: [GeoPoint, number, BoundingBox, number]) => {
                    return this.organizationService.boundingBox(position, distance, boundingBox, zoom);
                })
            )
            .subscribe((clusteredOrganizations: GeoPoint[]) => {
                this.clusteredOrganizations.next(clusteredOrganizations);
            });

        combineLatest([
            this._organization$.asObservable(),
            this._newAnswers$.asObservable(),
            this.position,
            this.zoom,
            this.distance
        ])
            .subscribe(([organization, answers, position, zoom, distance]: [Organization, string, GeoPoint, number, number]) => {
                let extras: NavigationExtras = {};
                if (this.explainScore) {
                    extras.fragment = 'compare';
                }
                this.router.navigate(['/helf-o-mat/volunteer/organization/' + organization.urlName, {
                    answers: answers,
                    position: UrlParamBuilder.buildGeoPoint(position),
                    zoom: zoom,
                    distance: distance,
                    scoreNorm: organization.scoreNorm
                }], extras);
            });
    }

    public continueWithoutLocation(mapSize?: string): void {
        if (!this.hasPosition) {
            this.hasPosition = true;
            if (mapSize !== 'normal') {
                this._mapSize$.next('normal');
            }
            this.changeDetectorRef.detectChanges();
        }
    }

    createOrganization() {
        this.createOrganizationDialogService.showCreateOrganizationDialog();
    }

    updateOrganizations(answers: UserAnswer[]) {
        this._answers$.next(answers);
    }

    public openOrganization(organization: Organization, explainScore: boolean = false): void {
        this.explainScore = explainScore;
        this._organization$.next(organization);
    }

    public isReadOnlyMode(): boolean {
        return environment.readonly;
    }

    public removeOrganizationTypeFilter(): void {
        this._organizationType$.next(null);
    }


    public showQrCode(): boolean {
        return environment.kiosk;
    }

    public getQrCodeLink(userAnswers: UserAnswer[]) {
        const questionAnswers: QuestionAnswers[] = [];
        if (userAnswers != null) {
            for (let i = 0; i < userAnswers.length; i++) {
                questionAnswers.push({
                    questionId: {value: userAnswers[i].id},
                    answer: userAnswers[i].answer
                });
            }
        }
        return this.qrCodeService.generateLink('all', questionAnswers);
    }

    private filterOrganizations(organizations: Organization[], organizationType: string | null): Organization[] {
        if (organizationType == null) {
            return organizations;
        }
        return organizations.filter(organization => organization.organizationType == organizationType);
    }
}
