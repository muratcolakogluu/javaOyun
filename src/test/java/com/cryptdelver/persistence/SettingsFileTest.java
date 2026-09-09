package com.cryptdelver.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.game.Settings;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Ayarlarin diske yazilip okunmasi.
 *
 * <p>Buradaki en onemli davranis bozuk dosyanin oyunu durdurmamasi: ayar
 * dosyasi yuzunden oyunun acilmamasi sacma olurdu.</p>
 */
class SettingsFileTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Yazilan ayarlar aynen geri okunur")
    void roundTripsThroughDisk() {
        SettingsFile file = new SettingsFile(tempDir.resolve("settings.txt"));
        Settings saved = new Settings();
        saved.setVolume(0.3);
        saved.setMuted(true);

        file.save(saved);

        Settings loaded = new Settings();
        file.load(loaded);

        assertEquals(0.3, loaded.getVolume(), 1e-9);
        assertTrue(loaded.isMuted());
    }

    @Test
    @DisplayName("Dosya yoksa varsayilanlar korunur")
    void missingFileLeavesDefaults() {
        Settings settings = new Settings();
        double before = settings.getVolume();

        new SettingsFile(tempDir.resolve("yok.txt")).load(settings);

        assertEquals(before, settings.getVolume(), 1e-9);
        assertFalse(settings.isMuted());
    }

    @Test
    @DisplayName("Bozuk dosya oyunu durdurmaz")
    void corruptFileIsIgnored() throws IOException {
        Path path = tempDir.resolve("settings.txt");
        Files.writeString(path, "volume|cok\nbilinmeyen|deger\n", StandardCharsets.UTF_8);

        Settings settings = new Settings();
        double before = settings.getVolume();

        new SettingsFile(path).load(settings);

        assertEquals(before, settings.getVolume(), 1e-9, "Bozuk deger yok sayilmali");
    }

    @Test
    @DisplayName("Tanimadigimiz satirlar atlanir")
    void unknownKeysAreSkipped() throws IOException {
        Path path = tempDir.resolve("settings.txt");
        Files.writeString(path, "volume|0.7\nmuzik|acik\n", StandardCharsets.UTF_8);

        Settings settings = new Settings();
        new SettingsFile(path).load(settings);

        assertEquals(0.7, settings.getVolume(), 1e-9, "Bilinen ayar yine okunmali");
    }

    @Test
    @DisplayName("Klasor yoksa olusturulur")
    void createsMissingDirectories() {
        SettingsFile file = new SettingsFile(tempDir.resolve("derin/klasor/settings.txt"));

        file.save(new Settings());

        assertTrue(Files.isRegularFile(file.getPath()));
    }
}
