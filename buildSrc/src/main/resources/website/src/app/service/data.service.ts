import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {CookieService} from "ngx-cookie-service";
import {Player} from "../entity/player";
import {Room} from "../entity/room";
import {Game} from "../entity/game";
import {Router} from "@angular/router";
import {Socket} from "../tool/socket";
import {Observable} from "rxjs";
import {BaseRequest} from "../entity/base-request";
import {BaseClientState} from "../entity/base-client-state";

const PLAYER_UUID_COOKIE = "player-uuid";
const PLAYER_TOKEN_COOKIE = "player-token";
const COOKIE_OPTIONS = {path: "/", expires: 999999};
const HOST = document.location.hostname == "localhost" ? "localhost:8080" : document.location.host;
const BASE_URL = `${document.location.protocol}//${HOST}`;

@Injectable({providedIn: "root"})
export class DataService {
	private player?: Player;
	private token?: string;
	private room?: Room;
	private game?: Game;
	private isPlayerJoined = false;
	private isGameStarted?: boolean;

	private readonly runRequest = <T>(request: (headers: object) => Observable<T>, onSuccess?: (value: T) => void, onNull?: () => void) => {
		if (this.token && this.player) {
			request({headers: {uuid: this.player.uuid, token: this.token}}).subscribe(data => {
				if (data) {
					if (onSuccess) {
						onSuccess(data);
					}
				} else {
					if (onNull) {
						onNull();
					}
				}
			});
		}
	};
	private readonly getRequest = <T>(url: string, onSuccess?: (value: T) => void, onNull?: () => void) => this.runRequest(headers => this.httpClient.get<T>(`${BASE_URL}${url}`, headers), onSuccess, onNull);
	private readonly postRequest = <T>(url: string, requestBody: T, onSuccess?: (value: Room) => void, onNull?: () => void) => this.runRequest(headers => this.httpClient.post<Room>(`${BASE_URL}${url}`, requestBody, headers), onSuccess, onNull);

	protected constructor(private readonly cookieService: CookieService, private readonly httpClient: HttpClient, private readonly router: Router) {
		this.httpClient.get<{ uuid: string, token: string }>(`${BASE_URL}/api/public/register?uuid=${cookieService.get(PLAYER_UUID_COOKIE)}&token=${cookieService.get(PLAYER_TOKEN_COOKIE)}`).subscribe(({uuid, token}) => {
			this.cookieService.set(PLAYER_UUID_COOKIE, uuid, COOKIE_OPTIONS);
			this.cookieService.set(PLAYER_TOKEN_COOKIE, token, COOKIE_OPTIONS);
			this.token = token;
			this.httpClient.get<Player>(`${BASE_URL}/api/public/getPlayer?playerUuid=${uuid}`).subscribe(player => {
				this.updatePlayer(player);
				new Socket(HOST, roomCode => this.getRequest<Room>(`/api/secured/getRoom?code=${roomCode}`, room => this.updateRoom(room), () => this.updateRoom(undefined)), () => this.updateRoom(undefined), this, this.router);
			});
		});
	}

	// Player operations

	public getPlayer() {
		return this.player;
	}

	public updateProfile(name: string, icon: string) {
		this.getRequest<Player>(`/api/secured/updatePlayer?name=${encodeURIComponent(name)}&icon=${icon}`, player => this.updatePlayer(player));
	}

	private updatePlayer(player: Player) {
		this.player = Player.copy(player);
	}

	// Room operations

	public getRoom() {
		return this.room;
	}

	public getGame() {
		return this.game;
	}

	public getIsPlayerJoined() {
		return this.isPlayerJoined;
	}

	public getIsGameStarted() {
		return this.isGameStarted;
	}

	public createRoom(game: string, callback: (code: string) => void) {
		this.getRequest<Room>(`/api/secured/createRoom?game=${game}`, ({code}) => callback(code));
	}

	public joinRoom() {
		if (this.room) {
			this.getRequest<Room>(`/api/secured/joinRoom?code=${this.room.code}`, room => this.updateRoom(room));
		}
	}

	public removePlayer(uuid: string) {
		if (this.room) {
			this.getRequest<Room>(`/api/secured/leaveRoom?code=${this.room.code}&playerUuid=${uuid}`, room => this.updateRoom(room));
		}
	}

	public deleteRoom() {
		if (this.room) {
			this.getRequest<Room>(`/api/secured/deleteRoom?code=${this.room.code}`, () => this.updateRoom(undefined));
		}
	}

	public sendUpdate<T extends BaseRequest>(requestBody: T) {
		if (this.room) {
			this.postRequest<T>(`/api/secured/update?code=${this.room.code}`, requestBody, room => this.updateRoom(room));
		}
	}

	private updateRoom(room?: Room) {
		this.room = room ? Room.copy(room) : undefined;
		this.game = Game.GAMES.filter(game => game.id == this.room?.game)[0];
		this.isPlayerJoined = (this.room?.players ?? []).some(player => player.uuid == this.player?.uuid);
		const stage = this.room?.getState<BaseClientState<string>, string>()?.stage;
		this.isGameStarted = stage ? stage.toString() != "LOBBY" : undefined;

		if (this.room) {
			const hostUuid = this.room.host.uuid;
			this.room.players.sort((player1, player2) => (hostUuid == player1.uuid ? "" : player1.formattedName).localeCompare(hostUuid == player2.uuid ? "" : player2.formattedName));
		} else {
			this.router.navigate([""]).then();
		}
	}
}
