package org.game.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.game.entity.Player;
import org.game.entity.Room;
import org.springframework.lang.NonNull;

public abstract class AbstractGameState<T extends Enum<T>, U extends AbstractRequest, V extends AbstractClientState<T>> extends AbstractState<T> {

	public abstract void process(Player player, Room room, U request);

	@NonNull
	public abstract V getStateForPlayer(Player player);

	@JsonIgnore
	protected abstract int getMinPlayers();

	@JsonIgnore
	protected abstract int getMaxPlayers();

	public boolean isValidPlayerCount(Room room) {
		final int playerCount = room.getPlayers().size();
		return playerCount >= getMinPlayers() && playerCount <= getMaxPlayers();
	}
}
