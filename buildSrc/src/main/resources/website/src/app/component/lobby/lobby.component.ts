import {Component, inject} from "@angular/core";
import {TitleComponent} from "../title/title.component";
import {MatFormFieldModule} from "@angular/material/form-field";
import {MatIconModule} from "@angular/material/icon";
import {MatInputModule} from "@angular/material/input";
import {MatButtonModule} from "@angular/material/button";
import {MatDialog} from "@angular/material/dialog";
import {PlayerEditDialogComponent} from "../player/player-edit-dialog.component";
import {MatSlideToggleModule} from "@angular/material/slide-toggle";
import {RouterLink} from "@angular/router";
import {MatListModule} from "@angular/material/list";
import {MatTooltipModule} from "@angular/material/tooltip";
import {BaseRequest} from "../../entity/base-request";
import {GameComponent} from "../game/game.component";
import {DataService} from "../../service/data.service";

@Component({
	selector: "app-lobby",
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
		GameComponent,
	],
	templateUrl: "./lobby.component.html",
	styleUrl: "./lobby.component.css",
})
export class LobbyComponent {
	private readonly dialog = inject(MatDialog);

	constructor(private readonly dataService: DataService) {
	}

	getRoomCode() {
		return this.dataService.getRoom()?.code ?? "";
	}

	getGameTitle() {
		return this.dataService.getGame()?.title ?? "";
	}

	getIsPlayerJoined() {
		return this.dataService.getIsPlayerJoined();
	}

	getRoomPlayers() {
		return this.dataService.getRoom()?.players ?? [];
	}

	isPlayerHost() {
		return this.isHost(this.dataService.getPlayer()?.uuid ?? "");
	}

	canRemovePlayer(uuid: string) {
		return this.isPlayerHost() && this.dataService.getPlayer()?.uuid != uuid;
	}

	getPlayerStatus(uuid: string) {
		return this.isHost(uuid) ? "Host" : "Player";
	}

	getGameMaxPlayers() {
		return this.dataService.getGame()?.maxPlayers;
	}

	isValidPlayerCount() {
		const playerCount = this.getRoomPlayers().length;
		return playerCount >= (this.dataService.getGame()?.minPlayers ?? 0) && playerCount <= (this.dataService.getGame()?.maxPlayers ?? 0);
	}

	isGameStarted() {
		return this.dataService.getIsGameStarted();
	}

	removePlayer(uuid: string) {
		this.dataService.removePlayer(uuid);
	}

	joinOrLeaveRoom(join: boolean) {
		if (join) {
			this.dataService.joinRoom();
		} else {
			const player = this.dataService.getPlayer();
			if (player) {
				this.dataService.removePlayer(player.uuid);
			}
		}
	}

	startGame() {
		this.dataService.sendUpdate<BaseRequest>({startGame: true});
	}

	deleteRoom() {
		this.dataService.deleteRoom();
	}

	editPlayerProfile() {
		this.dialog.open(PlayerEditDialogComponent);
	}

	private isHost(uuid: string) {
		return uuid && this.dataService.getRoom()?.host.uuid == uuid;
	}
}
