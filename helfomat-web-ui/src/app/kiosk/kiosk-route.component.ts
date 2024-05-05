import {Component} from "@angular/core";
import {ActivatedRoute, ParamMap, Router} from "@angular/router";
import {Question, QuestionService} from "../_internal/resources/question.service";
import {combineLatest} from "rxjs";
import {Answer, AnswerUtil} from "../shared/answer.model";
import {GeoPoint} from "../../_internal/geopoint";

@Component({
    selector: 'kiosk-route',
    template: ''
})
export class KioskRouteComponent {

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private questionService: QuestionService
    ) {
        combineLatest([
            this.questionService.findQuestions(),
            this.route.paramMap
        ])
            .subscribe(([questions, params]: [Question[], ParamMap]) => {
                const answers: Answer[] = [];
                for (const question of questions) {
                    const questionShortId = question.id.split("-")[0];
                    answers.push(AnswerUtil.toAnswer(params.get(questionShortId)));
                }

                let position = null;
                const latitude = Number(params.get('latitude'));
                const longitude = Number(params.get('longitude'));
                if (!isNaN(latitude) && !isNaN(longitude)) {
                    position = JSON.stringify(new GeoPoint(latitude, longitude))
                }
                let organizationType: string;
                organizationType = params.get('organizationType');
                if (organizationType == 'all') {
                    organizationType = '';
                }

                this.router.navigate(['/helf-o-mat/volunteer/result', {
                    answers: JSON.stringify(answers),
                    position: position,
                    mapSize: 'normal',
                    organizationType
                }]);
            });
    }

}