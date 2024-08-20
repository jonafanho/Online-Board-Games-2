import "reflect-metadata";
import {bootstrapApplication} from "@angular/platform-browser";
import {AppComponent} from "./app/app.component";
import {provideAnimationsAsync} from "@angular/platform-browser/animations/async";
import {provideHttpClient} from "@angular/common/http";
import {provideRouter} from "@angular/router";
import {routes} from "./app/app-routes";

bootstrapApplication(AppComponent, {providers: [provideAnimationsAsync(), provideHttpClient(), provideRouter(routes)]}).catch(err => console.error(err));
