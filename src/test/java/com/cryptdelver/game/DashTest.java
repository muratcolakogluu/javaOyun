package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Armor;
import com.cryptdelver.entity.Enchantment;
import com.cryptdelver.entity.Imp;
import com.cryptdelver.entity.Player;
import com.cryptdelver.world.Dungeon;
import com.cryptdelver.world.Tile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Kacis adimi: oyuncunun ikinci fiili.
 *
 * <p>Yirmi kat boyunca elindeki tek sey "yuru ve vur"du. Dusman tarafi
 * zenginlesse de her dovusun cevabi ayni kaliyordu: menzile gir, bosluga bas.
 * Buradaki testler sicramanin gercekten bir <em>hamle</em> oldugunu
 * dogruluyor -- duvardan gecmiyor, bedava degil, ve yon secimi oyuncunun.</p>
 */
class DashTest {

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

    /** Sicramayi isleten sey oyun donugusu; tek cerceve yetiyor. */
    private void tick() {
        game.update(FRAME);
    }

    private void simulate(double seconds) {
        for (int i = 0; i < seconds / FRAME; i++) {
            game.update(FRAME);
        }
    }

    @Test
    @DisplayName("Baktigin yone uc kare sicriyorsun")
    void dashLeapsThreeTiles() {
        player.setMoveInput(1, 0);
        player.setMoveInput(0, 0);

        player.requestDash();
        tick();

        assertEquals(7, player.getTileX(), "Uc kare saga");
        assertEquals(4, player.getTileY());
    }

    @Test
    @DisplayName("Yon degisince sicrama da yon degistiriyor")
    void dashFollowsFacing() {
        player.setMoveInput(0, -1);
        player.setMoveInput(0, 0);

        player.requestDash();
        tick();

        assertEquals(4, player.getTileX());
        assertEquals(1, player.getTileY(), "Uc kare yukari");
    }

    /** Duvardan gecmek kacis degil, duvarlari yok sayan bir hile olurdu. */
    @Test
    @DisplayName("Duvarin onunde duruyor, icinden gecmiyor")
    void wallsStopTheDash() {
        player.setTile(11, 4);
        player.setMoveInput(1, 0);
        player.setMoveInput(0, 0);

        player.requestDash();
        tick();

        assertEquals(13, player.getTileX(), "Duvardan onceki son bos kare");
        assertTrue(game.getDungeon().isWalkable(player.getTileX(), player.getTileY()));
    }

    @Test
    @DisplayName("Dusmanin icinden de gecmiyor")
    void enemiesStopTheDash() {
        game.addEnemy(new Imp(6, 4));
        player.setMoveInput(1, 0);
        player.setMoveInput(0, 0);

        player.requestDash();
        tick();

        assertEquals(5, player.getTileX(), "Dusmandan onceki kare");
    }

    /** Onu kapaliysa sicrama hic olmuyor; bekleme suresi de harcanmiyor. */
    @Test
    @DisplayName("Sicracak yer yoksa bekleme harcanmiyor")
    void ablockedDashCostsNothing() {
        player.setTile(13, 4);
        player.setMoveInput(1, 0);
        player.setMoveInput(0, 0);

        player.requestDash();
        tick();

        assertEquals(13, player.getTileX(), "Yerinde kaldi");
        assertTrue(player.canDash(), "Bosa giden sicrama bekleme baslatmamali");
    }

    /**
     * Bekleme suresi mekanigin kendisi kadar onemli: serbest olsaydi kacis
     * adimi yurumenin yerine gecer ve konum bir karar olmaktan cikardi.
     */
    @Test
    @DisplayName("Sicradiktan sonra bir sure hazir degil")
    void dashHasACooldown() {
        player.setMoveInput(1, 0);
        player.setMoveInput(0, 0);
        player.requestDash();
        tick();

        assertFalse(player.canDash());

        int landed = player.getTileX();
        player.requestDash();
        tick();

        assertEquals(landed, player.getTileX(), "Bekleme bitmeden ikinci sicrama yok");
    }

    @Test
    @DisplayName("Bekleme dolunca yeniden sicranabiliyor")
    void theCooldownRunsOut() {
        player.requestDash();
        tick();
        assertFalse(player.canDash());

        simulate(3.0);

        assertTrue(player.canDash());
    }

    /** Ceviklik zaten "daha hizli yuruyorsun" diyordu; artik "daha sik siciyorsun" da diyor. */
    @Test
    @DisplayName("Ceviklik beklemeyi kisaltiyor")
    void agilityShortensTheCooldown() {
        double plain = player.getDashCooldown();

        Armor mail = new Armor(0, 0, "Test Zirhi", 2, "armor_chain", 40);
        mail.enchant(Enchantment.CEVIKLIK);
        player.equip(mail);

        assertTrue(player.getDashCooldown() < plain, "Buyu beklemeyi kisaltmali");
    }

    @Test
    @DisplayName("Olu oyuncu sicramiyor")
    void theDeadDoNotDash() {
        player.takeDamage(player.getMaxHp());
        int where = player.getTileX();

        player.requestDash();
        tick();

        assertEquals(where, player.getTileX());
    }

    /** Sicrama anlik; ekran nereden geldigini gostersin diye iz birakiyor. */
    @Test
    @DisplayName("Sicrama arkasinda bir iz birakiyor")
    void dashLeavesATrail() {
        player.setMoveInput(1, 0);
        player.setMoveInput(0, 0);
        int from = player.getTileX();

        player.requestDash();
        tick();

        assertEquals(from, player.getDashFromX(), "Iz nereden geldigini tutmali");
        assertTrue(player.getDashTrail() > 0);
    }

    @Test
    @DisplayName("Yeniden baslayinca sicrama hazir")
    void restartResetsTheDash() {
        player.requestDash();
        tick();
        assertFalse(player.canDash());

        player.restore();

        assertTrue(player.canDash());
    }
}
