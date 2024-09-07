import {DEFAULT_ICON} from "../component/player/player-edit-dialog.component";

export class Player {

	private constructor(public readonly uuid: string, public readonly name: string, public readonly formattedName: string, public readonly icon: string) {
	}

	public static copy(player: Player) {
		const trimmedName = player.name.trim();
		const trimmedIcon = player.icon ? player.icon.trim().toLowerCase() : "";
		return new Player(player.uuid, trimmedName, trimmedName == "" ? `Player ${player.uuid.split("-")[0].toUpperCase()}` : trimmedName, trimmedIcon == "" ? DEFAULT_ICON : trimmedIcon);
	}
}
