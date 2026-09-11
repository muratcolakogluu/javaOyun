package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Archer;
import com.cryptdelver.entity.Imp;
import com.cryptdelver.entity.Player;
import com.cryptdelver.entity.Projectile;
import com.cryptdelver.entity.Skeleton;
import com.cryptdelver.world.BspGenerator;
import com.cryptdelver.world.Dungeon;
import com.cryptdelver.world.Tile;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Kosunun defteri: olum ekraninin anlattigi hikaye.
 *
 * <p>Olum perdesi uzun sure iki sayi gosteriyordu -- kacinci kat, kac altin.
 * Olen oyuncunun sordugu ilk soru ise <b>"beni ne oldurdu"</b>ydu ve o cevap
 * ekranda hic yoktu. Buradaki sinavlar defterin dogru sayip dogru
 * hatirladigini kovaliyor; ekranin nasil gorundugu ayri bir mesele, ama
 * gosterdigi sayilarin dogrulugu burada.</p>
 */
class RunSummaryTest {

    private static final double FRAME = 1.0 / 60;

    /** Sayaclar: tek bir kat uzerinde olup bitenler. */
    @Nested
    class Tally {

        private Player player;
        private Game game;

        @BeforeEach
        void setUp() {
            Dungeon dungeon = new Dungeon(15, 11);
            dungeon.fill(Tile.FLOOR);
            player = new Player(7, 5);
            game = new Game(dungeon, player);
        }

        /** Oyuncuyu dusmanin yanina koyup olene kadar vurur. */
        private void slay(com.cryptdelver.entity.Enemy enemy) {
            game.addEnemy(enemy);
            player.setTile(enemy.getTileX() - 1, enemy.getTileY());
            player.setMoveInput(1, 0);
            player.setMoveInput(0, 0);

            for (int i = 0; i < 600 && enemy.isAlive(); i++) {
                player.requestAttack();
                game.update(FRAME);
            }
        }

        @Test
        @DisplayName("Devrilen dusman sayiliyor")
        void fallenEnemiesAreCounted() {
            assertEquals(0, game.getRunLog().getKills());

            slay(new Imp(9, 5));

            assertEquals(1, game.getRunLog().getKills());
        }

        @Test
        @DisplayName("Bulunan altin birikiyor")
        void goldAddsUp() {
            game.addGold(80);
            game.addGold(40);

            assertEquals(120, game.getRunLog().getGoldFound());
        }

        /**
         * Kesedeki altin harcandikca azaliyor; defterin sordugu soru baska:
         * bu kosuda <em>ne kadar buldun</em>. Yalnizca keseye bakan bir ozet,
         * tezgahta dogru kararlar veren oyuncuyu cimri oyuncudan kotu
         * gosterirdi.
         */
        @Test
        @DisplayName("Harcamak toplanani azaltmiyor")
        void spendingDoesNotUndoTheTally() {
            RunLog log = new RunLog();
            log.recordGold(120);
            log.recordGold(-100);

            assertEquals(120, log.getGoldFound(), "Eksi tutar deftere yazilmamali");
        }

        @Test
        @DisplayName("Seni olduren seyin adi yaziliyor")
        void theKillerIsRemembered() {
            Skeleton bones = new Skeleton(8, 5);
            game.addEnemy(bones);
            player.takeDamage(player.getMaxHp() - 1);

            game.enemyAttacksPlayer(bones);

            assertFalse(player.isAlive());
            assertTrue(game.getRunLog().hasKiller());
            assertEquals(bones.getName(), game.getRunLog().getKilledBy());
        }

        /** Sekiz kare oteden gelen ok da bir katil. */
        @Test
        @DisplayName("Ok da katil olarak yaziliyor")
        void arrowsCountAsKillers() {
            Archer archer = new Archer(2, 5);
            game.addEnemy(archer);
            player.takeDamage(player.getMaxHp() - 1);

            game.projectileHitsPlayer(new Projectile(6, 5, 1, 0, archer, 8));

            assertEquals(archer.getName(), game.getRunLog().getKilledBy());
        }

        /**
         * Ayni karede hem diken hasari hem dusman vurusu islenebiliyor; olumu
         * <em>getiren</em> vurus ilk olan, sonrakiler defteri degistirmemeli.
         */
        @Test
        @DisplayName("Ilk katil yaziliyor, sonrakiler degil")
        void theFirstKillerWins() {
            Skeleton bones = new Skeleton(8, 5);
            Imp imp = new Imp(6, 5);
            game.addEnemy(bones);
            game.addEnemy(imp);
            player.takeDamage(player.getMaxHp() - 1);

            game.enemyAttacksPlayer(bones);
            game.enemyAttacksPlayer(imp);

            assertEquals(bones.getName(), game.getRunLog().getKilledBy());
        }

        @Test
        @DisplayName("Yasayan oyuncunun katili yok")
        void theLivingHaveNoKiller() {
            Skeleton bones = new Skeleton(8, 5);
            game.addEnemy(bones);

            game.enemyAttacksPlayer(bones);

            assertTrue(player.isAlive(), "Tek vurus dolu cani goturmemeli");
            assertFalse(game.getRunLog().hasKiller());
            assertNull(game.getRunLog().getKilledBy());
        }
    }

    /** Kat degistirmeyi gerektiren sayaclar. */
    @Nested
    class Journey {

        private final Player player = new Player(0, 0);
        private final Game game = new Game(List.of(new BspGenerator()), 40, 22, player);

        private void goDown() {
            player.setTile(game.getStairs());
            assertTrue(game.descend());
        }

        // Inen oyuncu zaten geldigi merdivenin ustunde duruyor; yeniden
        // yurumeye gerek yok.
        private void goUp() {
            assertTrue(game.ascend());
        }

        @Test
        @DisplayName("Ilk katta en derin kat 1")
        void theRunStartsOnFloorOne() {
            assertEquals(1, game.getRunLog().getDeepestFloor());
            assertEquals(0, game.getRunLog().getReturns());
        }

        @Test
        @DisplayName("Indikce en derin kat buyuyor")
        void goingDownDeepensTheRun() {
            goDown();
            goDown();

            assertEquals(3, game.getRunLog().getDeepestFloor());
        }

        /**
         * Geri donmek indigin yeri geri almiyor: "on bese kadar inmistim"
         * cumlesi, sonradan yukari cikmis olsan da dogru kaliyor.
         */
        @Test
        @DisplayName("Geri donmek en derin kati azaltmiyor")
        void climbingBackKeepsTheRecord() {
            goDown();
            goDown();
            goUp();

            assertEquals(3, game.getRunLog().getDeepestFloor());
            assertEquals(2, game.getDepth());
            assertEquals(1, game.getRunLog().getReturns());
        }

        @Test
        @DisplayName("Yeniden baslayinca defter beyaz sayfa")
        void restartingClearsTheLog() {
            goDown();
            game.addGold(50);
            player.takeDamage(player.getMaxHp());

            game.restart();

            RunLog run = game.getRunLog();
            assertEquals(1, run.getDeepestFloor());
            assertEquals(0, run.getGoldFound());
            assertEquals(0, run.getKills());
            assertEquals(0, run.getReturns());
            assertFalse(run.hasKiller());
        }
    }
}
