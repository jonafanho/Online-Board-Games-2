package org.game;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.gradle.api.Project;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class Generator {

	private static final Logger LOGGER = LogManager.getLogger("Generator");

	public static void generate(Project project) {
		final List<String> games = new ArrayList<>();
		final List<String> gameStates1 = new ArrayList<>();
		final List<String> gameStates2 = new ArrayList<>();
		final List<String> gameStates3 = new ArrayList<>();

		try (final Stream<Path> schemasStream = Files.list(project.getRootDir().toPath().resolve("buildSrc/src/main/resources/schema/library"))) {
			schemasStream.forEach(path -> {
				try {
					final JsonObject jsonObject = JsonParser.parseString(FileUtils.readFileToString(path.toFile(), StandardCharsets.UTF_8)).getAsJsonObject();
					final String id = path.getFileName().toString().split("\\.")[0];
					final String idSnake = id.replace("-", "_");
					final String title = jsonObject.get("title").getAsString();
					final String description = jsonObject.get("description").getAsString();
					final int minPlayers = jsonObject.get("minPlayers").getAsInt();
					final int maxPlayers = jsonObject.get("maxPlayers").getAsInt();
					final int idealMinPlayers = jsonObject.get("idealMinPlayers").getAsInt();
					final int idealMaxPlayers = jsonObject.get("idealMaxPlayers").getAsInt();

					games.add(String.format(
							"new Game(\"%s\",\"%s\",\"%s\",%s,%s,%s,%s,%s,%s),\n",
							id,
							title,
							description,
							getTypeScriptListFromJsonArray(jsonObject.getAsJsonArray("tags")),
							getTypeScriptListFromJsonArray(jsonObject.getAsJsonArray("hiddenTags")),
							minPlayers,
							maxPlayers,
							idealMinPlayers,
							idealMaxPlayers
					));

					gameStates1.add(String.format("case\"%s\"->new org.game.library.%s.GameState();\n", id, idSnake));
					gameStates2.add(String.format("case\"%1$s\"->room.processAndUpdateState(org.game.library.%2$s.GameState.class,org.game.library.%2$s.RequestBody.class,player,bodyString);\n", id, idSnake));
					gameStates3.add(String.format("case\"%1$s\"->room.getStateForPlayer(org.game.library.%2$s.GameState.class,player);\n", id, idSnake));

					final StringBuilder stringBuilder = new StringBuilder(String.format("package org.game.library.%s;public interface BaseGameProperties{\n", idSnake));
					stringBuilder.append(String.format("String GAME_TITLE=\"%s\";\n", title));
					stringBuilder.append(String.format("int MIN_PLAYERS=%s;\n", minPlayers));
					stringBuilder.append(String.format("int MAX_PLAYERS=%s;\n", maxPlayers));
					stringBuilder.append(String.format("int IDEAL_MIN_PLAYERS=%s;\n", idealMinPlayers));
					stringBuilder.append(String.format("int IDEAL_MAX_PLAYERS=%s;\n", idealMaxPlayers));
					stringBuilder.append("}");

					try {
						FileUtils.write(project.getRootDir().toPath().resolve(String.format("src/main/java/org/game/library/%s/BaseGameProperties.java", idSnake)).toFile(), stringBuilder.toString(), StandardCharsets.UTF_8);
					} catch (Exception e) {
						LOGGER.error("", e);
					}
				} catch (Exception e) {
					LOGGER.error("", e);
				}
			});
		} catch (Exception e) {
			LOGGER.error("", e);
		}

		final StringBuilder stringBuilderGame = new StringBuilder("export class Game{private constructor(");
		stringBuilderGame.append("public readonly id:string,");
		stringBuilderGame.append("public readonly title:string,");
		stringBuilderGame.append("public readonly description:string,");
		stringBuilderGame.append("public readonly tags:string[],");
		stringBuilderGame.append("public readonly hiddenTags:string[],");
		stringBuilderGame.append("public readonly minPlayers:number,");
		stringBuilderGame.append("public readonly maxPlayers:number,");
		stringBuilderGame.append("public readonly idealMinPlayers:number,");
		stringBuilderGame.append("public readonly idealMaxPlayers:number,");
		stringBuilderGame.append("){}public static readonly GAMES:Game[]=[\n");
		games.forEach(stringBuilderGame::append);
		stringBuilderGame.append("];}");

		try {
			FileUtils.write(project.getRootDir().toPath().resolve("buildSrc/src/main/resources/website/src/app/entity/game.ts").toFile(), stringBuilderGame.toString(), StandardCharsets.UTF_8);
		} catch (Exception e) {
			LOGGER.error("", e);
		}

		final StringBuilder stringBuilderState = new StringBuilder("package org.game.core;import org.game.entity.Player;import org.game.entity.Room;public final class GameStateHelper{\n");
		stringBuilderState.append("public static AbstractGameState<?>create(String game){return switch(game){\n");
		gameStates1.forEach(stringBuilderState::append);
		stringBuilderState.append("default->throw new IllegalArgumentException(\"Unknown game: \"+game);\n");
		stringBuilderState.append("};}\n");
		stringBuilderState.append("public static void processAndUpdateState(Player player,Room room,String bodyString){String game=room.getGame();switch(game){\n");
		gameStates2.forEach(stringBuilderState::append);
		stringBuilderState.append("default->throw new IllegalArgumentException(\"Unknown game: \"+game);\n");
		stringBuilderState.append("}}\n");
		stringBuilderState.append("public static AbstractGameState<?>getStateForPlayer(Player player,Room room){String game=room.getGame();return switch(game){\n");
		gameStates3.forEach(stringBuilderState::append);
		stringBuilderState.append("default->throw new IllegalArgumentException(\"Unknown game: \"+game);\n");
		stringBuilderState.append("};}\n");
		stringBuilderState.append("}");

		try {
			FileUtils.write(project.getRootDir().toPath().resolve("src/main/java/org/game/core/GameStateHelper.java").toFile(), stringBuilderState.toString(), StandardCharsets.UTF_8);
		} catch (Exception e) {
			LOGGER.error("", e);
		}
	}

	private static String getTypeScriptListFromJsonArray(JsonArray jsonArray) {
		final List<String> strings = new ArrayList<>();
		jsonArray.forEach(jsonElement -> strings.add(String.format("\"%s\"", jsonElement.getAsString())));
		return String.format("[%s]", String.join(",", strings));
	}
}
