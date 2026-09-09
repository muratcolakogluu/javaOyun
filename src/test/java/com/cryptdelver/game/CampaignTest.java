package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Boss;
import com.cryptdelver.entity.Player;
import com.cryptdelver.world.BspGenerator;
import com.cryptdelver.world.RandomWalkGenerator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Yirmi katlik yolculuk.
 *
 * <p>Zindanin bir sonu var: dort bolge, her biri bes kat ve sonunda bir boss.
 * Yirminci katin merdiveni asagi degil disari cikiyor. Bu testler o yapiyi
 * kilitliyor.</p>
 */
class CampaignTest {

    private static final int WIDTH = 40;
    private static final int HEIGHT = 22;

    /** Bolgelerin derinlige gore dagilimi; oyun kurmadan sinanabiliyor. */
    @Nested
    class Themes {

        @Test
        @DisplayName("Her bes kat bir bolge")
        void themesChangeEveryFiveFloors() {
            assertEquals(FloorTheme.MAHZEN, FloorTheme.forDepth(1));
            assertEquals(FloorTheme.MAHZEN, FloorTheme.forDepth(5));
            assertEquals(FloorTheme.SARNIC, FloorTheme.forDepth(6));
            assertEquals(FloorTheme.SARNIC, FloorTheme.forDepth(10));
            assertEquals(FloorTheme.KORLUK, FloorTheme.forDepth(11));
            assertEquals(FloorTheme.KRIPT, FloorTheme.forDepth(16));
            assertEquals(FloorTheme.KRIPT, FloorTheme.forDepth(20));
        }

        /** Bolgeler bossla bitiyor: son katlari boss katlariyla ayni. */
        @Test
        @DisplayName("Her bolgenin son kati boss kati")
        void themesEndOnABossFloor() {
            for (FloorTheme theme : FloorTheme.values()) {
                assertEquals(0, theme.getLastDepth() % 5,
                        theme.getLabel() + " bolgesi boss katinda bitmeli");
            }
        }

        @Test
        @DisplayName("Yirmi kat, dort bolge")
        void twentyFloorsInFourThemes() {
            assertEquals(20, FloorTheme.MAX_DEPTH);
            assertEquals(4, FloorTheme.values().length);
            assertEquals(FloorTheme.MAX_DEPTH,
                    FloorTheme.values()[FloorTheme.values().length - 1].getLastDepth());
        }

        /** Sinirin otesi diziyi tasirmiyor; son bolgede kaliyor. */
        @Test
        @DisplayName("Yirminin otesi son bolgede kaliyor")
        void beyondTheEndStaysInTheLastTheme() {
            assertEquals(FloorTheme.KRIPT, FloorTheme.forDepth(99));
            assertEquals(FloorTheme.MAHZEN, FloorTheme.forDepth(0), "Sifir da ilk bolge");
        }
    }

    /** Oyunun icindeki akis: inis, bolge degisimi ve bitis. */
    @Nested
    class Journey {

        private Player player;
        private Game game;

        @BeforeEach
        void setUp() {
            player = new Player(0, 0);
            game = new Game(List.of(new BspGenerator(), new RandomWalkGenerator()),
                    WIDTH, HEIGHT, player);
        }

        /** Bir kat asagi iner; bossu varsa once indirir. */
        private void goDownOneFloor() {
            Boss boss = game.getBoss();
            if (boss != null) {
                player.setTile(boss.getTileX() + 1, boss.getTileY());
                for (int i = 0; i < 4000 && boss.isAlive(); i++) {
                    game.playerAttacks();
                    game.getPlayer().heal(100);
                }
            }

            player.setTile(game.getStairs());
            assertTrue(game.descend(), "Inis calismali");
        }

        private void goDownTo(int depth) {
            while (game.getDepth() < depth) {
                goDownOneFloor();
            }
        }

        @Test
        @DisplayName("Oyun ilk bolgede basliyor")
        void startsInTheFirstTheme() {
            assertEquals(1, game.getDepth());
            assertEquals(FloorTheme.MAHZEN, game.getTheme());
            assertFalse(game.isFinalFloor());
            assertFalse(game.isWon());
        }

