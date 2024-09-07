package org.game.core;

import lombok.Getter;
import org.springframework.lang.NonNull;

@Getter
public abstract class AbstractState<T extends Enum<T>> {

	private T stage = getDefaultStage();

	public void setStage(@NonNull T stage) {
		this.stage = stage;
	}

	@NonNull
	protected abstract T getDefaultStage();
}
