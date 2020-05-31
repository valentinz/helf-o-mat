import {ModuleWithProviders} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {QuestionComponent} from './question/question.component';
import {ResultComponent} from './result/result.component';
import {OrganizationComponent} from './organization/organization.component';
import {NoAuthGuard} from "./_internal/authentication/no-auth.guard";
import {AuthenticateComponent} from "./authenticate/authenticate.component";

const appRoutes: Routes = [
    {
        path: 'volunteer',
        children: [
            {path: 'organization/:organization', component: OrganizationComponent, canActivate: [NoAuthGuard]},
            {path: 'result', component: ResultComponent, canActivate: [NoAuthGuard]},
            {path: 'question', component: QuestionComponent, canActivate: [NoAuthGuard]},
            {path: 'location', component: ResultComponent, canActivate: [NoAuthGuard]},
            {path: '', redirectTo: 'question', pathMatch: 'full'}
        ]
    },
    {path: 'result', redirectTo: '/volunteer/result'},
    {path: 'question', redirectTo: '/volunteer/question'},
    {path: 'location', redirectTo: '/volunteer/location'},
    {path: 'organisation/:organization', redirectTo: '/volunteer/organization/:organization'},
    {path: 'authenticate', component: AuthenticateComponent},
    {path: '', redirectTo: '/volunteer/question', pathMatch: 'full'}
];

export const appRoutingProviders: any[] = [];

export const routing: ModuleWithProviders = RouterModule.forRoot(appRoutes);