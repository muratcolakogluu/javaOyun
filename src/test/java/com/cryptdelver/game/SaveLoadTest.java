package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Enchantment;
import com.cryptdelver.entity.Enemy;
import com.cryptdelver.entity.Player;
import com.cryptdelver.entity.Weapon;
import com.cryptdelver.persistence.SaveData;
import com.cryptdelver.world.BspGenerator;
import com.cryptdelver.world.DungeonGenerator;
import com.cryptdelver.world.RandomWalkGenerator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Kaydetme ve yukleme: durumun tamami geri geliyor mu.
 *
 * <p>Kayit ayri bir oyuna yukleniyor; boylece "zaten ayni nesneler duruyordu"
 * yanilgisina dusmuyoruz.</p>
 */
class SaveLoadTest {

    private static final int WIDTH = 40;
    private static final int HEIGHT = 24;

    private Player player;
    private Game game;

    private static List<DungeonGenerator> generators() {
        return List.of(new BspGenerator(), new RandomWalkGenerator());
    }

    @BeforeEach
    void setUp() {
        player = new Player(0, 0);
        game = new Game(generators(), WIDTH, HEIGHT, player);
    }

    /** Kaydi yeni bir oyuna yukleyip o oyunu dondurur. */
    private Game reload() {
        SaveData data = game.captureSave();
        Game loaded = new Game(generators(), WIDTH, HEIGHT, new Player(0, 0));
        loaded.applySave(data);
        return loaded;
    }

