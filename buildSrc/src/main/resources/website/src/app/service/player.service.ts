import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {CookieService} from "ngx-cookie-service";
import {BASE_URL} from "./room.service";
import {Player} from "../entity/player";

const PLAYER_UUID_COOKIE = "player-uuid";
const PLAYER_TOKEN_COOKIE = "player-token";
const COOKIE_OPTIONS = {path: "/", expires: 999999};

@Injectable({providedIn: "root"})
export class PlayerService {
	private player = Player.create("");
	private token = "";

	private readonly getHeaders = () => ({headers: {uuid: this.player.uuid, token: this.token}});
	private readonly runCallback = <T>(callback: (value: T) => void) => ((value: T) => {
		if (value) {
			callback(value);
		}
	});

	constructor(private readonly cookieService: CookieService, private readonly httpClient: HttpClient) {
		this.httpClient.get<{ uuid: string, token: string }>(`${BASE_URL}/api/public/register?uuid=${cookieService.get(PLAYER_UUID_COOKIE)}&token=${cookieService.get(PLAYER_TOKEN_COOKIE)}`).subscribe(({uuid, token}) => {
			this.cookieService.set(PLAYER_UUID_COOKIE, uuid, COOKIE_OPTIONS);
			this.cookieService.set(PLAYER_TOKEN_COOKIE, token, COOKIE_OPTIONS);
			this.player = Player.create(uuid);
			this.token = token;
			this.httpClient.get<Player>(`${BASE_URL}/api/public/getPlayer?playerUuid=${uuid}`).subscribe(player => this.player = Player.copy(player));
		});
	}

	public getRequest<T>(url: string, callback: (value: T) => void = () => {
	}) {
		this.httpClient.get<T>(`${BASE_URL}${url}`, this.getHeaders()).subscribe(this.runCallback(callback));
	}

	public postRequest<T>(url: string, body: string, callback: (value: T) => void = () => {
	}) {
		this.httpClient.post<T>(`${BASE_URL}${url}`, body, this.getHeaders()).subscribe(this.runCallback(callback));
	}

	public getPlayer() {
		return this.player;
	}

	public updateProfile(name: string, icon: string) {
		this.getRequest<Player>(`/api/secured/updatePlayer?name=${encodeURIComponent(name)}&icon=${icon}`, player => this.player = Player.copy(player));
	}
}
