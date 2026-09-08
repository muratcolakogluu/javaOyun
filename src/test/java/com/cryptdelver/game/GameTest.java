package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Player;
import com.cryptdelver.entity.Skeleton;
import com.cryptdelver.world.Dungeon;
import com.cryptdelver.world.Tile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Izgaraya kilitli gercek zamanli hareketin kurallari. Oyun dongusunu elle
 * cevirebiliyoruz: game.update(delta) cagrisi JavaFX gerektirmiyor.
 *
 * <p>Oyuncu saniyede 6 kare gidiyor, yani bir adim yaklasik 10 kare (frame)
 * suruyor.</p>
 */
class GameTest {

    private static final double FRAME = 1.0 / 60;

    private Player player;
    private Game game;

    @BeforeEach
    void setUp() {
        Dungeon dungeon = new Dungeon(15, 9);
        dungeon.fill(Tile.FLOOR);
        for (int i = 0; i < 15; i++) {
            dungeon.setTile(i, 0, Tile.WALL);
            dungeon.setTile(i, 8, Tile.WALL);
        }
        for (int i = 0; i < 9; i++) {
            dungeon.setTile(0, i, Tile.WALL);
            dungeon.setTile(14, i, Tile.WALL);
        }

        player = new Player(4, 4);
        game = new Game(dungeon, player);
    }

    private void simulateFrames(int frames) {
        for (int i = 0; i < frames; i++) {
            game.update(FRAME);
        }
    }

    @Test
    @DisplayName("Adim tamamlanmadan kare degismez, ama arada cizilir")
    void stepIsAnimatedBetweenTiles() {
        player.setMoveInput(1, 0);
        simulateFrames(3);

        assertTrue(player.isMoving(), "Adim surmeli");
        assertEquals(4, player.getTileX(), "Kare ancak adim bitince degisir");
        assertTrue(player.getRenderX() > 4.5, "Cizim konumu ilerlemeli");
        assertTrue(player.getRenderX() < 5.5, "Cizim konumu hedefi asmamali");
    }

    @Test
    @DisplayName("Adim bitince tam bir kare ilerlenmis olur")
    void stepLandsOnTheNextTile() {
        player.setMoveInput(1, 0);
        simulateFrames(11);

        assertEquals(5, player.getTileX());
        assertEquals(4, player.getTileY());
    }

    @Test
    @DisplayName("Adim ortasinda yon degismez")
    void directionCannotChangeMidStep() {
        player.setMoveInput(1, 0);
        simulateFrames(3);

        player.setMoveInput(0, -1);
        simulateFrames(8);

        assertEquals(5, player.getTileX(), "Baslamis adim saga tamamlanmali");
        assertEquals(4, player.getTileY());
    }

    @Test
    @DisplayName("Girdi kesilince mevcut adim tamamlanip durulur")
    void releasingInputFinishesTheStep() {
        player.setMoveInput(1, 0);
        simulateFrames(3);
        player.setMoveInput(0, 0);

        simulateFrames(60);

        assertEquals(5, player.getTileX(), "Yarim kalan adim tamamlanmali");
        assertFalse(player.isMoving(), "Sonra durmali");
    }

    @Test
    @DisplayName("Capraz girdi tek eksene indirgenir")
    void diagonalInputBecomesSingleAxis() {
        player.setMoveInput(1, 1);
        simulateFrames(11);

        assertEquals(5, player.getTileX());
        assertEquals(4, player.getTileY(), "Izgarada capraz adim yok");
    }

    @Test
    @DisplayName("Duvara adim atilmaz")
    void wallBlocksTheStep() {
        player.setTile(13, 4);
        player.setMoveInput(1, 0);

        simulateFrames(30);

        assertEquals(13, player.getTileX(), "Duvarin icine girilmemeli");
        assertFalse(player.isMoving());
    }

    @Test
    @DisplayName("Dolu kareye adim atilmaz")
    void occupiedTileBlocksTheStep() {
        game.addEnemy(new Skeleton(5, 4));
        player.setMoveInput(1, 0);

        simulateFrames(10);

        assertEquals(4, player.getTileX(), "Dusmanin uzerine yurunmemeli");
    }

    @Test
    @DisplayName("Bos kare kontrolu doluluğu da hesaba katar")
    void tileFreedomAccountsForOccupants() {
        Skeleton skeleton = new Skeleton(6, 4);
        game.addEnemy(skeleton);

        assertFalse(game.isTileFree(6, 4, null), "Dusmanin karesi dolu");
        assertFalse(game.isTileFree(4, 4, null), "Oyuncunun karesi dolu");
        assertTrue(game.isTileFree(6, 4, skeleton), "Kendi karesi kendisi icin serbest");
        assertTrue(game.isTileFree(2, 2, null));
        assertFalse(game.isTileFree(0, 4, null), "Duvar bos sayilmaz");
    }

    @Test
    @DisplayName("Gecen sure isler, olunce durur")
    void elapsedTimeTracksTheRun() {
        simulateFrames(30);
        assertTrue(game.getElapsedSeconds() > 0.4);

        player.takeDamage(player.getMaxHp());
        double atDeath = game.getElapsedSeconds();
        simulateFrames(30);

        assertTrue(game.isOver());
        assertEquals(atDeath, game.getElapsedSeconds(), 1e-9, "Olumden sonra sure islememeli");
    }

    @Test
    @DisplayName("Olu oyuncu hareket etmez")
    void deadPlayerDoesNotMove() {
        player.takeDamage(player.getMaxHp());
        player.setMoveInput(1, 0);

        simulateFrames(30);

        assertEquals(4, player.getTileX());
        assertFalse(player.isAlive());
    }
}