    @Test
    @DisplayName("Harita tohumdan birebir geri gelir")
    void mapIsRebuiltFromTheSeed() {
        Game loaded = reload();

        assertEquals(game.getCurrentSeed(), loaded.getCurrentSeed());
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                assertEquals(game.getDungeon().getTile(x, y), loaded.getDungeon().getTile(x, y),
                        "Kare farkli: (" + x + ", " + y + ")");
            }
        }
    }

    @Test
    @DisplayName("Merdiven ayni karede kalir")
    void stairsLandOnTheSameTile() {
        assertEquals(game.getStairs(), reload().getStairs());
    }

    @Test
    @DisplayName("Derinlik, kese ve sure korunur")
    void progressIsPreserved() {
        game.addGold(137);
        for (int i = 0; i < 60; i++) {
            game.update(1.0 / 60);
        }

        Game loaded = reload();

        assertEquals(game.getDepth(), loaded.getDepth());
        assertEquals(137, loaded.getGold());
        assertEquals(game.getElapsedSeconds(), loaded.getElapsedSeconds(), 1e-6);
    }

    @Test
    @DisplayName("Oyuncunun yeri ve yarali cani korunur")
    void playerStateIsPreserved() {
        player.takeDamage(7);
        int expectedHp = player.getHp();

        Game loaded = reload();

        assertEquals(player.getTileX(), loaded.getPlayer().getTileX());
        assertEquals(player.getTileY(), loaded.getPlayer().getTileY());
        assertEquals(expectedHp, loaded.getPlayer().getHp(), "Yarali can dolu olarak yuklenmemeli");
    }

    @Test
    @DisplayName("Canta, kusanilan silah ve zirh korunur")
    void inventoryAndGearArePreserved() {
        game.getInventory().add(LootTable.weaponForTier(2, 0, 0));
        game.getInventory().add(LootTable.armorForTier(3, 0, 0));
        game.useItem(0);
        game.useItem(1);
        int attack = player.getAttackPower();
        int defense = player.getDefense();

        Game loaded = reload();

        assertEquals(2, loaded.getInventory().size());
        assertNotNull(loaded.getPlayer().getEquippedWeapon());
        assertNotNull(loaded.getPlayer().getEquippedArmor());
        assertEquals(attack, loaded.getPlayer().getAttackPower(), "Vurus gucu korunmali");
        assertEquals(defense, loaded.getPlayer().getDefense(), "Savunma korunmali");
        assertEquals("Çelik Kılıç", loaded.getPlayer().getEquippedWeapon().getName());
    }

    @Test
    @DisplayName("Dusmanlar yerleri ve yarali canlariyla geri gelir")
    void enemiesKeepPositionAndHealth() {
        Enemy wounded = game.getEnemies().get(0);
        wounded.takeDamage(2);
        int expectedHp = wounded.getHp();
        int expectedCount = game.getEnemies().size();

        Game loaded = reload();

        assertEquals(expectedCount, loaded.getEnemies().size(), "Dusman sayisi korunmali");
        Enemy restored = loaded.getEnemies().get(0);
        assertEquals(wounded.getSaveKind(), restored.getSaveKind());
        assertEquals(wounded.getTile(), restored.getTile());
        assertEquals(expectedHp, restored.getHp(), "Yarali dusman dolu canla donmemeli");
        assertEquals(wounded.getMaxHp(), restored.getMaxHp());
    }

    @Test
    @DisplayName("Yerdeki esyalar yerinde kalir")
    void groundItemsStayWhereTheyWere() {
        int expected = game.getGroundItems().size();
        assertTrue(expected > 0, "Katta esya olmali");

        Game loaded = reload();

        assertEquals(expected, loaded.getGroundItems().size());
        assertEquals(game.getGroundItems().get(0).getTile(),
                loaded.getGroundItems().get(0).getTile());
    }

    @Test
    @DisplayName("Derin kattaki guclendirilmis dusmanlar ayni guclerle doner")
    void strengthenedEnemiesKeepTheirBonuses() {
        for (int i = 0; i < 6; i++) {
            player.setTile(game.getStairs());
            if (!game.descend()) {
                // Boss kati: merdiven kilitli, bossu indirip devam et.
                while (game.getBoss() != null) {
                    player.setTile(game.getBoss().getTileX() + 1, game.getBoss().getTileY());
                    game.playerAttacks();
                }
                player.setTile(game.getStairs());
                game.descend();
            }
        }
        assertTrue(game.getDepth() > 5);

        Enemy tough = game.getEnemies().get(0);
        Game loaded = reload();
        Enemy restored = loaded.getEnemies().get(0);

        assertEquals(tough.getMaxHp(), restored.getMaxHp());
        assertEquals(tough.getAttackPower(), restored.getAttackPower());
        assertEquals(tough.getDefense(), restored.getDefense());
    }

    @Test
    @DisplayName("Boss kati yuklenince merdiven yine kilitli gelir")
    void bossFloorReloadsLocked() {
        for (int i = 0; i < 4; i++) {
            player.setTile(game.getStairs());
            game.descend();
        }
        assertTrue(game.isBossFloor());
        assertNotNull(game.getBoss());

        Game loaded = reload();

        assertEquals(5, loaded.getDepth());
        assertNotNull(loaded.getBoss(), "Boss geri gelmeli");
        assertTrue(loaded.isStairsLocked(), "Merdiven yine kilitli olmali");
    }

    /**
     * Yipranma kaydedilmeseydi kaydedip yuklemek bedava tamir olurdu; demirci
     * de anlamsizlasirdi.
     */
    @Test
    @DisplayName("Yipranma ve yukseltme kayitta korunur")
    void gearConditionSurvivesTheSave() {
        Weapon weapon = new Weapon(0, 0, "Test Kilici", 4, "sword", 40);
        weapon.restoreState(17, 2);
        game.getInventory().add(weapon);
        player.equip(weapon);

        Game loaded = reload();
        Weapon restored = loaded.getPlayer().getEquippedWeapon();

        assertNotNull(restored, "Kusanilan silah geri gelmeli");
        assertEquals(17, restored.getDurability(), "Kalan dayaniklilik korunmali");
        assertEquals(2, restored.getUpgradeLevel(), "Yukseltme kademesi korunmali");
        assertEquals(6, restored.getBonus(), "Taban bonus artı yukseltmeler");
    }

    @Test
    @DisplayName("Basili buyu kayitta korunur")
    void enchantmentSurvivesTheSave() {
        Weapon weapon = new Weapon(0, 0, "Test Kilici", 4, "sword", 40);
        weapon.enchant(Enchantment.VAMPIRLIK);
        game.getInventory().add(weapon);
        player.equip(weapon);

        Weapon restored = reload().getPlayer().getEquippedWeapon();

        assertEquals(Enchantment.VAMPIRLIK, restored.getEnchantment());
    }

    @Test
    @DisplayName("Kirik parca kirik olarak geri gelir")
    void brokenGearStaysBroken() {
        Weapon weapon = new Weapon(0, 0, "Test Kilici", 4, "sword", 40);
        weapon.restoreState(0, 0);
        game.getInventory().add(weapon);
        player.equip(weapon);

        Weapon restored = reload().getPlayer().getEquippedWeapon();

        assertTrue(restored.isBroken());
    }

    @Test
    @DisplayName("Uretici secimi korunur")
    void generatorChoiceIsPreserved() {
        game.cycleGenerator();
        String expected = game.getCurrentGenerator().getName();

        Game loaded = reload();

        assertEquals(expected, loaded.getCurrentGenerator().getName());
    }
}
