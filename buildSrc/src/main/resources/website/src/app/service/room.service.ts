import {Injectable} from "@angular/core";
import {Stomp, StompSubscription} from "@stomp/stompjs";
import {Room} from "../entity/room";
import {HttpClient} from "@angular/common/http";
import {NavigationEnd, Router} from "@angular/router";
import {PlayerService} from "./player.service";
import {Player} from "../entity/player";
import {Game} from "../entity/game";

const HOST = document.location.hostname == "localhost" ? "localhost:8080" : document.location.host;
export const BASE_URL = `${document.location.protocol}//${HOST}/api`;

@Injectable({providedIn: "root"})
export class RoomService {
	private room?: Room;
	private game?: Game;
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
		return this.game ? this.game.title : "";
	}

	public getGameMinPlayers() {
		return this.game ? this.game.minPlayers : 0;
	}

	public getGameMaxPlayers() {
		return this.game ? this.game.maxPlayers : 0;
	}

	public getIsPlayerJoined() {
		return this.isPlayerJoined;
	}

	public getRoomPlayers() {
		return this.room ? this.room.players : [];
	}

	public removePlayer(uuid: string, roomCode: string, b: boolean) {
		this.httpClient.get<Player>(`${BASE_URL}/leaveRoom?playerUuid=${uuid}&roomCode=${roomCode}`).subscribe();
	}

	public deleteRoom() {
		if (this.room) {
			this.httpClient.get<Player>(`${BASE_URL}/deleteRoom?code=${this.room.code}`).subscribe();
		}
	}

	private updateRoom(room?: Room) {
		this.room = room ? Room.copy(room) : undefined;
		this.game = Game.GAMES.filter(game => game.id == this.getRoomGame())[0];
		this.isPlayerJoined = this.getRoomPlayers().some(player => player.uuid == this.playerService.getUuid());

		if (this.room) {
			const hostUuid = this.room.host.uuid;
			this.room.players.sort((player1, player2) => (hostUuid == player1.uuid ? "" : player1.formattedName).localeCompare(hostUuid == player2.uuid ? "" : player2.formattedName));
		} else {
			this.router.navigate([""]).then();
		}
	}
}
