import {Component} from "@angular/core";
import {MatInputModule} from "@angular/material/input";
import {FormsModule} from "@angular/forms";
import {MatButtonModule} from "@angular/material/button";
import {TitleComponent} from "../title/title.component";
import {MatSelectModule} from "@angular/material/select";
import {MatTabsModule} from "@angular/material/tabs";
import {MatIconModule} from "@angular/material/icon";
import {MatCardModule} from "@angular/material/card";
import {MatProgressSpinnerModule} from "@angular/material/progress-spinner";
import {Router} from "@angular/router";
import {MatTooltipModule} from "@angular/material/tooltip";
import {Game} from "../../entity/game";
import {MatChipsModule} from "@angular/material/chips";
import {RoomService} from "../../service/room.service";

@Component({
	selector: "app-home",
	standalone: true,
	imports: [
		MatInputModule,
		MatButtonModule,
		FormsModule,
		TitleComponent,
		MatSelectModule,
		MatTabsModule,
		MatIconModule,
		MatCardModule,
		MatProgressSpinnerModule,
		MatTooltipModule,
		MatChipsModule,
	],
	templateUrl: "./home.component.html",
	styleUrl: "./home.component.css",
})
export class HomeComponent {
	protected roomCodeValue = "";
	protected filteredGames = Game.GAMES;
	protected joinRoomDisabled = true;

	constructor(private readonly roomService: RoomService, private readonly router: Router) {
	}

	textChanged(newValue: string) {
		this.roomCodeValue = newValue.toUpperCase().replaceAll(/[^A-Z0-9]/g, "");
		this.joinRoomDisabled = this.roomCodeValue.length < 6;
	}

	searchGames(searchText: string) {
		if (searchText == "") {
			this.filteredGames = Game.GAMES;
		} else {
			this.filteredGames = Game.GAMES.filter(game => `${game.id}\n${game.title}\n${game.hiddenTags}\n${game.description}`.toLowerCase().includes(searchText.toLowerCase()));
		}
	}

	onJoinRoom(roomCode: string) {
		this.router.navigate([`/game/${roomCode}`]).then();
	}

	onCreateRoom(game: string) {
		this.roomService.createRoom(game, code => this.onJoinRoom(code));
	}
}
