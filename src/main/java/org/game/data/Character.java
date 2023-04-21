package org.game.data;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import org.game.logic.Game;

public enum Character {

	GOOD(Team.GOOD),
	BAD(Team.BAD),
	MERLIN(Team.GOOD),
	PERCIVAL(Team.GOOD),
	MORGANA(Team.BAD),
	MORDRED(Team.BAD),
	OBERON(Team.BAD),
	TROUBLEMAKER(Team.GOOD),
	CLERIC(Team.GOOD),
	UNTRUSTWORTHY_SERVANT(Team.GOOD),
	TRICKSTER(Team.BAD),
	LUNATIC(Team.BAD),
	BRUTE(Team.BAD),
	REVEALER(Team.BAD),
	GOOD_LANCELOT(Team.GOOD),
	BAD_LANCELOT(Team.BAD),
	GOOD_ROGUE(Team.GOOD_ROGUE),
	BAD_ROGUE(Team.BAD_ROGUE),
	GOOD_SORCERER(Team.GOOD),
	BAD_SORCERER(Team.BAD),
	GOOD_MESSENGER_1(Team.GOOD),
	GOOD_MESSENGER_2(Team.GOOD),
	BAD_MESSENGER(Team.BAD);

	public final Team team;

	Character(Team team) {
		this.team = team;
	}

	public ObjectImmutableList<Card> getAllowedCards(Game game) {
		final ObjectArrayList<Card> cards = new ObjectArrayList<>();

		switch (this) {
			case LUNATIC:
				cards.add(Card.FAIL);
				break;
			case BRUTE:
				cards.add(Card.SUCCESS);
				if (game.getRound() < 3) {
					cards.add(Card.FAIL);
				}
				break;
			case GOOD_ROGUE:
				cards.add(Card.SUCCESS);
				cards.add(Card.SUCCESS_ROGUE);
				break;
			case BAD_ROGUE:
				cards.add(Card.SUCCESS);
				cards.add(Card.FAIL);
				cards.add(Card.FAIL_ROGUE);
				break;
			case GOOD_SORCERER:
			case BAD_SORCERER:
				cards.add(Card.SUCCESS);
				cards.add(Card.MAGIC);
				break;
			case GOOD_MESSENGER_1:
				cards.add(Card.SUCCESS);
				if (game.getGoodMessageCount1() < 2) {
					cards.add(Card.GOOD_MESSAGE);
				}
				break;
			case GOOD_MESSENGER_2:
				cards.add(Card.SUCCESS);
				if (game.getGoodMessageCount2() < 2) {
					cards.add(Card.GOOD_MESSAGE);
				}
				break;
			case BAD_MESSENGER:
				cards.add(Card.SUCCESS);
				cards.add(Card.FAIL);
				cards.add(Card.BAD_MESSAGE);
				break;
			default:
				cards.add(Card.SUCCESS);
				if (team.isBad) {
					cards.add(Card.FAIL);
				}
				break;
		}

		return new ObjectImmutableList<>(cards);
	}

	public boolean hasRequiredCharacters(int playerCount, ObjectArrayList<Character> currentCharacters) {
		switch (this) {
			case PERCIVAL:
				return currentCharacters.contains(MERLIN) && (playerCount > 5 || currentCharacters.contains(MORGANA) || currentCharacters.contains(MORDRED));
			case MORGANA:
				return currentCharacters.contains(PERCIVAL);
			case MORDRED:
				return currentCharacters.contains(MERLIN);
			case GOOD_LANCELOT:
			case BAD_LANCELOT:
				return currentCharacters.contains(GOOD_LANCELOT) && currentCharacters.contains(BAD_LANCELOT);
			case GOOD_ROGUE:
			case BAD_ROGUE:
				return currentCharacters.contains(GOOD_ROGUE) && currentCharacters.contains(BAD_ROGUE);
			case GOOD_SORCERER:
			case BAD_SORCERER:
				return currentCharacters.contains(GOOD_SORCERER) && currentCharacters.contains(BAD_SORCERER);
			case GOOD_MESSENGER_1:
			case GOOD_MESSENGER_2:
			case BAD_MESSENGER:
				return currentCharacters.contains(GOOD_MESSENGER_1) && currentCharacters.contains(GOOD_MESSENGER_2) && currentCharacters.contains(BAD_MESSENGER);
			default:
				return true;
		}
	}
}
