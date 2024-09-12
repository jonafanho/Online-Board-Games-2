import {Component, inject} from "@angular/core";
import {TitleComponent} from "../title/title.component";
import {MatFormFieldModule} from "@angular/material/form-field";
import {MatIconModule} from "@angular/material/icon";
import {MatInputModule} from "@angular/material/input";
import {MatButtonModule} from "@angular/material/button";
import {MatDialog} from "@angular/material/dialog";
import {PlayerEditDialogComponent} from "../player-edit-dialog/player-edit-dialog.component";
import {MatSlideToggleModule} from "@angular/material/slide-toggle";
import {RouterLink} from "@angular/router";
import {MatTooltipModule} from "@angular/material/tooltip";
import {BaseRequest} from "../../entity/base-request";
import {GameComponent} from "../game/game.component";
import {DataService} from "../../service/data.service";
import {GameSettingsComponent} from "../game-settings/game-settings.component";
import {MatCardModule} from "@angular/material/card";
import {InfoCardComponent} from "../info-card/info-card.component";
import {PlayerCardComponent} from "../player-card/player-card.component";
import {MatDividerModule} from "@angular/material/divider";

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
		MatDividerModule,
		MatTooltipModule,
		GameComponent,
		GameSettingsComponent,
		MatCardModule,
		InfoCardComponent,
		PlayerCardComponent,
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

	isHost() {
		return this.dataService.isHost();
	}

	canRemovePlayer(uuid: string) {
		return this.dataService.isHost() && this.dataService.getPlayer()?.uuid != uuid;
	}

	getPlayerStatus(uuid: string) {
		return this.dataService.getRoom()?.host.uuid == uuid ? "Host" : "Player";
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
}
