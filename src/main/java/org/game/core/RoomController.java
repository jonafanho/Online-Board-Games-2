package org.game.core;

import org.game.entity.Player;
import org.game.entity.Room;
import org.game.repository.PlayerRepository;
import org.game.repository.RoomRepository;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api")
@CrossOrigin("http://localhost:4200")
public final class RoomController {

	private final RoomRepository roomRepository;
	private final PlayerRepository playerRepository;

	public RoomController(RoomRepository roomRepository, PlayerRepository playerRepository) {
		this.roomRepository = roomRepository;
		this.playerRepository = playerRepository;
	}

	@GetMapping("/createRoom")
	public Room createRoom(@RequestParam(value = "game") String game) {
		return roomRepository.save(new Room(game));
	}

	@GetMapping("/getRoom")
	public Room getRoom(@RequestParam(value = "code") String code) {
		return roomRepository.findById(code).orElse(null);
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
			return playerRepository.save(player);
		}
	}
}
