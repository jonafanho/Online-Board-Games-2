package org.game.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import org.game.core.AbstractGameState;
import org.game.core.GameStateHelper;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

@Entity
@Getter
public final class Room extends AbstractEntity {

	private static final long MIN_CODE = Long.parseLong("100000", Character.MAX_RADIX);
	private static final long MAX_CODE = Long.parseLong("ZZZZZZ", Character.MAX_RADIX);

	@Id
	@Column(length = 6)
	private final String code;

	@Column(nullable = false)
	private String game;

	@Getter(AccessLevel.NONE)
	@JdbcTypeCode(SqlTypes.JSON)
	private String state;

	@ManyToOne
	private Player host;

	@ManyToMany
	private final Set<Player> players = new HashSet<>();

	/**
	 * @deprecated use {@link Room#Room(String, Player)}
	 */
	@Deprecated
	public Room() {
		code = generateRoomCode();
	}

	public Room(String game, Player host) {
		code = generateRoomCode();
		this.game = game;
		this.host = host;
		try {
			state = new ObjectMapper().writeValueAsString(GameStateHelper.create(game));
		} catch (JsonProcessingException ignored) {
		}
	}

	public Room(Room room) {
		code = room.code;
		game = room.game;
		state = room.state;
		host = room.host;
		players.addAll(room.players);
	}

	public <T extends AbstractGameState<U>, U> void processAndUpdateState(Class<T> gameStateClass, Class<U> requestBodyClass, Player player, String bodyString) {
		if (state == null) {
			state = "{}";
		}

		try {
			final T gameState = new ObjectMapper().readValue(state, gameStateClass);
			gameState.process(player, new ObjectMapper().readValue(bodyString, requestBodyClass));
			state = new ObjectMapper().writeValueAsString(gameState);
		} catch (JsonProcessingException ignored) {
		}
	}

	public <T extends AbstractGameState<U>, U> AbstractGameState<?> getStateForPlayer(Class<T> gameStateClass, Player player) {
		try {
			return new ObjectMapper().readValue(state, gameStateClass).getStateForPlayer(player);
		} catch (JsonProcessingException ignored) {
			return null;
		}
	}

	private static String generateRoomCode() {
		return Long.toString(new Random().nextLong(MIN_CODE, MAX_CODE), Character.MAX_RADIX).toUpperCase(Locale.ENGLISH).substring(0, 6);
	}
}
