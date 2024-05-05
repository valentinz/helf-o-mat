import {Component} from "@angular/core";
import {NgForm, NgModel} from "@angular/forms";
import {catchError, mergeMap} from "rxjs/operators";
import {of} from "rxjs";
import {ReCaptchaV3Service} from "ng-recaptcha";
import {ToastrService} from "ngx-toastr";
import {TranslateService} from "@ngx-translate/core";
import {LoadingOverlayService} from "../_internal/components/loading-overlay/loading-overlay.service";
import {ContactFormService} from "../_internal/resources/contact-form.service";

@Component({
    templateUrl: 'contact-form.component.html'
})
export class ContactFormComponent {

    public privacyNotice: boolean = false;
    public contactFormContent: ContactFormContent = ContactFormComponent.empty();

    constructor(
        private contactFormService: ContactFormService,
        private recaptchaV3Service: ReCaptchaV3Service,
        private toastr: ToastrService,
        private translateService: TranslateService,
        private loadingOverlayService: LoadingOverlayService
    ) {
    }

    public hasError(field: NgModel): boolean {
        return field.invalid && (field.dirty || field.touched);
    }

    public submit(form: NgForm) {
        if (!form.valid || !this.privacyNotice) {
            for (const i in form.controls) {
                form.controls[i].markAsTouched();
            }
            this.toastr.warning(this.translateService.instant('error.invalidForm'));
            return;
        }
        this.loadingOverlayService.open();
        this.recaptchaV3Service.execute('submit')
            .pipe(
                catchError(error => {
                    this.toastr.error(this.translateService.instant('error.captchaInvalid'));
                    this.loadingOverlayService.close();
                    console.warn('failed during submit', error);
                    return of()
                }),
                mergeMap(
                    (token: string) =>
                        this.contactFormService.createContactRequest({
                                name: this.contactFormContent.name,
                                email: this.contactFormContent.email,
                                subject: this.contactFormContent.subject,
                                message: this.contactFormContent.message,
                                location: this.contactFormContent.location,
                                address: this.contactFormContent.address,
                                captcha: token
                            }
                        )
                )
            )
            .subscribe(() => {
                form.resetForm();
                this.contactFormContent = ContactFormComponent.empty();
                this.loadingOverlayService.close();
                this.toastr.success(this.translateService.instant('contact-form.toast.success'));
            }, (error) => {
                this.toastr.error(this.translateService.instant('contact-form.error.errorSubmit'));
                this.loadingOverlayService.close();
                console.warn('failed during submit', error);
            });
    }

    private static empty(): ContactFormContent {
        return {
            name: '',
            email: '',
            subject: '',
            message: '',
            location: null,
            address: null
        };
    }

}


export interface ContactFormContent {
    name: string;
    email: string;
    subject: string;
    message: string;
    location?: string;
    address?: string;
}