import {Component, OnInit, Output, EventEmitter} from "@angular/core";
import AbstractQuestionComponent from "./abstractQuestion.component";
import {HelfomatService} from "./helfomat.service";
import {Router, ActivatedRoute} from "@angular/router";

@Component({
    selector: 'app-question-overview',
    templateUrl: './questionOverview.component.html',
    styleUrls: ['./questionOverview.component.css'],
    providers: [HelfomatService]
})
export class QuestionOverviewComponent extends AbstractQuestionComponent implements OnInit {

    @Output() public organisations: EventEmitter<any> = new EventEmitter();

    constructor(protected router: Router,
                protected route: ActivatedRoute,
                protected helfomatService: HelfomatService) {
        super();
    }

}
