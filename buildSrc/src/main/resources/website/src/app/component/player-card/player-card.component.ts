import {Component, Input} from "@angular/core";
import {Player} from "../../entity/player";
import {ListItemComponent} from "../list-item/list-item.component";

@Component({
	selector: "app-player-card",
	standalone: true,
	imports: [
		ListItemComponent,
	],
	templateUrl: "./player-card.component.html",
	styleUrl: "./player-card.component.css",
})
export class PlayerCardComponent {
	@Input({required: true}) player?: Player;
	@Input() descriptionText = "";
	@Input() hasContent = false;
}
