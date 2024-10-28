import {Injectable} from '@angular/core';
import {BehaviorSubject, Observable} from 'rxjs';
import {HttpClient} from "@angular/common/http";
import questions from "./questions.json";
import {environment} from '../../../environments/environment';

@Injectable({
    providedIn: 'root'
})
export class QuestionService {

    constructor(private httpClient: HttpClient) {
    }

    findQuestions(): Observable<Array<Question>> {
        if (environment.kiosk) {
            return new BehaviorSubject<Array<Question>>(questions).asObservable();
        } else {
            return this.httpClient.get<Array<Question>>('api/questions');
        }
    }

}

export class Question {
    public id: string;
    public question: string;
    public description: string;
    public position?: number;
}