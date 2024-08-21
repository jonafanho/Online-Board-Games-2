import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {CookieService} from "ngx-cookie-service";
import {BASE_URL} from "./room.service";
import {Player} from "../entity/player";
import {isUUID} from "validator";

const PLAYER_UUID_COOKIE = "player-uuid";

@Injectable({providedIn: "root"})
export class PlayerService {
	private player;

	constructor(private readonly cookieService: CookieService, private readonly httpClient: HttpClient) {
		const id = cookieService.get(PLAYER_UUID_COOKIE);
		this.player = Player.create(isUUID(id) ? id : "");
		this.httpClient.get<Player>(`${BASE_URL}/getPlayer?uuid=${this.player.uuid}`).subscribe(player => this.updatePlayer(player));
	}

	public getUuid() {
		return this.player.uuid;
	}

	public getName() {
		return this.player.name;
	}

	public getFormattedName() {
		return this.player.formattedName;
	}

	public getIcon() {
		return this.player.icon;
	}

	public setPlayerProfile(name: string, icon: string) {
		this.httpClient.get<Player>(`${BASE_URL}/updatePlayer?uuid=${this.player.uuid}&name=${encodeURIComponent(name)}&icon=${icon}`).subscribe(player => this.updatePlayer(player));
	}

	public joinOrLeaveRoom(roomCode: string, join: boolean) {
		this.httpClient.get<Player>(`${BASE_URL}/${join ? "joinRoom" : "leaveRoom"}?playerUuid=${this.player.uuid}&roomCode=${roomCode}`).subscribe(player => this.updatePlayer(player));
	}

	private updatePlayer(player: Player) {
		this.cookieService.set(PLAYER_UUID_COOKIE, player.uuid, {path: "/", expires: 999999});
		this.player = Player.copy(player);
	}
}
