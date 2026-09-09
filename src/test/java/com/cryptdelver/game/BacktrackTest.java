package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Player;
import com.cryptdelver.world.BspGenerator;
import com.cryptdelver.world.Position;
import com.cryptdelver.world.RandomWalkGenerator;
import com.cryptdelver.world.Tile;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Katlar arasi geri donus.
 *
 * <p>Geri donebilmek altina bir anlam kazandirdi: kesende para birikince
 * yukaridaki buyucuye donup takimina buyu bastirabiliyorsun. Oncesinde altin
 * yalnizca bulundugun katta buyucu varsa ise yariyordu.</p>
 *
 * <p>Buradaki en onemli kural birakilan katin <em>hatirlanmasi</em>: her
 * seferinde yeniden uretmek cok daha kolay olurdu ama sonsuz altin demek
 * olurdu -- cik, in, kat yepyeni ganimetle karsina gelsin.</p>
 */
class BacktrackTest {

    private Player player;
    private Game game;

    @BeforeEach
    void setUp() {
        player = new Player(0, 0);
        game = new Game(List.of(new BspGenerator(), new RandomWalkGenerator()), 40, 22, player);
    }

    private void goDown() {
        player.setTile(game.getStairs());
        assertTrue(game.descend(), "Inis calismali");
    }

    @Test
    @DisplayName("Kata inilen nokta yukari cikan merdiven")
    void theArrivalTileIsTheUpStaircase() {
        goDown();

        assertTrue(game.isPlayerOnUpStairs(), "Indigin yerde duruyorsun");
        assertEquals(Tile.STAIRS_UP,
                game.getDungeon().getTile(player.getTileX(), player.getTileY()));
    }

    @Test
    @DisplayName("Ilk kattan yukari cikilamiyor")
    void thereIsNoWayUpFromTheFirstFloor() {
        assertEquals(1, game.getDepth());

        assertFalse(game.ascend(), "Yuzeyin ustu yok");
        assertEquals(1, game.getDepth());
    }

    @Test
    @DisplayName("Yukari cikinca bir onceki kata donuluyor")
    void ascendingGoesBackOneFloor() {
        goDown();
        assertEquals(2, game.getDepth());

        assertTrue(game.ascend());

        assertEquals(1, game.getDepth());
    }

    /** Cikinca indigin merdivenin basina variyorsun, katin girisine degil. */
    @Test
    @DisplayName("Cikinca indigin merdivenin basindasin")
    void youArriveAtTheStaircaseYouCameDown() {
        Position downStairs = game.getStairs();
        goDown();

        game.ascend();

        assertEquals(downStairs, player.getTile());
        assertTrue(game.isPlayerOnStairs());
    }

    /**
     * Kat hatirlaniyor: ayni harita, ayni dusmanlar, ayni esyalar. Yeniden
     * uretilseydi cik-in yaparak sonsuz ganimet toplanabilirdi.
     */
    @Test
    @DisplayName("Geri donulen kat aynen duruyor")
    void aRevisitedFloorIsUnchanged() {
        var firstFloorMap = game.getDungeon();
        int enemiesBefore = game.getEnemies().size();
        int itemsBefore = game.getGroundItems().size();
        Position stairsBefore = game.getStairs();

        goDown();
        game.ascend();

        assertEquals(firstFloorMap, game.getDungeon(), "Ayni harita nesnesi");
        assertEquals(enemiesBefore, game.getEnemies().size());
        assertEquals(itemsBefore, game.getGroundItems().size());
        assertEquals(stairsBefore, game.getStairs());
    }

    /** Oldurulen dusman geri donunce dirilmiyor. */
    @Test
    @DisplayName("Temizlenen kat temiz kaliyor")
    void clearedEnemiesStayDead() {
        while (!game.getEnemies().isEmpty()) {
            var target = game.getEnemies().get(0);
            player.setTile(target.getTileX() + 1, target.getTileY());
            for (int i = 0; i < 400 && target.isAlive(); i++) {
                game.playerAttacks();
            }
        }

        goDown();
        game.ascend();

        assertTrue(game.getEnemies().isEmpty(), "Oldurulenler dirilmemeli");
    }

    /** Kesif de hatirlaniyor: geri donunce harita bastan karanlik olmuyor. */
    @Test
    @DisplayName("Geri donulen katin kesfi duruyor")
    void explorationIsRemembered() {
        Position spawn = player.getTile();
        assertTrue(game.getVision().isRemembered(spawn.x(), spawn.y()));

        goDown();
        game.ascend();

        assertTrue(game.getVision().isRemembered(spawn.x(), spawn.y()),
                "Gezdigin yer unutulmamali");
    }

    @Test
    @DisplayName("Asagi inip tekrar inince yeni kat uretiliyor")
    void goingDownAgainReusesTheSameFloor() {
        goDown();
        var secondFloorMap = game.getDungeon();

        game.ascend();
        goDown();

        assertEquals(secondFloorMap, game.getDungeon(), "Ikinci kat da hatirlanmali");
    }

    @Test
    @DisplayName("Yeniden baslayinca kat hafizasi siliniyor")
    void restartForgetsEveryFloor() {
        goDown();
        game.restart();

        assertEquals(1, game.getDepth());
        assertFalse(game.ascend(), "Ilk kattayiz, yukarisi yok");
    }
}
