package org.game;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.longs.Long2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import org.game.data.Character;
import org.game.logic.Game;
import org.mtr.webserver.Webserver;

import java.util.Collections;
import java.util.Random;

public class Room {

	private Game game;

	public final String roomCode;
	private final Webserver webserver;
	private final Long2ObjectAVLTreeMap<String> users = new Long2ObjectAVLTreeMap<>();
	private final long host;

	public Room(Webserver webserver, String roomCode, long host) {
		this.webserver = webserver;
		this.roomCode = roomCode;
		this.host = host;
	}

	public void start(boolean hasLady, boolean hasTrapper, ObjectArrayList<Character> characters) {
		final int playerCount = users.size();
		if (Utilities.invalidPlayerCount(playerCount)) {
			return;
		}

		final int totalGoodCount = Utilities.getGoodCount(playerCount);
		final int totalBadCount = Utilities.getBadCount(playerCount);

		int goodCount = 0;
		int badCount = 0;
		for (final Character character : characters) {
			if (character.team.isBad) {
				badCount++;
			} else {
				goodCount++;
			}
		}

		for (int i = goodCount; i < totalGoodCount; i++) {
			characters.add(Character.GOOD);
		}

		for (int i = badCount; i < totalBadCount; i++) {
			characters.add(Character.BAD);
		}

		if (characters.size() != playerCount || characters.stream().anyMatch(character -> !character.hasRequiredCharacters(playerCount, characters))) {
			return;
		}

		final LongArrayList playerIds = new LongArrayList();
		users.keySet().forEach(playerIds::add);
		Collections.shuffle(playerIds);
		Collections.shuffle(characters);
		game = new Game(hasLady, hasTrapper, users, new LongImmutableList(playerIds), new ObjectImmutableList<>(characters));
	}

	public void join(long id, JsonObject jsonObject) {
		users.put(id, jsonObject.get("name").getAsString());
		users.keySet().forEach(userId -> webserver.sendSocketEvent(userId, "lobby", getRoomObject(userId)));
	}

	public boolean remove(long id) {
		final boolean isHost = id == host;
		webserver.sendSocketEvent(id, "home", null);
		users.remove(id);
		users.keySet().forEach(userId -> {
			if (isHost) {
				webserver.sendSocketEvent(userId, "home", null);
			} else {
				webserver.sendSocketEvent(userId, "lobby", getRoomObject(userId));
			}
		});
		return isHost;
	}

	public boolean containsUser(long id) {
		return users.containsKey(id);
	}

	private JsonObject getRoomObject(long id) {
		final JsonArray jsonArray = new JsonArray();
		users.values().forEach(jsonArray::add);

		final JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("code", roomCode);
		jsonObject.add("players", jsonArray);
		jsonObject.addProperty("host", id == host);

		return jsonObject;
	}

	public static String generateRoomCode() {
		final String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		final Random random = new Random();
		final StringBuilder stringBuilder = new StringBuilder();
		for (int i = 0; i < 4; i++) {
			stringBuilder.append(characters.charAt(random.nextInt(characters.length())));
		}
		return stringBuilder.toString();
	}
}
