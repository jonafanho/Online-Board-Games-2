package org.game.library.the_round_table;

import org.game.core.AbstractGameState;
import org.game.entity.Player;

public final class GameState extends AbstractGameState<RequestBody> {

	@Override
	public void process(Player player, RequestBody requestBody) {

	}

	@Override
	public GameState getStateForPlayer(Player player) {
		return null;
	}
}
