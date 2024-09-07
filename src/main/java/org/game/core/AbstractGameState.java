package org.game.core;

import org.game.entity.Player;
import org.springframework.lang.NonNull;

public abstract class AbstractGameState<T extends Enum<T>, U extends AbstractRequest, V extends AbstractClientState<T>> extends AbstractState<T> {

	public abstract void process(Player player, U request);

	@NonNull
	public abstract V getStateForPlayer(Player player);
}
