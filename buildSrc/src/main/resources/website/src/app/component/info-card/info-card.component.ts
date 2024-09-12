import {Component, Input} from "@angular/core";
import {MatIconModule} from "@angular/material/icon";
import {MatCardModule} from "@angular/material/card";

@Component({
	selector: "app-info-card",
	standalone: true,
	imports: [
		MatIconModule,
		MatCardModule,
	],
	templateUrl: "./info-card.component.html",
	styleUrl: "./info-card.component.css",
})
export class InfoCardComponent {
	@Input({required: true}) icon = "";
	@Input({required: true}) text = "";
}
