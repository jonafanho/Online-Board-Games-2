package org.game.library.the_round_table;

public interface Utilities {

	int MIN_PLAYERS = 5;
	int MAX_PLAYERS = 10;
	int ROUNDS = 5;
	int MAX_REJECTED_VOTES = 5;
	int QUESTS_TO_END = 3;

	static int getQuestPlayerCount(int playerCount, int round) {
		final int[][] counts = new int[][]{
				{2, 2, 2, 3, 3, 3},
				{3, 3, 3, 4, 4, 4},
				{2, 4, 3, 4, 4, 4},
				{3, 3, 4, 5, 5, 5},
				{3, 4, 4, 5, 5, 5},
		};
		return invalidPlayerCount(playerCount) || invalidRound(round) ? 0 : counts[round][playerCount - MIN_PLAYERS];
	}

	static int getMinimumFails(int playerCount, int round) {
		return round == 3 && playerCount >= 7 ? 2 : 1;
	}

	static int getGoodCount(int playerCount) {
		final int[] counts = new int[]{3, 4, 4, 5, 6, 6};
		return invalidPlayerCount(playerCount) ? 0 : counts[playerCount - MIN_PLAYERS];
	}

	static int getBadCount(int playerCount) {
		return invalidPlayerCount(playerCount) ? 0 : playerCount - getGoodCount(playerCount);
	}

	static boolean invalidPlayerCount(int playerCount) {
		return playerCount < MIN_PLAYERS || playerCount > MAX_PLAYERS;
	}

	static boolean invalidRound(int round) {
		return round < 0 || round >= ROUNDS;
	}
}
