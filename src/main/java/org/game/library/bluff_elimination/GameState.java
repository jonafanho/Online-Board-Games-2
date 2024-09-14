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
	private final Map<UUID, Integer> returnQueue = new HashMap<>();
	private final Map<UUID, Integer> eliminationQueue = new HashMap<>();
	private final Map<UUID, UUID> peekQueue = new HashMap<>();
	@Nullable
	private PlayAction queuedPlayAction;
	@Nullable
	private Character queuedShowCharacter;

	private final GameHistory gameHistory = new GameHistory();

	@Override
	public void process(Player player, Room room, Request request) {
		switch (getStage()) {
			case LOBBY:
				if (room.isHost(player)) {
					processLobby(room, request);
				}
				break;
			case MAIN:
				if (returnQueue.containsKey(player.getUuid())) {
					// Process return
					if (request.selectCharacters != null && !request.selectCharacters.isEmpty()) {
						processEliminationOrReturn(player.getUuid(), request.selectCharacters, returnQueue, false);
					}
				} else if (eliminationQueue.containsKey(player.getUuid())) {
					// Process elimination
					if (request.selectCharacters != null && !request.selectCharacters.isEmpty()) {
						processEliminationOrReturn(player.getUuid(), request.selectCharacters, eliminationQueue, true);
					}
				} else if (waitingForPlayers.contains(player.getUuid())) {
					if (peekQueue.containsKey(player.getUuid())) {
						// Process peek
						if (request.selectCharacters != null && request.selectCharacters.size() == 1) {
							processShow(peekQueue.get(player.getUuid()), player.getUuid(), request.selectCharacters.get(0));
						}
					}

					final PlayAction lastPlayAction = Utilities.getElement(currentPlayActions, -1);
					if (request.challenge) {
						// Process challenge
						if (lastPlayAction != null && lastPlayAction.action().characterNeeded != null && !lastPlayAction.hasBeenChallenged()) {
							setLastActionAlreadyChallenged();
							gameHistory.addChallengeToHistory(player.getUuid(), lastPlayAction.sender());
							if (processChallenge(lastPlayAction.sender(), player.getUuid(), lastPlayAction.action().characterNeeded)) {
								waitingForPlayers.removeIf(playerUuid -> !playerUuid.equals(lastPlayAction.target()));
							} else {
								waitingForPlayers.clear();
							}
						}
					} else if (request.accept) {
						// Process accept
						if (lastPlayAction != null && (lastPlayAction.action().characterNeeded != null || lastPlayAction.action() == Action.FOREIGN_AID || lastPlayAction.action() == Action.SHOW)) {
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
					if (currentPlayActions.isEmpty() && returnQueue.isEmpty() && eliminationQueue.isEmpty()) {
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
		final Map<UUID, WaitingForSelection> waitingForEliminationMap = new HashMap<>();
		final Map<UUID, WaitingForSelection> waitingForReturnMap = new HashMap<>();
		final List<TurnHistory> turnHistoryList = new ArrayList<>();

		if (getStage() != Stage.LOBBY) {
			playerDetails.forEach(playerDetailsEntry -> {
				final UUID uuid = playerDetailsEntry.uuid();
				final boolean isPlayer = getStage() == Stage.END || uuid.equals(player.getUuid());
				clientPlayerDetails.add(new PlayerDetails(uuid, playerDetailsEntry.coins(), isPlayer ? playerDetailsEntry.visibleCharacters() : new ArrayList<>(), isPlayer ? 0 : playerDetailsEntry.visibleCharacters().size()));
			});

			eliminationQueue.forEach((playerUuid, charactersToEliminate) -> waitingForEliminationMap.put(playerUuid, new WaitingForSelection(playerUuid, charactersToEliminate)));
			peekQueue.forEach((showingPlayerUuid, askingPlayerUuid) -> waitingForReturnMap.put(showingPlayerUuid, new WaitingForSelection(showingPlayerUuid, 1)));
			returnQueue.forEach((playerUuid, charactersToReturn) -> waitingForReturnMap.put(playerUuid, new WaitingForSelection(playerUuid, charactersToReturn)));

			gameHistory.getHistory().forEach(historyEvents -> {
				final List<HistoryEvent> newHistoryEvents = new ArrayList<>();
				historyEvents.forEach(historyEvent -> {
					final boolean isSender = player.getUuid().equals(historyEvent.sender());
					final boolean isTarget = player.getUuid().equals(historyEvent.target());
					final boolean showCharacters = getStage() == Stage.END || switch (historyEvent.event()) {
						case SHOW -> isSender || isTarget;
						case RETURN -> isSender;
						case ELIMINATION -> revealCharacterOnEliminated || isSender;
						default -> false;
					};
					newHistoryEvents.add(new HistoryEvent(historyEvent.sender(), historyEvent.event(), historyEvent.target(), showCharacters ? historyEvent.visibleCharacters() : new ArrayList<>(), showCharacters ? 0 : historyEvent.visibleCharacters().size()));
				});
				turnHistoryList.add(new TurnHistory(newHistoryEvents));
			});
		}

		final ClientState clientState = new ClientState(
				characterSet, startingCardsPerPlayer, startingCoinsPerPlayer, enableTeams, revealCharacterOnEliminated,
				clientPlayerDetails, currentPlayerTurnIndex, currentPlayActions,
				new ArrayList<>(waitingForPlayers), new ArrayList<>(waitingForEliminationMap.values()), new ArrayList<>(waitingForReturnMap.values()), turnHistoryList
		);
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
			case INCOME, FOREIGN_AID, TAX, EXCHANGE_AMBASSADOR, EXCHANGE_INQUISITOR -> true;
			case ELIMINATE, ASSASSINATE -> getAlivePlayerDetails(playAction.target(), targetPlayerDetails -> true);
			case STEAL -> getAlivePlayerDetails(playAction.target(), targetPlayerDetails -> targetPlayerDetails.coins() >= playAction.action().cost);
			case PEEK -> getAlivePlayerDetails(playAction.target(), targetPlayerDetails -> {
				peekQueue.put(playAction.target(), playAction.sender());
				return true;
			});
			default -> false;
		}) {
			currentPlayActions.add(playAction);
			gameHistory.addPlayActionToHistory(playAction);
			prepareForChallenge();
		}
	}

	private void processSecondaryAction(PlayAction lastPlayAction, PlayAction playAction) {
		final boolean senderWasTargeted = playAction.sender().equals(lastPlayAction.target());
		if (switch (lastPlayAction.action()) {
			case FOREIGN_AID -> playAction.action() == Action.BLOCK_FOREIGN_AID;
			case ASSASSINATE -> senderWasTargeted && playAction.action() == Action.BLOCK_ASSASSINATION;
			case STEAL -> senderWasTargeted && (playAction.action() == Action.BLOCK_STEALING_CAPTAIN || playAction.action() == Action.BLOCK_STEALING_AMBASSADOR || playAction.action() == Action.BLOCK_STEALING_INQUISITOR);
			case SHOW -> senderWasTargeted && lastPlayAction.sender().equals(playAction.target()) && playAction.action() == Action.FORCE_EXCHANGE;
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
				if (queuedPlayAction.action() == Action.SHOW) {
					gameHistory.addShowToHistory(queuedPlayAction.sender(), queuedPlayAction.target(), queuedShowCharacter);
				} else {
					gameHistory.addPlayActionToHistory(queuedPlayAction);
				}
				queuedPlayAction = null;
			}
			prepareForChallenge();
		}
	}

	private void processForceExchange(UUID target) {
		getAlivePlayerDetails(target, targetPlayerDetails -> {
			if (queuedShowCharacter != null) {
				targetPlayerDetails.visibleCharacters().remove(queuedShowCharacter);
				deck.add(queuedShowCharacter);
				targetPlayerDetails.visibleCharacters().add(deck.remove(0));
			}
			return false;
		});
	}

	private void prepareForChallenge() {
		final PlayAction playAction = Utilities.getElement(currentPlayActions, -1);
		if (playAction != null) {
			// If action requires a character, it can be challenged
			if (!playAction.hasBeenChallenged() && (playAction.action().characterNeeded != null || playAction.action() == Action.FOREIGN_AID || playAction.action() == Action.SHOW)) {
				if (playAction.action() == Action.SHOW) {
					waitingForPlayers.add(playAction.target());
				} else {
					playerDetails.forEach(playerDetailsEntry -> {
						if (!playerDetailsEntry.visibleCharacters().isEmpty()) {
							waitingForPlayers.add(playerDetailsEntry.uuid());
						}
					});
				}
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
				case FORCE_EXCHANGE:
					processForceExchange(playAction.target());
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
				gameHistory.addEliminationToHistory(playerUuid, new ArrayList<>(eliminatePlayerDetails.visibleCharacters()));
				eliminatePlayerDetails.visibleCharacters().clear();
				eliminationQueue.remove(playerUuid);
				waitingForPlayers.remove(playerUuid);
			} else {
				eliminationQueue.put(playerUuid, charactersToEliminate);
			}
			return false;
		});
	}

	private void queueReturn(UUID playerUuid, int count) {
		getAlivePlayerDetails(playerUuid, eliminatePlayerDetails -> {
			returnQueue.put(playerUuid, count);
			return false;
		});
	}

	private void processEliminationOrReturn(UUID playerUuid, List<Character> selectedCharacters, Map<UUID, Integer> queue, boolean isElimination) {
		final int charactersToEliminate = queue.get(playerUuid);
		if (charactersToEliminate > 0 && charactersToEliminate == selectedCharacters.size()) {
			getAlivePlayerDetails(playerUuid, eliminatePlayerDetails -> {
				final List<Character> visibleCharacters = eliminatePlayerDetails.visibleCharacters();
				if (new HashSet<>(visibleCharacters).containsAll(selectedCharacters)) {
					selectedCharacters.forEach(character -> {
						visibleCharacters.remove(character);
						deck.add(character);
					});

					if (isElimination) {
						gameHistory.addEliminationToHistory(playerUuid, selectedCharacters);
					} else {
						gameHistory.addReturnToHistory(playerUuid, selectedCharacters);
					}

					queue.remove(playerUuid);

					if (peekQueue.containsKey(playerUuid) && visibleCharacters.size() == 1) {
						processShow(peekQueue.get(playerUuid), playerUuid, visibleCharacters.get(0));
					}
				}
				return false;
			});
		}
	}

	private void processShow(UUID askingPlayerUuid, UUID showingPlayerUuid, Character selectedCharacter) {
		queuedPlayAction = new PlayAction(showingPlayerUuid, Action.SHOW, askingPlayerUuid, false);
		queuedShowCharacter = selectedCharacter;
		peekQueue.remove(showingPlayerUuid);
		processResponse(showingPlayerUuid);
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

	private void exchangeCards(UUID playerUuid, int count) {
		getAlivePlayerDetails(playerUuid, exchangePlayerDetails -> {
			for (int i = 0; i < count; i++) {
				exchangePlayerDetails.visibleCharacters().add(deck.remove(0));
			}
			queueReturn(playerUuid, count);
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

	private void nextTurn() {
		peekQueue.clear();
		queuedShowCharacter = null;
		// If no other players are alive, end the game
		if (playerDetails.stream().filter(playerDetailsEntry -> !playerDetailsEntry.visibleCharacters().isEmpty()).count() == 1) {
			setStage(Stage.END);
		} else {
			for (int i = 0; i < playerDetails.size() - 1; i++) {
				currentPlayerTurnIndex = (currentPlayerTurnIndex + 1) % playerDetails.size();
				if (!playerDetails.get(currentPlayerTurnIndex).visibleCharacters().isEmpty()) {
					waitingForPlayers.add(playerDetails.get(currentPlayerTurnIndex).uuid());
					gameHistory.getHistory().add(new ArrayList<>());
					return;
				}
			}
		}
	}
}
