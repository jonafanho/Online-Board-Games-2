package org.game.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter
public final class Player extends AbstractEntity {

	@Id
	@GeneratedValue
	private UUID uuid;

	@Setter
	@Column(nullable = false)
	private String name = "";

	@Setter
	@Column
	private String icon;

	@JsonIgnore
	@ManyToMany(mappedBy = "players")
	private final Set<Room> rooms = new HashSet<>();
}
