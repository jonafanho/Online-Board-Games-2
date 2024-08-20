import {Injectable} from "@angular/core";
import {CompatClient, Stomp, StompSubscription} from "@stomp/stompjs";
import {Room} from "../entity/room";
import {HttpClient} from "@angular/common/http";
import {Router} from "@angular/router";
import {GAMES} from "./game.service";

const HOST = document.location.hostname == "localhost" ? "localhost:8080" : document.location.host;
export const BASE_URL = `${document.location.protocol}//${HOST}/api`;

@Injectable({providedIn: "root"})
export class SocketService {
	private readonly queue: ((client: CompatClient) => void)[] = [];
	private room?: Room;
	private gameTitle = "";
	private stompSubscription?: StompSubscription;

	constructor(private readonly httpClient: HttpClient, private readonly router: Router) {
		const client = Stomp.client(`ws://${HOST}/socket`);
		client.onWebSocketError = error => console.error("WebSocket error!", error);
		client.onStompError = frame => console.error("Broker error!", frame);
		client.activate();
		client.onConnect = () => {
			this.queue.forEach(callback => callback(client));
			this.queue.length = 0;
		};
	}

	public initWithRoomCode(roomCode: string) {
		if (roomCode != this.room?.code) {
			this.httpClient.get<Room>(`${BASE_URL}/getRoom?code=${roomCode}`).subscribe(room => this.initWithRoom(room));
		}
	}

	public initWithRoom(room: Room) {
		if (room) {
			this.updateRoom(room);
			if (this.stompSubscription) {
				this.stompSubscription.unsubscribe();
				this.stompSubscription = undefined;
			}
			this.queue.push(client => client.subscribe(`/topic/${this.room?.code}`, ({body}) => this.updateRoom(JSON.parse(body))));
		} else {
			this.router.navigate([""]).then();
		}
	}

	public getRoomCode() {
		return this.room ? this.room.code : "";
	}

	public getRoomGame() {
		return this.room ? this.room.game : "";
	}

	public getGameTitle() {
		return this.gameTitle;
	}

	private updateRoom(room: Room) {
		this.room = room;
		const filteredGames = GAMES.filter(game => game.id == this.getRoomGame());
		this.gameTitle = filteredGames.length > 0 ? filteredGames[0].title : "";
	}
}
