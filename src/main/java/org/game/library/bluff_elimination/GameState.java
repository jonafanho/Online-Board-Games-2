package org.game.library.bluff_elimination;

import lombok.Getter;
import org.game.core.AbstractGameState;
import org.game.core.Utilities;
import org.game.entity.Player;
import org.game.entity.Room;
import org.game.library.bluff_elimination.generated.Character;
import org.game.library.bluff_elimination.generated.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.*;
import java.util.function.Predicate;

@Getter
public final class GameState extends AbstractGameState<Stage, Request, ClientState> implements Reference {

	@NonNull
	private CharacterSet characterSet = CharacterSet.WITH_AMBASSADOR;
	private int startingCardsPerPlayer = DEFAULT_STARTING_CARDS;
	private int startingCoinsPerPlayer = DEFAULT_STARTING_COINS;
	private boolean enableTeams;
	private boolean revealCharacterOnEliminated;

	private final List<Character> deck = new ArrayList<>();
	private final List<PlayerDetails> playerDetails = new ArrayList<>();

	private int currentPlayerTurnIndex;
	private final List<PlayAction> currentPlayActions = new ArrayList<>();
	private final Set<UUID> waitingForPlayers = new HashSet<>();
	private final Map<UUID, Integer> eliminationQueue = new HashMap<>();
	@Nullable
	private PlayAction queuedPlayAction;

	private final List<List<HistoryEvent>> history = new ArrayList<>();

	@Override
	public void process(Player player, Room room, Request request) {
		switch (getStage()) {
			case LOBBY:
				if (room.isHost(player)) {
					processLobby(room, request);
				}
				break;
			case MAIN:
				if (eliminationQueue.containsKey(player.getUuid())) {
					// Process elimination
					if (request.selectCharacters != null && !request.selectCharacters.isEmpty()) {
						processElimination(player.getUuid(), request.selectCharacters);
					}
				} else if (waitingForPlayers.contains(player.getUuid())) {
					final PlayAction lastPlayAction = Utilities.getElement(currentPlayActions, -1);
					if (request.challenge) {
						// Process challenge
						if (lastPlayAction != null && lastPlayAction.action().characterNeeded != null && !lastPlayAction.hasBeenChallenged()) {
							setLastActionAlreadyChallenged();
							addChallengeToHistory(player.getUuid(), lastPlayAction.sender());
							if (processChallenge(lastPlayAction.sender(), player.getUuid(), lastPlayAction.action().characterNeeded)) {
								waitingForPlayers.removeIf(playerUuid -> playerUuid != lastPlayAction.target());
							} else {
								waitingForPlayers.clear();
							}
						}
					} else if (request.accept) {
						// Process accept
						if (lastPlayAction != null && (lastPlayAction.action().characterNeeded != null || lastPlayAction.action() == Action.FOREIGN_AID)) {
							processResponse(player.getUuid());
						}
					} else if (request.playAction != null) {
						// Process action
						final PlayAction playAction = new PlayAction(player.getUuid(), request.playAction.action(), request.playAction.action().hasTarget ? request.playAction.target() : null, false);
						if ((!playAction.action().hasTarget || playAction.target() != null && !playAction.target().equals(player.getUuid())) && playerDetails.get(currentPlayerTurnIndex).coins() >= playAction.action().cost) {
							if (lastPlayAction == null) {
								// If no previous actions, process initial action
								processInitialAction(playAction);
							} else {
								// Process secondary action
								processSecondaryAction(lastPlayAction, playAction);
							}
						}
					}
				}

				if (waitingForPlayers.isEmpty()) {
					processAccept();
					// If no more pending tasks, move on to the next turn
					if (currentPlayActions.isEmpty() && eliminationQueue.isEmpty()) {
						nextTurn();
					}
				}
				break;
		}
	}

