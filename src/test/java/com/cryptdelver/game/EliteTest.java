package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.EliteTrait;
import com.cryptdelver.entity.Enemy;
import com.cryptdelver.entity.Imp;
import com.cryptdelver.entity.Item;
import com.cryptdelver.entity.Player;
import com.cryptdelver.entity.Skeleton;
import com.cryptdelver.world.BspGenerator;
import com.cryptdelver.world.Dungeon;
import com.cryptdelver.world.Position;
import com.cryptdelver.world.Tile;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Elit dusmanlar: kalabaligin icindeki "bu farkli" ani.
 *
 * <p>Bir kat, baska bir katin aynisiydi -- yalnizca sayilari buyuktu. Yirmi
 * imp, yirmi impti. Elit, katin ortasinda durup bakmani saglayan sey.</p>
 *
 * <p>Sinavlar iki soruyu kovaliyor: ozellik gercekten <b>bir sey degistiriyor
 * mu</b> (yoksa yalnizca isim mi) ve elit <b>seyrek</b> mi kaliyor -- ozel
 * olmasi icin seyrek olmasi gerekiyor.</p>
 */
class EliteTest {

    /** Ozelligin kendisi: sayilara ne yapiyor. */
    @Nested
    class Traits {

        @Test
        @DisplayName("Zirhli daha sert kabuklu ama daha yavas")
        void theArmouredTradesSpeedForShell() {
            Imp plain = new Imp(0, 0);
            Imp elite = new Imp(0, 0);
            elite.makeElite(EliteTrait.ZIRHLI);

            assertTrue(elite.getDefense() > plain.getDefense(), "Kabuk kalinlasmali");
            assertTrue(elite.getSpeed() < plain.getSpeed(), "Adimi agirlasmali");
        }

        @Test
        @DisplayName("Cevik daha hizli ve daha sik vuruyor")
        void theSwiftIsQuickerInBothWays() {
            Imp plain = new Imp(0, 0);
            Imp elite = new Imp(0, 0);
            elite.makeElite(EliteTrait.CEVIK);

            assertTrue(elite.getSpeed() > plain.getSpeed());
            assertTrue(elite.getAttackCooldown() < plain.getAttackCooldown(),
                    "Vuruslari sikllasmali");
        }

        /** Kanlinin tehdidi bitmemesinden geliyor, hizindan degil. */
        @Test
        @DisplayName("Kanli cok daha fazla can tasiyor")
        void theBloodyIsAWall() {
            Imp plain = new Imp(0, 0);
            Imp elite = new Imp(0, 0);
            elite.makeElite(EliteTrait.KANLI);

            assertTrue(elite.getMaxHp() > plain.getMaxHp() * 2, "Duvar gibi olmali");
            assertEquals(plain.getSpeed(), elite.getSpeed(), "Hizi siradan kalmali");
        }

        /** Uc ozellik uc ayri cevap istiyor; ikisi ayni sey yapsaydi biri gereksizdi. */
        @Test
        @DisplayName("Uc ozellik birbirinden farkli")
        void thethreeTraitsDiffer() {
            Set<String> fingerprints = new HashSet<>();

            for (EliteTrait trait : EliteTrait.values()) {
                Imp elite = new Imp(0, 0);
                elite.makeElite(trait);
                fingerprints.add(elite.getMaxHp() + "|" + elite.getDefense() + "|"
                        + elite.getSpeed() + "|" + elite.getAttackCooldown());
            }

            assertEquals(EliteTrait.values().length, fingerprints.size());
        }

        @Test
        @DisplayName("Elitin adinda sifati var")
        void theEliteWearsItsName() {
            Skeleton plain = new Skeleton(0, 0);
            Skeleton elite = new Skeleton(0, 0);
            elite.makeElite(EliteTrait.KANLI);

            assertNotEquals(plain.getName(), elite.getName());
            assertTrue(elite.getName().endsWith(plain.getName()),
                    "Sifat turun adinin onune gelmeli: " + elite.getName());
        }

        @Test
        @DisplayName("Siradan dusman elit degil")
        void theOrdinaryStayOrdinary() {
            assertFalse(new Imp(0, 0).isElite());
        }
    }

    /** Kat uzerinde: elit dusmanin katta ne yaptigi. */
    @Nested
    class OnTheFloor {

        private final Player player = new Player(0, 0);
        private final Game game = new Game(List.of(new BspGenerator()), 40, 22, player);

        /**
         * Elit bir ceza degil bir firsat olmali: kacmak da bir cevap ama
         * kalip devirmenin bir karsiligi var.
         */
        @Test
        @DisplayName("Elit olunce kese birakiyor")
        void theEliteDropsItsPurse() {
            Dungeon dungeon = new Dungeon(11, 9);
            dungeon.fill(Tile.FLOOR);
            Player hero = new Player(4, 4);
            Game arena = new Game(dungeon, hero);

            Imp elite = new Imp(5, 4);
            arena.addEnemy(elite);
            elite.makeElite(EliteTrait.KANLI);

            for (int i = 0; i < 400 && elite.isAlive(); i++) {
                arena.playerAttacks();
            }
            assertFalse(elite.isAlive());

            int gold = 0;
            for (Item item : arena.getGroundItems()) {
                gold += item.getKind().equals("GOLD") ? 1 : 0;
            }
            assertTrue(gold > 0, "Elitin kesesi yerde olmali");
        }

        /**
         * Ilk iki kat oyunun temel dovusunu ogretiyor ve elit tam o dersin
         * istisnasi; istisnayi kural ogrenilmeden gostermek ogretmiyor,
         * sasirtiyor.
         */
        @Test
        @DisplayName("Ilk iki katta elit yok")
        void theFirstFloorsStayClean() {
            FloorBuilder builder = new FloorBuilder(List.of(new BspGenerator()), 40, 22);

            for (int attempt = 0; attempt < 300; attempt++) {
                Enemy enemy = builder.createEnemyForDepth(new Position(3, 3), 1, Difficulty.NORMAL);
                assertFalse(enemy.isElite(), "1. katta elit cikmamali");
            }
        }

        /**
         * Bir seyin ozel olmasi icin seyrek kalmasi gerekiyor: kalabaligin
         * yarisi elit olsaydi elit "bu farkli" demeyi birakirdi.
         */
        @Test
        @DisplayName("Elit seyrek ama var")
        void elitesAreRareButReal() {
            FloorBuilder builder = new FloorBuilder(List.of(new BspGenerator()), 40, 22);
            int elites = 0;
            int rolls = 600;

            for (int attempt = 0; attempt < rolls; attempt++) {
                if (builder.createEnemyForDepth(new Position(3, 3), 8, Difficulty.NORMAL)
                        .isElite()) {
                    elites++;
                }
            }

            assertTrue(elites > 0, "Alti yuz denemede hic elit cikmadi");
            assertTrue(elites < rolls / 3, "Kalabaligin ucte biri elit olmamali: " + elites);
        }
    }
}
