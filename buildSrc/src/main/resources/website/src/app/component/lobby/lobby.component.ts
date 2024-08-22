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
import {MatTooltipModule} from "@angular/material/tooltip";
import {Player} from "../../entity/player";

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
		MatTooltipModule,
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

	isPlayerHost() {
		return this.isHost(this.playerService.getUuid());
	}

	canRemovePlayer(uuid: string) {
		return this.isPlayerHost() && this.playerService.getUuid() != uuid;
	}

	getPlayerStatus(uuid: string) {
		return this.isHost(uuid) ? "Host" : "Player";
	}

	getGameMaxPlayers() {
		return this.roomService.getGameMaxPlayers();
	}

	isValidPlayerCount() {
		const playerCount = this.getRoomPlayers().length;
		return playerCount >= this.roomService.getGameMinPlayers() && playerCount <= this.roomService.getGameMaxPlayers();
	}

	removePlayer(uuid: string) {
		this.roomService.removePlayer(uuid, this.getRoomCode(), false);
	}

	joinOrLeaveRoom(join: boolean) {
		this.playerService.joinOrLeaveRoom(this.getRoomCode(), join);
	}

	startGame() {
		console.log("Start");
	}

	deleteRoom() {
		this.roomService.deleteRoom();
	}

	editPlayerProfile() {
		this.dialog.open(PlayerEditDialogComponent);
	}

	private isHost(uuid: string) {
		return this.roomService.getHostUuid() == uuid;
	}
}
