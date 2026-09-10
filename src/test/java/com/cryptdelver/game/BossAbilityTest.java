package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Bekci;
import com.cryptdelver.entity.Bogucu;
import com.cryptdelver.entity.Boss;
import com.cryptdelver.entity.Lort;
import com.cryptdelver.entity.Player;
import com.cryptdelver.entity.Seytan;
import com.cryptdelver.world.Dungeon;
import com.cryptdelver.world.Tile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Dort bossun dort ayri yetenegi.
 *
 * <p>Uzun sure tek boss sinifi vardi ve dordunun farki yalnizca adi, govdesi ve
 * sayilariydi: 20. kattaki boss, 5. kattakinin buyutulmus haliydi. Sayi
 * buyutmek bir bossu <em>sertlestiriyor</em> ama <em>degistirmiyor</em> --
 * on bes kat inip ayni dovusu tekrar veriyordun. Buradaki testler dordunun
 * artik baska bir soru sordugunu dogruluyor.</p>
 */
class BossAbilityTest {

    private static final double FRAME = 1.0 / 60;

    private Player player;
    private Game game;

    @BeforeEach
    void setUp() {
        Dungeon dungeon = new Dungeon(25, 15);
        dungeon.fill(Tile.FLOOR);
        for (int x = 0; x < 25; x++) {
            dungeon.setTile(x, 0, Tile.WALL);
            dungeon.setTile(x, 14, Tile.WALL);
        }
        for (int y = 0; y < 15; y++) {
            dungeon.setTile(0, y, Tile.WALL);
            dungeon.setTile(24, y, Tile.WALL);
        }

        player = new Player(4, 7);
        game = new Game(dungeon, player);
    }

    /** Oyuncuyu ayakta tutarak simule eder: dovus testleri olumle kesilmesin. */
    private void simulate(double seconds) {
        for (int i = 0; i < seconds / FRAME; i++) {
            game.update(FRAME);
            player.restore();
        }
    }

    @Test
    @DisplayName("Her bolgenin sahibi ayri bir sinif")
    void everyRegionHasItsOwnBoss() {
        assertInstanceOf(Bekci.class, Boss.forNumber(0, 0, 1));
        assertInstanceOf(Bogucu.class, Boss.forNumber(0, 0, 2));
        assertInstanceOf(Seytan.class, Boss.forNumber(0, 0, 3));
        assertInstanceOf(Lort.class, Boss.forNumber(0, 0, 4));
    }

    /** Sira disi bir sayi son bolgenin sahibine dusuyor; kayit bozuk olsa da acilsin. */
    @Test
    @DisplayName("Bilinmeyen sira son bossa dusuyor")
    void anUnknownNumberFallsBackToTheLastLord() {
        assertInstanceOf(Lort.class, Boss.forNumber(0, 0, 99));
        assertInstanceOf(Bekci.class, Boss.forNumber(0, 0, 0));
    }

    @Nested
    class ChokerVolley {

        /**
         * Salvo habersiz gelseydi kacinilamazdi; once isaret veriyor. Isaretin
         * <em>gorulmesi</em> mekanigin kendisi kadar onemli.
         */
        @Test
        @DisplayName("Salvodan once isaret veriyor")
        void itTelegraphsBeforeFiring() {
            Bogucu choker = new Bogucu(12, 7);
            game.addEnemy(choker);

            boolean sawWindup = false;
            for (int i = 0; i < 60 * 6 && game.getProjectiles().isEmpty(); i++) {
                game.update(FRAME);
                player.restore();
                sawWindup |= choker.isWindingUp();
            }

            assertTrue(sawWindup, "Salvodan once sisme evresi olmali");
            assertFalse(game.getProjectiles().isEmpty(), "Sonunda salvo cikmali");
        }

        /** Dort yone birden: cevabi yana degil koseye gecmek. */
        @Test
        @DisplayName("Salvo dort yone birden gidiyor")
        void theVolleyCoversFourDirections() {
            game.addEnemy(new Bogucu(12, 7));

            for (int i = 0; i < 60 * 6 && game.getProjectiles().isEmpty(); i++) {
                game.update(FRAME);
                player.restore();
            }

            assertEquals(4, game.getProjectiles().size(), "Dort ok birden");
        }

