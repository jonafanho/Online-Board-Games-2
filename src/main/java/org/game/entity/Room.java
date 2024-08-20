package org.game.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

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

	@JsonIgnore
	@ManyToMany
	private final List<Player> players = new ArrayList<>();

	/**
	 * @deprecated use {@link Room#Room(String)}
	 */
	@Deprecated
	public Room() {
		code = generateRoomCode();
	}

	public Room(String game) {
		code = generateRoomCode();
		this.game = game;
	}

	private static String generateRoomCode() {
		return Long.toString(new Random().nextLong(MIN_CODE, MAX_CODE), Character.MAX_RADIX).toUpperCase(Locale.ENGLISH).substring(0, 6);
	}
}
