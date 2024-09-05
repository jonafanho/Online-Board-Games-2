import {Component, inject} from "@angular/core";
import {MatInputModule} from "@angular/material/input";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {MatButtonModule} from "@angular/material/button";
import {MatFormFieldModule} from "@angular/material/form-field";
import {MatDialogModule, MatDialogRef} from "@angular/material/dialog";
import {PlayerService} from "../../service/player.service";
import {MatButtonToggleModule} from "@angular/material/button-toggle";
import {MatIconModule} from "@angular/material/icon";

const ICONS = [
	"person", "group", "face_1", "face_2", "face_3", "face_4", "face_5", "face_6", "child_care", "support_agent",
	"light_mode", "dark_mode", "star", "bolt", "favorite", "public", "eco", "park", "forest", "pets",
	"cruelty_free", "bug_report", "restaurant", "ramen_dining", "cake", "icecream", "cookie", "warning", "info", "verified",
	"train", "tram", "directions_car", "local_shipping", "fire_truck", "pedal_bike", "electric_scooter", "rocket", "hub", "front_hand",
	"celebration", "token", "diamond", "lightbulb", "vpn_key", "fingerprint", "flag", "explore", "traffic", "school",
	"music_note", "piano", "confirmation_number", "sports_soccer", "sports_baseball", "sports_volleyball", "sports_football", "sports_basketball", "sports_tennis", "pool",
	"sports_esports", "smart_toy", "casino", "desktop_windows", "computer", "phone_android", "headphones", "church", "castle", "stadium",
];
export const DEFAULT_ICON = ICONS[0];

@Component({
	selector: "app-player-edit-dialog",
	templateUrl: "./player-edit-dialog.component.html",
	styleUrl: "./player-edit-dialog.component.css",
	standalone: true,
	imports: [
		MatFormFieldModule,
		MatInputModule,
		FormsModule,
		MatButtonModule,
		MatDialogModule,
		MatButtonToggleModule,
		MatIconModule,
		ReactiveFormsModule,
	],
})
export class PlayerEditDialogComponent {
	private readonly dialogRef = inject(MatDialogRef<PlayerEditDialogComponent>);
	protected readonly icons = ICONS;
	private iconToggle;

	constructor(private playerService: PlayerService) {
		this.iconToggle = playerService.getPlayer().icon;
		if (!ICONS.some(iconSet => iconSet.includes(this.iconToggle))) {
			this.iconToggle = DEFAULT_ICON;
		}
	}

	getPlayerName() {
		return this.playerService.getPlayer().name;
	}

	chooseIcon(icon: string) {
		this.iconToggle = icon;
	}

	getSelectedIconColor(icon: string) {
		return this.iconToggle == icon ? "primary" : "";
	}

	onUpdate(name: string) {
		this.playerService.updateProfile(name, this.iconToggle);
		this.onCancel();
	}

	onCancel() {
		this.dialogRef.close();
	}
}
