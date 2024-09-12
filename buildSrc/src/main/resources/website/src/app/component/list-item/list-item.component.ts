import {Component, Input} from "@angular/core";
import {MatIconModule} from "@angular/material/icon";

@Component({
	selector: "app-list-item",
	standalone: true,
	imports: [
		MatIconModule,
	],
	templateUrl: "./list-item.component.html",
	styleUrl: "./list-item.component.css",
})
export class ListItemComponent {
	@Input() icon = "";
	@Input() titleText = "";
	@Input() descriptionText = "";
	@Input() hasContent = false;
}
