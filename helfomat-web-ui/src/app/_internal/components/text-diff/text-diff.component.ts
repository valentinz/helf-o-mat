import {Component, Input} from "@angular/core";

const JSDiff = require('diff');

@Component({
    selector: 'text-diff',
    templateUrl: './text-diff.component.html',
    styleUrls: [
        './text-diff.component.scss'
    ]
})
export class TextDiffComponent {

    @Input()
    private text1: string;

    @Input()
    private text2: string;

    public getDiff(): Array<{value: string, added: boolean, removed: boolean}> {
        return JSDiff.diffChars(this.text1, this.text2);
    }

}