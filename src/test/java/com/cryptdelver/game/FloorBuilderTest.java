package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.world.BspGenerator;
import com.cryptdelver.world.RandomWalkGenerator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Kat kurulumu, oyun kurmadan.
 *
 * <p>Bu sinif once Game'in icindeydi ve kati kurmak icin bir oyun baslatmak
 * gerekiyordu. Ayrildiktan sonra kurulum yan etkisiz bir <em>deger</em>
 * uretiyor, yani dogrudan sinanabiliyor. Bu testler ozellikle o ayrimi
 * kilitliyor: kurulum oyunun durumuna dokunmuyor.</p>
 */
class FloorBuilderTest {

    private static final int WIDTH = 40;
    private static final int HEIGHT = 22;

    private FloorBuilder builder() {
        return new FloorBuilder(List.of(new BspGenerator(), new RandomWalkGenerator()),
                WIDTH, HEIGHT);
    }

    @Test
    @DisplayName("Doseme harita, dogus, merdiven ve buyucuden ibaret")
    void theLayoutHasNoEnemiesOrItems() {
        Floor floor = builder().layout(0, 1, 42L);

        assertNotNull(floor.dungeon());
        assertNotNull(floor.spawn());
        assertNotNull(floor.stairs());
        assertNull(floor.boss(), "Doseme bossu icermez");
        assertTrue(floor.enemies().isEmpty(), "Doseme dusman icermez");
        assertTrue(floor.groundItems().isEmpty(), "Doseme esya icermez");
    }

    /** Kayit yuklerken doseme tohumdan yeniden kuruluyor; ayni cikmak zorunda. */
    @Test
    @DisplayName("Ayni tohum ayni dosemeyi veriyor")
    void theLayoutIsDeterministic() {
        Floor first = builder().layout(0, 3, 12345L);
        Floor second = builder().layout(0, 3, 12345L);

        assertEquals(first.spawn(), second.spawn());
        assertEquals(first.stairs(), second.stairs());
        assertEquals(first.wizard() == null, second.wizard() == null, "Buyucu de belirlenimci");
    }

    @Test
    @DisplayName("Dolu kat dusman ve esya iceriyor")
    void aBuiltFloorIsPopulated() {
        Floor floor = builder().build(0, 1, 7L, Difficulty.NORMAL);

        assertFalse(floor.enemies().isEmpty(), "Katta dusman olmali");
        assertFalse(floor.groundItems().isEmpty(), "Katta esya olmali");
    }

    @Test
    @DisplayName("Boss kati bossla, siradan kat bossuz kuruluyor")
    void bossesOnlyAppearOnBossFloors() {
        assertNull(builder().build(0, 4, 7L, Difficulty.NORMAL).boss());

        Floor bossFloor = builder().build(0, 5, 7L, Difficulty.NORMAL);
        assertNotNull(bossFloor.boss());
        assertEquals(bossFloor.stairs(), bossFloor.boss().getTile(),
                "Boss merdivenin ustunde durmali");
        assertTrue(bossFloor.enemies().contains(bossFloor.boss()),
                "Boss dusman listesinde de olmali");
    }

    @Test
    @DisplayName("Boss katlari her bes katta bir")
    void bossCadenceIsEveryFifthFloor() {
        assertFalse(FloorBuilder.isBossFloor(4));
        assertTrue(FloorBuilder.isBossFloor(5));
        assertTrue(FloorBuilder.isBossFloor(20));

        assertEquals(1, FloorBuilder.bossNumber(5));
        assertEquals(4, FloorBuilder.bossNumber(20));
    }

    @Test
    @DisplayName("Zorluk kalabaligi olcekliyor")
    void difficultyChangesTheCrowd() {
        int easy = builder().build(0, 3, 7L, Difficulty.KOLAY).enemies().size();
        int hard = builder().build(0, 3, 7L, Difficulty.ZOR).enemies().size();

        assertTrue(hard > easy, "Zorda daha kalabalik: " + hard + " > " + easy);
    }

    /** Kurulum yan etkisiz: ayni yapici ustunde iki kat kurmak birbirini bozmuyor. */
    @Test
    @DisplayName("Iki kat kurmak birbirini etkilemiyor")
    void buildingTwiceKeepsBothFloorsIntact() {
        FloorBuilder builder = builder();

        Floor first = builder.build(0, 1, 11L, Difficulty.NORMAL);
        Floor second = builder.build(0, 2, 22L, Difficulty.NORMAL);

        assertFalse(first.enemies().isEmpty());
        assertFalse(second.enemies().isEmpty());
        assertFalse(first.dungeon() == second.dungeon(), "Iki ayri harita olmali");
    }
}
