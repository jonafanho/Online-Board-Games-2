package org.game.core;

public abstract class AbstractRequest {

	public final boolean startGame;
	public final boolean endGame;

	protected AbstractRequest() {
		this.startGame = false;
		this.endGame = false;
	}
}
