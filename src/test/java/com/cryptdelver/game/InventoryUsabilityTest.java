package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Bomb;
import com.cryptdelver.entity.HastePotion;
import com.cryptdelver.entity.Item;
import com.cryptdelver.entity.Player;
import com.cryptdelver.entity.Potion;
import com.cryptdelver.entity.Weapon;
import com.cryptdelver.world.Dungeon;
import com.cryptdelver.world.Tile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Cantayi kullanilabilir kilan seyler.
 *
 * <p>Slotta yalnizca ikon vardi ve canta doluyken takas etmek icin kareden
 * cikip geri gelmek gerekiyordu. Ikisi de ezber ya da ugras istiyordu.</p>
 */
class InventoryUsabilityTest {

    private Player player;
    private Game game;

    @BeforeEach
    void setUp() {
        Dungeon dungeon = new Dungeon(11, 9);
        dungeon.fill(Tile.FLOOR);
        player = new Player(4, 4);
        game = new Game(dungeon, player);
    }

    @Test
    @DisplayName("Her esya ne yaptigini soyluyor")
    void everyItemExplainsItself() {
        assertFalse(new Potion(0, 0).getDescription().isEmpty());
        assertFalse(new Bomb(0, 0).getDescription().isEmpty());
        assertFalse(new HastePotion(0, 0).getDescription().isEmpty());
    }

    /** Ayni adi tasiyan iki kilic ancak yukseltmesiyle ayrilabiliyor. */
    @Test
    @DisplayName("Ekipmanin balon adi yukseltmeyi de gosteriyor")
    void gearNamesShowTheirUpgrades() {
        Weapon weapon = new Weapon(0, 0, "Test Kilici", 4, "sword");
        assertEquals("Test Kilici", weapon.getFullTooltipName());

        weapon.upgrade();

        assertTrue(weapon.getFullTooltipName().contains("+1"), weapon.getFullTooltipName());
    }

    @Test
    @DisplayName("Ekipmanin aciklamasi bonusu ve dayanikligi veriyor")
    void gearDescriptionsShowTheNumbers() {
        Weapon weapon = new Weapon(0, 0, "Test Kilici", 6, "sword", 90);

        String detail = weapon.getDescription();

        assertTrue(detail.contains("+6"), detail);
        assertTrue(detail.contains("90/90"), detail);
    }

    /**
     * Canta doluyken takas: birakilan parca yere dusuyor, ayagin altindaki
     * hemen cantaya giriyor. Once kareden cikip geri gelmek gerekiyordu.
     */
    @Test
    @DisplayName("Bir sey birakinca ayagindaki hemen aliniyor")
    void droppingSwapsWithWhatIsUnderfoot() {
        for (int i = 0; i < Inventory.CAPACITY; i++) {
            game.getInventory().add(new Weapon(0, 0, "Doldurma " + i, 2, "sword"));
        }
        assertTrue(game.getInventory().isFull(), "Canta dolu olmali");

        Item onFloor = new Bomb(player.getTileX(), player.getTileY());
        game.addGroundItem(onFloor);

        game.dropItem(0);

        assertTrue(game.getInventory().getItems().contains(onFloor), "Yerdeki alinmali");
        assertFalse(game.getGroundItems().contains(onFloor), "Yerde kalmamali");
    }

    /** Biraktigin parcayi geri toplamiyorsun; yoksa takas hic olmazdi. */
    @Test
    @DisplayName("Yeni birakilan parca geri alinmiyor")
    void theJustDroppedItemStaysOnTheGround() {
        Weapon carried = new Weapon(0, 0, "Birakilan", 2, "sword");
        game.getInventory().add(carried);

        game.dropItem(0);

        assertTrue(game.getGroundItems().contains(carried), "Yerde kalmali");
        assertFalse(game.getInventory().getItems().contains(carried));
    }
}
