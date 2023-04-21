package org.game.logic;

import it.unimi.dsi.fastutil.longs.Long2BooleanAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.game.data.Card;

public class Quest {

	private Result result;
	protected final LongArraySet playerIds = new LongArraySet();
	protected final Long2ObjectAVLTreeMap<Card> cards = new Long2ObjectAVLTreeMap<>();
	private final ObjectArrayList<Long2BooleanAVLTreeMap> votes = new ObjectArrayList<>();

	protected void prepareVoting() {
		votes.add(new Long2BooleanAVLTreeMap());
	}

	protected Long2BooleanAVLTreeMap getMostRecentVotes() {
		return votes.isEmpty() ? new Long2BooleanAVLTreeMap() : votes.get(votes.size() - 1);
	}

	protected int getVoteTrack() {
		return votes.size();
	}

	protected void addResult(boolean successful, int allFailCount, int rogueSuccessCount, int rogueFailCount, int magicCount, int goodMessageCount1, int goodMessageCount2, int badMessageCount) {
		result = new Result(successful, allFailCount, rogueSuccessCount, rogueFailCount, magicCount, goodMessageCount1, goodMessageCount2, badMessageCount);
	}

	protected Result getResult() {
		return result;
	}

	protected static class Result {

		protected final boolean successful;
		protected final int allFailCount;
		protected final int rogueSuccessCount;
		protected final int rogueFailCount;
		protected final int magicCount;
		protected final int goodMessageCount1;
		protected final int goodMessageCount2;
		protected final int badMessageCount;

		private Result(boolean successful, int allFailCount, int rogueSuccessCount, int rogueFailCount, int magicCount, int goodMessageCount1, int goodMessageCount2, int badMessageCount) {
			this.successful = successful;
			this.allFailCount = allFailCount;
			this.rogueSuccessCount = rogueSuccessCount;
			this.rogueFailCount = rogueFailCount;
			this.magicCount = magicCount;
			this.goodMessageCount1 = goodMessageCount1;
			this.goodMessageCount2 = goodMessageCount2;
			this.badMessageCount = badMessageCount;
		}
	}
}
