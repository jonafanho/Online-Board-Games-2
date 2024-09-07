package org.game.core;

import org.game.entity.Player;
import org.game.entity.Room;
import org.game.repository.PlayerRepository;
import org.game.repository.RoomRepository;
import org.game.security.AuthenticationFilter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A REST controller for secured endpoints. The calling user's UUID (as string) can be obtained by reading the request header.
 */
@RestController
@RequestMapping("api/secured")
@CrossOrigin("http://localhost:4200")
public final class SecuredRestController {

	private final RoomRepository roomRepository;
	private final PlayerRepository playerRepository;
	private final SimpMessagingTemplate messagingTemplate;

	public SecuredRestController(RoomRepository roomRepository, PlayerRepository playerRepository, SimpMessagingTemplate messagingTemplate) {
		this.roomRepository = roomRepository;
		this.playerRepository = playerRepository;
		this.messagingTemplate = messagingTemplate;
	}

	/**
	 * Creates a new room. The calling user is automatically the host.
	 *
	 * @param uuidString the UUID (as string) of the calling user
	 * @param game       the game ID
	 * @return the newly created room or {@code null} if the room couldn't be created
	 */
	@Nullable
	@GetMapping("/createRoom")
	public Room createRoom(@RequestHeader(AuthenticationFilter.UUID_HEADER) String uuidString, @RequestParam(value = "game") String game) {
		return getPlayer(uuidString, player -> roomRepository.save(new Room(game, player)));
	}

	/**
	 * Deletes an existing room. The calling user must be the host of the room.
	 *
	 * @param uuidString the UUID (as string) of the calling user
	 * @param code       the room code
	 * @return the deleted room or {@code null} if the room couldn't be deleted
	 */
	@Nullable
	@GetMapping("/deleteRoom")
	public Room deleteRoom(@RequestHeader(AuthenticationFilter.UUID_HEADER) String uuidString, @RequestParam(value = "code") String code) {
		return getPlayerAndRoom(uuidString, code, (player, room) -> {
			if (room.getHost().getUuid().equals(player.getUuid())) {
				final Room deletedRoom = new Room(room, null);
				roomRepository.delete(room);
				broadcastRoomUpdate(player, deletedRoom);
				return deletedRoom;
			} else {
				return null;
			}
		});
	}

	/**
	 * Update the player's name or icon. All parameters are optional.
	 *
	 * @param uuidString the UUID (as string) of the calling user
	 * @param name       the new name for the player
	 * @param icon       the <a href="https://fonts.google.com/icons">new icon</a> for the player
	 * @return the player with updated data or {@code null} if the player couldn't be updated
	 */
	@Nullable
	@GetMapping("/updatePlayer")
	public Player updatePlayer(@RequestHeader(AuthenticationFilter.UUID_HEADER) String uuidString, @RequestParam(value = "name", required = false) String name, @RequestParam(value = "icon", required = false) String icon) {
		return getPlayer(uuidString, player -> {
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
				newPlayer.getRooms().forEach(room -> broadcastRoomUpdate(null, room));
				return newPlayer;
			}
		});
	}

	/**
	 * Join an existing room (even if the room is full).
	 *
	 * @param uuidString the UUID (as string) of the calling user
	 * @param code       the room code
	 * @return the joined room or {@code null} if the room couldn't be joined
	 */
	@Nullable
	@GetMapping("/joinRoom")
	public Room joinRoom(@RequestHeader(AuthenticationFilter.UUID_HEADER) String uuidString, @RequestParam(value = "code") String code) {
		return getPlayerAndRoom(uuidString, code, (player, room) -> {
			room.getPlayers().add(player);
			final Room newRoom = roomRepository.save(room);
			broadcastRoomUpdate(player, newRoom);
			return newRoom;
		});
	}

	/**
	 * Leave or remove a player from a room. Only the host can remove players from a room; if a non-host tries to remove another player, nothing happens.
	 *
	 * @param uuidString       the UUID (as string) of the calling user
	 * @param code             the room code
	 * @param playerUuidString the player to remove from the room
	 * @return the exited room or {@code null} if the room couldn't be exited
	 */
	@Nullable
	@GetMapping("/leaveRoom")
	public Room leaveRoom(@RequestHeader(AuthenticationFilter.UUID_HEADER) String uuidString, @RequestParam(value = "code") String code, @RequestParam(value = "playerUuid") String playerUuidString) {
		return getPlayer(uuidString, player -> getPlayerAndRoom(playerUuidString, code, (playerToRemove, room) -> {
			if (uuidString.equals(playerUuidString) || room.getHost().getUuid().equals(player.getUuid())) {
				room.getPlayers().remove(playerToRemove);
				final Room newRoom = roomRepository.save(room);
				broadcastRoomUpdate(player, newRoom);
				return newRoom;
			} else {
				return null;
			}
		}));
	}

	/**
	 * Get basic details about a room. The game state returned is dependent on the calling user.
	 *
	 * @param uuidString the UUID (as string) of the calling user
	 * @param code       room code
	 * @return room details or {@code null} if room code is not found
	 */
	@Nullable
	@GetMapping("/getRoom")
	public Room getRoom(@RequestHeader(AuthenticationFilter.UUID_HEADER) String uuidString, @RequestParam(value = "code") String code) {
		return getPlayerAndRoom(uuidString, code, GameStateHelper::getRoomWithStateForPlayer);
	}

	/**
	 * Send a game update. The game state returned is dependent on the calling user.
	 *
	 * @param uuidString the UUID (as string) of the calling user
	 * @param code       the room code
	 * @param bodyString the request body
	 * @return the updated room with state (specific to the calling user) or {@code null} if the room state couldn't be updated
	 */
	@Nullable
	@PostMapping("/update")
	public Room update(@RequestHeader(AuthenticationFilter.UUID_HEADER) String uuidString, @RequestParam(value = "code") String code, @RequestBody String bodyString) {
		return getPlayerAndRoom(uuidString, code, (player, room) -> {
			GameStateHelper.processAndUpdateState(player, room, bodyString);
			final Room newRoom = roomRepository.save(room);
			broadcastRoomUpdate(player, newRoom);
			return GameStateHelper.getRoomWithStateForPlayer(player, newRoom);
		});
	}

	@Nullable
	private <T> T getPlayer(String uuidString, Function<Player, T> callback) {
		return Utilities.parseUuid(uuidString, uuid -> {
			final Player player = playerRepository.findById(uuid).orElse(null);
			return player == null ? null : callback.apply(player);
		}, null);
	}

	@Nullable
	private <T> T getPlayerAndRoom(String uuidString, String code, BiFunction<Player, Room, T> callback) {
		final Room room = roomRepository.findById(code).orElse(null);
		return room == null ? null : getPlayer(uuidString, player -> callback.apply(player, room));
	}

	private void broadcastRoomUpdate(@Nullable Player player, Room room) {
		messagingTemplate.convertAndSend("/topic/" + room.getCode(), new SocketUpdate(player == null ? null : player.getUuid()));
	}

	private record SocketUpdate(@Nullable UUID sender) {
	}
}
