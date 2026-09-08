package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Player;
import com.cryptdelver.entity.Rat;
import com.cryptdelver.entity.Skeleton;
import com.cryptdelver.world.Dungeon;
import com.cryptdelver.world.Tile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Savas kurallari. Hasar rastgele oldugundan kesin sayilar yerine yon
 * dogrulaniyor: can azaldi mi, dusman listeden dustu mu, bekleme suresi
 * calisti mi.
 */
class CombatTest {

    private static final double FRAME = 1.0 / 60;

    private Player player;
    private Game game;

    @BeforeEach
    void setUp() {
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

        player = new Player(5, 5);
        game = new Game(dungeon, player);
    }

    private void simulateFrames(int frames) {
        for (int i = 0; i < frames; i++) {
            game.update(FRAME);
        }
    }

    /** Sureyi ilerletirken her karede saldiri tusuna basili tutar. */
    private void simulateAttacking(int frames) {
        for (int i = 0; i < frames; i++) {
            player.requestAttack();
            game.update(FRAME);
        }
    }

    @Test
    @DisplayName("Yan karedeki dusman vurulur")
    void attackHitsAdjacentEnemy() {
        Skeleton skeleton = new Skeleton(6, 5);
        game.addEnemy(skeleton);

        player.requestAttack();
        game.update(FRAME);

        assertTrue(skeleton.getHp() < skeleton.getMaxHp(), "Dusman hasar almali");
    }

    @Test
    @DisplayName("Uzaktaki dusman vurulmaz")
    void attackMissesDistantEnemy() {
        Skeleton skeleton = new Skeleton(8, 5);
        game.addEnemy(skeleton);

        player.requestAttack();
        game.update(FRAME);

        assertEquals(skeleton.getMaxHp(), skeleton.getHp());
    }

    @Test
    @DisplayName("Capraz komsu vurulmaz")
    void attackIgnoresDiagonalNeighbour() {
        Skeleton skeleton = new Skeleton(6, 6);
        game.addEnemy(skeleton);

        player.requestAttack();
        game.update(FRAME);

        assertEquals(skeleton.getMaxHp(), skeleton.getHp());
    }

    @Test
    @DisplayName("Bekleme suresi dolmadan ikinci vurus islemez")
    void attackRespectsCooldown() {
        Skeleton skeleton = new Skeleton(6, 5);
        game.addEnemy(skeleton);

        player.requestAttack();
        game.update(FRAME);
        int afterFirstHit = skeleton.getHp();

        player.requestAttack();
        game.update(FRAME);

        assertEquals(afterFirstHit, skeleton.getHp());
    }

    @Test
    @DisplayName("Bekleme suresi dolunca tekrar vurulabilir")
    void attackWorksAgainAfterCooldown() {
        Skeleton skeleton = new Skeleton(6, 5);
        game.addEnemy(skeleton);

        player.requestAttack();
        game.update(FRAME);
        int afterFirstHit = skeleton.getHp();

        simulateFrames(30);
        player.requestAttack();
        game.update(FRAME);

        assertTrue(skeleton.getHp() < afterFirstHit, "Bekleme sonrasi ikinci vurus islemeli");
    }

    @Test
    @DisplayName("Cani biten dusman oyundan cikar")
    void deadEnemyLeavesTheGame() {
        Rat rat = new Rat(6, 5);
        game.addEnemy(rat);

        simulateAttacking(180);

        assertFalse(rat.isAlive());
        assertTrue(game.getEnemies().isEmpty(), "Olu dusman listede kalmamali");
    }

    @Test
    @DisplayName("Yan karedeki dusman oyuncuya vurur")
    void adjacentEnemyDamagesPlayer() {
        game.addEnemy(new Skeleton(6, 5));

        simulateFrames(10);

        assertTrue(player.getHp() < player.getMaxHp(), "Oyuncu hasar almali");
    }

    @Test
    @DisplayName("Menzildeki dusman kare kare yaklasir")
    void enemyChasesPlayer() {
        Rat rat = new Rat(11, 5);
        game.addEnemy(rat);
        int distanceBefore = rat.tileDistanceTo(player);

        simulateFrames(60);

        assertTrue(rat.tileDistanceTo(player) < distanceBefore - 2,
                "Fare menzil icindeyken belirgin sekilde yaklasmali");
    }

    @Test
    @DisplayName("Uzaktaki iskelet yerinde durur")
    void distantSkeletonHoldsPosition() {
        Skeleton skeleton = new Skeleton(18, 5);
        game.addEnemy(skeleton);

        simulateFrames(60);

        assertEquals(18, skeleton.getTileX(), "Menzil disindaki iskelet nobet tutmali");
        assertEquals(5, skeleton.getTileY());
    }

    @Test
    @DisplayName("Dusmanlar ayni kareye giremez")
    void enemiesNeverShareATile() {
        Skeleton first = new Skeleton(9, 5);
        Skeleton second = new Skeleton(10, 5);
        game.addEnemy(first);
        game.addEnemy(second);

        for (int i = 0; i < 120; i++) {
            game.update(FRAME);
            assertFalse(first.getTile().equals(second.getTile()),
                    "Iki dusman ayni karede olmamali");
        }
    }

    @Test
    @DisplayName("Dusman oyuncunun karesine girmez")
    void enemyDoesNotStepOntoThePlayer() {
        Rat rat = new Rat(8, 5);
        game.addEnemy(rat);

        for (int i = 0; i < 120; i++) {
            game.update(FRAME);
            assertFalse(rat.getTile().equals(player.getTile()),
                    "Dusman oyuncunun uzerine binmemeli");
        }
    }

    @Test
    @DisplayName("Oyuncu olunce oyun biter")
    void gameEndsWhenPlayerDies() {
        player.takeDamage(player.getMaxHp());

        assertTrue(game.isOver());
    }
}
