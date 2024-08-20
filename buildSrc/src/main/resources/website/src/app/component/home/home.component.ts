import {Component} from "@angular/core";
import {MatInputModule} from "@angular/material/input";
import {FormsModule} from "@angular/forms";
import {MatButtonModule} from "@angular/material/button";
import {TitleComponent} from "../title/title.component";
import {MatSelectModule} from "@angular/material/select";
import {GAMES} from "../../service/game.service";
import {MatTabsModule} from "@angular/material/tabs";
import {MatIconModule} from "@angular/material/icon";
import {MatCardModule} from "@angular/material/card";
import {MatProgressSpinnerModule} from "@angular/material/progress-spinner";
import {HttpClient} from "@angular/common/http";
import {BASE_URL, SocketService} from "../../service/socket.service";
import {Router} from "@angular/router";
import {Room} from "../../entity/room";

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
	],
	templateUrl: "./home.component.html",
	styleUrl: "./home.component.css",
})
export class HomeComponent {
	protected roomCodeValue = "";
	protected filteredGames = GAMES;
	protected joinRoomDisabled = true;
	protected loading = false;

	constructor(private readonly socketService: SocketService, private readonly httpClient: HttpClient, private readonly router: Router) {
	}

	textChanged(newValue: string) {
		this.roomCodeValue = newValue.toUpperCase().replaceAll(/[^A-Z0-9]/g, "");
		this.joinRoomDisabled = this.roomCodeValue.length < 6;
	}

	searchGames(searchText: string) {
		if (searchText == "") {
			this.filteredGames = GAMES;
		} else {
			this.filteredGames = GAMES.filter(game => `${game.id}\n${game.title}\n${game.tags}\n${game.description}`.toLowerCase().includes(searchText.toLowerCase()));
		}
	}

	onJoinRoom(roomCode: string) {
		this.router.navigate([`/lobby/${roomCode}`]).then();
	}

	onCreateRoom(game: string) {
		this.loading = true;
		this.httpClient.get<Room>(`${BASE_URL}/createRoom?game=${game}`).subscribe(room => {
			this.socketService.initWithRoom(room);
			this.router.navigate([`/lobby/${room.code}`]).then();
		});
	}
}