        @Test
        @DisplayName("Inildikce bolge degisiyor")
        void themeFollowsTheDescent() {
            goDownTo(6);

            assertEquals(FloorTheme.SARNIC, game.getTheme());
        }

        @Test
        @DisplayName("Yirminci kat son kat")
        void twentyIsTheLastFloor() {
            goDownTo(FloorTheme.MAX_DEPTH);

            assertTrue(game.isFinalFloor());
            assertEquals(FloorTheme.KRIPT, game.getTheme());
            assertNotNull(game.getBoss(), "Son katta da boss var");
        }

        /**
         * Son katin merdiveni yeni kat uretmiyor: oyun kazanilmis oluyor ve
         * derinlik yirmide kaliyor.
         */
        @Test
        @DisplayName("Son kattan cikinca oyun kazaniliyor")
        void leavingTheLastFloorWinsTheGame() {
            goDownTo(FloorTheme.MAX_DEPTH);
            goDownOneFloor();

            assertTrue(game.isWon());
            assertEquals(FloorTheme.MAX_DEPTH, game.getDepth(), "Yirmi birinci kat yok");
            assertTrue(game.isFrozen(), "Kazanilinca dunya duruyor");
        }

        @Test
        @DisplayName("Yeniden baslayinca zafer siliniyor")
        void restartClearsTheVictory() {
            goDownTo(FloorTheme.MAX_DEPTH);
            goDownOneFloor();

            game.restart();

            assertFalse(game.isWon());
            assertEquals(1, game.getDepth());
        }
    }

    /** Gezgin buyucu: boss katlarinin disinda da cikabiliyor. */
    @Nested
    class WanderingWizard {

        @Test
        @DisplayName("Boss katinda buyucu her zaman var")
        void bossFloorsAlwaysHaveOne() {
            Player player = new Player(0, 0);
            Game game = new Game(List.of(new BspGenerator()), WIDTH, HEIGHT, player);

            while (game.getDepth() < 5) {
                player.setTile(game.getStairs());
                assertTrue(game.descend());
            }

            assertNotNull(game.getWizard());
        }

        /**
         * Siradan katlarda buyucu arada bir cikiyor. Olasilik dusuk oldugu icin
         * tek kat bakmak yeterli degil: cok sayida kat uretip "hem cikiyor hem
         * her katta cikmiyor" ikilisini birlikte ariyoruz.
         */
        @Test
        @DisplayName("Siradan katlarda buyucu arada bir cikiyor")
        void ordinaryFloorsSometimesHaveOne() {
            Player player = new Player(0, 0);
            Game game = new Game(List.of(new BspGenerator()), WIDTH, HEIGHT, player);

            int withWizard = 0;
            int floors = 400;
            for (int i = 0; i < floors; i++) {
                game.regenerateFloor();
                if (game.getWizard() != null) {
                    withWizard++;
                }
            }

            assertTrue(withWizard > 0, "Hic cikmiyorsa sans isletmiyor demektir");
            assertTrue(withWizard < floors / 4,
                    "Her katta cikiyorsa nadir olma amaci kalmaz: " + withWizard + "/" + floors);
        }

        /**
         * Zar katin tohumundan atiliyor. Genel rastgelelikten atsaydik ayni
         * kaydi yuklemek buyucuyu kaybettirebilirdi.
         */
        @Test
        @DisplayName("Buyucunun varligi kayit yuklenince degismiyor")
        void theWizardSurvivesAReload() {
            Player player = new Player(0, 0);
            Game game = new Game(List.of(new BspGenerator()), WIDTH, HEIGHT, player);

            for (int i = 0; i < 60; i++) {
                game.regenerateFloor();
                boolean before = game.getWizard() != null;

                game.applySave(game.captureSave());

                if (before) {
                    assertNotNull(game.getWizard(), "Kayitta olan buyucu kaybolmamali");
                } else {
                    assertNull(game.getWizard(), "Kayitta olmayan buyucu belirmemeli");
                }
            }
        }
    }
}
