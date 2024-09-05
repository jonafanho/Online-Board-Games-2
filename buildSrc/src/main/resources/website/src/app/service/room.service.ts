import {Injectable} from "@angular/core";
import {Stomp, StompSubscription} from "@stomp/stompjs";
import {Room} from "../entity/room";
import {NavigationEnd, Router} from "@angular/router";
import {PlayerService} from "./player.service";
import {Game} from "../entity/game";
import {HttpClient} from "@angular/common/http";

const HOST = document.location.hostname == "localhost" ? "localhost:8080" : document.location.host;
export const BASE_URL = `${document.location.protocol}//${HOST}`;

@Injectable({providedIn: "root"})
export class RoomService {
	private room?: Room;
	private game?: Game;
	private isPlayerJoined = false;
	private stompSubscription?: StompSubscription;

	constructor(private playerService: PlayerService, private readonly router: Router, private readonly httpClient: HttpClient) {
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

			const getRoomUpdate = (roomCode: string) => this.httpClient.get<Room>(`${BASE_URL}/api/public/getRoom?code=${roomCode}`).subscribe(room => this.updateRoom(room));
			const pathSplit = router.url.split("/");
			const roomCode = pathSplit[pathSplit.length - 1];

			if (roomCode) {
				try {
					this.stompSubscription = client.subscribe(`/topic/${roomCode}`, ({body}) => {
						const {socketUpdateType, sender} = JSON.parse(body) as { socketUpdateType: string, sender: string };
						if (sender != this.playerService.getPlayer().uuid) {
							if (socketUpdateType == "ROOM") {
								getRoomUpdate(roomCode);
							}
							if (socketUpdateType == "STATE") {
								this.playerService.postRequest(`/api/secured/getState?code=${roomCode}`, body, test => console.log(test));
							}
						}
					});
				} catch (e) {
				}

				if (roomCode != this.room?.code) {
					getRoomUpdate(roomCode);
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

	public createRoom(game: string, callback: (code: string) => void) {
		this.playerService.getRequest<Room>(`/api/secured/createRoom?game=${game}`, ({code}) => callback(code));
	}

	public joinRoom(roomCode: string) {
		this.playerService.getRequest<Room>(`/api/secured/joinRoom?code=${roomCode}`, room => this.updateRoom(room));
	}

	public removePlayer(uuid: string, roomCode: string) {
		this.playerService.getRequest<Room>(`/api/secured/leaveRoom?code=${roomCode}&playerUuid=${uuid}`, room => this.updateRoom(room));
	}

	public deleteRoom() {
		if (this.room) {
			this.playerService.getRequest(`/api/secured/deleteRoom?code=${this.room.code}`, () => this.updateRoom(undefined));
		}
	}

	public sendUpdate(body: string) {
		if (this.room) {
			this.playerService.postRequest(`/api/secured/update?code=${this.room.code}`, body, test => console.log(test));
		}
	}

	private updateRoom(room?: Room) {
		this.room = room ? Room.copy(room) : undefined;
		this.game = Game.GAMES.filter(game => game.id == this.getRoomGame())[0];
		this.isPlayerJoined = this.getRoomPlayers().some(player => player.uuid == this.playerService.getPlayer().uuid);

		if (this.room) {
			const hostUuid = this.room.host.uuid;
			this.room.players.sort((player1, player2) => (hostUuid == player1.uuid ? "" : player1.formattedName).localeCompare(hostUuid == player2.uuid ? "" : player2.formattedName));
		} else {
			this.router.navigate([""]).then();
		}
	}
}