	@NonNull
	@Override
	public ClientState getStateForPlayer(Player player) {
		final List<PlayerDetails> clientPlayerDetails = new ArrayList<>();
		final Map<UUID, WaitingForPlayer> waitingForPlayersList = new HashMap<>();
		final List<TurnHistory> turnHistoryList = new ArrayList<>();
		final boolean newRevealCharacterOnEliminated = revealCharacterOnEliminated || getStage() == Stage.END;

		if (getStage() != Stage.LOBBY) {
			playerDetails.forEach(playerDetailsEntry -> {
				final UUID uuid = playerDetailsEntry.uuid();
				final boolean isPlayer = getStage() == Stage.END || uuid.equals(player.getUuid());
				clientPlayerDetails.add(new PlayerDetails(uuid, playerDetailsEntry.coins(), isPlayer ? playerDetailsEntry.visibleCharacters() : new ArrayList<>(), isPlayer ? 0 : playerDetailsEntry.visibleCharacters().size()));
			});

			waitingForPlayers.forEach(playerUuid -> waitingForPlayersList.put(playerUuid, new WaitingForPlayer(playerUuid, 0)));
			eliminationQueue.forEach((playerUuid, charactersToEliminate) -> waitingForPlayersList.put(playerUuid, new WaitingForPlayer(playerUuid, charactersToEliminate)));

			history.forEach(historyEvents -> {
				final List<HistoryEvent> newHistoryEvents = new ArrayList<>();
				historyEvents.forEach(historyEvent -> newHistoryEvents.add(new HistoryEvent(historyEvent.sender(), historyEvent.event(), historyEvent.target(), newRevealCharacterOnEliminated ? historyEvent.visibleCharacters() : new ArrayList<>(), newRevealCharacterOnEliminated ? 0 : historyEvent.visibleCharacters().size())));
				turnHistoryList.add(new TurnHistory(newHistoryEvents));
			});
		}

		final ClientState clientState = new ClientState(characterSet, startingCardsPerPlayer, startingCoinsPerPlayer, enableTeams, newRevealCharacterOnEliminated, clientPlayerDetails, currentPlayerTurnIndex, currentPlayActions, new ArrayList<>(waitingForPlayersList.values()), turnHistoryList);
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

	private void processLobby(Room room, Request request) {
		// Update game settings
		if (request.updateSettings) {
			if (request.characterSet != null) {
				characterSet = request.characterSet;
			}
			startingCardsPerPlayer = Math.max(MIN_STARTING_CARDS, Math.min(MAX_STARTING_CARDS, request.startingCardsPerPlayer));
			startingCoinsPerPlayer = Math.max(MIN_STARTING_COINS, Math.min(MAX_STARTING_COINS, request.startingCoinsPerPlayer));
			enableTeams = request.enableTeams;
			revealCharacterOnEliminated = request.revealCharacterOnElimination;
		}

		// Prepare to start the game
		if (request.startGame && isValidPlayerCount(room)) {
			// Generate deck
			final List<Player> players = new ArrayList<>(room.getPlayers());
			final Character[] characters = {Character.DUKE, Character.ASSASSIN, Character.CAPTAIN, characterSet == CharacterSet.WITH_AMBASSADOR ? Character.AMBASSADOR : Character.INQUISITOR, Character.CONTESSA};
			final int cardsPerCharacter = Math.max(MIN_CARDS_PER_CHARACTER, (int) Math.ceil((startingCardsPerPlayer * players.size() + 1F) / characters.length));
			for (final Character character : characters) {
				for (int i = 0; i < cardsPerCharacter; i++) {
					deck.add(character);
				}
			}

			// Shuffle deck and player order
			Collections.shuffle(deck);
			Collections.shuffle(players);

			// Deal cards
			players.forEach(roomPlayer -> {
				final List<Character> charactersForPlayer = new ArrayList<>();
				for (int i = 0; i < startingCardsPerPlayer; i++) {
					charactersForPlayer.add(deck.remove(0));
				}
				playerDetails.add(new PlayerDetails(roomPlayer.getUuid(), startingCoinsPerPlayer, charactersForPlayer, 0));
			});

			// Setup turn
			currentPlayerTurnIndex = -1;
			nextTurn();

			// Next stage
			setStage(Stage.MAIN);
		}
	}

	private void processInitialAction(PlayAction playAction) {
		if (switch (playAction.action()) {
			case INCOME, FOREIGN_AID, TAX, EXCHANGE_AMBASSADOR, EXCHANGE_INQUISITOR, PEEK -> true;
			case ELIMINATE, ASSASSINATE -> getAlivePlayerDetails(playAction.target(), targetPlayerDetails -> true);
			case STEAL -> getAlivePlayerDetails(playAction.target(), targetPlayerDetails -> targetPlayerDetails.coins() >= playAction.action().cost);
			default -> false;
		}) {
			currentPlayActions.add(playAction);
			addPlayActionToHistory(playAction);
			prepareForChallenge();
		}
	}

	private void processSecondaryAction(PlayAction lastPlayAction, PlayAction playAction) {
		if (switch (lastPlayAction.action()) {
			case FOREIGN_AID -> playAction.action() == Action.BLOCK_FOREIGN_AID;
			case ASSASSINATE -> playAction.sender().equals(lastPlayAction.target()) && playAction.action() == Action.BLOCK_ASSASSINATION;
			case STEAL -> playAction.sender().equals(lastPlayAction.target()) && (playAction.action() == Action.BLOCK_STEALING_CAPTAIN || playAction.action() == Action.BLOCK_STEALING_AMBASSADOR || playAction.action() == Action.BLOCK_STEALING_INQUISITOR);
			default -> false;
		}) {
			if (queuedPlayAction == null) {
				queuedPlayAction = playAction;
			}
			processResponse(playAction.sender());
		}
	}

	private void processResponse(UUID playerUuid) {
		waitingForPlayers.remove(playerUuid);
		if (waitingForPlayers.isEmpty()) {
			setLastActionAlreadyChallenged();
			if (queuedPlayAction != null) {
				currentPlayActions.add(queuedPlayAction);
				addPlayActionToHistory(queuedPlayAction);
				queuedPlayAction = null;
			}
			prepareForChallenge();
		}
	}

	private void prepareForChallenge() {
		final PlayAction playAction = Utilities.getElement(currentPlayActions, -1);
		if (playAction != null) {
			// If action requires a character, it can be challenged
			if (!playAction.hasBeenChallenged() && (playAction.action().characterNeeded != null || playAction.action() == Action.FOREIGN_AID)) {
				playerDetails.forEach(playerDetailsEntry -> {
					if (!playerDetailsEntry.visibleCharacters().isEmpty()) {
						waitingForPlayers.add(playerDetailsEntry.uuid());
					}
				});
			}
			waitingForPlayers.remove(playAction.sender());
		}
	}

	private boolean processChallenge(UUID challengedPlayerUuid, UUID senderUuid, Character challengedCharacter) {
		return getAlivePlayerDetails(challengedPlayerUuid, challengedPlayerDetails -> {
			if (challengedPlayerDetails.visibleCharacters().remove(challengedCharacter)) {
				// The player being challenged actually had the character
				queueElimination(senderUuid);
				deck.add(challengedCharacter);
				challengedPlayerDetails.visibleCharacters().add(deck.get(0));
				return true;
			} else {
				// The player being challenged was bluffing
				queueElimination(challengedPlayerDetails.uuid());
				currentPlayActions.remove(currentPlayActions.size() - 1);
				queuedPlayAction = null;
				return false;
			}
		});
	}

	private void processAccept() {
		while (!currentPlayActions.isEmpty()) {
			final PlayAction playAction = currentPlayActions.remove(currentPlayActions.size() - 1);
			switch (playAction.action()) {
				case INCOME:
				case FOREIGN_AID:
				case TAX:
					payForAction(playAction.sender(), playAction.action(), false);
					break;
				case ELIMINATE:
				case ASSASSINATE:
					payForAction(playAction.sender(), playAction.action(), false);
					queueElimination(playAction.target());
					break;
				case STEAL:
					payForAction(playAction.sender(), playAction.action(), false);
					payForAction(playAction.target(), playAction.action(), true);
					break;
				case EXCHANGE_AMBASSADOR:
					exchangeCards(playAction.sender(), 2);
					break;
				case EXCHANGE_INQUISITOR:
					exchangeCards(playAction.sender(), 1);
					break;
				case PEEK:
					// TODO
					break;
				case BLOCK_FOREIGN_AID:
				case BLOCK_ASSASSINATION:
				case BLOCK_STEALING_CAPTAIN:
				case BLOCK_STEALING_AMBASSADOR:
				case BLOCK_STEALING_INQUISITOR:
					currentPlayActions.remove(currentPlayActions.size() - 1);
					break;
			}
		}
	}

	private void setLastActionAlreadyChallenged() {
		final PlayAction playAction = Utilities.getElement(currentPlayActions, -1);
		if (playAction != null) {
			currentPlayActions.set(currentPlayActions.size() - 1, new PlayAction(playAction.sender(), playAction.action(), playAction.target(), true));
		}
	}

	private void queueElimination(UUID playerUuid) {
		getAlivePlayerDetails(playerUuid, eliminatePlayerDetails -> {
			final int existingCardCount = eliminatePlayerDetails.visibleCharacters().size();
			final int charactersToEliminate = Math.min(existingCardCount, eliminationQueue.getOrDefault(playerUuid, 0) + 1);
			if (existingCardCount == charactersToEliminate) {
				addEliminationToHistory(playerUuid, new ArrayList<>(eliminatePlayerDetails.visibleCharacters()));
				eliminatePlayerDetails.visibleCharacters().clear();
				eliminationQueue.remove(playerUuid);
				waitingForPlayers.remove(playerUuid);
			} else {
				eliminationQueue.put(playerUuid, charactersToEliminate);
			}
			return false;
		});
	}

	private void processElimination(UUID playerUuid, List<Character> selectedCharacters) {
		final int charactersToEliminate = eliminationQueue.get(playerUuid);
		if (charactersToEliminate > 0 && charactersToEliminate == selectedCharacters.size()) {
			getAlivePlayerDetails(playerUuid, eliminatePlayerDetails -> {
				final List<Character> visibleCharacters = eliminatePlayerDetails.visibleCharacters();
				if (new HashSet<>(visibleCharacters).containsAll(selectedCharacters)) {
					selectedCharacters.forEach(character -> {
						visibleCharacters.remove(character);
						deck.add(character);
					});
					addEliminationToHistory(playerUuid, selectedCharacters);
					eliminationQueue.remove(playerUuid);
				}
				return false;
			});
		}
	}

	private void payForAction(UUID playerUuid, Action action, boolean negative) {
		for (int i = 0; i < playerDetails.size(); i++) {
			final PlayerDetails currentPlayerDetails = playerDetails.get(i);
			if (currentPlayerDetails.uuid().equals(playerUuid)) {
				playerDetails.set(i, new PlayerDetails(currentPlayerDetails.uuid(), currentPlayerDetails.coins() - action.cost * (negative ? -1 : 1), currentPlayerDetails.visibleCharacters(), 0));
				return;
			}
		}
	}

	private void exchangeCards(UUID playerUuid, int cards) {
		getAlivePlayerDetails(playerUuid, exchangePlayerDetails -> {
			for (int i = 0; i < cards; i++) {
				exchangePlayerDetails.visibleCharacters().add(deck.remove(0));
				queueElimination(playerUuid);
			}
			return false;
		});
	}

	private boolean getAlivePlayerDetails(UUID playerUuid, Predicate<PlayerDetails> callback) {
		for (final PlayerDetails playerDetailsEntry : playerDetails) {
			if (playerDetailsEntry.uuid().equals(playerUuid) && !playerDetailsEntry.visibleCharacters().isEmpty()) {
				return callback.test(playerDetailsEntry);
			}
		}
		return false;
	}

	private void addChallengeToHistory(UUID sender, UUID target) {
		addToHistory(new HistoryEvent(sender, Event.CHALLENGE, target, new ArrayList<>(), 0));
	}

	private void addEliminationToHistory(UUID sender, List<Character> characters) {
		addToHistory(new HistoryEvent(sender, Event.ELIMINATION, null, characters, 0));
	}

	private void addPlayActionToHistory(PlayAction playAction) {
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
			case BLOCK_FOREIGN_AID -> Event.BLOCK_FOREIGN_AID;
			case BLOCK_ASSASSINATION -> Event.BLOCK_ASSASSINATION;
			case BLOCK_STEALING_CAPTAIN -> Event.BLOCK_STEALING_CAPTAIN;
			case BLOCK_STEALING_AMBASSADOR -> Event.BLOCK_STEALING_AMBASSADOR;
			case BLOCK_STEALING_INQUISITOR -> Event.BLOCK_STEALING_INQUISITOR;
		}, playAction.target(), new ArrayList<>(), 0));
	}

	private void addToHistory(HistoryEvent historyEvent) {
		Utilities.getElement(history, -1).add(historyEvent);
	}

	private void nextTurn() {
		// If no other players are alive, end the game
		if (playerDetails.stream().filter(playerDetailsEntry -> !playerDetailsEntry.visibleCharacters().isEmpty()).count() == 1) {
			setStage(Stage.END);
		} else {
			for (int i = 0; i < playerDetails.size() - 1; i++) {
				currentPlayerTurnIndex = (currentPlayerTurnIndex + 1) % playerDetails.size();
				if (!playerDetails.get(currentPlayerTurnIndex).visibleCharacters().isEmpty()) {
					waitingForPlayers.add(playerDetails.get(currentPlayerTurnIndex).uuid());
					history.add(new ArrayList<>());
					return;
				}
			}
		}
	}
}
