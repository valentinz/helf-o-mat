import {AfterViewInit, Component, OnInit} from "@angular/core";
import {Router} from "@angular/router";

@Component({
    templateUrl: './markdown.component.html',
    styleUrls: []
})
export class MarkdownComponent implements OnInit, AfterViewInit {

    public markdownUrl: string = null;

    constructor(private router: Router) {
    }

    ngAfterViewInit(): void {
    }

    ngOnInit(): void {
        let routerUrl = this.router.url;
        if (routerUrl.endsWith('/')) {
            routerUrl = routerUrl.substring(0, routerUrl.length - 1);
        }
        this.markdownUrl = `assets/markdown${routerUrl}.md`;
    }

}