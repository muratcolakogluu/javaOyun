package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Armor;
import com.cryptdelver.entity.Player;
import com.cryptdelver.entity.Imp;
import com.cryptdelver.entity.Skeleton;
import com.cryptdelver.entity.Weapon;
import com.cryptdelver.world.Dungeon;
import com.cryptdelver.world.Tile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Zirh ve savunmanin savasa etkisi.
 *
 * <p>Hasar rastgele oldugu icin testler bunu hesaba katiyor: kalin zirhla
 * gelen hasar her zaman alt sinira (1) dusuyor, o yuzden orada kesin sayi
 * beklenebiliyor.</p>
 */
class ArmorTest {

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

    @Test
    @DisplayName("Zirhsiz oyuncunun savunmasi sifir")
    void bareHandedDefenseIsZero() {
        assertEquals(0, player.getDefense());
        assertNull(player.getEquippedArmor());
    }

    @Test
    @DisplayName("Zirh kusanilinca savunma artar ve cantada kalir")
    void equippingArmorRaisesDefense() {
        Armor armor = LootTable.armorForTier(2, 0, 0);
        game.getInventory().add(armor);

        game.useItem(0);

        assertSame(armor, player.getEquippedArmor());
        assertEquals(armor.getDefenseBonus(), player.getDefense());
        assertEquals(1, game.getInventory().size(), "Zirh tukenmemeli");
    }

    @Test
    @DisplayName("Silah ve zirh ayri slotlarda, ikisi birden kusanilir")
    void weaponAndArmorAreSeparateSlots() {
        Weapon weapon = LootTable.weaponForTier(1, 0, 0);
        Armor armor = LootTable.armorForTier(1, 0, 0);
        game.getInventory().add(weapon);
        game.getInventory().add(armor);

        game.useItem(0);
        game.useItem(1);

        assertSame(weapon, player.getEquippedWeapon());
        assertSame(armor, player.getEquippedArmor());
    }

    @Test
    @DisplayName("Kalin zirh gelen hasari alt sinira indirir")
    void heavyArmorFloorsIncomingDamage() {
        // Kript Plakasi +6; iskeletin vurusu 3-5, yani hasar hep 1'e duser.
        game.getInventory().add(LootTable.armorForTier(LootTable.MAX_TIER, 0, 0));
        game.useItem(0);
        game.addEnemy(new Skeleton(5, 4));

        int hpBefore = player.getHp();
        for (int i = 0; i < 60 * 3; i++) {
            game.update(FRAME);
        }
        int totalDamage = hpBefore - player.getHp();

        assertTrue(totalDamage > 0, "Iskelet vurmali");
        assertTrue(totalDamage <= 3, "Her vurus 1 hasara dusmeli, olculen: " + totalDamage);
    }

    @Test
    @DisplayName("Zirh vurusu tamamen engelleyemez")
    void armorNeverBlocksAllDamage() {
        game.getInventory().add(new Armor(0, 0, "Test Zirhi", 999, "armor_plate"));
        game.useItem(0);
        game.addEnemy(new Imp(5, 4));

        int hpBefore = player.getHp();
        for (int i = 0; i < 60 * 2; i++) {
            game.update(FRAME);
        }

        assertTrue(player.getHp() < hpBefore, "En az 1 hasar hep gecmeli");
    }

    @Test
    @DisplayName("Dusman savunmasi alinan hasari azaltir")
    void enemyDefenseReducesDamageTaken() {
        Skeleton armored = new Skeleton(5, 4);
        armored.strengthen(0, 0, 20);
        game.addEnemy(armored);

        int hpBefore = armored.getHp();
        game.playerAttacks();

        assertEquals(hpBefore - 1, armored.getHp(), "Kalin savunmada vurus 1 hasara dusmeli");
    }

    @Test
    @DisplayName("Yeniden baslayinca zirh cikar")
    void restartRemovesArmor() {
        game.getInventory().add(LootTable.armorForTier(1, 0, 0));
        game.useItem(0);

        game.restart();

        assertNull(player.getEquippedArmor());
        assertEquals(0, player.getDefense());
    }

    /**
     * Kusanilan zirh karakterin gorunumunu degistirmeli: ciplak govde, hafif
     * zirhli ve agir zirhli olmak uzere uc ayri sprite.
     */
    @Test
    @DisplayName("Karakterin gorunumu kusanilan zirha gore degisir")
    void spriteReflectsEquippedArmor() {
        assertEquals("player", player.getSpriteName(), "Zirhsizken ciplak govde");

        game.getInventory().add(LootTable.armorForTier(1, 0, 0));
        game.useItem(0);
        assertEquals("player_light", player.getSpriteName(), "Deri zirhla hafif gorunum");

        game.getInventory().add(LootTable.armorForTier(LootTable.MAX_TIER, 0, 0));
        game.useItem(1);
        assertEquals("player_heavy", player.getSpriteName(), "Kript plakasiyla agir gorunum");
    }
}
