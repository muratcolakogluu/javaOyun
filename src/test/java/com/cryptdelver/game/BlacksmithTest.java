package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Armor;
import com.cryptdelver.entity.Blacksmith;
import com.cryptdelver.entity.Enchantment;
import com.cryptdelver.entity.Player;
import com.cryptdelver.entity.Weapon;
import com.cryptdelver.world.BspGenerator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Demirci: altinin harcandigi tek yer.
 *
 * <p>Iki soru ayri ayri kovalaniyor. <b>Yerlesim</b>: demirci yalnizca boss
 * katlarinda ve ulasilabilir bir yerde mi. <b>Tezgah</b>: tamir ve yukseltme
 * altini dogru dusuyor mu, tavan tutuyor mu.</p>
 */
class BlacksmithTest {

    private static final int WIDTH = 40;
    private static final int HEIGHT = 22;

    /** Bos katlarina inip demirciyi bulmak. */
    @Nested
    class Placement {

        private final Player player = new Player(0, 0);
        private final Game game = new Game(List.of(new BspGenerator()), WIDTH, HEIGHT, player);

        /** Bir kat asagi iner; 5. kata kadar merdiveni tutan bir sey yok. */
        private void goDownOneFloor() {
            player.setTile(game.getStairs());
            assertTrue(game.descend(), "Inis calismali");
        }

        @Test
        @DisplayName("Siradan katlarda demirci yok")
        void ordinaryFloorsHaveNoBlacksmith() {
            assertFalse(game.isBossFloor());
            assertNull(game.getBlacksmith());
        }

        @Test
        @DisplayName("Boss katinda demirci var")
        void bossFloorsHaveABlacksmith() {
            for (int i = 0; i < 4; i++) {
                goDownOneFloor();
            }

            assertTrue(game.isBossFloor(), "5. kat boss kati");
            assertNotNull(game.getBlacksmith(), "Boss katinda demirci olmali");
        }

        /**
         * Demirci merdivenin uzerinde olsaydi bossun dibinde durur, tezgaha
         * ancak dovusun ortasinda gidilebilirdi.
         */
        @Test
        @DisplayName("Demirci merdivenin ve dogulan yerin uzerinde degil")
        void blacksmithKeepsItsOwnTile() {
            for (int i = 0; i < 4; i++) {
                goDownOneFloor();
            }

            Blacksmith smith = game.getBlacksmith();
            assertFalse(smith.getTile().equals(game.getStairs()), "Merdiveni kapatmamali");
            assertTrue(game.getDungeon().isWalkable(smith.getTileX(), smith.getTileY()),
                    "Duvarin icinde olmamali");
        }

        @Test
        @DisplayName("Demircinin karesinden gecilemez")
        void blacksmithBlocksItsTile() {
            for (int i = 0; i < 4; i++) {
                goDownOneFloor();
            }

            Blacksmith smith = game.getBlacksmith();
            assertFalse(game.isTileFree(smith.getTileX(), smith.getTileY(), player));
        }

        /** Ayni tohum ayni kati verdigi icin demirci de ayni yere dusmeli. */
        @Test
        @DisplayName("Kayit yuklenince demirci yerinde duruyor")
        void blacksmithSurvivesSaveAndLoad() {
            for (int i = 0; i < 4; i++) {
                goDownOneFloor();
            }
            var before = game.getBlacksmith().getTile();

            game.applySave(game.captureSave());

            assertNotNull(game.getBlacksmith());
            assertEquals(before, game.getBlacksmith().getTile());
        }
    }

    /** Tezgahin kendisi: fiyatlar, kese ve tavan. */
    @Nested
    class Counter {

        private final Player player = new Player(0, 0);
        private final Game game = new Game(List.of(new BspGenerator()), WIDTH, HEIGHT, player);

