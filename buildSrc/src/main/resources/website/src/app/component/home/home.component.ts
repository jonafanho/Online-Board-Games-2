import {Component} from "@angular/core";
import {MatInputModule} from "@angular/material/input";
import {FormsModule} from "@angular/forms";
import {SocketService} from "../../service/socket.service";
import {MatButtonModule} from "@angular/material/button";
import {TitleComponent} from "../title/title.component";
import {MatSelectModule} from "@angular/material/select";
import {GAMES} from "../../service/game.service";
import {MatTabsModule} from "@angular/material/tabs";
import {MatIconModule} from "@angular/material/icon";
import {MatCardModule} from "@angular/material/card";

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
	],
	templateUrl: "./home.component.html",
	styleUrl: "./home.component.css",
})
export class HomeComponent {
	protected filteredGames = GAMES;
	protected joinRoomDisabled = true;

	constructor(private socketService: SocketService) {
	}

	textChanged(roomCodeInput: HTMLInputElement) {
		const newValue = roomCodeInput.value.toString().toUpperCase().replaceAll(/[^A-Z0-9]/g, "");
		roomCodeInput.value = newValue;
		this.joinRoomDisabled = newValue.length < roomCodeInput.maxLength;
	}

	searchGames(searchText: string) {
		if (searchText == "") {
			this.filteredGames = GAMES;
		} else {
			this.filteredGames = GAMES.filter(game => `${game.id}\n${game.title}\n${game.tags}\n${game.description}`.toLowerCase().includes(searchText.toLowerCase()));
		}
	}

	onJoinRoom(roomCode: string) {
		console.log(roomCode);
	}

	onCreateRoom(game: string) {
		console.log(game);
	}
}
