package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Armor;
import com.cryptdelver.entity.Player;
import com.cryptdelver.entity.Potion;
import com.cryptdelver.entity.Weapon;
import com.cryptdelver.world.Dungeon;
import com.cryptdelver.world.Tile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Cantadan yere birakma.
 *
 * <p>Kritik davranis: birakilan esya ayni karede dururken geri toplanmiyor,
 * ama o kareden cikip donunce toplaniyor.</p>
 */
class DropTest {

    private static final double FRAME = 1.0 / 60;

    private Player player;
    private Game game;

    @BeforeEach
    void setUp() {
        Dungeon dungeon = new Dungeon(15, 9);
        dungeon.fill(Tile.FLOOR);
        for (int i = 0; i < 15; i++) {
            dungeon.setTile(i, 0, Tile.WALL);
            dungeon.setTile(i, 8, Tile.WALL);
        }
        for (int i = 0; i < 9; i++) {
            dungeon.setTile(0, i, Tile.WALL);
            dungeon.setTile(14, i, Tile.WALL);
        }

        player = new Player(4, 4);
        game = new Game(dungeon, player);
    }

    /**
     * Oyuncuyu verilen kareye tasir ve bir kare isletir.
     *
     * <p>Yuruyerek degil dogrudan tasiyoruz: burada test edilen sey hareketin
     * kendisi degil, "yeni bir kareye girince toplama denenir" kurali. Yuruyus
     * ItemTest'te ayrica test ediliyor.</p>
     */
    private void moveTo(int x, int y) {
        player.setTile(x, y);
        game.update(FRAME);
    }

    private void waitFrames(int frames) {
        for (int i = 0; i < frames; i++) {
            game.update(FRAME);
        }
    }

    @Test
    @DisplayName("Birakilan esya cantadan cikip ayagimizin dibine duser")
    void droppedItemLandsUnderThePlayer() {
        Potion potion = new Potion(0, 0);
        game.getInventory().add(potion);

        assertTrue(game.dropItem(0));

        assertTrue(game.getInventory().isEmpty(), "Esya cantadan cikmali");
        assertEquals(1, game.getGroundItems().size());
        assertEquals(player.getTile(), potion.getTile(), "Esya durdugumuz kareye dusmeli");
    }

    @Test
    @DisplayName("Birakilan esya ayni karede beklerken geri alinmaz")
    void droppedItemIsNotImmediatelyPickedUpAgain() {
        game.getInventory().add(new Potion(0, 0));
        game.dropItem(0);

        waitFrames(120);

        assertTrue(game.getInventory().isEmpty(), "Esya kendiliginden geri alinmamali");
        assertEquals(1, game.getGroundItems().size());
    }

    @Test
    @DisplayName("Uzaklasip donunce esya tekrar alinir")
    void leavingAndReturningPicksItUpAgain() {
        game.getInventory().add(new Potion(0, 0));
        game.dropItem(0);

        moveTo(6, 4);
        moveTo(4, 4);

        assertEquals(1, game.getInventory().size(), "Kareye tekrar girince alinmali");
        assertTrue(game.getGroundItems().isEmpty());
    }

    @Test
    @DisplayName("Kusanilan silah birakilinca elden de cikar")
    void droppingEquippedWeaponUnequipsIt() {
        Weapon sword = LootTable.weaponForTier(2, 0, 0);
        game.getInventory().add(sword);
        game.useItem(0);
        int armedAttack = player.getAttackPower();

        game.dropItem(0);

        assertNull(player.getEquippedWeapon(), "Silah elde kalmamali");
        assertTrue(player.getAttackPower() < armedAttack, "Vurus gucu ciplak ele donmeli");
    }

    @Test
    @DisplayName("Kusanilan zirh birakilinca ustumuzden cikar")
    void droppingEquippedArmorUnequipsIt() {
        Armor armor = LootTable.armorForTier(3, 0, 0);
        game.getInventory().add(armor);
        game.useItem(0);
        assertTrue(player.getDefense() > 0);

        game.dropItem(0);

        assertNull(player.getEquippedArmor());
        assertEquals(0, player.getDefense());
    }

    @Test
    @DisplayName("Birakilan esya sonra tekrar kusanilabilir")
    void droppedGearCanBeReclaimed() {
        Weapon sword = LootTable.weaponForTier(2, 0, 0);
        game.getInventory().add(sword);
        game.useItem(0);

        game.dropItem(0);
        moveTo(6, 4);
        moveTo(4, 4);
        game.useItem(0);

        assertSame(sword, player.getEquippedWeapon());
    }

    @Test
    @DisplayName("Bos slot ve olu oyuncu icin birakma bir sey yapmaz")
    void droppingEmptySlotOrWhenDeadDoesNothing() {
        assertFalse(game.dropItem(0), "Bos slot birakilamaz");
        assertFalse(game.dropItem(-1), "Gecersiz slot birakilamaz");

        game.getInventory().add(new Potion(0, 0));
        player.takeDamage(player.getMaxHp());

        assertFalse(game.dropItem(0), "Olu oyuncu esya birakamaz");
        assertEquals(1, game.getInventory().size());
    }

    @Test
    @DisplayName("Canta doluyken bosaltip yer acilabilir")
    void droppingFreesSpaceInTheBag() {
        for (int i = 0; i < Inventory.CAPACITY; i++) {
            game.getInventory().add(new Potion(0, 0));
        }
        assertTrue(game.getInventory().isFull());

        game.dropItem(0);

        assertFalse(game.getInventory().isFull());
        assertEquals(Inventory.CAPACITY - 1, game.getInventory().size());
    }
}
