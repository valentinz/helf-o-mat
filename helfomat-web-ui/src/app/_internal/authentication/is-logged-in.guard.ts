import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot} from '@angular/router';
import {OAuthService} from "angular-oauth2-oidc";
import {BaseAuthenticationGuard} from "./base-authentication.guard";

@Injectable({providedIn: 'root'})
export class IsLoggedInGuard extends BaseAuthenticationGuard implements CanActivate {

    constructor(
        private router: Router,
        oAuthService: OAuthService
    ) {
        super(oAuthService);
    }

    canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Promise<boolean> {
        this.setupOAuthContext();
        return this.oAuthService.loadDiscoveryDocumentAndTryLogin()
            .then(() => {
                let withoutUrlParams = BaseAuthenticationGuard.removeOAuthUrlParams(state.url);
                if (withoutUrlParams !== state.url) {
                    this.router.navigateByUrl(withoutUrlParams);
                    return false;
                }
                let authenticated = this.oAuthService.hasValidAccessToken();
                if (!authenticated) {
                    this.router.navigate([
                        '/helf-o-mat/authenticate'
                    ], {
                        queryParams: {
                            redirectTo: state.url
                        }
                    })
                    return false;
                }
                return authenticated;
            });
    }
}