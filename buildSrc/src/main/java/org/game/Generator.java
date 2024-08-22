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

		try (final Stream<Path> schemasStream = Files.list(project.getRootDir().toPath().resolve("buildSrc/src/main/resources/schema/library"))) {
			schemasStream.forEach(path -> {
				try {
					final JsonObject jsonObject = JsonParser.parseString(FileUtils.readFileToString(path.toFile(), StandardCharsets.UTF_8)).getAsJsonObject();
					games.add(String.format(
							"new Game(\"%s\",\"%s\",\"%s\",%s,%s,%s,%s,%s,%s),\n",
							path.getFileName().toString().split("\\.")[0],
							jsonObject.get("title").getAsString(),
							jsonObject.get("description").getAsString(),
							getTypeScriptListFromJsonArray(jsonObject.getAsJsonArray("tags")),
							getTypeScriptListFromJsonArray(jsonObject.getAsJsonArray("hiddenTags")),
							jsonObject.get("minPlayers").getAsInt(),
							jsonObject.get("maxPlayers").getAsInt(),
							jsonObject.get("idealMinPlayers").getAsInt(),
							jsonObject.get("idealMaxPlayers").getAsInt()
					));
				} catch (Exception e) {
					LOGGER.error("", e);
				}
			});
		} catch (Exception e) {
			LOGGER.error("", e);
		}

		final StringBuilder stringBuilder = new StringBuilder("export class Game{private constructor(");
		stringBuilder.append("public readonly id:string,");
		stringBuilder.append("public readonly title:string,");
		stringBuilder.append("public readonly description:string,");
		stringBuilder.append("public readonly tags:string[],");
		stringBuilder.append("public readonly hiddenTags:string[],");
		stringBuilder.append("public readonly minPlayers:number,");
		stringBuilder.append("public readonly maxPlayers:number,");
		stringBuilder.append("public readonly idealMinPlayers:number,");
		stringBuilder.append("public readonly idealMaxPlayers:number,");
		stringBuilder.append("){}public static readonly GAMES:Game[]=[\n");
		games.forEach(stringBuilder::append);
		stringBuilder.append("];}");

		try {
			FileUtils.write(project.getRootDir().toPath().resolve("buildSrc/src/main/resources/website/src/app/entity/game.ts").toFile(), stringBuilder.toString(), StandardCharsets.UTF_8);
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
