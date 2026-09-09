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
                12, 7, 14, 26,
                1, 0,
                List.of(
                        new SaveData.ItemData("ARMOR", 0, 0, "Deri Zırh", 1, "armor_leather"),
                        new SaveData.ItemData("WEAPON", 0, 0, "Çelik Kılıç", 4, "sword_steel"),
                        new SaveData.ItemData("POTION", 0, 0, "İksir", 0, "potion")),
                List.of(new SaveData.ItemData("GOLD", 5, 6, "Altın", 17, "gold")),
                List.of(
                        new SaveData.EnemyData("IMP", 10, 4, 3, 5, 2, 0),
                        new SaveData.EnemyData("BOSS", 20, 11, 30, 45, 6, 3)));
    }

    /** Ayni kayit, ustune gezilmis kat hafizasi ve kesif maskesi. */
    private SaveData sampleWithFloorMemory() {
        SaveData base = sampleData();
        return new SaveData(
                base.depth(), base.seed(), base.generatorIndex(), base.gold(), base.elapsedSeconds(),
                base.playerX(), base.playerY(), base.playerHp(), base.playerMaxHp(),
                base.equippedWeaponSlot(), base.equippedArmorSlot(),
                "110010",
                base.inventory(), base.groundItems(), base.enemies(),
                List.of(
                        new SaveData.FloorData(1, 918273645L, 0, 41.5, true, "101010",
                                List.of(new SaveData.ItemData("POTION", 3, 4, "İksir", 0, "potion")),
                                List.of(new SaveData.EnemyData("IMP", 9, 3, 4, 5, 2, 0))),
                        new SaveData.FloorData(2, -55L, 1, 0.0, false, "000111",
                                List.of(), List.of())));
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

    /**
     * Gezilmis katlar da dosyaya yaziliyor. Kat bloklari ic ice bir yapi:
     * {@code floor} satirindan sonraki icerik satirlari o kata ait, o yuzden
     * sirasinin ve sahipliginin bozulmadigini ayrica dogruluyoruz.
     */
    @Test
    @DisplayName("Gezilmis katlar ve kesif maskesi aynen geri okunur")
    void floorMemoryRoundTripsThroughDisk() throws IOException {
        SaveFile file = new SaveFile(tempDir.resolve("save.txt"));
        SaveData original = sampleWithFloorMemory();

        file.write(original);
        Optional<SaveData> loaded = file.read();

        assertTrue(loaded.isPresent());
        assertEquals(original, loaded.get(), "Kat hafizasi birebir geri gelmeli");
    }

    @Test
    @DisplayName("Kat icerigi yanlis kata yazilmiyor")
    void floorContentStaysWithItsOwnFloor() throws IOException {
        SaveFile file = new SaveFile(tempDir.resolve("save.txt"));
        file.write(sampleWithFloorMemory());

        SaveData loaded = file.read().orElseThrow();

        assertEquals(1, loaded.visitedFloors().get(0).enemies().size(), "Dusman ilk kata ait");
        assertTrue(loaded.visitedFloors().get(1).enemies().isEmpty(), "Ikinci kat bos");
    }

    /** Kat satiri olmadan gelen kat icerigi sessizce yutulmuyor. */
    @Test
    @DisplayName("Sahipsiz kat satiri bozuk kayit sayilir")
    void orphanFloorContentIsRejected() throws IOException {
        Path path = tempDir.resolve("save.txt");
        Files.write(path, List.of("version|8", "depth|1", "fenemy|IMP|1|1|1|1|1|0"),
                StandardCharsets.UTF_8);

        assertThrows(IOException.class, () -> new SaveFile(path).read());
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
     * Surum 1 kayitlarinda kalkan/kask slotu yoktu, sonraki surumlerde vardi ve
     * simdi ikisi de oyundan kalkti. Okuyucu her uc durumu da kaldirmali.
     */
    @Test
    @DisplayName("Eski surum kaydi hala okunur")
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
