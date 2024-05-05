import {Injectable} from "@angular/core";
import {CreateOrganizationDialogComponent} from "./create-organization-dialog.component";
import {NgbModal} from "@ng-bootstrap/ng-bootstrap";
import {Router} from "@angular/router";
import {OrganizationService} from "../../resources/organization.service";

@Injectable()
export class CreateOrganizationDialogService {


    constructor(
        private modalService: NgbModal,
        private organizationService: OrganizationService,
        private router: Router
    ) {
    }

    public showCreateOrganizationDialog() {
        this.organizationService.findOrganizationTypes()
            .subscribe(organizationTypes => {
                const modalRef = this.modalService.open(CreateOrganizationDialogComponent, {
                    size: 'md',
                });
                modalRef.componentInstance.organizationTypes = organizationTypes;
                modalRef.result.then(organizationType => {
                    this.router.navigate([`/helf-o-mat/volunteer/organization/${organizationType}/create`]);
                });
            });

    }

}