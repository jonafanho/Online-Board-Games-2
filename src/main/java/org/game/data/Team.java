package org.game.data;

public enum Team {

	GOOD(false), BAD(true), GOOD_ROGUE(false), BAD_ROGUE(true);

	public final boolean isBad;

	Team(boolean isBad) {
		this.isBad = isBad;
	}
}
