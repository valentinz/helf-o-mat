import {ModuleWithProviders} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {QuestionComponent} from './question/question.component';
import {ResultComponent} from './result/result.component';
import {OrganizationComponent} from './organization/organization.component';
import {NoAuthGuard} from "./_internal/authentication/no-auth.guard";
import {AuthenticateComponent} from "./authenticate/authenticate.component";
import {KioskRouteComponent} from "./kiosk/kiosk-route.component";
import {AppModule} from "./app.module";
import {ConfirmEmailComponent} from "./confirm-email/confirm-email.component";
import {MarkdownComponent} from "./markdown/markdown.component";

const appRoutes: Routes = [
    {
        path: 'helf-o-mat/volunteer',
        children: [
            {path: 'organization/:organization', component: OrganizationComponent, canActivate: [NoAuthGuard]},
            {path: 'result', component: ResultComponent, canActivate: [NoAuthGuard]},
            {path: 'question', component: QuestionComponent, canActivate: [NoAuthGuard]},
            {path: 'location', component: ResultComponent, canActivate: [NoAuthGuard]},
            {path: 'confirm-email/:contactRequestId/:contactConfirmationToken', component: ConfirmEmailComponent, canActivate: [NoAuthGuard]},
            {path: '', redirectTo: 'question', pathMatch: 'full'}
        ]
    },
    {
        path: 'helf-o-mat/kiosk/:organizationType/:latitude/:longitude',
        component: KioskRouteComponent
    },
    {path: 'helf-o-mat/result', redirectTo: '/helf-o-mat/volunteer/result'},
    {path: 'helf-o-mat/question', redirectTo: '/helf-o-mat/volunteer/question'},
    {path: 'helf-o-mat/location', redirectTo: '/helf-o-mat/volunteer/location'},
    {path: 'helf-o-mat/organisation/:organization', redirectTo: '/helf-o-mat/volunteer/organization/:organization'},
    {path: 'helf-o-mat/authenticate', component: AuthenticateComponent},
    {path: 'authenticate', component: AuthenticateComponent},
    {path: 'impressum', component: MarkdownComponent},
    {path: 'datenschutz', component: MarkdownComponent},
    {path: 'partner', component: MarkdownComponent},
    {path: 'verein/ueber-uns', redirectTo: '/verein'},
    {path: 'verein/spenden', redirectTo: '/verein'},
    {path: 'verein', component: MarkdownComponent},
    {path: '', redirectTo: '/helf-o-mat/volunteer/question', pathMatch: 'full'}
];

export const appRoutingProviders: any[] = [];

export const routing: ModuleWithProviders<AppModule> = RouterModule.forRoot(appRoutes);