import {NgModule} from "@angular/core";
import {ContactFormComponent} from "./contact-form.component";
import {TranslateModule} from "@ngx-translate/core";
import {FormsModule} from "@angular/forms";
import {RecaptchaV3Module} from "ng-recaptcha";
import {BrowserModule} from "@angular/platform-browser";

@NgModule({
    declarations: [
        ContactFormComponent
    ],
    imports: [
        TranslateModule,
        BrowserModule,
        FormsModule,
        RecaptchaV3Module
    ],
    exports: [
        ContactFormComponent
    ]
})
export class ContactFormModule {
}