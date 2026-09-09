package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Enemy;
import com.cryptdelver.entity.Player;
import com.cryptdelver.world.BspGenerator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Zorluk kademesinin oyuna yansimasi.
 *
 * <p>Zorluk iki kaldiraca dokunuyor: kattaki kalabalik ve dusmanlarin
 * derinlikle guclenme hizi. Tur degerlerine (EnemyStats) dokunmuyor -- bir
 * iskeletin ne kadar sert oldugu turun tanimi, ayar penceresinin karari
 * degil.</p>
 */
class DifficultyTest {

    private static final int WIDTH = 40;
    private static final int HEIGHT = 22;

    private Game newGame(Difficulty difficulty) {
        Player player = new Player(0, 0);
        Game game = new Game(List.of(new BspGenerator()), WIDTH, HEIGHT, player);
        game.getSettings().setDifficulty(difficulty);
        game.regenerateFloor();
        return game;
    }

    @Test
    @DisplayName("Varsayilan kademe Normal")
    void normalIsTheDefault() {
        assertEquals(Difficulty.NORMAL, new Settings().getDifficulty());
    }

    @Test
    @DisplayName("Zor kat daha kalabalik, kolay kat daha tenha")
    void crowdFollowsTheDifficulty() {
        int easy = newGame(Difficulty.KOLAY).getEnemies().size();
        int normal = newGame(Difficulty.NORMAL).getEnemies().size();
        int hard = newGame(Difficulty.ZOR).getEnemies().size();

        assertTrue(easy < normal, "Kolayda daha az dusman: " + easy + " < " + normal);
        assertTrue(hard > normal, "Zorda daha cok dusman: " + hard + " > " + normal);
    }

    /**
     * Derinlik bonusu, kalabaligin aksine ancak birkac kat sonra fark
     * ediliyor; o yuzden asagi inip olcuyoruz.
     */
    @Test
    @DisplayName("Zorda dusmanlar derinlikle daha hizli sertlesiyor")
    void depthBonusFollowsTheDifficulty() {
        assertTrue(toughestAtDepth(Difficulty.ZOR, 7) > toughestAtDepth(Difficulty.KOLAY, 7),
                "Zorda ayni katin dusmanlari daha dayanikli olmali");
    }

    /** Verilen kademede o derinlige inip en dayanikli dusmanin canini verir. */
    private int toughestAtDepth(Difficulty difficulty, int depth) {
        Player player = new Player(0, 0);
        Game game = new Game(List.of(new BspGenerator()), WIDTH, HEIGHT, player);
        game.getSettings().setDifficulty(difficulty);

        while (game.getDepth() < depth) {
            Enemy boss = game.getBoss();
            if (boss != null) {
                player.setTile(boss.getTileX() + 1, boss.getTileY());
                for (int i = 0; i < 800 && boss.isAlive(); i++) {
                    game.playerAttacks();
                }
            }
            player.setTile(game.getStairs());
            assertTrue(game.descend(), "Inis calismali");
        }

        return game.getEnemies().stream().mapToInt(Enemy::getMaxHp).max().orElse(0);
    }

    @Test
    @DisplayName("Kademeler sirayla donuyor")
    void difficultyCyclesBothWays() {
        assertEquals(Difficulty.NORMAL, Difficulty.KOLAY.next());
        assertEquals(Difficulty.KOLAY, Difficulty.NORMAL.previous());
        assertEquals(Difficulty.KOLAY, Difficulty.ZOR.next(), "Sondan basa sarmali");
        assertEquals(Difficulty.ZOR, Difficulty.KOLAY.previous(), "Bastan sona sarmali");
    }

    @Test
    @DisplayName("Otomatik kaydetme varsayilan olarak acik")
    void autoSaveDefaultsToOn() {
        assertTrue(new Settings().isAutoSave(),
                "F5'i bilmeyen oyuncunun ilk olumde her seyi kaybetmesi kotu bir karsilama olurdu");
    }
}
