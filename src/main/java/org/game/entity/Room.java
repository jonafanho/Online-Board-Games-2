package org.game.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.Getter;
import org.game.core.AbstractClientState;
import org.game.core.AbstractGameState;
import org.game.core.AbstractRequest;
import org.game.core.GameStateHelper;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.lang.Nullable;

import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

@Entity
@Getter
public final class Room extends AbstractEntity {

	private static final long MIN_CODE = Long.parseLong("100000", Character.MAX_RADIX);
	private static final long MAX_CODE = Long.parseLong("ZZZZZZ", Character.MAX_RADIX);
	private static final String EMPTY_JSON = "{}";

	@Id
	@Column(length = 6)
	private final String code;

	@Column(nullable = false)
	private String game;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false)
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
			state = EMPTY_JSON;
		}
	}

	public Room(Room room, @Nullable String newState) {
		code = room.code;
		game = room.game;
		state = newState == null ? EMPTY_JSON : newState;
		host = room.host;
		players.addAll(room.players);
	}

	public <W extends AbstractGameState<T, U, V>, T extends Enum<T>, U extends AbstractRequest, V extends AbstractClientState<T>> void processAndUpdateState(Class<W> gameStateClass, Class<U> requestClass, Player player, String bodyString) {
		try {
			final W gameState = new ObjectMapper().readValue(state, gameStateClass);
			gameState.process(player, new ObjectMapper().readValue(bodyString, requestClass));
			state = new ObjectMapper().writeValueAsString(gameState);
		} catch (JsonProcessingException ignored) {
		}
	}

	public <W extends AbstractGameState<T, U, V>, T extends Enum<T>, U extends AbstractRequest, V extends AbstractClientState<T>> Room getRoomWithStateForPlayer(Class<W> gameStateClass, Player player) {
		try {
			return new Room(this, new ObjectMapper().writeValueAsString(new ObjectMapper().readValue(state, gameStateClass).getStateForPlayer(player)));
		} catch (JsonProcessingException ignored) {
			return new Room(this, null);
		}
	}

	private static String generateRoomCode() {
		return Long.toString(new Random().nextLong(MIN_CODE, MAX_CODE), Character.MAX_RADIX).toUpperCase(Locale.ENGLISH).substring(0, 6);
	}
}
