import {Injectable} from "@angular/core";
import {Stomp, StompSubscription} from "@stomp/stompjs";
import {Room} from "../entity/room";
import {HttpClient} from "@angular/common/http";
import {NavigationEnd, Router} from "@angular/router";
import {GAMES} from "./game.service";
import {PlayerService} from "./player.service";

const HOST = document.location.hostname == "localhost" ? "localhost:8080" : document.location.host;
export const BASE_URL = `${document.location.protocol}//${HOST}/api`;

@Injectable({providedIn: "root"})
export class RoomService {
	private room?: Room;
	private gameTitle = "";
	private isPlayerJoined = false;
	private stompSubscription?: StompSubscription;

	constructor(private playerService: PlayerService, private readonly httpClient: HttpClient, private readonly router: Router) {
		const client = Stomp.client(`ws://${HOST}/socket`);
		client.onWebSocketError = error => console.error("WebSocket error!", error);
		client.onStompError = frame => console.error("Broker error!", frame);
		client.reconnectDelay = 2000;
		client.activate();

		const checkSubscriptions = () => {
			if (this.stompSubscription) {
				this.stompSubscription.unsubscribe();
				this.stompSubscription = undefined;
			}

			const pathSplit = router.url.split("/");
			const roomCode = pathSplit[pathSplit.length - 1];

			if (roomCode) {
				try {
					this.stompSubscription = client.subscribe(`/topic/${roomCode}`, ({body}) => this.updateRoom(JSON.parse(body)));
				} catch (e) {
				}

				if (roomCode != this.room?.code) {
					this.httpClient.get<Room>(`${BASE_URL}/getRoom?code=${roomCode}`).subscribe(room => this.updateRoom(room));
				}
			} else {
				this.updateRoom(undefined);
			}
		};

		client.onConnect = () => checkSubscriptions();
		router.events.subscribe((event) => {
			if (event instanceof NavigationEnd) {
				checkSubscriptions();
			}
		});
	}

	public getRoomCode() {
		return this.room ? this.room.code : "";
	}

	public getRoomGame() {
		return this.room ? this.room.game : "";
	}

	public getHostUuid() {
		return this.room ? this.room.host.uuid : "";
	}

	public getGameTitle() {
		return this.gameTitle;
	}

	public getIsPlayerJoined() {
		return this.isPlayerJoined;
	}

	public getRoomPlayers() {
		return this.room ? this.room.players : [];
	}

	private updateRoom(room?: Room) {
		this.room = room ? Room.copy(room) : undefined;
		const filteredGames = GAMES.filter(game => game.id == this.getRoomGame());
		this.gameTitle = filteredGames.length > 0 ? filteredGames[0].title : "";
		this.isPlayerJoined = this.getRoomPlayers().some(player => player.uuid == this.playerService.getUuid());

		if (this.room) {
			this.room.players.sort((player1, player2) => player1.formattedName.localeCompare(player2.formattedName));
		} else {
			this.router.navigate([""]).then();
		}
	}
}
