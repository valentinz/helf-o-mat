import {Component} from "@angular/core";
import {ActivatedRoute, Router} from "@angular/router";
import {ObservableUtil} from "../shared/observable.util";
import {combineLatest} from "rxjs";
import {ContactService} from "../_internal/resources/contact.service";
import {mergeMap} from "rxjs/operators";
import {LoadingOverlayService} from "../_internal/components/loading-overlay/loading-overlay.service";
import {ToastrService} from "ngx-toastr";
import {TranslateService} from "@ngx-translate/core";

@Component({
    templateUrl: './confirm-email.component.html'
})
export class ConfirmEmailComponent {

    constructor(
        private route: ActivatedRoute,
        private contactService: ContactService,
        private loadingOverlayService: LoadingOverlayService,
        private translateService: TranslateService,
        private toastrService: ToastrService,
        private router: Router
    ) {
        this.loadingOverlayService.open(this.translateService.instant('confirm-email.wait'));
        const contactRequestId = ObservableUtil.extractObjectMember<string>(this.route.params, 'contactRequestId');
        contactRequestId
            .pipe(
                mergeMap(contactRequestId => this.contactService.getContactRequest({value: contactRequestId}))
            )
            .subscribe(contactRequest => {
                this.router.navigate([`/helf-o-mat/volunteer/organization/${contactRequest.organizationUrl}`]);
            });

        combineLatest([
            contactRequestId,
            ObservableUtil.extractObjectMember<string>(this.route.params, 'contactConfirmationToken')
        ])
            .pipe(
                mergeMap(([contactRequestId, contactConfirmationToken]) => {
                    return this.contactService.confirmContactRequest({
                        contactRequestId: {value: contactRequestId},
                        confirmationCode: contactConfirmationToken
                    });
                })
            )
            .subscribe(() => {
                this.toastrService.success(this.translateService.instant('confirm-email.toast.success'), null, {
                    disableTimeOut: true,
                    closeButton: true
                });
                this.loadingOverlayService.close();
            }, () => {
                this.toastrService.warning(this.translateService.instant('confirm-email.toast.failed'));
                this.loadingOverlayService.close();
            });
    }

}