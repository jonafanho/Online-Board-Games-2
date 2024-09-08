package org.game.library.bluff_elimination;

import org.game.core.AbstractGameState;
import org.game.entity.Player;
import org.game.entity.Room;
import org.game.library.bluff_elimination.generated.CharacterSet;
import org.game.library.bluff_elimination.generated.ClientState;
import org.game.library.bluff_elimination.generated.Request;
import org.game.library.bluff_elimination.generated.Stage;
import org.springframework.lang.NonNull;

import java.util.ArrayList;

public final class GameState extends AbstractGameState<Stage, Request, ClientState> {

	@NonNull
	public CharacterSet characterSet = CharacterSet.WITH_AMBASSADOR;
	public boolean revealCharacterOnEliminated;

	@Override
	public void process(Player player, Room room, Request request) {
		final boolean isHost = room.isHost(player);
		final boolean isValidPlayerCount = isValidPlayerCount(room);

		switch (getStage()) {
			case LOBBY:
				if (isHost) {
					if (request.updateSettings) {
						if (request.characterSet != null) {
							characterSet = request.characterSet;
						}
						revealCharacterOnEliminated = request.revealCharacterOnElimination;
					}
					if (request.startGame && isValidPlayerCount) {
						setStage(Stage.MAIN);
					}
				}
				break;
			case MAIN:
				break;
		}
	}

	@NonNull
	@Override
	public ClientState getStateForPlayer(Player player) {
		final ClientState clientState;
		switch (getStage()) {
			case MAIN:
				clientState = new ClientState(characterSet, revealCharacterOnEliminated, new ArrayList<>());
				break;
			default:
				clientState = new ClientState(characterSet, revealCharacterOnEliminated, new ArrayList<>());
				break;
		}
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
