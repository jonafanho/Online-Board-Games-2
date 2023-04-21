package org.game.logic;

import it.unimi.dsi.fastutil.ints.IntIntImmutablePair;
import it.unimi.dsi.fastutil.longs.Long2BooleanAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import org.game.Utilities;
import org.game.data.Card;
import org.game.data.Character;
import org.game.data.Team;

import java.util.function.ToIntFunction;

public class Game implements Utilities {

	private State state;
	private int kingIndex;
	private Team winningTeam;
	private final Long2ObjectAVLTreeMap<String> users;
	private final LongImmutableList playerIds;
	private final ObjectImmutableList<Character> characters;
	private final ObjectArrayList<Quest> quests = new ObjectArrayList<>();
	private final boolean hasLady;
	private final boolean hasTrapper;
	private final int playerCount;

	public Game(boolean hasLady, boolean hasTrapper, Long2ObjectAVLTreeMap<String> users, LongImmutableList playerIds, ObjectImmutableList<Character> characters) {
		this.users = users;
		this.playerIds = playerIds;
		this.characters = characters;
		this.hasLady = hasLady;
		this.hasTrapper = hasTrapper;
		playerCount = playerIds.size();
		quests.add(new Quest());
	}

	public void selectPlayerForQuest(long playerId, long playerIdForQuest, boolean isAdd) {
		run(State.CHOOSE_QUEST, playerId, isKing(playerId) && containsPlayer(playerIdForQuest), () -> {
			final LongArraySet questPlayerIds = getCurrentQuest().playerIds;
			if (isAdd) {
				questPlayerIds.add(playerIdForQuest);
			} else {
				questPlayerIds.remove(playerIdForQuest);
			}
		});
	}

	public void confirmChooseQuest(long playerId) {
		final Quest quest = getCurrentQuest();
		run(State.CHOOSE_QUEST, playerId, isKing(playerId) && Utilities.getQuestPlayerCount(playerCount, getRound()) == quest.playerIds.size(), () -> {
			quest.prepareVoting();
			state = State.VOTE_FOR_QUEST;
		});
	}

	public void voteForQuest(long playerId, boolean approve) {
		run(State.VOTE_FOR_QUEST, playerId, true, () -> {
			final Quest quest = getCurrentQuest();
			final Long2BooleanAVLTreeMap votes = quest.getMostRecentVotes();
			votes.put(playerId, approve);
			if (votes.size() == playerCount) {
				int approveCount = 0;
				for (final boolean vote : votes.values()) {
					if (vote) {
						approveCount++;
					}
				}
				if (approveCount > playerCount / 2) {
					state = State.GO_ON_QUEST;
				} else if (quest.getVoteTrack() == MAX_REJECTED_VOTES) {
					winningTeam = Team.BAD;
					state = State.END;
				} else {
					setChooseQuestState();
				}
			}
		});
	}

	public void goOnQuest(long playerId, Card card) {
		final Character character = getCharacter(playerId);
		run(State.GO_ON_QUEST, playerId, character != null && character.getAllowedCards(this).contains(card), () -> {
			final Quest quest = getCurrentQuest();
			final Long2ObjectAVLTreeMap<Card> cards = quest.cards;
			cards.put(playerId, card);
			if (cards.size() == playerCount) {
				int rogueSuccessCount = 0;
				int rogueFailCount = 0;
				int magicCount = 0;
				int goodMessageCount1 = 0;
				int goodMessageCount2 = 0;
				int badMessageCount = 0;
				int allFailCount = 0;

				for (final long checkPlayerId : cards.keySet()) {
					final Card checkCard = cards.get(checkPlayerId);
					switch (checkCard) {
						case SUCCESS_ROGUE:
							rogueSuccessCount++;
							break;
						case FAIL_ROGUE:
							rogueFailCount++;
							allFailCount++;
							break;
						case MAGIC:
							magicCount++;
							break;
						case GOOD_MESSAGE:
							if (getCharacter(checkPlayerId) == Character.GOOD_MESSENGER_1) {
								goodMessageCount1++;
							} else {
								goodMessageCount2++;
							}
							break;
						case BAD_MESSAGE:
							badMessageCount++;
							allFailCount++;
							break;
						case FAIL:
							allFailCount++;
							break;
					}
				}

				if (getRound() == ROUNDS - 1) {
					if (getQuestResultTotals(result -> result.goodMessageCount1 + result.goodMessageCount2) >= 3) {
						allFailCount--;
					}
					if (getQuestResultTotals(result -> result.badMessageCount) >= 2) {
						allFailCount++;
					}
				}

				quest.addResult(
						(allFailCount >= Utilities.getMinimumFails(playerCount, getRound())) == (magicCount % 2 == 1),
						allFailCount,
						rogueSuccessCount,
						rogueFailCount,
						magicCount,
						goodMessageCount1,
						goodMessageCount2,
						badMessageCount
				);

				final IntIntImmutablePair questResultTotals = getQuestResultTotals();
				if (questResultTotals.leftInt() == QUESTS_TO_END) {
					if (rogueSuccessCount > 0 && getQuestResultTotals(result -> result.rogueSuccessCount) > 1) {
						winningTeam = Team.GOOD_ROGUE;
						state = State.END;
					} else {
						state = State.FIND_MERLIN;
					}
				} else if (questResultTotals.rightInt() == QUESTS_TO_END) {
					if (rogueFailCount > 0 && getQuestResultTotals(result -> result.rogueFailCount) > 1) {
						winningTeam = Team.BAD_ROGUE;
					} else {
						winningTeam = Team.BAD;
					}
					state = State.END;
				}
			}
		});
	}

	public int getRound() {
		return quests.size();
	}

	public int getGoodMessageCount1() {
		return getQuestResultTotals(result -> result.goodMessageCount1);
	}

	public int getGoodMessageCount2() {
		return getQuestResultTotals(result -> result.goodMessageCount2);
	}

	private void setChooseQuestState() {
		kingIndex++;
		if (kingIndex > playerCount) {
			kingIndex = 0;
		}
		state = State.CHOOSE_QUEST;
	}

	private boolean isKing(long playerId) {
		return playerIds.getLong(kingIndex) == playerId;
	}

	private boolean containsPlayer(long playerId) {
		return playerIds.contains(playerId);
	}

	private Character getCharacter(long playerId) {
		final int index = playerIds.indexOf(playerId);
		return index >= 0 ? characters.get(index) : null;
	}

	private Quest getCurrentQuest() {
		return quests.get(getRound() - 1);
	}

	private int getQuestResultTotals(ToIntFunction<Quest.Result> getField) {
		int total = 0;
		for (final Quest quest : quests) {
			total += getField.applyAsInt(quest.getResult());
		}
		return total;
	}

	private IntIntImmutablePair getQuestResultTotals() {
		int totalSuccesses = 0;
		int totalFails = 0;
		for (final Quest quest : quests) {
			if (quest.getResult().successful) {
				totalSuccesses++;
			} else {
				totalFails++;
			}
		}
		return new IntIntImmutablePair(totalSuccesses, totalFails);
	}

	private void run(State expectedState, long playerId, boolean additionalConditions, Runnable runnable) {
		if (expectedState == state && playerIds.contains(playerId) && additionalConditions) {
			runnable.run();
		}
	}

	private enum State {CHOOSE_QUEST, VOTE_FOR_QUEST, GO_ON_QUEST, FIND_UNTRUSTWORTHY_SERVANT, FIND_MERLIN, END}
}
