package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Armor;
import com.cryptdelver.entity.Enchantment;
import com.cryptdelver.entity.Player;
import com.cryptdelver.entity.Skeleton;
import com.cryptdelver.entity.Weapon;
import com.cryptdelver.world.Dungeon;
import com.cryptdelver.world.Tile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Buyuler: parca basina bir yuva ve sayiya dokunmayan etkiler.
 *
 * <p>Buradaki kural sunun sinaviyor: buyuler vurus ya da savunmaya sayi
 * eklemiyor, cunku eklerse "buyulu parca bossunkinden iyi olamaz" kurali
 * bozulurdu. Onun yerine baska bir eksende calisiyorlar.</p>
 */
class EnchantmentTest {

    private Player player;
    private Game game;

    @BeforeEach
    void setUp() {
        Dungeon dungeon = new Dungeon(11, 9);
        dungeon.fill(Tile.FLOOR);
        for (int i = 0; i < 11; i++) {
            dungeon.setTile(i, 0, Tile.WALL);
            dungeon.setTile(i, 8, Tile.WALL);
        }
        for (int i = 0; i < 9; i++) {
            dungeon.setTile(0, i, Tile.WALL);
            dungeon.setTile(10, i, Tile.WALL);
        }

        player = new Player(4, 4);
        game = new Game(dungeon, player);
    }

    private Weapon sword(int durability) {
        return new Weapon(0, 0, "Test Kilici", 9, "sword", durability);
    }

    private Armor mail(int durability) {
        return new Armor(0, 0, "Test Zirhi", 2, "armor_chain", durability);
    }

    @Test
    @DisplayName("Bir parcada bir buyu durur; yenisi eskisinin yerine gecer")
    void oneSlotPerItem() {
        Weapon weapon = sword(20);

        weapon.enchant(Enchantment.VAMPIRLIK);
        weapon.enchant(Enchantment.SAGLAMLIK);

        assertEquals(Enchantment.SAGLAMLIK, weapon.getEnchantment(), "Son basilan kalmali");
    }

    @Test
    @DisplayName("Silah ve zirh farkli buyuler kabul ediyor")
    void gearAcceptsItsOwnEnchantments() {
        Weapon weapon = sword(20);
        Armor armor = mail(20);

        assertTrue(weapon.accepts(Enchantment.VAMPIRLIK), "Vampirlik silaha ozel");
        assertFalse(weapon.accepts(Enchantment.DIKEN), "Diken silaha basilmaz");
        assertTrue(armor.accepts(Enchantment.DIKEN), "Diken zirha ozel");
        assertFalse(armor.accepts(Enchantment.VAMPIRLIK), "Vampirlik zirha basilmaz");
        assertTrue(weapon.accepts(Enchantment.SAGLAMLIK) && armor.accepts(Enchantment.SAGLAMLIK),
                "Saglamlik ikisinde de var");
    }

    /**
     * Buyunun sayiya dokunmamasi tavan kuralinin dayanagi: dokunsaydi buyulu
     * parca bossun birakacagini gecebilirdi.
     */
    @Test
    @DisplayName("Buyu vurus gucunu degistirmiyor")
    void enchantingDoesNotChangeRawPower() {
        Weapon weapon = sword(20);
        player.equip(weapon);
        int before = player.getAttackPower();

        weapon.enchant(Enchantment.VAMPIRLIK);

        assertEquals(before, player.getAttackPower());
        assertEquals(9, weapon.getBonus());
    }

    @Test
    @DisplayName("Vampirlik oldurulen dusman basina can veriyor")
    void vampirismHealsOnKill() {
        Weapon weapon = sword(20);
        weapon.enchant(Enchantment.VAMPIRLIK);
        player.equip(weapon);
        player.takeDamage(10);
        int wounded = player.getHp();
        game.addEnemy(new Skeleton(5, 4));

        for (int i = 0; i < 20 && !game.getEnemies().isEmpty(); i++) {
            game.playerAttacks();
        }

        assertTrue(game.getEnemies().isEmpty(), "Dusman olmeliydi");
        assertTrue(player.getHp() > wounded, "Oldurmek can emmeli");
    }

    @Test
    @DisplayName("Vampirliksiz kilic can emmiyor")
    void plainWeaponDoesNotHeal() {
        Weapon weapon = sword(20);
        player.equip(weapon);
        player.takeDamage(10);
        int wounded = player.getHp();
        game.addEnemy(new Skeleton(5, 4));

        for (int i = 0; i < 20 && !game.getEnemies().isEmpty(); i++) {
            game.playerAttacks();
        }

        assertEquals(wounded, player.getHp());
    }

    @Test
    @DisplayName("Diken sana vurana hasar yansitiyor")
    void thornsHurtTheAttacker() {
        Armor armor = mail(20);
        armor.enchant(Enchantment.DIKEN);
        player.equip(armor);
        Skeleton skeleton = new Skeleton(5, 4);
        game.addEnemy(skeleton);
        int before = skeleton.getHp();

        game.enemyAttacksPlayer(skeleton);

        assertTrue(skeleton.getHp() < before, "Vuran hasar almali");
    }

    @Test
    @DisplayName("Dikensiz zirh hicbir sey yansitmiyor")
    void plainArmorReflectsNothing() {
        Armor armor = mail(20);
        player.equip(armor);
        Skeleton skeleton = new Skeleton(5, 4);
        game.addEnemy(skeleton);
        int before = skeleton.getHp();

        game.enemyAttacksPlayer(skeleton);

        assertEquals(before, skeleton.getHp());
    }

    /** Yansiyan hasar oldurebiliyor; olen dusman listeden dusmeli. */
    @Test
    @DisplayName("Diken oldurdugu dusmani listeden dusuruyor")
    void thornsCanFinishAnEnemy() {
        Armor armor = mail(20);
        armor.enchant(Enchantment.DIKEN);
        player.equip(armor);
        Skeleton skeleton = new Skeleton(5, 4);
        skeleton.takeDamage(skeleton.getHp() - 1);
        game.addEnemy(skeleton);

        game.enemyAttacksPlayer(skeleton);

        assertFalse(skeleton.isAlive());
        assertTrue(game.getEnemies().isEmpty(), "Olen dusman listede kalmamali");
    }

    @Test
    @DisplayName("Saglamlik yipranmayi yariya indiriyor")
    void toughnessHalvesTheWear() {
        Weapon plain = sword(20);
        Weapon tough = sword(20);
        tough.enchant(Enchantment.SAGLAMLIK);

        for (int i = 0; i < 10; i++) {
            plain.wear();
            tough.wear();
        }

        assertEquals(10, plain.getDurability(), "Buyusuz parca her kullanimda yipraniyor");
        assertEquals(15, tough.getDurability(), "Saglamlik yarisini yutmali");
    }

    @Test
    @DisplayName("Parca varsayilan olarak buyusuz ve kayitta bos etiket yaziyor")
    void gearStartsWithoutAnEnchantment() {
        Weapon plain = sword(20);

        assertNull(plain.getEnchantment());
        assertEquals("", plain.getSaveEnchantment());

        plain.enchant(Enchantment.VAMPIRLIK);

        assertEquals("VAMPIRLIK", plain.getSaveEnchantment());
    }

    @Test
    @DisplayName("Buyulu parcanin adi buyuyu de gosteriyor")
    void nameShowsTheEnchantment() {
        Weapon weapon = sword(20);
        weapon.enchant(Enchantment.VAMPIRLIK);

        assertTrue(weapon.getFullName().contains(Enchantment.VAMPIRLIK.getLabel()));
    }
}
