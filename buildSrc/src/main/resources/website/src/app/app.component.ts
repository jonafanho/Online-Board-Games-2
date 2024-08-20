import {Component} from "@angular/core";
import {MatInputModule} from "@angular/material/input";
import {MatButtonModule} from "@angular/material/button";
import {MatDividerModule} from "@angular/material/divider";
import {FormsModule} from "@angular/forms";
import {HomeComponent} from "./component/home/home.component";
import {RouterOutlet} from "@angular/router";

@Component({
	selector: "app-root",
	standalone: true,
	imports: [
		MatInputModule,
		MatButtonModule,
		MatDividerModule,
		FormsModule,
		HomeComponent,
		RouterOutlet,
	],
	templateUrl: "./app.component.html",
	styleUrls: ["./app.component.css"],
})
export class AppComponent {
}
