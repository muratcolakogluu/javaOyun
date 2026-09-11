package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Gold;
import com.cryptdelver.entity.Player;
import com.cryptdelver.entity.Potion;
import com.cryptdelver.entity.Weapon;
import com.cryptdelver.world.Dungeon;
import com.cryptdelver.world.Tile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Esya toplama ve kullanma kurallari.
 *
 * <p>Oyuncu saniyede 6 kare gittigi icin bir adim 11 kareyi (frame) buluyor;
 * testlerde bir kare ilerlemek icin bu kadar simule ediyoruz.</p>
 */
class ItemTest {

    private static final double FRAME = 1.0 / 60;
    private static final int FRAMES_PER_STEP = 11;

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

    /** Oyuncuyu saga bir kare yurutup ayagindakini almasini soyler. */
    private void stepRightAndTake() {
        stepRight();
        game.pickUp();
    }

    /** Oyuncuyu saga bir kare yurutur; toplama ayri bir is. */
    private void stepRight() {
        player.setMoveInput(1, 0);
        for (int i = 0; i < FRAMES_PER_STEP; i++) {
            game.update(FRAME);
        }
        player.setMoveInput(0, 0);
    }

    /**
     * Sinir su: <b>cantani sikistirabilen sey tus istiyor, sikistiramayan
     * istemiyor.</b> Iksir tek slotta yigiliyor, altin cantaya hic girmiyor;
     * ikisi icin de tus beklemek hicbir karara karsilik gelmeyen fazladan bir
     * isti. Ekipman ise slot doldurdugu icin karar gerektiriyor.
     */
    @Test
    @DisplayName("Ustune basilan iksir kendiliginden cantaya girer")
    void steppingOnAPotionPicksItUp() {
        Potion potion = new Potion(5, 4);
        game.addGroundItem(potion);

        stepRight();

        assertEquals(5, player.getTileX());
        assertTrue(game.getGroundItems().isEmpty(), "Iksir yerden kalkmali");
        assertSame(potion, game.getInventory().get(0));
    }

    @Test
    @DisplayName("Ekipman ustunden gecmekle alinmiyor")
    void walkingOverGearLeavesIt() {
        game.addGroundItem(LootTable.weaponForTier(1, 5, 4));

        stepRight();

        assertEquals(5, player.getTileX(), "Uzerinde duruyoruz");
        assertEquals(1, game.getGroundItems().size(), "Ekipman yerinde kalmali");
        assertTrue(game.getInventory().isEmpty());
    }

    @Test
    @DisplayName("Ekipman F ile aliniyor")
    void gearIsTakenWithTheKey() {
        game.addGroundItem(LootTable.weaponForTier(1, 5, 4));

        stepRightAndTake();

        assertTrue(game.getGroundItems().isEmpty(), "Ekipman yerden kalkmali");
        assertEquals(1, game.getInventory().size());
    }

    /** Ayaginin altinda bir sey yokken F bosa basiliyor. */
    @Test
    @DisplayName("Bos karede toplama bir sey yapmaz")
    void takingOnAnEmptyTileDoesNothing() {
        assertFalse(game.pickUp(), "Alinacak bir sey yok");
        assertTrue(game.getInventory().isEmpty());
    }

    @Test
    @DisplayName("Altin kendiliginden keseye gider")
    void goldGoesToThePurse() {
        game.addGroundItem(new Gold(5, 4, 12));

        stepRight();

        assertEquals(12, game.getGold());
        assertTrue(game.getInventory().isEmpty(), "Altin canta yeri kaplamamali");
    }

    @Test
    @DisplayName("Canta doluysa esya yerde kalir")
    void fullInventoryLeavesItemOnGround() {
        // Yigilmayan parcalarla dolduruyoruz: iksirler tek slotu paylasirdi.

        for (int i = 0; i < Inventory.CAPACITY; i++) {
            game.getInventory().add(LootTable.weaponForTier(1, 0, 0));
        }
        game.addGroundItem(new Potion(5, 4));

        stepRight();

        assertEquals(1, game.getGroundItems().size(), "Esya yerde kalmali");
    }

