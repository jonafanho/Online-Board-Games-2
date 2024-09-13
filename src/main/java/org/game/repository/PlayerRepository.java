package org.game.repository;

import org.game.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayerRepository extends JpaRepository<Player, UUID> {

	Optional<Player> getPlayerByToken(UUID token);

	Optional<Player> getPlayerByUuidAndToken(UUID uuid, UUID token);
}
