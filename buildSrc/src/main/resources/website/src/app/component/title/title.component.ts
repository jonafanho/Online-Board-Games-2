import {Component, Input} from "@angular/core";
import {Router, RouterLink} from "@angular/router";
import {MatButtonModule} from "@angular/material/button";
import {MatIconModule} from "@angular/material/icon";
import {MatTooltipModule} from "@angular/material/tooltip";

@Component({
	selector: "app-title",
	standalone: true,
	imports: [
		RouterLink,
		MatButtonModule,
		MatIconModule,
		MatTooltipModule,
	],
	templateUrl: "./title.component.html",
	styleUrl: "./title.component.css",
})
export class TitleComponent {
	@Input({required: true}) titleText = "";
	@Input() descriptionText = "";
	protected readonly showHomeButton;

	constructor(private readonly router: Router) {
		this.showHomeButton = this.router.url.length > 1;
	}
}
