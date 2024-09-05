package org.game.core;

import org.game.entity.Player;

public abstract class AbstractGameState<T> {

	public int stage = 0;

	public abstract void process(Player player, T requestBody);

	public abstract AbstractGameState<T> getStateForPlayer(Player player);
}
