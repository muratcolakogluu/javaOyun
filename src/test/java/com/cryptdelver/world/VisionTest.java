package com.cryptdelver.world;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Gorus alani.
 *
 * <p>Once kata iner inmez butun harita gorunuyordu, yani zindanda kesif
 * yoktu. Simdi fener kadar goruyorsun ve gordugun yeri hatirliyorsun. Bu
 * testler uc hali de kilitliyor: gorunur, hatirlanan, karanlik.</p>
 */
class VisionTest {

    private Dungeon dungeon;
    private Vision vision;

    @BeforeEach
    void setUp() {
        dungeon = new Dungeon(41, 21);
        dungeon.fill(Tile.FLOOR);
        vision = new Vision(41, 21);
    }

    @Test
    @DisplayName("Bastan hicbir sey gorunmuyor")
    void nothingIsVisibleBeforeLooking() {
        assertFalse(vision.isVisible(5, 5));
        assertFalse(vision.isRemembered(5, 5));
    }

    @Test
    @DisplayName("Durdugun kare ve yakini gorunuyor")
    void nearbyTilesBecomeVisible() {
        vision.update(dungeon, 20, 10);

        assertTrue(vision.isVisible(20, 10), "Kendi karen");
        assertTrue(vision.isVisible(22, 10), "Iki kare otesi");
        assertTrue(vision.isVisible(20, 13), "Uc kare asagi");
    }

    /** Yaricap disini gormuyorsun: fener kadar goruyorsun. */
    @Test
    @DisplayName("Yaricapin disi karanlik")
    void distantTilesStayDark() {
        vision.update(dungeon, 20, 10);

        assertFalse(vision.isVisible(20 + Vision.RADIUS + 1, 10));
        assertFalse(vision.isRemembered(20 + Vision.RADIUS + 1, 10));
    }

    @Test
    @DisplayName("Duvar arkasi gorunmuyor")
    void wallsBlockTheView() {
        for (int y = 0; y < 21; y++) {
            dungeon.setTile(23, y, Tile.WALL);
        }

        vision.update(dungeon, 20, 10);

        assertTrue(vision.isVisible(23, 10), "Duvarin kendisi gorunuyor");
        assertFalse(vision.isVisible(25, 10), "Duvarin arkasi gorunmuyor");
    }

    /**
     * Merdivenin yerini hatirlaman bu sayede: gordugun zemin sende kaliyor,
     * uzaklassan da.
     */
    @Test
    @DisplayName("Gorulen kare uzaklasinca hatirlaniyor")
    void seenTilesAreRemembered() {
        vision.update(dungeon, 5, 10);
        assertTrue(vision.isVisible(7, 10));

        vision.update(dungeon, 30, 10);

        assertFalse(vision.isVisible(7, 10), "Artik isik altinda degil");
        assertTrue(vision.isRemembered(7, 10), "Ama yerini biliyorsun");
    }

    @Test
    @DisplayName("Harita disi her zaman karanlik")
    void outOfBoundsIsAlwaysDark() {
        vision.update(dungeon, 20, 10);

        assertFalse(vision.isVisible(-1, 10));
        assertFalse(vision.isRemembered(41, 10));
    }

    /** Yeni kat bastan karanlik; onceki katin hatirladiklari tasinmiyor. */
    @Test
    @DisplayName("Sifirlamak her seyi unutturuyor")
    void resetForgetsEverything() {
        vision.update(dungeon, 20, 10);
        assertTrue(vision.isRemembered(20, 10));

        vision.reset();

        assertFalse(vision.isVisible(20, 10));
        assertFalse(vision.isRemembered(20, 10));
    }

    /** Ayni kareden ikinci cagri bosa is yapmiyor ama sonucu da bozmuyor. */
    @Test
    @DisplayName("Ayni karede tekrar hesaplamak sonucu degistirmiyor")
    void recomputingFromTheSameTileIsStable() {
        vision.update(dungeon, 20, 10);
        vision.update(dungeon, 20, 10);

        assertTrue(vision.isVisible(20, 10));
        assertTrue(vision.isVisible(23, 10));
    }
}
