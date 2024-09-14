package org.game.library.bluff_elimination;

import lombok.Getter;
import org.game.core.Utilities;
import org.game.library.bluff_elimination.generated.Character;
import org.game.library.bluff_elimination.generated.Event;
import org.game.library.bluff_elimination.generated.HistoryEvent;
import org.game.library.bluff_elimination.generated.PlayAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
public final class GameHistory {

	private final List<List<HistoryEvent>> history = new ArrayList<>();

	public void addChallengeToHistory(UUID sender, UUID target) {
		addToHistory(new HistoryEvent(sender, Event.CHALLENGE, target, new ArrayList<>(), 0));
	}

	public void addEliminationToHistory(UUID sender, List<Character> characters) {
		addToHistory(new HistoryEvent(sender, Event.ELIMINATION, null, characters, 0));
	}

	public void addReturnToHistory(UUID sender, List<Character> characters) {
		addToHistory(new HistoryEvent(sender, Event.RETURN, null, characters, 0));
	}

	public void addShowToHistory(UUID sender, UUID target, Character character) {
		addToHistory(new HistoryEvent(sender, Event.SHOW, target, Collections.singletonList(character), 0));
	}

	public void addPlayActionToHistory(PlayAction playAction) {
		addToHistory(new HistoryEvent(playAction.sender(), switch (playAction.action()) {
			case INCOME -> Event.INCOME;
			case FOREIGN_AID -> Event.FOREIGN_AID;
			case ELIMINATE -> Event.ELIMINATE;
			case TAX -> Event.TAX;
			case ASSASSINATE -> Event.ASSASSINATE;
			case STEAL -> Event.STEAL;
			case EXCHANGE_AMBASSADOR -> Event.EXCHANGE_AMBASSADOR;
			case EXCHANGE_INQUISITOR -> Event.EXCHANGE_INQUISITOR;
			case PEEK -> Event.PEEK;
			case SHOW -> Event.SHOW;
			case FORCE_EXCHANGE -> Event.FORCE_EXCHANGE;
			case BLOCK_FOREIGN_AID -> Event.BLOCK_FOREIGN_AID;
			case BLOCK_ASSASSINATION -> Event.BLOCK_ASSASSINATION;
			case BLOCK_STEALING_CAPTAIN -> Event.BLOCK_STEALING_CAPTAIN;
			case BLOCK_STEALING_AMBASSADOR -> Event.BLOCK_STEALING_AMBASSADOR;
			case BLOCK_STEALING_INQUISITOR -> Event.BLOCK_STEALING_INQUISITOR;
		}, playAction.target(), new ArrayList<>(), 0));
	}

	private void addToHistory(HistoryEvent historyEvent) {
		if (!history.isEmpty()) {
			Utilities.getElement(history, -1).add(historyEvent);
		}
	}
}
