import {Injectable} from "@angular/core";
import {Http, Response} from "@angular/http";
import "rxjs/add/operator/map";
import {Observable, Subject} from "rxjs";
import Organisation from "../organisation/organisation.model";
import Answer from "../organisation/answer.model";
import GeoPoint from "../organisation/geopoint.model";
import BoundingBox from "../organisation/boundingbox.model";
import ClusteredGeoPoint from "../organisation/clusteredGeoPoint.model";

@Injectable()
export class SearchService {

    private _organisations$: Subject<Organisation[]>;
    private _clusteredOrganisations$: Subject<ClusteredGeoPoint[]>;
    private dataStore: {
        organisations: Organisation[],
        clusteredOrganisations: ClusteredGeoPoint[]
    };

    constructor(private http: Http) {
        this._organisations$ = <Subject<Organisation[]>>new Subject();
        this._clusteredOrganisations$ = <Subject<ClusteredGeoPoint[]>>new Subject();
        this.dataStore = {
            organisations: [],
            clusteredOrganisations: []
        };
    }

    searchOrganisations(): Observable<Array<Organisation>> {
        return this.http.post('api/search', {}).map((r: Response) => r.json());
    }

    get organisations$() {
        return this._organisations$.asObservable();
    }

    get clusteredOrganisations$() {
        return this._clusteredOrganisations$.asObservable();
    }

    search(answers: Answer[], position: GeoPoint, distance: number) {
        this.http.post('api/organisation/search', {
            answers,
            position,
            distance
        }).map((response: Response) => response.json()).subscribe(data => {
            this.dataStore.organisations = data;
            this._organisations$.next(this.dataStore.organisations);
        });
    }

    boundingBox(position: GeoPoint, distance: number, boundingBox: BoundingBox, zoom: number) {
        this.http.post('api/organisation/boundingBox', {
            position,
            distance,
            boundingBox,
            zoom
        }).map((response: Response) => response.json()).subscribe(data => {
            this.dataStore.clusteredOrganisations = data;
            this._clusteredOrganisations$.next(this.dataStore.clusteredOrganisations);
        });
    }

}
