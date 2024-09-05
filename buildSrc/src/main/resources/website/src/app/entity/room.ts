import {Player} from "./player";

export class Room {

	private constructor(public readonly code: string, public readonly game: string, public readonly host: Player, public readonly players: Player[], public readonly lastModifiedDate: Date) {
	}

	public static copy(room: Room) {
		return new Room(room.code, room.game, Player.copy(room.host), room.players.map(player => Player.copy(player)), room.lastModifiedDate);
	}
}
