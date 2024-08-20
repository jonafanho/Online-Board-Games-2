export class Game {

	constructor(
		public readonly id: string,
		public readonly tags: string[],
		public readonly title: string,
		public readonly description: string,
	) {
	}
}

export const GAMES: Game[] = [
	new Game("the-round-table", ["resistance", "avalon"], "The Round Table", "A fun game"),
	new Game("bluff-elimination", ["coup"], "Bluff Elimination", "Another fun game"),
];
