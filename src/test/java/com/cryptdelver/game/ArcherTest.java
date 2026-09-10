package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Archer;
import com.cryptdelver.entity.Player;
import com.cryptdelver.world.Dungeon;
import com.cryptdelver.world.Tile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Okcu: oyunun ilk menzilli tehdidi.
 *
 * <p>Geldigi ana kadar <b>kacmak her zaman dogru hamleydi</b>: oyuncu saniyede
 * 6 kare gidiyor, en hizli dusman 5.5. Yani "vur, geri cekil, tekrar vur"
 * hicbir zaman cezalandirilmiyordu ve butun dovuslerin tek bir dogru cevabi
 * vardi. Buradaki testler o cevabin artik her zaman gecerli olmadigini
 * dogruluyor.</p>
 */
class ArcherTest {

    private static final double FRAME = 1.0 / 60;

    private Player player;
    private Game game;

    @BeforeEach
    void setUp() {
        Dungeon dungeon = new Dungeon(21, 11);
        dungeon.fill(Tile.FLOOR);
        for (int x = 0; x < 21; x++) {
            dungeon.setTile(x, 0, Tile.WALL);
            dungeon.setTile(x, 10, Tile.WALL);
        }
        for (int y = 0; y < 11; y++) {
            dungeon.setTile(0, y, Tile.WALL);
            dungeon.setTile(20, y, Tile.WALL);
        }

        player = new Player(3, 5);
        game = new Game(dungeon, player);
    }

    private void simulate(double seconds) {
        for (int i = 0; i < seconds / FRAME; i++) {
            game.update(FRAME);
        }
    }

    private Archer archerAt(int x, int y) {
        Archer archer = new Archer(x, y);
        game.addEnemy(archer);
        return archer;
    }

