import {Injectable} from '@angular/core';
import {Observable, of} from 'rxjs';
import {Answer} from '../../shared/answer.model';
import {GeoPoint} from '../../../_internal/geopoint';
import {HttpClient} from "@angular/common/http";
import {PictureId} from "./picture.service";
import organizations from "./organizations.json";
import {map} from "rxjs/operators";
import {environment} from "../../../environments/environment";

@Injectable({
    providedIn: 'root'
})
export class OrganizationService {

    constructor(private httpClient: HttpClient) {
    }

    findOrganizationTypes(): Observable<OrganizationType[]> {
        return this.httpClient.get<OrganizationType[]>('api/organization/types');
    }

    findGlobal(): Observable<Organization[]> {
        return this.httpClient.get<Organization[]>('api/organization/global');
    }

    getGlobalOrganizationByType(organizationType: string): Observable<Organization> {
        return this.httpClient.get<Organization>(`api/organization/global/${organizationType}`);
    }

    findGlobalByQuestionAnswers(answers: UserAnswer[]): Observable<Organization[]> {
        if (environment.kiosk) {
            return of<Array<Organization>>(this.getScoredOrganizations(answers))
                .pipe(map((organizations) => {
                    return organizations
                        .sort((a, b) => {
                            return b.scoreNorm - a.scoreNorm;
                        });
                }));
        } else {
            return this.httpClient.post<Organization[]>('api/organization/global/byQuestionAnswers', answers);
        }
    }

    findByPosition(position: GeoPoint, distance: number): Observable<Organization[]> {
        return this.httpClient.get<Organization[]>('api/organization/byPosition', {
            params: {
                position: GeoPoint.asString(position),
                distance: String(distance)
            }
        });
    }

    findByQuestionAnswersAndPosition(answers: UserAnswer[], position: GeoPoint, distance: number): Observable<Organization[]> {
        return this.httpClient.post<Organization[]>('api/organization/byQuestionAnswersAndPosition',
            answers,
            {
                params: {
                    position: GeoPoint.asString(position),
                    distance: String(distance)
                }
            });
    }

    boundingBox(position: GeoPoint, distance: number, boundingBox: BoundingBox, zoom: number): Observable<GeoPoint[]> {
        return this.httpClient.post<GeoPoint[]>('api/organization/boundingBox', {
            position,
            distance,
            boundingBox,
            zoom
        });
    }

    getOrganization(urlName: string): Observable<Organization> {
        if (environment.kiosk) {
            for (const organization of organizations) {
                if (organization.urlName == urlName) {
                    return of(organization);
                }
            }

            return of(null);
        } else {
            return this.httpClient.get<Organization>('api/organization/' + urlName);
        }
    }

    getTravelDistances(id: string, location: GeoPoint): Observable<Array<TravelDistance>> {
        return this.httpClient.get<Array<TravelDistance>>(`api/organization/${id}/travelDistances`, {
            params: {
                lat: String(location.lat),
                lon: String(location.lon)
            }
        });
    }

    compareOrganization(original: Organization, updated: Organization): Observable<Array<OrganizationEvent>> {
        return this.httpClient.post<Array<OrganizationEvent>>('api/organization/compare', {
            original,
            updated
        });
    }

    submitOrganizationEvents(organizationId: OrganizationId, sources: string, events: OrganizationEvent[]): Observable<void> {
        return this.httpClient.post<void>('api/organization/submit', {
            organizationId,
            sources,
            events
        });
    }

    validateWebsite(websiteUrl: string): Observable<ValidateWebsiteResultDto> {
        return this.httpClient.post<ValidateWebsiteResultDto>('api/organization/validate-website', {
            websiteUrl
        });
    }

    private getScoredOrganizations(answers: UserAnswer[]) {
        const answerMap: { [key: string]: number } = {};

        for (const answer of answers) {
            answerMap[answer.id] = this.answerToNumber(answer.answer.toString());
        }

        const scoredOrganizations = [];
        for (const organization of organizations) {
            scoredOrganizations.push({
                ...organization,
                scoreNorm: this.organizationScore(organization, answerMap)
            })
        }
        return scoredOrganizations;
    }

