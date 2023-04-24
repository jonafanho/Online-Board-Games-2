package org.game;

import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import org.mtr.webserver.Webserver;

import java.util.Optional;

public class Main {

	private static final Object2ObjectAVLTreeMap<String, Room> ROOMS = new Object2ObjectAVLTreeMap<>();
	private static final int PORT = 8888;

	public static void main(String[] args) {
		final Webserver webserver = new Webserver(
				Main.class,
				"/assets/website",
				PORT,
				jsonObject -> Long.parseLong(jsonObject.get("id").getAsString(), 16)
		);

		webserver.addSocketListener("init", (client, id, jsonObject) -> {
			final Optional<Room> optional = ROOMS.values().stream().filter(room -> room.containsUser(id)).findFirst();
			if (optional.isPresent()) {
				optional.get().join(id, jsonObject);
			} else {
				webserver.sendSocketEvent(id, "home", null);
			}
		});

		webserver.addSocketListener("create-room", (client, id, jsonObject) -> {
			while (true) {
				final String roomCode = Room.generateRoomCode();
				if (!ROOMS.containsKey(roomCode)) {
					final Room room = new Room(webserver, roomCode, id);
					room.join(id, jsonObject);
					ROOMS.put(roomCode, room);
					break;
				}
			}
		});

		webserver.addSocketListener("join-room", (client, id, jsonObject) -> {
			final String roomCode = jsonObject.get("code").getAsString();
			final Room room = ROOMS.get(roomCode);
			if (room == null) {
				webserver.sendSocketEvent(id, "error-no-room", null);
			} else {
				room.join(id, jsonObject);
			}
		});

		webserver.addSocketListener("leave-room", (client, id, jsonObject) -> {
			final String roomCode = jsonObject.get("code").getAsString();
			final Room room = ROOMS.get(roomCode);
			if (room != null && room.remove(id)) {
				ROOMS.remove(roomCode);
			}
		});

		webserver.start();
	}
}