    @Test
    @DisplayName("Canta doluyken bile altin alinir")
    void goldIsPickedUpEvenWhenBagIsFull() {
        // Yigilmayan parcalarla dolduruyoruz: iksirler tek slotu paylasirdi.

        for (int i = 0; i < Inventory.CAPACITY; i++) {
            game.getInventory().add(LootTable.weaponForTier(1, 0, 0));
        }
        game.addGroundItem(new Gold(5, 4, 7));

        stepRight();

        assertEquals(7, game.getGold());
        assertTrue(game.getGroundItems().isEmpty());
    }

    @Test
    @DisplayName("Iksir can doldurur ve tukenir")
    void potionHealsAndIsConsumed() {
        player.takeDamage(10);
        game.getInventory().add(new Potion(0, 0));

        game.useItem(0);

        assertEquals(18, player.getHp(), "10 hasarin 8'i iyilesmeli");
        assertTrue(game.getInventory().isEmpty(), "Iksir tukenmeli");
    }

    @Test
    @DisplayName("Dolu canla iksir harcanmaz")
    void potionIsNotWastedAtFullHealth() {
        game.getInventory().add(new Potion(0, 0));

        game.useItem(0);

        assertEquals(player.getMaxHp(), player.getHp());
        assertEquals(1, game.getInventory().size(), "Iksir cantada kalmali");
    }

    @Test
    @DisplayName("Silah kusanilinca vurus gucu artar ve cantada kalir")
    void weaponIsEquippedAndKept() {
        int baseAttack = player.getAttackPower();
        Weapon sword = LootTable.weaponForTier(1, 0, 0);
        game.getInventory().add(sword);

        game.useItem(0);

        assertSame(sword, player.getEquippedWeapon());
        assertEquals(baseAttack + sword.getAttackBonus(), player.getAttackPower());
        assertEquals(1, game.getInventory().size(), "Silah tukenmemeli");
    }

    @Test
    @DisplayName("Ikinci silah oncekinin yerini alir")
    void equippingAnotherWeaponSwaps() {
        Weapon sword = LootTable.weaponForTier(1, 0, 0);
        Weapon axe = LootTable.weaponForTier(3, 0, 0);
        game.getInventory().add(sword);
        game.getInventory().add(axe);

        game.useItem(0);
        game.useItem(1);

        assertSame(axe, player.getEquippedWeapon());
        assertEquals(2, game.getInventory().size(), "Iki silah da cantada kalmali");
    }

    @Test
    @DisplayName("Bos slot ve olu oyuncu icin kullanma bir sey yapmaz")
    void usingEmptySlotOrWhenDeadDoesNothing() {
        game.useItem(3);
        assertTrue(game.getInventory().isEmpty());

        game.getInventory().add(new Potion(0, 0));
        player.takeDamage(player.getMaxHp());

        game.useItem(0);

        assertEquals(1, game.getInventory().size(), "Olu oyuncu esya kullanamaz");
        assertEquals(0, player.getHp());
    }

    /**
     * Yeni kosu kendi yolunun verdigiyle basliyor, dolayisiyla "canta bos"
     * demek artik dogru degil. Sorulmasi gereken sey once kosudan hicbir
     * seyin tasinmadigi.
     */
    @Test
    @DisplayName("Yeniden baslayinca onceki kosudan bir sey kalmiyor")
    void restartClearsInventoryAndPurse() {
        Potion carried = new Potion(0, 0);
        game.getInventory().add(carried);
        game.addGold(30);

        game.setStartPath(StartPath.HAYDUT);
        game.restart();

        assertFalse(game.getInventory().getItems().contains(carried), "Eski iksir gitmeli");
        assertEquals(0, game.getGold(), "Haydutun kesesi bos");
        assertFalse(game.isOver());
    }
}
