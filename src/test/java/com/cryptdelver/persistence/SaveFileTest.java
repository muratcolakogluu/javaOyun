package com.cryptdelver.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Kayit dosyasinin yazilip okunmasi. Gercek dosya sistemi kullaniliyor ama
 * gecici bir klasorde, kullanicinin gercek kaydina dokunmadan.
 */
class SaveFileTest {

    @TempDir
    Path tempDir;

    private SaveData sampleData() {
        return new SaveData(
                3, -4829157263L, 1, 137, 92.5,
                12, 7, 14,
                1, 0,
                List.of(
                        new SaveData.ItemData("ARMOR", 0, 0, "Deri Zırh", 1, "armor_leather"),
                        new SaveData.ItemData("WEAPON", 0, 0, "Çelik Kılıç", 4, "sword_steel"),
                        new SaveData.ItemData("POTION", 0, 0, "İksir", 0, "potion")),
                List.of(new SaveData.ItemData("GOLD", 5, 6, "Altın", 17, "gold")),
                List.of(
                        new SaveData.EnemyData("RAT", 10, 4, 3, 5, 2, 0),
                        new SaveData.EnemyData("BOSS", 20, 11, 30, 45, 6, 3)));
    }

    @Test
    @DisplayName("Yazilan kayit aynen geri okunur")
    void roundTripsThroughDisk() throws IOException {
        SaveFile file = new SaveFile(tempDir.resolve("save.txt"));
        SaveData original = sampleData();

        file.write(original);
        Optional<SaveData> loaded = file.read();

        assertTrue(loaded.isPresent());
        assertEquals(original, loaded.get(), "Kayit birebir geri gelmeli");
    }

    @Test
    @DisplayName("Turkce karakterler bozulmadan saklanir")
    void keepsTurkishCharacters() throws IOException {
        SaveFile file = new SaveFile(tempDir.resolve("save.txt"));
        file.write(sampleData());

        String contents = Files.readString(file.getPath(), StandardCharsets.UTF_8);

        assertTrue(contents.contains("Çelik Kılıç"), "Esya adi UTF-8 olarak yazilmali");
        assertTrue(contents.contains("Deri Zırh"));
    }

    @Test
    @DisplayName("Kayit yoksa bos doner, hata firlatmaz")
    void missingFileReturnsEmpty() throws IOException {
        SaveFile file = new SaveFile(tempDir.resolve("yok.txt"));

        assertFalse(file.exists());
        assertTrue(file.read().isEmpty());
    }

    @Test
    @DisplayName("Klasor yoksa olusturulur")
    void createsMissingDirectories() throws IOException {
        SaveFile file = new SaveFile(tempDir.resolve("derin/klasor/save.txt"));

        file.write(sampleData());

        assertTrue(file.exists());
    }

    @Test
    @DisplayName("Bozuk dosya sessizce yanlis okunmaz")
    void corruptFileIsRejected() throws IOException {
        Path path = tempDir.resolve("save.txt");
        Files.writeString(path, "version|1\ndepth|uc\n", StandardCharsets.UTF_8);

        assertThrows(IOException.class, () -> new SaveFile(path).read());
    }

    @Test
    @DisplayName("Desteklenmeyen surum reddedilir")
    void unknownVersionIsRejected() throws IOException {
        Path path = tempDir.resolve("save.txt");
        Files.writeString(path, "version|99\ndepth|3\n", StandardCharsets.UTF_8);

        assertThrows(IOException.class, () -> new SaveFile(path).read());
    }

    @Test
    @DisplayName("Kayit silinebilir")
    void deleteRemovesTheFile() throws IOException {
        SaveFile file = new SaveFile(tempDir.resolve("save.txt"));
        file.write(sampleData());

        file.delete();

        assertFalse(file.exists());
    }
}
