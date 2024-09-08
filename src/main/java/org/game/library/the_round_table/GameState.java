package org.game.library.the_round_table;

import org.game.core.AbstractGameState;
import org.game.entity.Player;
import org.game.entity.Room;
import org.game.library.the_round_table.generated.ClientState;
import org.game.library.the_round_table.generated.Request;
import org.game.library.the_round_table.generated.Stage;
import org.springframework.lang.NonNull;

import java.util.ArrayList;

public final class GameState extends AbstractGameState<Stage, Request, ClientState> {

	@Override
	public void process(Player player, Room room, Request request) {
	}

	@NonNull
	@Override
	public ClientState getStateForPlayer(Player player) {
		final ClientState clientState = new ClientState(new ArrayList<>(), 0, new ArrayList<>(), new ArrayList<>());
		clientState.setStage(getStage());
		return clientState;
	}

	@Override
	protected int getMinPlayers() {
		return BaseGameProperties.MIN_PLAYERS;
	}

	@Override
	protected int getMaxPlayers() {
		return BaseGameProperties.MAX_PLAYERS;
	}

	@NonNull
	@Override
	protected Stage getDefaultStage() {
		return Stage.LOBBY;
	}
}
