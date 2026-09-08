package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Player;
import com.cryptdelver.entity.Potion;
import com.cryptdelver.entity.Skeleton;
import com.cryptdelver.world.Dungeon;
import com.cryptdelver.world.Tile;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Duraklatma ve ses olaylari.
 *
 * <p>Ses testte gercekten calmiyor: oyun yalnizca "ne oldugunu" bildiriyor,
 * biz de kaydeden sahte bir dinleyici takiyoruz. Kurallarin ses
 * kutuphanesinden bagimsiz kalmasinin karsiligi bu.</p>
 */
class PauseAndSoundTest {

    private static final double FRAME = 1.0 / 60;

    private Player player;
    private Game game;
    private List<SoundEffect> heard;

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

        heard = new ArrayList<>();
        game.setSoundListener(heard::add);
    }

    private void simulateFrames(int frames) {
        for (int i = 0; i < frames; i++) {
            game.update(FRAME);
        }
    }

    @Test
    @DisplayName("Duraklatilan oyunda hicbir sey ilerlemez")
    void pauseFreezesEverything() {
        game.addEnemy(new Skeleton(10, 4));
        player.setMoveInput(1, 0);

        game.togglePause();
        assertTrue(game.isPaused());

        double timeBefore = game.getElapsedSeconds();
        int tileBefore = player.getTileX();
        int enemyTileBefore = game.getEnemies().get(0).getTileX();

        simulateFrames(120);

        assertEquals(tileBefore, player.getTileX(), "Oyuncu duraklatmada yurumemeli");
        assertEquals(enemyTileBefore, game.getEnemies().get(0).getTileX(),
                "Dusman da duraklatmada yurumemeli");
        assertEquals(timeBefore, game.getElapsedSeconds(), 1e-9, "Sure islememeli");
    }

    @Test
    @DisplayName("Duraklatma acilinca oyun kaldigi yerden devam eder")
    void unpauseResumesTheGame() {
        player.setMoveInput(1, 0);
        game.togglePause();
        simulateFrames(30);

        game.togglePause();
        assertFalse(game.isPaused());
        simulateFrames(30);

        assertTrue(player.getTileX() > 4, "Devam edince yurumeli");
    }

    @Test
    @DisplayName("Olu oyunda duraklatma acilmaz")
    void deadGameCannotBePaused() {
        player.takeDamage(player.getMaxHp());

        game.togglePause();

        assertFalse(game.isPaused(), "Oyun bittiyse duraklatmanin anlami yok");
    }

    @Test
    @DisplayName("Vurus ve isabet ses olayi uretir")
    void attackingMakesSound() {
        game.addEnemy(new Skeleton(5, 4));

        player.requestAttack();
        game.update(FRAME);

        assertTrue(heard.contains(SoundEffect.SWING), "Savurus duyulmali");
        assertTrue(heard.contains(SoundEffect.HIT), "Isabet duyulmali");
    }

    @Test
    @DisplayName("Bosa savurusta isabet sesi cikmaz")
    void missingMakesOnlySwingSound() {
        player.requestAttack();
        game.update(FRAME);

        assertTrue(heard.contains(SoundEffect.SWING));
        assertFalse(heard.contains(SoundEffect.HIT), "Kimseye degmedi");
    }

    @Test
    @DisplayName("Hasar almak ve olmek ayri sesler cikarir")
    void takingDamageAndDyingMakeSounds() {
        game.addEnemy(new Skeleton(5, 4));

        simulateFrames(60 * 6);

        assertTrue(heard.contains(SoundEffect.HURT), "Hasar sesi duyulmali");
        if (game.isOver()) {
            assertTrue(heard.contains(SoundEffect.DEATH), "Olum sesi duyulmali");
        }
    }

    @Test
    @DisplayName("Iksir ve ekipman farkli sesler cikarir")
    void itemsMakeDistinctSounds() {
        player.takeDamage(5);
        game.getInventory().add(new Potion(0, 0));
        game.getInventory().add(LootTable.weaponForTier(1, 0, 0));

        game.useItem(0);
        assertTrue(heard.contains(SoundEffect.POTION), "Iksir sesi");

        game.useItem(0);
        assertTrue(heard.contains(SoundEffect.EQUIP), "Kusanma sesi");
    }

    @Test
    @DisplayName("Ses dinleyicisi takilmadan oyun calisir")
    void gameRunsWithoutAnyListener() {
        Game silent = new Game(game.getDungeon(), new Player(4, 4));

        silent.getPlayer().requestAttack();
        silent.update(FRAME);

        assertFalse(silent.isOver(), "Sessiz oyun da sorunsuz ilerlemeli");
    }
}
