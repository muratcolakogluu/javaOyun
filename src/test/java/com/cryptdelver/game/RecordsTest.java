package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.persistence.RecordsFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Kosularin ardinda kalan iz.
 *
 * <p>Olunce her sey siliniyordu ve geriye hicbir kayit kalmiyordu. "15'i
 * gecebildim" diye bir hedef ancak onceki denemeyi hatirlarsan doguyor.</p>
 */
class RecordsTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Bastan hicbir kosu yok")
    void startsEmpty() {
        Records records = new Records();

        assertFalse(records.hasAnyRun());
        assertEquals(0, records.getDeepestFloor());
        assertEquals(0, records.getMostGold());
    }

    @Test
    @DisplayName("Biten kosu sayiliyor")
    void aFinishedRunIsCounted() {
        Records records = new Records();

        records.recordRun(7, 240, false);

        assertTrue(records.hasAnyRun());
        assertEquals(1, records.getRuns());
        assertEquals(7, records.getDeepestFloor());
        assertEquals(240, records.getMostGold());
        assertEquals(0, records.getWins(), "Olmek zafer degil");
    }

    /** Yalnizca en iyiler saklaniyor: kotu bir kosu rekoru bozmuyor. */
    @Test
    @DisplayName("Daha kotu kosu rekoru dusurmuyor")
    void aWorseRunDoesNotLowerTheRecord() {
        Records records = new Records();
        records.recordRun(12, 500, false);

        records.recordRun(3, 40, false);

        assertEquals(12, records.getDeepestFloor());
        assertEquals(500, records.getMostGold());
        assertEquals(2, records.getRuns(), "Ama kosu yine de sayiliyor");
    }

    @Test
    @DisplayName("Kazanilan kosu ayrica sayiliyor")
    void winsAreCountedSeparately() {
        Records records = new Records();

        records.recordRun(FloorTheme.MAX_DEPTH, 900, true);

        assertEquals(1, records.getRuns());
        assertEquals(1, records.getWins());
        assertEquals(FloorTheme.MAX_DEPTH, records.getDeepestFloor());
    }

    @Test
    @DisplayName("Rekorlar diske yazilip geri okunuyor")
    void recordsRoundTripThroughDisk() {
        RecordsFile file = new RecordsFile(tempDir.resolve("records.txt"));
        Records saved = new Records();
        saved.recordRun(14, 730, false);
        saved.recordRun(20, 600, true);

        file.save(saved);

        Records loaded = new Records();
        file.load(loaded);

        assertEquals(20, loaded.getDeepestFloor());
        assertEquals(730, loaded.getMostGold());
        assertEquals(2, loaded.getRuns());
        assertEquals(1, loaded.getWins());
    }

    @Test
    @DisplayName("Dosya yoksa rekorlar bos kaliyor")
    void aMissingFileLeavesRecordsEmpty() {
        Records records = new Records();

        new RecordsFile(tempDir.resolve("yok.txt")).load(records);

        assertFalse(records.hasAnyRun());
    }

    /** Rekor kaybi can sikici, oyunun acilmamasi cok daha kotu. */
    @Test
    @DisplayName("Bozuk dosya oyunu durdurmuyor")
    void aCorruptFileIsIgnored() throws IOException {
        Path path = tempDir.resolve("records.txt");
        Files.writeString(path, "deepest|cok\nbilinmeyen|deger\n", StandardCharsets.UTF_8);

        Records records = new Records();
        new RecordsFile(path).load(records);

        assertEquals(0, records.getDeepestFloor(), "Bozuk deger yok sayilmali");
    }

    @Test
    @DisplayName("Klasor yoksa olusturuluyor")
    void createsMissingDirectories() {
        RecordsFile file = new RecordsFile(tempDir.resolve("derin/klasor/records.txt"));

        file.save(new Records());

        assertTrue(Files.isRegularFile(file.getPath()));
    }
}
