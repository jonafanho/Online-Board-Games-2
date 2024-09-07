package org.game.library.bluff_elimination;

import org.game.core.AbstractGameState;
import org.game.entity.Player;
import org.game.library.bluff_elimination.generated.ClientState;
import org.game.library.bluff_elimination.generated.Request;
import org.game.library.bluff_elimination.generated.Stage;
import org.springframework.lang.NonNull;

public final class GameState extends AbstractGameState<Stage, Request, ClientState> {

	@Override
	public void process(Player player, Request request) {
	}

	@NonNull
	@Override
	public ClientState getStateForPlayer(Player player) {
		return null;
	}

	@NonNull
	@Override
	protected Stage getDefaultStage() {
		return Stage.LOBBY;
	}
}
