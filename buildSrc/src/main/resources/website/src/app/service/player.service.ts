import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {CookieService} from "ngx-cookie-service";
import {BASE_URL} from "./socket.service";
import {Player} from "../entity/player";
import {isUUID} from "validator";

const PLAYER_UUID_COOKIE = "player-uuid";

@Injectable({providedIn: "root"})
export class PlayerService {
	private player;

	constructor(private readonly cookieService: CookieService, private readonly httpClient: HttpClient) {
		const id = cookieService.get(PLAYER_UUID_COOKIE);
		this.player = new Player(isUUID(id) ? id : "", "", "");
		this.httpClient.get<Player>(`${BASE_URL}/getPlayer?uuid=${this.player.uuid}`).subscribe(player => this.updatePlayer(player));
	}

	public getName() {
		return this.player.name;
	}

	public getIcon() {
		return this.player.icon;
	}

	public setPlayerProfile(name: string, icon: string) {
		this.httpClient.get<Player>(`${BASE_URL}/updatePlayer?uuid=${this.player.uuid}&name=${encodeURIComponent(name)}&icon=${icon}`).subscribe(player => this.updatePlayer(player));
	}

	private updatePlayer(player: Player) {
		this.cookieService.set(PLAYER_UUID_COOKIE, player.uuid);
		this.player = player;
	}
}
