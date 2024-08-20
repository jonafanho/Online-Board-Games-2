import {Component, inject} from "@angular/core";
import {SocketService} from "../../service/socket.service";
import {TitleComponent} from "../title/title.component";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {MatFormFieldModule} from "@angular/material/form-field";
import {MatIconModule} from "@angular/material/icon";
import {MatInputModule} from "@angular/material/input";
import {MatButtonModule} from "@angular/material/button";
import {PlayerService} from "../../service/player.service";
import {MatDialog} from "@angular/material/dialog";
import {PlayerEditDialogComponent} from "../player/player-edit-dialog.component";

@Component({
	selector: "app-home",
	standalone: true,
	imports: [
		TitleComponent,
		FormsModule,
		MatFormFieldModule,
		MatIconModule,
		MatInputModule,
		MatButtonModule,
		ReactiveFormsModule,
	],
	templateUrl: "./lobby.component.html",
	styleUrl: "./lobby.component.css",
})
export class LobbyComponent {
	private readonly dialog = inject(MatDialog);

	constructor(private readonly socketService: SocketService, private readonly playerService: PlayerService) {
		const pathSplit = document.location.pathname.split("/");
		this.socketService.initWithRoomCode(pathSplit[pathSplit.length - 1]);
	}

	getRoomCode() {
		return this.socketService.getRoomCode();
	}

	getGameTitle() {
		return this.socketService.getGameTitle();
	}

	onJoinRoom() {
		console.log(this.playerService.getName());
	}

	editPlayerProfile() {
		this.dialog.open(PlayerEditDialogComponent);
	}
}
