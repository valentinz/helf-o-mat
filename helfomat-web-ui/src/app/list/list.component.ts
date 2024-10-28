import {ChangeDetectorRef, Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {combineLatest, Observable} from 'rxjs';
import {Organization} from '../_internal/resources/organization.service';
import {PictureId} from "../_internal/resources/picture.service";
import {GeoPoint} from "../../_internal/geopoint";
import {environment} from "../../environments/environment";

@Component({
    selector: 'helfomat-list',
    templateUrl: './list.component.html',
    styleUrls: ['./list.component.scss']
})
export class ListComponent implements OnInit {

    @Input() organizations: Observable<Organization[]>;

    @Input() position: Observable<GeoPoint>;

    @Output() openOrganization: EventEmitter<Organization> = new EventEmitter<Organization>();

    @Output() openOrganizationScoreExplanation: EventEmitter<Organization> = new EventEmitter<Organization>();

    public currentOrganizations: { organization: Organization, distance: number }[] = [];

    constructor(private changeDetectorRef: ChangeDetectorRef) {
    }

    ngOnInit() {
        combineLatest([
            this.organizations,
            this.position
        ])
            .subscribe(([organizations, position]: [Organization[], GeoPoint]) => {
                this.currentOrganizations = organizations.map(organization => ({
                    organization,
                    distance: position != null && organization?.defaultAddress?.location != null
                        ? Math.round(GeoPoint.distanceInKm(position, organization.defaultAddress.location) * 1000) : null
                }));
                this.changeDetectorRef.detectChanges();
            });
    }

    public getImagePath(image: PictureId): string {
        return environment.kiosk ? `assets/images/logos/${image.value}.jpg` : `api/picture/${image.value}/icon`;
    }

    public showUrls(): boolean {
        return !environment.kiosk;
    }

}
