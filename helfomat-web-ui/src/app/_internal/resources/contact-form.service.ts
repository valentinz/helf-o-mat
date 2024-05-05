import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";

@Injectable({
    providedIn: 'root'
})
export class ContactFormService {

    constructor(
        private httpClient: HttpClient
    ) {
    }

    public createContactRequest(createContactRequest: CreateContactRequest): Observable<void> {
        return this.httpClient.post<void>('api/contact-form', createContactRequest);
    }

}


export interface CreateContactRequest {
    captcha: string;
    name: string;
    email: string;
    subject: string;
    message: string;
    location?: string;
    address?: string;
}