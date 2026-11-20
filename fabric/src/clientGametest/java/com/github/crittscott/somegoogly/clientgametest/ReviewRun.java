package com.github.crittscott.somegoogly.clientgametest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

/**
 * One run's review folder: the screenshots copied out of Fabric's screenshot directory, and
 * {@code manifest.json}, which the screenshot-review app reads. The manifest is rewritten after every entry, so a
 * run that stops early still leaves a reviewable folder. Its format is documented in the review app's README.
 */
final class ReviewRun {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static ReviewRun current;

    private final JsonArray entries = new JsonArray();
    private final Path folder;
    private final JsonObject manifest = new JsonObject();
    private int sequence;

    private ReviewRun(String group) {
        String started = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss", Locale.ROOT));
        this.folder = FabricLoader.getInstance().getGameDir().resolve("review")
                .resolve(group.isEmpty() ? started : started + "-" + group);
        manifest.addProperty("minecraftVersion", version("minecraft"));
        manifest.addProperty("loader", "fabric");
        manifest.addProperty("modVersion", version("somegoogly"));
        manifest.addProperty("launchGroup", group);
        manifest.addProperty("started", started);
        manifest.add("shots", entries);
        try {
            Files.createDirectories(folder);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        write();
    }

    /** The run every test class in this launch writes into. */
    static synchronized ReviewRun get() {
        if (current == null) {
            current = new ReviewRun(Scene.launchGroup());
        }
        return current;
    }

    private static String version(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
    }

    /** Records a scenario that could not finish, so the reviewer sees it in place of its missing shots. */
    synchronized void error(String test, String title, String message) {
        JsonObject entry = new JsonObject();
        entry.addProperty("test", test);
        entry.addProperty("title", title);
        entry.addProperty("file", "");
        entry.addProperty("expect", "Scenario did not finish.");
        entry.addProperty("error", message);
        entries.add(entry);
        write();
    }

    /** Copies a screenshot Fabric saved into the run folder and records what it should show. */
    synchronized void shot(String test, String title, Path screenshot, String expect) {
        sequence++;
        String file = String.format(Locale.ROOT, "%04d_%s.png", sequence, test.replace('.', '-'));
        try {
            Files.copy(screenshot, folder.resolve(file), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        JsonObject entry = new JsonObject();
        entry.addProperty("test", test);
        entry.addProperty("title", title);
        entry.addProperty("file", file);
        entry.addProperty("expect", expect);
        entries.add(entry);
        write();
    }

    private void write() {
        Path target = folder.resolve("manifest.json");
        Path temporary = folder.resolve("manifest.json.tmp");
        try {
            Files.writeString(temporary, GSON.toJson(manifest), StandardCharsets.UTF_8);
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