        /**
         * Oyuncuyu demircinin yanina tasiyip tezgahi acar.
         *
         * <p>Bes kat inmek yerine katin kendi demircisini kullaniyoruz:
         * yerlesimi zaten {@link Placement} sinaviyor, burada tezgah lazim.</p>
         */
        private void openForgeAt(int depth) {
            while (game.getDepth() < depth) {
                player.setTile(game.getStairs());
                assertTrue(game.descend());
            }

            Blacksmith smith = game.getBlacksmith();
            assertNotNull(smith, "Bu kat boss kati olmali");
            player.setTile(smith.getTileX() + 1, smith.getTileY());
            game.toggleForge();
            assertTrue(game.isForgeOpen());
        }

        private Weapon wornSword() {
            Weapon weapon = new Weapon(0, 0, "Test Kilici", 4, "sword", 20);
            weapon.restoreState(10, 0);
            player.equip(weapon);
            return weapon;
        }

        @Test
        @DisplayName("Tezgah yalnizca demircinin yaninda acilir")
        void forgeNeedsTheBlacksmith() {
            game.toggleForge();

            assertFalse(game.isForgeOpen(), "Ortada demirci yokken acilmamali");
        }

        @Test
        @DisplayName("Tezgah acikken dunya durur")
        void forgeFreezesTheWorld() {
            openForgeAt(5);

            assertTrue(game.isFrozen());

            double before = game.getElapsedSeconds();
            game.update(1.0 / 60);

            assertEquals(before, game.getElapsedSeconds(), 1e-9, "Sure bile ilerlememeli");
        }

        @Test
        @DisplayName("Tamir altin dusurur ve parcayi doldurur")
        void repairingCostsGoldAndRefills() {
            openForgeAt(5);
            Weapon weapon = wornSword();
            int cost = Forge.repairCost(weapon);
            game.addGold(cost);
            int before = game.getGold();

            game.repairWeapon();

            assertEquals(20, weapon.getDurability());
            assertEquals(before - cost, game.getGold());
        }

        @Test
        @DisplayName("Altin yetmezse hicbir sey olmaz")
        void repairNeedsEnoughGold() {
            openForgeAt(5);
            Weapon weapon = wornSword();

            game.repairWeapon();

            assertEquals(10, weapon.getDurability(), "Bedava tamir yok");
        }

        @Test
        @DisplayName("Saglam parca icin tamir istenmiyor")
        void healthyGearIsNotRepaired() {
            openForgeAt(5);
            Weapon weapon = new Weapon(0, 0, "Test Kilici", 4, "sword", 20);
            player.equip(weapon);
            game.addGold(500);
            int before = game.getGold();

            game.repairWeapon();

            assertEquals(before, game.getGold(), "Bos yere altin gitmemeli");
        }

        @Test
        @DisplayName("Yukseltme bonusu artirir ve parcayi yeniler")
        void upgradingRaisesTheBonus() {
            openForgeAt(5);
            Weapon weapon = wornSword();
            game.addGold(1000);

            game.upgradeWeapon();

            assertEquals(1, weapon.getUpgradeLevel());
            assertEquals(5, weapon.getBonus());
            assertEquals(20, weapon.getDurability(), "Dovulen parca yenilenmeli");
        }

        /**
         * Oyunun can alici kurali: altin biriktirerek bossu atlamak yok.
         * Yukseltme tavani, o katta bossun birakacagi parcanin seviyesi.
         */
        @Test
        @DisplayName("Yukseltme bossun verecegi parcayi gecemez")
        void upgradesStopAtTheBossReward() {
            openForgeAt(5);
            Weapon weapon = new Weapon(0, 0, "Test Kilici", 2, "sword", 20);
            player.equip(weapon);
            game.addGold(100000);

            for (int i = 0; i < 20; i++) {
                game.upgradeWeapon();
            }

            int ceiling = LootTable.weaponBonusForTier(LootTable.bossTierForDepth(game.getDepth()));
            assertEquals(ceiling, weapon.getBonus(), "Tam tavanda durmali");
            assertFalse(weapon.canUpgrade(game.getDepth()), "Daha ileri gitmemeli");
        }