        /** Isaret ilerledikce koyulasiyor: kalan sureyi sayi okumadan goruyorsun. */
        @Test
        @DisplayName("Isaret ilerledikce doluyor")
        void theTellFillsUpAsItNears() {
            Bogucu choker = new Bogucu(12, 7);
            game.addEnemy(choker);

            while (!choker.isWindingUp()) {
                game.update(FRAME);
                player.restore();
            }

            double early = choker.getWindupProgress();
            simulate(0.4);

            assertTrue(choker.getWindupProgress() > early, "Isaret ilerlemeli");
        }
    }

    @Nested
    class DevilRage {

        @Test
        @DisplayName("Dolu canla ofkelenmiyor")
        void itStartsCalm() {
            Seytan devil = new Seytan(12, 7);
            game.addEnemy(devil);

            simulate(1.0);

            assertFalse(devil.isEnraged());
        }

        /** Ofke dovusun ikinci yarisini birincisinden farkli yapiyor. */
        @Test
        @DisplayName("Cani yarilaninca ofkeleniyor ve hizlaniyor")
        void itEnragesBelowHalfHealth() {
            Seytan devil = new Seytan(12, 7);
            game.addEnemy(devil);
            double calmSpeed = devil.getSpeed();
            double calmCooldown = devil.getAttackCooldown();

            devil.takeDamage(devil.getMaxHp() / 2 + 1);
            simulate(0.1);

            assertTrue(devil.isEnraged(), "Yari canin altinda ofkelenmeli");
            assertTrue(devil.getSpeed() > calmSpeed, "Hizlanmali");
            assertTrue(devil.getAttackCooldown() < calmCooldown, "Daha sik vurmali");
        }

        /** Kacis tamamen kapansaydi dovus degil zar atisi olurdu. */
        @Test
        @DisplayName("Ofkeliyken bile oyuncudan yavas")
        void evenEnragedItIsSlowerThanThePlayer() {
            Seytan devil = new Seytan(12, 7);
            game.addEnemy(devil);
            devil.takeDamage(devil.getMaxHp() / 2 + 1);
            simulate(0.1);

            assertTrue(devil.getSpeed() < player.getSpeed(),
                    "Koridorda soluklanmak hala mumkun olmali");
        }

        /**
         * Ofke bir daha kapanmiyor: kapansaydi oyuncu bossu esigin etrafinda
         * tutmayi ogrenir, dovus bir hileye donusurdu.
         */
        @Test
        @DisplayName("Cani dolsa bile ofke gecmiyor")
        void theRageNeverCoolsDown() {
            Seytan devil = new Seytan(12, 7);
            game.addEnemy(devil);
            devil.takeDamage(devil.getMaxHp() / 2 + 1);
            simulate(0.1);

            devil.heal(devil.getMaxHp());
            simulate(0.5);

            assertTrue(devil.isEnraged(), "Ofke geri donmemeli");
        }
    }

    @Nested
    class LordBlink {

        /**
         * Yirmi kat boyunca ogrenilen tek evrensel dogru "kacabilirsin"di.
         * Son boss o kapiyi kapatiyor.
         */
        @Test
        @DisplayName("Uzaklasinca yanina isinlaniyor")
        void itBlinksToThePlayer() {
            Lort lord = new Lort(20, 12);
            game.addEnemy(lord);

            simulate(6.0);

            assertTrue(lord.tileDistanceTo(player) <= 2,
                    "Uzakta kalamamali, kalan uzaklik: " + lord.tileDistanceTo(player));
        }

        /** Yakindayken isinlanmiyor: yalnizca kacisi kapatiyor, bedava vurus vermiyor. */
        @Test
        @DisplayName("Yani basindayken isinlanmiyor")
        void itDoesNotBlinkWhenAlreadyClose() {
            Lort lord = new Lort(6, 7);
            game.addEnemy(lord);
            player.setTile(5, 7);

            simulate(6.0);

            assertTrue(lord.tileDistanceTo(player) <= 2, "Yaninda kalmali");
        }

        /** Dovusun basinda bir soluk payi var; ilk saniyede belirmiyor. */
        @Test
        @DisplayName("Ilk saniyelerde isinlanmiyor")
        void theFirstBlinkWaits() {
            Lort lord = new Lort(20, 12);
            game.addEnemy(lord);
            int start = lord.tileDistanceTo(player);

            simulate(0.5);

            assertTrue(lord.tileDistanceTo(player) > start - 3,
                    "Ilk yarim saniyede yanina gelmemeli");
        }
    }
}
