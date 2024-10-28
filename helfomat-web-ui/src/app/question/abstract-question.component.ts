import {Component, EventEmitter, OnDestroy, OnInit} from '@angular/core';
import {BehaviorSubject, combineLatest, merge, Observable, of, Subject, Subscription} from 'rxjs';
import {Answer} from '../shared/answer.model';
import {UserAnswer} from '../_internal/resources/organization.service';
import {distinctUntilChanged, map} from "rxjs/operators";
import {Question, QuestionService} from "../_internal/resources/question.service";

@Component({
    template: ''
})
export abstract class AbstractQuestionComponent implements OnInit, OnDestroy {

    // input & output
    public currentAnswers: Observable<string>;
    public newAnswers: EventEmitter<string> = new EventEmitter<string>();
    public questionWithUserAnswers: Subject<Array<QuestionWithUserAnswer>> = new BehaviorSubject<Array<QuestionWithUserAnswer>>([]);

    // internal
    private nextAnswer: EventEmitter<UserAnswer> = new EventEmitter<UserAnswer>();
    private userAnswers: Observable<UserAnswer[]>;
    private questionsAndAnswersSubscription: Subscription;
    private newAnswersSubscription: Subscription;

    protected questionService: QuestionService;

    constructor() {
    }

    public ngOnInit(): void {
        this.userAnswers = this.getCurrentUserAnswers();

        let questionsAndAnswers: Observable<[Array<UserAnswer>, Array<Question>]> = combineLatest([
            this.getCombinedUserAnswersWithNewUserAnswer(),
            this.questionService.findQuestions()
        ]);

        this.questionsAndAnswersSubscription = questionsAndAnswers
            .pipe(
                map(([userAnswers, questions]: [Array<UserAnswer>, Array<Question>]) => this.combineUserAnswersWithQuestions(userAnswers, questions))
            )
            .subscribe((questionWithUserAnswers: Array<QuestionWithUserAnswer>) => {
                this.questionWithUserAnswers.next(questionWithUserAnswers);
            });

        this.newAnswersSubscription = questionsAndAnswers
            .pipe(
                map(([userAnswers, questions]: [Array<UserAnswer>, Array<Question>]) => this.toAnswers(questions, userAnswers)),
                map(answers => JSON.stringify(answers)),
                distinctUntilChanged()
            )
            .subscribe((answers: string) => {
                this.newAnswers.emit(answers);
            });
    }

    public ngOnDestroy(): void {
        this.questionsAndAnswersSubscription.unsubscribe();
        this.newAnswersSubscription.unsubscribe();
    }

    public answerQuestion(answer: Answer, question: Question): void {
        let userAnswer = new UserAnswer();
        userAnswer.id = question.id;
        userAnswer.answer = answer;
        this.nextAnswer.emit(userAnswer)
    }

    protected abstract getCurrentAnswers(): Observable<string>;

    private getCombinedUserAnswersWithNewUserAnswer(): Observable<Array<UserAnswer>> {
        return combineLatest([
            merge(
                this.getCurrentUserAnswers()
            ),
            merge(
                of(null),
                this.nextAnswer
            )
        ])
            .pipe(map(([userAnswers, newAnswer]: [Array<UserAnswer>, UserAnswer]) => AbstractQuestionComponent.combineUserAnswersWithNewAnswer(userAnswers, newAnswer)));
    }

    private combineUserAnswersWithQuestions(userAnswers: Array<UserAnswer>, questions: Array<Question>): Array<QuestionWithUserAnswer> {
        let questionWithUserAnswers = [];
        for (let question of questions) {
            let userAnswer = this.getUserAnswer(userAnswers, question);
            questionWithUserAnswers.push(
                new QuestionWithUserAnswer(question, userAnswer)
            );
        }
        return questionWithUserAnswers;
    }

    private static combineUserAnswersWithNewAnswer(userAnswers: Array<UserAnswer>, newAnswer: UserAnswer): Array<UserAnswer> {
        if (newAnswer == null) {
            return userAnswers;
        }

        let replaced = false;
        for (let userAnswer of userAnswers) {
            if (userAnswer.id == newAnswer.id) {
                userAnswer.answer = newAnswer.answer;
                replaced = true;
            }
        }

        if (!replaced) {
            userAnswers.push(newAnswer);
        }
        return userAnswers;
    }

    private getUserAnswer(userAnswers: Array<UserAnswer>, question: Question): UserAnswer {
        for (let userAnswer of userAnswers) {
            if (userAnswer.id == question.id) {
                return userAnswer;
            }
        }
        return null;
    }

    private toAnswers(questions: Array<Question>, userAnswers: UserAnswer[]): Answer[] {
        let answers = [];
        for (let userAnswer of userAnswers) {
            let position = this.findPositionOfQuestion(questions, userAnswer.id);
            if (position != null) {
                answers[position] = userAnswer.answer;
            }
        }
        return answers;
    }

    private findPositionOfQuestion(questions: Array<Question>, questionId: string) {
        let i = 0;
        for (let question of questions) {
            if (question.id == questionId) {
                return i;
            }
            i++;
        }
        return null;
    }

    private getCurrentUserAnswers() {
        return combineLatest([
            this.getCurrentAnswers().pipe(
                map(string => JSON.parse(string))
            ),
            this.questionService.findQuestions()
        ])
            .pipe(
                map(([answers, questions]: [Array<Answer>, Array<Question>]) => this.toUserAnswers(questions, answers))
            );
    }

    private toUserAnswers(questions: Array<Question>, answers: Array<Answer>): Array<UserAnswer> {
        let userAnswers: Array<UserAnswer> = [];
        if (answers == null) {
            return userAnswers;
        }
        answers.forEach((answer, index) => {
            if (questions[index] !== undefined) {
                let id = questions[index].id;
                userAnswers.push({id, answer});
            }
        });
        return userAnswers;
    }

}

export class QuestionWithUserAnswer {

    constructor(public question: Question,
                public userAnswer: UserAnswer) {
    }

    public isCurrentAnswer(button: Answer) {
        if (this.userAnswer == null) {
            return false;
        }
        let answer = this.userAnswer.answer;
        return answer === button;
    }

}