    private answerToNumber(answer: string) {
        switch (answer) {
            case "NO":
                return -1;
            case "MAYBE":
                return 0;
            case "YES":
                return 1;
        }
    };


    private organizationScore(organization: Omit<Organization, "scoreNorm">, answerMap: { [key: string]: number }) {
        let score = 0;
        let numberOfQuestions = 0;
        for (const question of organization.questions) {
            const answerAsNumber = this.answerToNumber(question.answer);
            const userAnswer = answerMap[question.questionId.value];
            score += Math.abs(answerAsNumber - userAnswer);
            numberOfQuestions++;
        }
        const RANGE_BETWEEN_ANSWERS = 2;
        return Math.ceil(100 - (score / (numberOfQuestions * RANGE_BETWEEN_ANSWERS) * 100));
    }
}

export class Organization {
    public id: string;
    public name: string;
    public urlName: string;
    public organizationType: string;
    public description: string;
    public website: string;
    public scoreNorm?: number;
    public pictures: PictureId[] = [];
    public contactPersons: ContactPerson[] = [];
    public defaultAddress: Address = null;
    public addresses: Address[] = [];
    public groups: Group[] = [];
    public questions: AnsweredQuestion[] = [];
    public logo: PictureId;
    public volunteers: Volunteer[] = [];
    public attendanceTimes: AttendanceTime[] = [];
    public isPreview: boolean = false;
}

export class TravelDistance {
    public travelMode: TravelMode;
    public timeInSeconds: number;
    public distanceInMeters: number;
}

export enum TravelMode {
    FLIGHT = 'FLIGHT',
    WALKING = 'WALKING',
    CYCLING = 'CYCLING',
    DRIVING = 'DRIVING',
    TRANSIT = 'TRANSIT'
}

export class UserAnswer {
    public id: string;
    public answer: Answer;
}

export class Volunteer {
    public firstname: string;
    public motivation: string;
    public picture: PictureId;
}

export class BoundingBox {
    public northEast: GeoPoint;
    public southWest: GeoPoint;


    constructor(northEast: GeoPoint, southWest: GeoPoint) {
        this.northEast = northEast;
        this.southWest = southWest;
    }
}

export class ContactPerson {
    public firstname: string;
    public lastname: string;
    public rank: string;
    public telephone: string;
    public mail: string;
    public picture: PictureId;
}

export class Address {
    public street: string;
    public addressAppendix?: string;
    public city: string;
    public zipcode: string;
    public location: GeoPoint;
    public telephone?: string;
    public website?: string;

    static isEqual(address1: Address, address2: Address): boolean {
        return address1 != null && address2 != null
            && GeoPoint.isEqual(address1.location, address2.location)
            && address1.addressAppendix == address2.addressAppendix
            && address1.city == address2.city
            && address1.street == address2.street
            && address1.telephone == address2.telephone
            && address1.zipcode == address2.zipcode
            && address1.website == address2.website;
    }
}

export class AnsweredQuestion {
    public questionId: QuestionId;
    public question: string;
    public answer: string;
    public description?: string;
    public position?: number;
}

export interface QuestionId {
    value: string;
}

export class AttendanceTime {
    public day: DayOfWeek;
    public start: number[];
    public end: number[];
    public note: string;
    public groups: Array<Group>;
}

export enum DayOfWeek {
    MONDAY = "MONDAY",
    TUESDAY = "TUESDAY",
    WEDNESDAY = "WEDNESDAY",
    THURSDAY = "THURSDAY",
    FRIDAY = "FRIDAY",
    SATURDAY = "SATURDAY",
    SUNDAY = "SUNDAY"
}

export class Group {
    public name: string;
    public description: string;
}

export class OrganizationEvent {
    public organizationId: OrganizationId;
    public type: string;
    public eventApplicable: "FULL" | "SOURCE_MISMATCH" | "NONE" | "UNAPPLICABLE";
}

export class OrganizationId {
    public value: string;
}

export interface OrganizationType {
    type: string;
    name: string;
}

export interface ValidateWebsiteResultDto {
    resultUrl: string;
    reachable: boolean;
}