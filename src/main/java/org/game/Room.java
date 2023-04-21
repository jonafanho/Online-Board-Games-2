package org.game;

import com.corundumstudio.socketio.SocketIOClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.longs.Long2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import org.game.data.Character;
import org.game.logic.Game;

import java.util.Collections;
import java.util.Random;
import java.util.function.BiConsumer;

public class Room {

	private Game game;

	public final String roomCode;
	private final BiConsumer<String, JsonObject> broadcast;
	private final Long2ObjectAVLTreeMap<String> users = new Long2ObjectAVLTreeMap<>();

	public Room(BiConsumer<String, JsonObject> broadcast, String roomCode) {
		this.broadcast = broadcast;
		this.roomCode = roomCode;
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

	public void join(SocketIOClient client, long id, JsonObject receiveObject) {
		client.getAllRooms().forEach(client::leaveRoom);
		client.joinRoom(roomCode);
		users.put(id, receiveObject.get("name").getAsString());

		final JsonArray playerArray = new JsonArray();
		users.values().forEach(playerArray::add);

		final JsonObject sendObject = new JsonObject();
		sendObject.addProperty("code", roomCode);
		sendObject.add("players", playerArray);

		broadcast.accept("lobby", sendObject);
	}

	public void remove(long id) {
		users.remove(id);
	}

	public boolean containsUser(long id) {
		return users.containsKey(id);
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
