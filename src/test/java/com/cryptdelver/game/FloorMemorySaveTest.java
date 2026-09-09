package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Enemy;
import com.cryptdelver.entity.Player;
import com.cryptdelver.persistence.SaveData;
import com.cryptdelver.world.BspGenerator;
import com.cryptdelver.world.Dungeon;
import com.cryptdelver.world.DungeonGenerator;
import com.cryptdelver.world.Position;
import com.cryptdelver.world.RandomWalkGenerator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Gezilmis katlarin kayitta yasamasi.
 *
 * <p>Geri donus geldiginde kat hafizasini yalnizca oyun icinde tutuyorduk:
 * kaydedip yukleyince gezilen katlar unutuluyor, geri donulen kat sifirdan
 * uretiliyordu. Yani oyun icinde kapattigimiz "cik-in, ganimet yenilensin"
 * kapisi kaydet-yukle ile hala acikti. Burasi o kapinin kapali kaldigini
 * dogruluyor.</p>
 */
class FloorMemorySaveTest {

    private static final int WIDTH = 40;
    private static final int HEIGHT = 24;

    private Player player;
    private Game game;

    private static List<DungeonGenerator> generators() {
        return List.of(new BspGenerator(), new RandomWalkGenerator());
    }

    @BeforeEach
    void setUp() {
        player = new Player(0, 0);
        game = new Game(generators(), WIDTH, HEIGHT, player);
    }

    private void goDown() {
        player.setTile(game.getStairs());
        assertTrue(game.descend(), "Inis calismali");
    }

    /** Kaydi yepyeni bir oyuna yukler; "zaten ayni nesneler duruyordu" yanilgisi olmasin. */
    private Game reload() {
        SaveData data = game.captureSave();
        Game loaded = new Game(generators(), WIDTH, HEIGHT, new Player(0, 0));
        loaded.applySave(data);
        return loaded;
    }

    /** Kattaki her karenin ayni olmasi: harita gercekten ayni tohumdan gelmis mi. */
    private void assertSameMap(Dungeon expected, Dungeon actual) {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                assertEquals(expected.getTile(x, y), actual.getTile(x, y),
                        "Kare farkli: (" + x + ", " + y + ")");
            }
        }
    }

    private void clearFloor() {
        while (!game.getEnemies().isEmpty()) {
            Enemy target = game.getEnemies().get(0);
            player.setTile(target.getTileX() + 1, target.getTileY());
            for (int i = 0; i < 400 && target.isAlive(); i++) {
                game.playerAttacks();
            }
        }
    }

    @Test
    @DisplayName("Yuklenen oyunda ust kat ayni harita")
    void theFloorAboveComesBackFromTheSave() {
        Position stairsAbove = game.getStairs();
        Dungeon mapAbove = game.getDungeon();
        goDown();

        Game loaded = reload();
        assertTrue(loaded.ascend(), "Yukleyince de yukari cikilabilmeli");

        assertEquals(1, loaded.getDepth());
        assertEquals(stairsAbove, loaded.getStairs(), "Merdiven ayni karede");
        assertSameMap(mapAbove, loaded.getDungeon());
    }

    /**
     * Asil mesele bu: temizledigin kat kaydet-yukle sonrasi da temiz kalmali.
     * Aksi halde her kayit yuklemesi yeni bir ganimet turu demek olurdu.
     */
    @Test
    @DisplayName("Temizlenen kat kayittan sonra da temiz")
    void aClearedFloorStaysClearedAcrossASave() {
        clearFloor();
        goDown();

        Game loaded = reload();
        loaded.ascend();

        assertTrue(loaded.getEnemies().isEmpty(), "Oldurulenler kayit yuklenince dirilmemeli");
    }

    @Test
    @DisplayName("Ust kattaki yer esyalari kayittan geliyor")
    void groundLootOnTheFloorAboveIsRemembered() {
        int itemsAbove = game.getGroundItems().size();
        goDown();

        Game loaded = reload();
        loaded.ascend();

        assertEquals(itemsAbove, loaded.getGroundItems().size(),
                "Yerdeki esya sayisi degismemeli");
    }

    /** Kayit alinan kattaki kesif de dosyada: yukleyen oyuncu koridorlari yeniden aramiyor. */
    @Test
    @DisplayName("Bulunulan katin kesfi kayitta duruyor")
    void explorationOfTheCurrentFloorSurvives() {
        Position spawn = player.getTile();
        assertTrue(game.getVision().isRemembered(spawn.x(), spawn.y()));

        Game loaded = reload();

        assertTrue(loaded.getVision().isRemembered(spawn.x(), spawn.y()),
                "Gezdigin yer kayittan sonra da acik olmali");
    }

    @Test
    @DisplayName("Gezilmis katin kesfi de kayitta duruyor")
    void explorationOfAVisitedFloorSurvives() {
        Position spawn = player.getTile();
        goDown();

        Game loaded = reload();
        loaded.ascend();

        assertTrue(loaded.getVision().isRemembered(spawn.x(), spawn.y()),
                "Ust katin kesfi unutulmamali");
    }

    /** Gormedigin yer hala karanlik: kayit haritayi acmiyor, yalnizca gezdigini hatirliyor. */
    @Test
    @DisplayName("Gorulmemis kareler kayittan sonra da karanlik")
    void unseenTilesStayDark() {
        Game loaded = reload();

        int unseen = 0;
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                if (!loaded.getVision().isRemembered(x, y)) {
                    unseen++;
                }
            }
        }

        assertNotEquals(0, unseen, "Butun kat acilmis olamaz");
    }

    /** Kayittan once ve sonra ayni katin ayni tohumu: kat gercekten hatirlaniyor. */
    @Test
    @DisplayName("Gezilmis katin tohumu degismiyor")
    void theRememberedFloorKeepsItsSeed() {
        long firstFloorSeed = game.getCurrentSeed();
        goDown();

        Game loaded = reload();
        loaded.ascend();

        assertEquals(firstFloorSeed, loaded.getCurrentSeed());
    }
}
