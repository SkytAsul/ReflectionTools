package fr.skytasul.reflection.shrieker.minecraft;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fr.skytasul.reflection.Version;
import fr.skytasul.reflection.mappings.Mappings;
import fr.skytasul.reflection.mappings.files.ProguardMapping;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class MinecraftMappingsProvider {

	private static final String MOJANG_MAPPINGS_URL =
			"https://raw.githubusercontent.com/InventivetalentDev/minecraft-assets/{VERSION}/mappings/server.txt";
	private static final String SPIGOT_VERSION_URL = "https://hub.spigotmc.org/versions/{VERSION}.json";
	private static final String SPIGOT_INFO_URL =
			"https://hub.spigotmc.org/stash/projects/SPIGOT/repos/builddata/raw/info.json?at={COMMIT}";
	private static final String SPIGOT_MAPPING_URL =
			"https://hub.spigotmc.org/stash/projects/SPIGOT/repos/builddata/raw/mappings/{MAPPING}?at={COMMIT}";

	private static final ProguardMapping PROGUARD_MAPPING = new ProguardMapping(true);
	private static final SpigotClassMapping SPIGOT_CLASS_MAPPING = new SpigotClassMapping(true);
	private static final SpigotMemberMapping SPIGOT_MEMBER_MAPPING = new SpigotMemberMapping(true);

	private static final Gson GSON = new Gson();

	private final @NotNull Path dataFolder;

	public MinecraftMappingsProvider(@NotNull Path dataFolder) throws IOException {
		this.dataFolder = dataFolder;

		Files.createDirectories(dataFolder);
	}

	public @NotNull Mappings loadMinecraftMappings(@NotNull Version version) throws IOException {
		var filePath = dataFolder.resolve(version.toString() + ".txt");

		saveFromUrl(new URL(MOJANG_MAPPINGS_URL.replace("{VERSION}", version.toString(true))), filePath);

		return PROGUARD_MAPPING.parse(Files.readAllLines(filePath));
	}

	public @NotNull Mappings loadSpigotMappings(@NotNull Version version) throws IOException {
		var versionDataRaw = readFromUrl(new URL(SPIGOT_VERSION_URL.replace("{VERSION}", version.toString(true))));
		var versionData = GSON.fromJson(versionDataRaw, JsonObject.class);
		var commit = versionData.get("refs").getAsJsonObject().get("BuildData").getAsString();

		var infoDataRaw = readFromUrl(new URL(SPIGOT_INFO_URL.replace("{COMMIT}", commit)));
		var infoData = GSON.fromJson(infoDataRaw, JsonObject.class);
		var classMappingFile = infoData.get("classMappings").getAsString();

		var classMappingPath = dataFolder.resolve(classMappingFile);
		saveFromUrl(new URL(SPIGOT_MAPPING_URL.replace("{COMMIT}", commit).replace("{MAPPING}", classMappingFile)),
				classMappingPath);
		var classMapping = SPIGOT_CLASS_MAPPING.parse(Files.readAllLines(classMappingPath));

		if (infoData.has("memberMappings")) {
			// 1.17.1
			var memberMappingFile = infoData.get("memberMappings").getAsString();
			var memberMappingPath = dataFolder.resolve(memberMappingFile);
			saveFromUrl(new URL(SPIGOT_MAPPING_URL.replace("{COMMIT}", commit).replace("{MAPPING}", memberMappingFile)),
					memberMappingPath);
			var memberMapping = SPIGOT_MEMBER_MAPPING.parse(Files.readAllLines(memberMappingPath));

			return SpigotMappingsMerger.merge(classMapping, memberMapping);
		} else {
			return classMapping;
		}
	}

	private static @NotNull String readFromUrl(@NotNull URL url) throws IOException {
		try (InputStream in = url.openStream()) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static void saveFromUrl(@NotNull URL url, @NotNull Path filePath) throws IOException {
		if (Files.exists(filePath))
			return;

		try (var downloadChannel = Channels.newChannel(url.openStream());
				var fileChannel = FileChannel.open(filePath, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {

			fileChannel.transferFrom(downloadChannel, 0, Long.MAX_VALUE);
		}
	}

}
