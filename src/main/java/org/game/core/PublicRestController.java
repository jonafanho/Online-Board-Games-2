package org.game.core;

import org.game.entity.Player;
import org.game.repository.PlayerRepository;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;

/**
 * A REST controller for public endpoints. Authentication is not required.
 */
@RestController
@RequestMapping("api/public")
@CrossOrigin("http://localhost:4200")
public final class PublicRestController {

	private final PlayerRepository playerRepository;

	public PublicRestController(PlayerRepository playerRepository) {
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
	public Player.PlayerRegistration register(@RequestParam(value = "uuid", required = false) String uuidString, @RequestParam(value = "token", required = false) String tokenString, @RequestParam(value = "debugIndex", required = false) Integer debugIndex) {
		if (debugIndex != null) {
			return new Player.PlayerRegistration(Objects.requireNonNullElseGet(playerRepository.getPlayerByToken(new UUID(0, debugIndex)).orElse(null), () -> playerRepository.save(new Player(debugIndex))));
		} else {
			return Utilities.parseUuid(uuidString, tokenString, (uuid, token) -> {
				final Player player = playerRepository.getPlayerByUuidAndToken(uuid, token).orElse(null);
				return player == null ? createNewPlayerRegistration() : new Player.PlayerRegistration(player);
			}, this::createNewPlayerRegistration);
		}
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

	private Player.PlayerRegistration createNewPlayerRegistration() {
		return new Player.PlayerRegistration(playerRepository.save(new Player()));
	}
}