    /**
     * Ilk ok cikana kadar simule eder.
     *
     * <p>Okcu hat kurmak icin yuruyor, yani "capraz duruyorken atmiyor" gibi bir
     * kurali sabit sureyle sinamak yaniltici olurdu: yeterince beklersen zaten
     * hattı kurup atacak. Onun yerine <em>atisin ciktigi ani</em> yakalayip o
     * anda kuralin gecerli olup olmadigina bakiyoruz.</p>
     *
     * @return ok cikan cerceve sayisi; hic cikmadiysa {@code -1}
     */
    private int simulateUntilShot(double seconds) {
        for (int i = 0; i < seconds / FRAME; i++) {
            game.update(FRAME);
            if (!game.getProjectiles().isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    @Nested
    class Shooting {

        /** Nisan hatti aynı satir ya da sutun; oyuncunun kacinma yolu bu kuraldan cikiyor. */
        @Test
        @DisplayName("Ayni satirda duran oyuncuya ok atiyor")
        void shootsAlongARow() {
            archerAt(9, 5);

            simulate(1.0);

            assertFalse(game.getProjectiles().isEmpty(), "Ok havada olmali");
        }

        @Test
        @DisplayName("Ayni sutunda da atiyor")
        void shootsAlongAColumn() {
            player.setTile(9, 2);
            archerAt(9, 8);

            simulate(1.0);

            assertFalse(game.getProjectiles().isEmpty());
        }

        /**
         * Capraz duruyorsan hat yok. Okcu hattı kurmak icin yuruyecek, o yuzden
         * "hic atmiyor" diye bakmak yanlis olurdu; atisin <em>ciktigi anda</em>
         * hattın kurulmus olmasi gerekiyor.
         */
        @Test
        @DisplayName("Ok yalnizca hat kurulunca cikiyor")
        void onlyShootsOnceItIsLinedUp() {
            Archer archer = archerAt(9, 6);

            assertTrue(simulateUntilShot(5.0) >= 0, "Sonunda hattı kurup atmali");
            assertTrue(archer.getTileX() == player.getTileX()
                            || archer.getTileY() == player.getTileY(),
                    "Ok cikarken okcu oyuncuyla ayni satir ya da sutunda olmali");
        }

        @Test
        @DisplayName("Arada duvar varsa atmiyor")
        void wallsBlockTheShot() {
            game.getDungeon().setTile(6, 5, Tile.WALL);
            archerAt(9, 5);

            simulate(0.8);

            assertTrue(game.getProjectiles().isEmpty(), "Duvarin arkasindan atmamali");
        }

        /** Menzil disindaysan once yaklasmasi gerekiyor: uzaklik hala bir siper. */
        @Test
        @DisplayName("Ok yalnizca menzil icinden cikiyor")
        void onlyShootsFromWithinRange() {
            player.setTile(2, 5);
            Archer archer = archerAt(13, 5);
            assertEquals(11, archer.tileDistanceTo(player), "Once menzil disinda ama fark etme menzilinde");

            assertTrue(simulateUntilShot(6.0) >= 0, "Yaklasip atmali");
            assertTrue(archer.tileDistanceTo(player) <= 8,
                    "Ok cikarken menzil icinde olmali");
        }
    }

    @Nested
    class Movement {

        /**
         * Asil mesele bu: okcu sana gelmiyor, seni bekliyor. Mesafeyi korumasi
         * "geri cekil" hamlesini kotu bir hamle yapiyor.
         */
        @Test
        @DisplayName("Nisan hattindayken yerinde duruyor")
        void holdsItsGroundWhileAiming() {
            Archer archer = archerAt(9, 5);

            simulate(2.0);

            assertEquals(9, archer.getTileX(), "Yaklasmamali");
            assertEquals(5, archer.getTileY());
        }

        @Test
        @DisplayName("Hat yokken hattı kurmak icin yaklasiyor")
        void closesInWhenItHasNoLine() {
            Archer archer = archerAt(12, 8);
            int before = archer.tileDistanceTo(player);

            simulate(2.0);

            assertTrue(archer.tileDistanceTo(player) < before, "Yaklasmali");
        }

        /** Yanina varabilirsen kaciyor: yaklasmanin odulu bu. */
        @Test
        @DisplayName("Yanina gelince geri cekiliyor")
        void backsAwayWhenCornered() {
            Archer archer = archerAt(4, 5);

            simulate(1.0);

            assertTrue(archer.tileDistanceTo(player) > 1, "Yanindan uzaklasmali");
        }
    }

    @Nested
    class Flight {

        /** Ok havada gercekten yol aliyor; goremedigin bir hasar, hasar degil vergidir. */
        @Test
        @DisplayName("Ok atildigi yerden uzaklasiyor")
        void theArrowTravels() {
            archerAt(11, 5);
            simulate(0.7);

            var arrow = game.getProjectiles().get(0);
            int launched = arrow.getTileX();
            simulate(0.15);

            assertTrue(arrow.getTileX() < launched, "Ok oyuncuya dogru ilerlemeli");
        }

        @Test
        @DisplayName("Ok oyuncuya isabet edince can gidiyor")
        void theArrowHurts() {
            int before = player.getHp();
            archerAt(9, 5);

            simulate(2.0);

            assertTrue(player.getHp() < before, "Ok isabet etmeli");
        }

        /** Hattan cikarsan ok bosa gidiyor: kacinma gercekten mumkun. */
        @Test
        @DisplayName("Hattan cikinca ok isabet etmiyor")
        void steppingOffTheLineDodgesIt() {
            archerAt(11, 5);
            simulate(0.7);
            assertFalse(game.getProjectiles().isEmpty(), "Once ok atilmis olmali");

            int before = player.getHp();
            player.setTile(3, 6);
            simulate(1.0);

            assertEquals(before, player.getHp(), "Yan adim seni kurtarmali");
        }

        @Test
        @DisplayName("Ok duvara carpinca kayboluyor")
        void wallsStopTheArrow() {
            player.setTile(3, 5);
            Archer archer = archerAt(11, 5);
            simulate(0.7);
            assertFalse(game.getProjectiles().isEmpty());

            // Oyuncuyu hattan cekip okun onune duvar koyuyoruz.
            player.setTile(3, 8);
            game.getDungeon().setTile(7, 5, Tile.WALL);
            simulate(1.5);

            assertTrue(game.getProjectiles().isEmpty(), "Ok duvarda durmali");
            assertTrue(archer.isAlive());
        }
    }
}