        @Test
        @DisplayName("Tavana dayanan parca icin altin alinmaz")
        void cappedUpgradesAreFree() {
            openForgeAt(5);
            Weapon weapon = new Weapon(0, 0, "Test Kilici",
                    LootTable.weaponBonusForTier(LootTable.MAX_TIER), "sword", 20);
            player.equip(weapon);
            game.addGold(1000);
            int before = game.getGold();

            game.upgradeWeapon();

            assertEquals(before, game.getGold());
            assertEquals(0, weapon.getUpgradeLevel());
        }

        @Test
        @DisplayName("Zirh da ayni tezgahtan geciyor")
        void armorUsesTheSameCounter() {
            openForgeAt(5);
            Armor armor = new Armor(0, 0, "Test Zirhi", 1, "armor_leather", 20);
            player.equip(armor);
            game.addGold(1000);

            game.upgradeArmor();

            assertEquals(2, armor.getBonus());
            assertTrue(player.getDefense() >= 2, "Yukseltme savunmaya yansimali");
        }

        @Test
        @DisplayName("Kusanilmamis parca icin islem yapilmaz")
        void nothingHappensWithoutGear() {
            openForgeAt(5);
            game.addGold(1000);
            int before = game.getGold();

            game.upgradeArmor();
            game.repairArmor();

            assertEquals(before, game.getGold());
        }

        @Test
        @DisplayName("Buyu basmak altin dusurur")
        void enchantingCostsGold() {
            openForgeAt(5);
            Weapon weapon = wornSword();
            game.addGold(Enchantment.VAMPIRLIK.getCost());

            game.enchantWeapon(Enchantment.VAMPIRLIK);

            assertEquals(Enchantment.VAMPIRLIK, weapon.getEnchantment());
            assertEquals(0, game.getGold(), "Butun altin gitmis olmali");
        }

        @Test
        @DisplayName("Altin yetmezse buyu basilmaz")
        void enchantingNeedsEnoughGold() {
            openForgeAt(5);
            Weapon weapon = wornSword();

            game.enchantWeapon(Enchantment.VAMPIRLIK);

            assertNull(weapon.getEnchantment(), "Bedava buyu yok");
        }

        /** "1 seye 1 tane buyu": yenisi eskisinin yerine geciyor, ustune degil. */
        @Test
        @DisplayName("Ikinci buyu birincinin yerine gecer")
        void thesecondEnchantmentReplacesTheFirst() {
            openForgeAt(5);
            Weapon weapon = wornSword();
            game.addGold(1000);

            game.enchantWeapon(Enchantment.VAMPIRLIK);
            game.enchantWeapon(Enchantment.SAGLAMLIK);

            assertEquals(Enchantment.SAGLAMLIK, weapon.getEnchantment());
        }

        @Test
        @DisplayName("Ayni buyu ikinci kez basilmaz")
        void repeatingTheSameEnchantmentIsFree() {
            openForgeAt(5);
            Weapon weapon = wornSword();
            game.addGold(1000);
            game.enchantWeapon(Enchantment.VAMPIRLIK);
            int after = game.getGold();

            game.enchantWeapon(Enchantment.VAMPIRLIK);

            assertEquals(after, game.getGold(), "Ayni buyu icin ikinci kez odeme alinmamali");
        }

        @Test
        @DisplayName("Silaha zirh buyusu basilmaz")
        void gearRejectsTheWrongEnchantment() {
            openForgeAt(5);
            Weapon weapon = wornSword();
            game.addGold(1000);
            int before = game.getGold();

            game.enchantWeapon(Enchantment.DIKEN);

            assertNull(weapon.getEnchantment());
            assertEquals(before, game.getGold());
        }

        @Test
        @DisplayName("ESC tezgahi kapatir, oyunu duraklatmaz")
        void escapeLeavesTheCounter() {
            openForgeAt(5);

            game.togglePause();

            assertFalse(game.isForgeOpen(), "Tezgah kapanmali");
            assertFalse(game.isPaused(), "Ustune bir de duraklamamali");
        }
    }
}
