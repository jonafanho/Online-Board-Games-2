package org.game.core;

import org.game.entity.Player;
import org.game.entity.Room;
import org.game.repository.PlayerRepository;
import org.game.repository.RoomRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api")
@CrossOrigin("http://localhost:4200")
public final class RoomController {

	private final RoomRepository roomRepository;
	private final PlayerRepository playerRepository;
	private final SimpMessagingTemplate messagingTemplate;

	public RoomController(RoomRepository roomRepository, PlayerRepository playerRepository, SimpMessagingTemplate messagingTemplate) {
		this.roomRepository = roomRepository;
		this.playerRepository = playerRepository;
		this.messagingTemplate = messagingTemplate;
	}

	@GetMapping("/createRoom")
	public Room createRoom(@RequestParam(value = "hostUuid") String hostUuidString, @RequestParam(value = "game") String game) {
		return roomRepository.save(new Room(game, getPlayer(hostUuidString)));
	}

	@GetMapping("/getRoom")
	public Room getRoom(@RequestParam(value = "code") String code) {
		return roomRepository.findById(code).orElse(null);
	}

	@GetMapping("/deleteRoom")
	public Room deleteRoom(@RequestParam(value = "code") String code) {
		roomRepository.deleteById(code);
		broadcastRoomUpdate(code);
		return null;
	}

	@GetMapping("/getPlayer")
	public Player getPlayer(@RequestParam(value = "uuid") String uuidString) {
		try {
			return playerRepository.findById(UUID.fromString(uuidString)).orElseGet(() -> playerRepository.save(new Player()));
		} catch (Exception ignored) {
			return playerRepository.save(new Player());
		}
	}

	@GetMapping("/updatePlayer")
	public Player updatePlayer(
			@RequestParam(value = "uuid") String uuidString,
			@RequestParam(value = "name", required = false) String name,
			@RequestParam(value = "icon", required = false) String icon
	) {
		final Player player = getPlayer(uuidString);

		if (name == null && icon == null) {
			return player;
		} else {
			if (name != null) {
				player.setName(name);
			}
			if (icon != null) {
				player.setIcon(icon);
			}

			final Player newPlayer = playerRepository.save(player);
			broadcastRoomUpdate(newPlayer);
			return newPlayer;
		}
	}

	@GetMapping("/joinRoom")
	public Player joinRoom(@RequestParam(value = "playerUuid") String uuidString, @RequestParam(value = "roomCode") String roomCode) {
		final Player player = getPlayer(uuidString);
		roomRepository.findById(roomCode).ifPresent(room -> {
			room.getPlayers().add(player);
			broadcastRoomUpdate(roomRepository.save(room));
		});
		return player;
	}

	@GetMapping("/leaveRoom")
	public Player leaveRoom(@RequestParam(value = "playerUuid") String uuidString, @RequestParam(value = "roomCode") String roomCode) {
		final Player player = getPlayer(uuidString);
		roomRepository.findById(roomCode).ifPresent(room -> {
			room.getPlayers().remove(player);
			broadcastRoomUpdate(roomRepository.save(room));
		});
		return player;
	}

	private void broadcastRoomUpdate(Player player) {
		player.getRooms().forEach(this::broadcastRoomUpdate);
	}

	private void broadcastRoomUpdate(String roomCode) {
		messagingTemplate.convertAndSend("/topic/" + roomCode, String.valueOf((Object) null));
	}

	private void broadcastRoomUpdate(Room room) {
		messagingTemplate.convertAndSend("/topic/" + room.getCode(), room);
	}
}
