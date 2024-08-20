package org.game.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
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
	private final List<Room> rooms = new ArrayList<>();
}
