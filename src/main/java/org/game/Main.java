package org.game;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;

import java.util.Optional;

public class Main {

	private static final Object2ObjectAVLTreeMap<String, Room> ROOMS = new Object2ObjectAVLTreeMap<>();
	private static final int PORT = 8888;

	public static void main(String[] args) {
		final SocketIOServer server = new Webserver(PORT).server;
		server.addConnectListener(client -> System.out.println("Client has Connected!"));
		server.addDisconnectListener(client -> System.out.println("Client has disconnected!"));

		addListener(server, "init", (client, id, jsonObject) -> {
			final Optional<Room> optional = ROOMS.values().stream().filter(room -> room.containsUser(id)).findFirst();
			if (optional.isPresent()) {
				optional.get().join(client, id, jsonObject);
			} else {
				client.sendEvent("home");
			}
		});

		addListener(server, "create-room", (client, id, jsonObject) -> {
			while (true) {
				final String roomCode = Room.generateRoomCode();
				if (!ROOMS.containsKey(roomCode)) {
					final Room room = new Room((channel, messageObject) -> server.getRoomOperations(roomCode).sendEvent(channel, messageObject.toString()), roomCode);
					room.join(client, id, jsonObject);
					ROOMS.put(roomCode, room);
					break;
				}
			}
		});

		addListener(server, "join-room", (client, id, jsonObject) -> {
			final String roomCode = jsonObject.get("code").getAsString();
			final Room room = ROOMS.get(roomCode);
			if (room == null) {
				client.sendEvent("error-no-room");
			} else {
				room.join(client, id, jsonObject);
			}
		});

		server.start();
	}

	private static void addListener(SocketIOServer server, String channel, Listener listener) {
		server.addEventListener(channel, String.class, (client, message, ackRequest) -> {
			try {
				final JsonObject jsonObject = JsonParser.parseString(message).getAsJsonObject();
				listener.accept(client, Long.parseLong(jsonObject.get("id").getAsString(), 16), jsonObject);
			} catch (Exception ignored) {
			}
		});
	}

	@FunctionalInterface
	private interface Listener {
		void accept(SocketIOClient client, long id, JsonObject jsonObject);
	}
}
