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
	"person", "face_1", "face_2", "face_3", "face_4", "face_5", "face_6", "face_7", "face_8", "face_9", "support_agent",
	"train", "light_mode", "dark_mode", "star", "smart_toy", "casino", "sports_basketball", "explore", "castle", "park", "flag",
];

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
		this.iconToggle = playerService.getIcon();
		if (!ICONS.some(iconSet => iconSet.includes(this.iconToggle))) {
			this.iconToggle = ICONS[0];
		}
	}

	getPlayerName() {
		return this.playerService.getName();
	}

	chooseIcon(icon: string) {
		this.iconToggle = icon;
	}

	getSelectedIconColor(icon: string) {
		return this.iconToggle == icon ? "accent" : "";
	}

	onUpdate(name: string) {
		this.playerService.setPlayerProfile(name, this.iconToggle);
		this.onCancel();
	}

	onCancel() {
		this.dialogRef.close();
	}
}
