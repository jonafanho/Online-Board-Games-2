package org.game.entity;

import jakarta.persistence.*;
import lombok.Getter;

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
	}

	private static String generateRoomCode() {
		return Long.toString(new Random().nextLong(MIN_CODE, MAX_CODE), Character.MAX_RADIX).toUpperCase(Locale.ENGLISH).substring(0, 6);
	}
}
