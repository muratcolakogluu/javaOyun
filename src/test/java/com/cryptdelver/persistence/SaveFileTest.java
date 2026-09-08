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
                1, 0, 3,
                List.of(
                        new SaveData.ItemData("ARMOR", 0, 0, "Deri Zırh", 1, "armor_leather"),
                        new SaveData.ItemData("WEAPON", 0, 0, "Çelik Kılıç", 4, "sword_steel"),
                        new SaveData.ItemData("POTION", 0, 0, "İksir", 0, "potion"),
                        new SaveData.ItemData("SHIELD", 0, 0, "Demir Kalkan", 1, "shield_iron")),
                List.of(new SaveData.ItemData("GOLD", 5, 6, "Altın", 17, "gold")),
                List.of(
                        new SaveData.EnemyData("IMP", 10, 4, 3, 5, 2, 0),
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

    /**
     * Kalkan slotu surum 2'de eklendi; eski kayitlarda o alan yok ve okuyucu
     * bunu bos slot sayarak devam etmeli.
     */
    @Test
    @DisplayName("Kalkan oncesi surum 1 kaydi hala okunur")
    void olderVersionOneSaveStillLoads() throws IOException {
        Path path = tempDir.resolve("save.txt");
        Files.writeString(path, """
                version|1
                depth|2
                seed|42
                generator|0
                gold|10
                elapsed|5.0
                player|3|4|18
                equipped|0|1
                inv|WEAPON|0|0|Paslı Kılıç|2|sword
                """, StandardCharsets.UTF_8);

        Optional<SaveData> loaded = new SaveFile(path).read();

        assertTrue(loaded.isPresent());
        assertEquals(0, loaded.get().equippedWeaponSlot());
        assertEquals(1, loaded.get().equippedArmorSlot());
        assertEquals(SaveData.NO_SLOT, loaded.get().equippedShieldSlot(),
                "Eski kayitta kalkan yok");
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
