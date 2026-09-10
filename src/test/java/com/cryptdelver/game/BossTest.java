package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Boss;
import com.cryptdelver.entity.Enemy;
import com.cryptdelver.entity.Item;
import com.cryptdelver.entity.Player;
import com.cryptdelver.entity.Skeleton;
import com.cryptdelver.entity.Weapon;
import com.cryptdelver.world.BspGenerator;
import com.cryptdelver.world.Dungeon;
import com.cryptdelver.world.Tile;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Boss katlari: merdiveni tutan dusman, cagirdigi yaratiklar ve ganimeti.
 */
class BossTest {

    private static final double FRAME = 1.0 / 60;
    private static final int WIDTH = 40;
    private static final int HEIGHT = 24;

    @Nested
    @DisplayName("Kat akisi")
    class FloorFlow {

        private final Player player = new Player(0, 0);
        private final Game game = new Game(List.of(new BspGenerator()), WIDTH, HEIGHT, player);

        /** Oyuncuyu merdivene tasiyip indirir. */
        private void goDownOneFloor() {
            player.setTile(game.getStairs());
            assertTrue(game.descend(), "Inis calismali");
        }

        /** Bosun canini bitene kadar vurur; oyuncu yanina konumlandirilir. */
        private void killBoss() {
            Boss boss = game.getBoss();
            player.setTile(boss.getTileX() + 1, boss.getTileY());

            for (int i = 0; i < 200 && boss.isAlive(); i++) {
                game.playerAttacks();
            }
            assertFalse(boss.isAlive(), "Boss olmeliydi");
        }

        @Test
        @DisplayName("Ilk katlarda boss yok")
        void earlyFloorsHaveNoBoss() {
            assertFalse(game.isBossFloor());
            assertNull(game.getBoss());
            assertFalse(game.isStairsLocked());
        }

        @Test
        @DisplayName("Bes katta bir boss merdiveni tutar")
        void bossGuardsTheStairsEveryFifthFloor() {
            for (int i = 0; i < 4; i++) {
                goDownOneFloor();
            }

            assertEquals(5, game.getDepth());
            assertTrue(game.isBossFloor());

            Boss boss = game.getBoss();
            assertNotNull(boss, "5. katta boss olmali");
            assertEquals(game.getStairs(), boss.getTile(), "Boss merdivenin ustunde durmali");
            assertTrue(game.isStairsLocked());
        }

        /**
         * Can tavani oyuncunun tek kalici buyumesi ve artik tek kaynagi bu.
         * Eskiden her inisle geliyordu; o zaman dovusmeden kacan oyuncu da
         * gucleniyordu.
         */
        @Test
        @DisplayName("Bossu gecmek azami cani buyutur")
        void killingTheBossRaisesMaxHealth() {
            for (int i = 0; i < 4; i++) {
                goDownOneFloor();
            }
            int maxBefore = player.getMaxHp();
            int hpBefore = player.getHp();

            killBoss();

            assertTrue(player.getMaxHp() > maxBefore, "Tavan yukselmeli");
            assertEquals(hpBefore, player.getHp(), "Mevcut cani doldurmamali");
        }

        /** Kazanilan can mezara gidiyor: olmek bedelsiz olmamali. */
        @Test
        @DisplayName("Olunce bossdan kazanilan can gider")
        void deathTakesBackTheBossHealth() {
            for (int i = 0; i < 4; i++) {
                goDownOneFloor();
            }
            int startingMax = new Player(0, 0).getMaxHp();
            killBoss();
            assertTrue(player.getMaxHp() > startingMax, "Once kazanmis olmali");

            player.takeDamage(player.getMaxHp());
            assertTrue(game.isOver());
            game.restart();

            assertEquals(startingMax, player.getMaxHp(), "Yeni oyun taban canla baslamali");
        }

        @Test
        @DisplayName("Boss yasarken inilemez, oldukten sonra inilir")
        void stairsUnlockWhenTheBossDies() {
            for (int i = 0; i < 4; i++) {
                goDownOneFloor();
            }

            player.setTile(game.getStairs());
            assertFalse(game.descend(), "Boss yasarken inilmemeli");
            assertEquals(5, game.getDepth());

            killBoss();

            assertNull(game.getBoss());
            assertFalse(game.isStairsLocked());
            player.setTile(game.getStairs());
            assertTrue(game.descend(), "Boss oldukten sonra inilebilmeli");
            assertEquals(6, game.getDepth());
        }

        @Test
        @DisplayName("Boss oldugunde altin ve silah birakir")
        void bossDropsLoot() {
            for (int i = 0; i < 4; i++) {
                goDownOneFloor();
            }
            int itemsBefore = game.getGroundItems().size();

            killBoss();

            List<Item> dropped = game.getGroundItems();
            assertTrue(dropped.size() >= itemsBefore + 2, "En az iki parca ganimet dusmeli");
            assertTrue(dropped.stream().anyMatch(item -> item instanceof Weapon),
                    "Ganimette silah olmali");
        }
    }

    @Nested
    @DisplayName("Yetenek")
    class Ability {

        @Test
        @DisplayName("Mahzen Bekcisi zamanla yaratik cagirir")
        void bossSummonsMinionsOverTime() {
            Dungeon dungeon = new Dungeon(20, 12);
            dungeon.fill(Tile.FLOOR);
            for (int i = 0; i < 20; i++) {
                dungeon.setTile(i, 0, Tile.WALL);
                dungeon.setTile(i, 11, Tile.WALL);
            }
            for (int i = 0; i < 12; i++) {
                dungeon.setTile(0, i, Tile.WALL);
                dungeon.setTile(19, i, Tile.WALL);
            }

            Player player = new Player(2, 2);
            Game game = new Game(dungeon, player);
            // Cagirma birinci bossun yetenegi; digerlerinin baska yetenegi var.
            game.addEnemy(Boss.forNumber(16, 9, 1));

            for (int i = 0; i < 60 * 8; i++) {
                game.update(FRAME);
            }

            assertTrue(game.getEnemies().size() > 1,
                    "Boss etrafina yaratik doldurmali, sayilan: " + game.getEnemies().size());
            assertTrue(game.getEnemies().stream().anyMatch(enemy -> enemy.getName().equals("İmp")),
                    "Cagrilan yaratiklar imp olmali");
        }

        /**
         * Kesin sayilar yerine oran araniyor: denge ayarlari (bossun vurusu
         * 6'dan 5'e indi gibi) testi patlatmasin, ama boss her zaman siradan
         * dusmandan belirgin sekilde sert kalsin.
         */
        @Test
        @DisplayName("Boss siradan dusmandan cok daha dayanikli")
        void bossIsTougherThanRegularEnemies() {
            Enemy boss = Boss.forNumber(3, 3, 1);
            Enemy skeleton = new Skeleton(3, 3);

            assertTrue(boss.getMaxHp() > skeleton.getMaxHp() * 3,
                    "Boss cani iskeletin en az uc kati olmali");
            assertTrue(boss.getAttackPower() > skeleton.getAttackPower(),
                    "Boss daha sert vurmali");
            assertTrue(boss.getDefense() > skeleton.getDefense(),
                    "Boss daha zirhli olmali");
        }
    }
}
