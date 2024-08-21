import {Component, inject} from "@angular/core";
import {RoomService} from "../../service/room.service";
import {TitleComponent} from "../title/title.component";
import {MatFormFieldModule} from "@angular/material/form-field";
import {MatIconModule} from "@angular/material/icon";
import {MatInputModule} from "@angular/material/input";
import {MatButtonModule} from "@angular/material/button";
import {PlayerService} from "../../service/player.service";
import {MatDialog} from "@angular/material/dialog";
import {PlayerEditDialogComponent} from "../player/player-edit-dialog.component";
import {MatSlideToggleModule} from "@angular/material/slide-toggle";
import {RouterLink} from "@angular/router";
import {MatListModule} from "@angular/material/list";

@Component({
	selector: "app-home",
	standalone: true,
	imports: [
		TitleComponent,
		MatFormFieldModule,
		MatIconModule,
		MatInputModule,
		MatButtonModule,
		MatSlideToggleModule,
		RouterLink,
		MatListModule,
	],
	templateUrl: "./lobby.component.html",
	styleUrl: "./lobby.component.css",
})
export class LobbyComponent {
	private readonly dialog = inject(MatDialog);

	constructor(private readonly roomService: RoomService, private readonly playerService: PlayerService) {
	}

	getRoomCode() {
		return this.roomService.getRoomCode();
	}

	getGameTitle() {
		return this.roomService.getGameTitle();
	}

	getIsPlayerJoined() {
		return this.roomService.getIsPlayerJoined();
	}

	getRoomPlayers() {
		return this.roomService.getRoomPlayers();
	}

	getPlayerStatus(uuid: string) {
		return this.roomService.getHostUuid() == uuid ? "Host" : "Player";
	}

	joinOrLeaveRoom(join: boolean) {
		this.playerService.joinOrLeaveRoom(this.getRoomCode(), join);
	}

	editPlayerProfile() {
		this.dialog.open(PlayerEditDialogComponent);
	}
}
