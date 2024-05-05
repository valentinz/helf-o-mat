import {Routes} from "@angular/router";
import {ProfileComponent} from "./profile.component";
import {IsLoggedInGuard} from "../_internal/authentication/is-logged-in.guard";

export const manageRoutes: Routes = [
    {
        path: 'helf-o-mat/profile',
        component: ProfileComponent,
        canActivate: [
            IsLoggedInGuard
        ]
    }
];
