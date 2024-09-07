import {Player} from "./player";
import {BaseClientState} from "./base-client-state";

export class Room {

	private constructor(public readonly code: string, public readonly game: string, public readonly host: Player, public readonly players: Player[], private readonly state: string, private readonly stateObject: object) {
	}

	public getState<T extends BaseClientState<U>, U>(): T {
		return this.stateObject as T;
	}

	public static copy(room: Room) {
		return new Room(room.code, room.game, Player.copy(room.host), room.players.map(player => Player.copy(player)), room.state, JSON.parse(room.state));
	}
}
