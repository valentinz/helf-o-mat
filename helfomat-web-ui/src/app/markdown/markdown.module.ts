import {NgModule} from "@angular/core";
import {MarkdownComponent} from "./markdown.component";
import {MarkdownModule as NgxMarkdownModule} from "ngx-markdown";
import {CommonModule} from "@angular/common";

@NgModule({
    imports: [
        NgxMarkdownModule.forChild(),
        CommonModule
    ],
    declarations: [
        MarkdownComponent
    ],
    exports: [
        MarkdownComponent
    ]
})
export class MarkdownModule {
}