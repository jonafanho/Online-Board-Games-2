package org.game;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.gradle.api.Project;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class Generator {

	private static final Logger LOGGER = LogManager.getLogger("Generator");

	public static void generate(Project project) {
		final List<String> games = new ArrayList<>();
		final List<String> gameStates1 = new ArrayList<>();
		final List<String> gameStates2 = new ArrayList<>();
		final List<String> gameStates3 = new ArrayList<>();
		final StringBuilder stringBuilderGameComponentHtml = new StringBuilder("@switch(getGame()){\n");
		final StringBuilder stringBuilderGameComponentTs = new StringBuilder("import{Component}from'@angular/core';import{DataService}from'../../service/data.service';");
		final List<String> gameImports = new ArrayList<>();

		iterateFolder(project.getRootDir().toPath().resolve("buildSrc/src/main/resources/schema"), gamePath -> {
			final String id = gamePath.getFileName().toString();
			final String idSnake = id.replace("-", "_");
			final String idPascal = toPascalCase(id);
			final List<String> typeScriptObjects = new ArrayList<>();

			iterateFolder(gamePath, path -> {
				final String gameItem = path.getFileName().toString();
				final boolean isEnum = gameItem.equals("enum");
				final boolean isObject = gameItem.equals("object");

				if (gameItem.equals("properties.json")) {
					try {
						final JsonObject jsonObject = JsonParser.parseString(FileUtils.readFileToString(path.toFile(), StandardCharsets.UTF_8)).getAsJsonObject();
						final String title = jsonObject.get("title").getAsString();
						final String description = jsonObject.get("description").getAsString();
						final int minPlayers = jsonObject.get("minPlayers").getAsInt();
						final int maxPlayers = jsonObject.get("maxPlayers").getAsInt();
						final int idealMinPlayers = jsonObject.get("idealMinPlayers").getAsInt();
						final int idealMaxPlayers = jsonObject.get("idealMaxPlayers").getAsInt();

						games.add(String.format(
								"new Game('%s','%s','%s',%s,%s,%s,%s,%s,%s),\n",
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
						gameStates2.add(String.format("case\"%1$s\"->room.processAndUpdateState(org.game.library.%2$s.GameState.class,org.game.library.%2$s.generated.Request.class,player,bodyString);\n", id, idSnake));
						gameStates3.add(String.format("case\"%1$s\"->room.getRoomWithStateForPlayer(org.game.library.%2$s.GameState.class,player);\n", id, idSnake));

						stringBuilderGameComponentHtml.append(String.format("@case('%1$s'){<game-%1$s/>}\n", id));
						stringBuilderGameComponentTs.append(String.format("import{%1$sComponent}from'../library/%2$s/%2$s.component';", idPascal, id));
						gameImports.add(String.format("%sComponent", idPascal));

						final StringBuilder stringBuilder = new StringBuilder(String.format("package org.game.library.%s;public interface BaseGameProperties{\n", idSnake));
						stringBuilder.append(String.format("String GAME_TITLE=\"%s\";\n", title));
						stringBuilder.append(String.format("int MIN_PLAYERS=%s;\n", minPlayers));
						stringBuilder.append(String.format("int MAX_PLAYERS=%s;\n", maxPlayers));
						stringBuilder.append(String.format("int IDEAL_MIN_PLAYERS=%s;\n", idealMinPlayers));
						stringBuilder.append(String.format("int IDEAL_MAX_PLAYERS=%s;\n", idealMaxPlayers));
						stringBuilder.append("}");

						writeFile(project.getRootDir().toPath().resolve(String.format("src/main/java/org/game/library/%s/BaseGameProperties.java", idSnake)), stringBuilder);
					} catch (IOException e) {
						LOGGER.error("", e);
					}
				} else {
					iterateFolder(path, gameObjectPath -> {
						try {
							final JsonObject jsonObject = JsonParser.parseString(FileUtils.readFileToString(gameObjectPath.toFile(), StandardCharsets.UTF_8)).getAsJsonObject();
							final String name = capitalizeFirstLetter(gameObjectPath.getFileName().toString().split("\\.")[0]);
							final boolean isStage = isEnum && name.equals("Stage");
							final boolean isRequest = isObject && name.equals("Request");
							final boolean isClientState = isObject && name.equals("ClientState");
							final String textJava;

							if (isEnum) {
								final JsonObject jsonObjectValues = jsonObject.getAsJsonObject("values");
								final List<String> values = new ArrayList<>();
								final StringBuilder stringBuilderValuesTypeScript = new StringBuilder();

								if (isStage) {
									values.add("/**The default stage when the game hasn't started yet**/LOBBY()");
									stringBuilderValuesTypeScript.append("Lobby='LOBBY',");
								}

								jsonObjectValues.keySet().forEach(key -> {
									final List<String> parameters = new ArrayList<>();
									jsonObjectValues.getAsJsonArray(key).forEach(value -> parameters.add(value.toString()));
									values.add(String.format("%s(%s)", toEnumCase(key), String.join(",", parameters)));
									stringBuilderValuesTypeScript.append(String.format("%s='%s',", capitalizeFirstLetter(key), toEnumCase(key)));
								});

								final StringBuilder stringBuilderJava = new StringBuilder(String.join(",", values));
								final StringBuilder stringBuilderAssignments = new StringBuilder();
								final JsonObject jsonObjectFields = jsonObject.getAsJsonObject("fields");
								final List<String> parameters = new ArrayList<>();

								jsonObjectFields.keySet().forEach(fieldName -> {
									final String fieldType = getJavaField(jsonObjectFields, fieldName, isClientState);
									stringBuilderJava.append(String.format(";public final %s", fieldType));
									parameters.add(fieldType);
									stringBuilderAssignments.append(String.format("this.%1$s=%1$s;", fieldName));
								});

								if (!parameters.isEmpty()) {
									stringBuilderJava.append(String.format(";%s(%s){%s}", name, String.join(",", parameters), stringBuilderAssignments));
								}

								textJava = String.format("package org.game.library.%s.generated;public enum %s{%s}", idSnake, name, stringBuilderJava);
								typeScriptObjects.add(String.format("export enum %s{%s}", name, stringBuilderValuesTypeScript));
							} else if (isObject) {
								final List<String> parametersJava = new ArrayList<>();
								final StringBuilder stringBuilderFieldsJava = new StringBuilder();
								final StringBuilder stringBuilderParametersTypeScript = new StringBuilder();

								jsonObject.keySet().forEach(parameterName -> {
									final String field = getJavaField(jsonObject, parameterName, isClientState);
									parametersJava.add(field);
									stringBuilderFieldsJava.append(String.format("public final %s;", field));
									stringBuilderParametersTypeScript.append(getTypeScriptField(jsonObject, parameterName, isClientState));
								});

								if (isRequest) {
									textJava = String.format("package org.game.library.%s.generated;@lombok.RequiredArgsConstructor public final class %s extends org.game.core.AbstractRequest{%s}", idSnake, name, stringBuilderFieldsJava);
								} else if (isClientState) {
									textJava = String.format("package org.game.library.%s.generated;@lombok.RequiredArgsConstructor public final class %s extends org.game.core.AbstractClientState<Stage>{%s@org.springframework.lang.NonNull @Override protected Stage getDefaultStage(){return Stage.LOBBY;}}", idSnake, name, stringBuilderFieldsJava);
								} else {
									textJava = String.format("package org.game.library.%s.generated;public record %s(%s){}", idSnake, name, String.join(",", parametersJava));
								}

								typeScriptObjects.add(String.format("export interface %s%s{%s}", name, isRequest ? " extends BaseRequest" : isClientState ? " extends BaseClientState<Stage>" : "", stringBuilderParametersTypeScript));
							} else {
								textJava = "";
							}

							writeFile(project.getRootDir().toPath().resolve(String.format("src/main/java/org/game/library/%s/generated/%s.java", idSnake, name)), textJava);
						} catch (IOException e) {
							LOGGER.error("", e);
						}
					});
				}
			});

			writeFile(project.getRootDir().toPath().resolve(String.format("buildSrc/src/main/resources/website/src/app/entity/generated/%s.ts", idSnake)), String.format("import{BaseClientState}from'../base-client-state';import{BaseRequest}from'../base-request';\n%s", String.join("\n", typeScriptObjects)));
		});

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

		writeFile(project.getRootDir().toPath().resolve("buildSrc/src/main/resources/website/src/app/entity/game.ts"), stringBuilderGame);

		final StringBuilder stringBuilderState = new StringBuilder("package org.game.core;import org.game.entity.Player;import org.game.entity.Room;public final class GameStateHelper{\n");
		stringBuilderState.append("public static AbstractGameState<?,?,?>create(String game){return switch(game){\n");
		gameStates1.forEach(stringBuilderState::append);
		stringBuilderState.append("default->throw new IllegalArgumentException(\"Unknown game: \"+game);\n");
		stringBuilderState.append("};}\n");
		stringBuilderState.append("public static void processAndUpdateState(Player player,Room room,String bodyString){String game=room.getGame();switch(game){\n");
		gameStates2.forEach(stringBuilderState::append);
		stringBuilderState.append("default->throw new IllegalArgumentException(\"Unknown game: \"+game);\n");
		stringBuilderState.append("}}\n");
		stringBuilderState.append("public static Room getRoomWithStateForPlayer(Player player,Room room){String game=room.getGame();return switch(game){\n");
		gameStates3.forEach(stringBuilderState::append);
		stringBuilderState.append("default->throw new IllegalArgumentException(\"Unknown game: \"+game);\n");
		stringBuilderState.append("};}\n");
		stringBuilderState.append("}");

		writeFile(project.getRootDir().toPath().resolve("src/main/java/org/game/core/GameStateHelper.java"), stringBuilderState);

		stringBuilderGameComponentHtml.append("}");
		stringBuilderGameComponentTs.append("@Component({selector:'app-game',standalone:true,imports:[").append(String.join(",", gameImports));
		stringBuilderGameComponentTs.append("],templateUrl:'./game.component.html',styleUrl:'./game.component.css',})");
		stringBuilderGameComponentTs.append("export class GameComponent{constructor(private readonly dataService:DataService){}protected getGame(){return this.dataService.getRoom()?.game??'';}}");

		writeFile(project.getRootDir().toPath().resolve("buildSrc/src/main/resources/website/src/app/component/game/game.component.html"), stringBuilderGameComponentHtml);
		writeFile(project.getRootDir().toPath().resolve("buildSrc/src/main/resources/website/src/app/component/game/game.component.ts"), stringBuilderGameComponentTs);
	}

	private static void iterateFolder(Path path, Consumer<Path> callback) {
		try (final Stream<Path> schemasStream = Files.list(path)) {
			schemasStream.forEach(callback);
		} catch (IOException e) {
			LOGGER.error("", e);
		}
	}

	private static void writeFile(Path path, StringBuilder stringBuilder) {
		writeFile(path, stringBuilder.toString());
	}

	private static void writeFile(Path path, String text) {
		try {
			FileUtils.write(path.toFile(), text, StandardCharsets.UTF_8);
		} catch (IOException e) {
			LOGGER.error("", e);
		}
	}

	private static String capitalizeFirstLetter(String text) {
		return text.isEmpty() ? "" : text.substring(0, 1).toUpperCase() + text.substring(1);
	}

	private static String toPascalCase(String text) {
		final String[] textSplit = text.split("-");
		final StringBuilder stringBuilder = new StringBuilder();
		for (final String textPart : textSplit) {
			stringBuilder.append(capitalizeFirstLetter(textPart));
		}
		return stringBuilder.toString();
	}

	private static String toEnumCase(String text) {
		final StringBuilder stringBuilder = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			final char character = text.charAt(i);
			if (!stringBuilder.isEmpty() && (Character.isUpperCase(character) || Character.isDigit(character))) {
				stringBuilder.append("_");
			}
			stringBuilder.append(Character.toUpperCase(character));
		}
		return stringBuilder.toString();
	}

	private static boolean getJsonOptionalBooleanField(JsonObject jsonObject, String key) {
		return jsonObject.has(key) && jsonObject.get(key).getAsBoolean();
	}

	private static String getJavaField(JsonObject jsonObject, String key, boolean requiredField) {
		final JsonObject fieldDetails = jsonObject.getAsJsonObject(key);
		final String rawType = fieldDetails.get("type").getAsString();
		final String type = switch (rawType) {
			case "boolean" -> "boolean";
			case "integer" -> "int";
			case "uuid" -> "java.util.UUID";
			default -> capitalizeFirstLetter(rawType);
		};
		return String.format("%s%s %s", requiredField ? "@org.springframework.lang.NonNull " : "", getJsonOptionalBooleanField(fieldDetails, "array") ? String.format("java.util.List<%s>", type) : type, key);
	}

	private static String getTypeScriptField(JsonObject jsonObject, String key, boolean requiredField) {
		final JsonObject fieldDetails = jsonObject.getAsJsonObject(key);
		final String rawType = fieldDetails.get("type").getAsString();
		final String type = switch (rawType) {
			case "boolean" -> "boolean";
			case "integer" -> "number";
			case "uuid" -> "string";
			default -> capitalizeFirstLetter(rawType);
		};
		return String.format("readonly %s%s:%s%s;", key, requiredField ? "" : "?", type, getJsonOptionalBooleanField(fieldDetails, "array") ? "[]" : "");
	}

	private static String getTypeScriptListFromJsonArray(JsonArray jsonArray) {
		final List<String> strings = new ArrayList<>();
		jsonArray.forEach(jsonElement -> strings.add(String.format("'%s'", jsonElement.getAsString())));
		return String.format("[%s]", String.join(",", strings));
	}
}
