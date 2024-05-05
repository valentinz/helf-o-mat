import {Component, OnInit} from "@angular/core";
import {NullValidationHandler, OAuthService} from "angular-oauth2-oidc";
import {ActivatedRoute, Router} from "@angular/router";
import {getOAuth2Configuration} from "../_internal/authentication/util";

@Component({
    selector: "authenticate",
    template: "Authenticating..."
})
export class AuthenticateComponent implements OnInit {

    constructor(
        private oAuthService: OAuthService,
        private router: Router,
        private route: ActivatedRoute
    ) {
    }

    ngOnInit(): void {
        this.oAuthService.configure(getOAuth2Configuration());
        this.oAuthService.setupAutomaticSilentRefresh();
        this.oAuthService.tokenValidationHandler = new NullValidationHandler();
        this.oAuthService.loadDiscoveryDocumentAndTryLogin()
            .then(() => {
                let authenticated = this.oAuthService.hasValidAccessToken();
                if (authenticated) {
                    let newRoute = decodeURIComponent(this.oAuthService.state);
                    if (newRoute == null || newRoute  == "" || newRoute.startsWith('/helf-o-mat/authenticate') || newRoute.startsWith('/authenticate')) {
                        newRoute = '/';
                    }
                    this.router.navigate([newRoute])
                        .catch(() => {
                            this.router.navigate(['/']);
                        })
                } else {
                    this.route.queryParamMap.subscribe(queryParams => {
                        const redirectTo = queryParams.get('redirectTo');
                        this.oAuthService.initLoginFlow(redirectTo);
                    });
                }
            });
    }
}

