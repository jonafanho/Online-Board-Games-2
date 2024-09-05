package org.game.core;

import org.game.entity.Player;
import org.game.entity.Room;
import org.game.repository.PlayerRepository;
import org.game.repository.RoomRepository;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

/**
 * A REST controller for public endpoints. Authentication is not required.
 */
@RestController
@RequestMapping("api/public")
@CrossOrigin("http://localhost:4200")
public final class PublicRestController {

	private final RoomRepository roomRepository;
	private final PlayerRepository playerRepository;

	public PublicRestController(RoomRepository roomRepository, PlayerRepository playerRepository) {
		this.roomRepository = roomRepository;
		this.playerRepository = playerRepository;
	}

	/**
	 * Registers a new player or tries to validate a cached player.
	 *
	 * @param uuidString  if supplied, attempts to match an existing player's UUID
	 * @param tokenString if supplied, attempts to match an existing player's token
	 * @return the UUID and token of a newly created player or the UUID and token of an existing player if both parameters are provided and match an existing player
	 */
	@NonNull
	@GetMapping("/register")
	public Player.PlayerRegistration register(@RequestParam(value = "uuid", required = false) String uuidString, @RequestParam(value = "token", required = false) String tokenString) {
		return Utilities.parseUuid(uuidString, tokenString, (uuid, token) -> {
			final Player player = playerRepository.getPlayerByUuidAndToken(uuid, token).orElse(null);
			return player == null ? createNewPlayerRegistration() : new Player.PlayerRegistration(player);
		}, this::createNewPlayerRegistration);
	}

	/**
	 * Get basic details about a player.
	 *
	 * @param uuidString player UUID
	 * @return player details or {@code null} if player UUID is not found
	 */
	@Nullable
	@GetMapping("/getPlayer")
	public Player getPlayer(@RequestParam(value = "playerUuid") String uuidString) {
		return Utilities.parseUuid(uuidString, uuid -> playerRepository.findById(uuid).orElse(null), null);
	}

	/**
	 * Get basic details about a room. To prevent cheating, the game state is never returned.
	 *
	 * @param code room code
	 * @return room details or {@code null} if room code is not found
	 */
	@Nullable
	@GetMapping("/getRoom")
	public Room getRoom(@RequestParam(value = "code") String code) {
		return roomRepository.findById(code).orElse(null);
	}

	private Player.PlayerRegistration createNewPlayerRegistration() {
		return new Player.PlayerRegistration(playerRepository.save(new Player()));
	}
}
