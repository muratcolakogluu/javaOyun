package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Bolgelerin kendi tasarimi.
 *
 * <p>Uzun sure bolgeler arasinda yalnizca renk perdesi degisiyordu: dort bolge
 * de ayni tugla duvari farkli renkte gosteriyordu, yani "baska bir yerdeyim"
 * hissi tamamen renkten geliyordu. Artik her bolgenin kendi duvar tasi, kendi
 * zemin cesitleri ve kendi duvar isareti var.</p>
 */
class RegionLookTest {

    /** Duvar tasi bolgenin asil kimligi; ikisi ayni tasi kullanmamali. */
    @Test
    @DisplayName("Her bolgenin duvari kendine ait")
    void everyRegionHasItsOwnWall() {
        Set<String> firstWalls = new HashSet<>();
        for (FloorTheme theme : FloorTheme.values()) {
            firstWalls.add(theme.getWallSprites()[0]);
        }

        assertEquals(FloorTheme.values().length, firstWalls.size(),
                "Iki bolge ayni duvarla baslamamali");
    }

    @Test
    @DisplayName("Her bolgenin zemini kendine ait")
    void everyRegionHasItsOwnFloors() {
        Set<String> firstFloors = new HashSet<>();
        for (FloorTheme theme : FloorTheme.values()) {
            firstFloors.add(theme.getFloorSprites()[0]);
        }

        assertEquals(FloorTheme.values().length, firstFloors.size());
    }

    /**
     * Ayni resmi kirk kere yan yana koymak zemini duvar kagidina ceviriyordu;
     * goz tekrari hemen yakaliyor.
     */
    @Test
    @DisplayName("Her bolgede birden fazla zemin cesidi var")
    void floorsComeInVariants() {
        for (FloorTheme theme : FloorTheme.values()) {
            assertTrue(theme.getFloorSprites().length > 1,
                    theme + " tek zemin karosuyla duvar kagidi gibi durur");
        }
    }

    /** Korluk cokmus bir yer: duvari da tek tip degil. */
    @Test
    @DisplayName("Korluk'un duvari birden fazla cesit")
    void theEmbersHaveCrumblingWalls() {
        assertTrue(FloorTheme.KORLUK.getWallSprites().length > 1);
        assertEquals(1, FloorTheme.KRIPT.getWallSprites().length,
                "Kript yapilmis bir yer, duvari duzgun");
    }

    @Test
    @DisplayName("Her bolgenin duvar isareti kendine ait")
    void everyRegionHasItsOwnWallMark() {
        Set<String> marks = new HashSet<>();
        for (FloorTheme theme : FloorTheme.values()) {
            marks.add(theme.getWallMarkSprite());
        }

        assertEquals(FloorTheme.values().length, marks.size());
    }

    /** Disari verilen dizi kopyalaniyor; cagiran tarafin bolgeyi bozmasi mumkun degil. */
    @Test
    @DisplayName("Karo listeleri disaridan degistirilemiyor")
    void spriteListsAreCopies() {
        String[] floors = FloorTheme.MAHZEN.getFloorSprites();
        String original = floors[0];
        floors[0] = "bozuk";

        assertEquals(original, FloorTheme.MAHZEN.getFloorSprites()[0]);
    }

    /** Renk perdesi de duruyor: tas degisti diye renk kimligi kalkmadi. */
    @Test
    @DisplayName("Bolgelerin rengi hala farkli")
    void regionsStillDifferInColour() {
        List<FloorTheme> themes = List.of(FloorTheme.values());

        for (int i = 0; i < themes.size(); i++) {
            for (int j = i + 1; j < themes.size(); j++) {
                assertNotEquals(themes.get(i).getTint(false), themes.get(j).getTint(false));
            }
        }
    }
}
