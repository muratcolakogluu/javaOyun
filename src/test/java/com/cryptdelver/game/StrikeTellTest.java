package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Enemy;
import com.cryptdelver.entity.Imp;
import com.cryptdelver.entity.Orc;
import com.cryptdelver.entity.Player;
import com.cryptdelver.entity.Saman;
import com.cryptdelver.entity.Skeleton;
import com.cryptdelver.world.Dungeon;
import com.cryptdelver.world.Tile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Kalkan kol: agir vuranlarin okunabilir hazirlik ani.
 *
 * <p>Orkun yaninda durmak saf bir hesapti: canin yetiyorsa vurusurdun,
 * yetmiyorsa olurdun ve arada verilecek bir karar yoktu. Isaret o karari
 * geri veriyor -- bir adim geri cekil, vurus bosa gitsin, sonra geri gir.
 * Buradaki sinavlar isaretin gercekten <b>kacinilabilir</b> oldugunu
 * kovaliyor: gorunuyor mu, kacinca bosa gidiyor mu, kalinca iniyor mu.</p>
 */
class StrikeTellTest {

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

        player = new Player(10, 5);
        game = new Game(dungeon, player);
    }

    private void simulate(double seconds) {
        for (int i = 0; i < seconds / FRAME; i++) {
            game.update(FRAME);
        }
    }

    /** Dusman kolunu kaldirana kadar isletir. */
    private void runUntilWindup(Enemy enemy) {
        for (int i = 0; i < 600 && !enemy.isWindingUp(); i++) {
            game.update(FRAME);
        }
        assertTrue(enemy.isWindingUp(), "Yanindaki dusman vurusa hazirlanmaliydi");
    }

    @Test
    @DisplayName("Ork vurmadan once hazirlaniyor")
    void theOrcRaisesItsAxeFirst() {
        Orc orc = new Orc(11, 5);
        game.addEnemy(orc);
        int before = player.getHp();

        game.update(FRAME);

        assertTrue(orc.isWindingUp(), "Ilk karede kol kalkmali");
        assertEquals(before, player.getHp(), "Hazirlik aninda hasar yok");
    }

    /**
     * Isaretin gorulmesi mekanigin kendisi kadar onemli: gorunmeyen bir
     * hazirlik, oyuncu icin yalnizca gecikmis bir vurus olurdu.
     */
    @Test
    @DisplayName("Hazirligin ne kadari gectigi disaridan okunabiliyor")
    void theWindupIsReadable() {
        Orc orc = new Orc(11, 5);
        game.addEnemy(orc);

        game.update(FRAME);
        double early = orc.getWindupProgress();

        simulate(0.3);
        double later = orc.getWindupProgress();

        assertTrue(early >= 0 && early < 0.2, "Kol yeni kalkti: " + early);
        assertTrue(later > early, "Ilerleme artmali");
        assertTrue(later <= 1.0);
    }

    /** Kacmanin anlami bu: kol iniyor ama bosluga. */
    @Test
    @DisplayName("Geri cekilince vurus bosa gidiyor")
    void steppingBackMakesTheBlowMiss() {
        Orc orc = new Orc(11, 5);
        game.addEnemy(orc);
        int before = player.getHp();

        runUntilWindup(orc);

        // Bir adim geri: ork hala isaretteyken menzilden cikiyoruz.
        player.setTile(6, 5);
        simulate(1.0);

        assertEquals(before, player.getHp(), "Bosa giden vurus hasar vermemeli");
        assertFalse(orc.isWindingUp(), "Vurus tamamlanmis olmali");
    }

    @Test
    @DisplayName("Yerinde kalirsan vurus iniyor")
    void standingStillTakesTheHit() {
        Orc orc = new Orc(11, 5);
        game.addEnemy(orc);
        int before = player.getHp();

        runUntilWindup(orc);
        simulate(1.0);

        assertTrue(player.getHp() < before, "Kacmayan oyuncu hasar almali");
    }

    /**
     * Isaretin ekranda cizildigi yerle vurusun ciktigi yer ayni kare olmali,
     * yoksa uyari yalan soylerdi. Bogucuda ogrendigimiz dersin aynisi.
     */
    @Test
    @DisplayName("Hazirlanan dusman yerinden kipirdamiyor")
    void theWindupPinsTheEnemy() {
        Orc orc = new Orc(11, 5);
        game.addEnemy(orc);

        runUntilWindup(orc);
        int x = orc.getTileX();
        int y = orc.getTileY();

        player.setTile(15, 5);
        simulate(0.3);

        assertTrue(orc.isWindingUp(), "Isaret hala surmeli");
        assertEquals(x, orc.getTileX(), "Hazirlanirken yurumemeli");
        assertEquals(y, orc.getTileY());
    }

    /** Saman da ayni dili konusuyor ama cumlesi daha kisa. */
    @Test
    @DisplayName("Samanin isareti orkunkinden kisa")
    void theShamanIsQuicker() {
        Saman shaman = new Saman(0, 0);
        Orc orc = new Orc(0, 0);

        assertTrue(shaman.getWindup() > 0, "Samanin da isareti olmali");
        assertTrue(shaman.getWindup() < orc.getWindup(),
                "Hizli govde, kisa isaret");
    }

    /**
     * Her dusmana isaret koymak dovusu yavaslatirdi: kucuk vuranlarin
     * hasari zaten dusuk, hazirlanmalari yalnizca bekleme olurdu.
     */
    @Test
    @DisplayName("Hafif vuranlarin isareti yok")
    void lightHittersStrikeWithoutWarning() {
        assertEquals(0.0, new Imp(0, 0).getWindup());
        assertEquals(0.0, new Skeleton(0, 0).getWindup());
    }

    @Test
    @DisplayName("Isaretsiz dusman aninda vuruyor")
    void theUnannouncedBlowLandsAtOnce() {
        Skeleton bones = new Skeleton(11, 5);
        game.addEnemy(bones);
        int before = player.getHp();

        game.update(FRAME);

        assertFalse(bones.isWindingUp());
        assertTrue(player.getHp() < before, "Isaretsiz vurus ilk karede inmeli");
    }

    /**
     * Vurus indikten sonra bekleme basliyor: dovusun ritmi <em>isaret, vurus,
     * nefes</em> olmali. Ikinci kol hemen kalksaydi isaret bir pencere degil
     * yalnizca gorsel bir susleme olurdu.
     */
    @Test
    @DisplayName("Vurustan sonra kol hemen yeniden kalkmiyor")
    void theBlowIsFollowedByABreath() {
        Orc orc = new Orc(11, 5);
        game.addEnemy(orc);

        runUntilWindup(orc);
        simulate(0.7);
        assertFalse(orc.isWindingUp(), "Vurus inmis olmali");

        simulate(0.5);
        assertFalse(orc.isWindingUp(), "Bekleme dolmadan ikinci hazirlik yok");

        simulate(1.2);
        assertTrue(orc.isWindingUp(), "Bekleme dolunca kol yeniden kalkmali");
    }

    @Test
    @DisplayName("Olu dusman hazirlanmiyor")
    void theDeadDoNotSwing() {
        Orc orc = new Orc(11, 5);
        game.addEnemy(orc);

        runUntilWindup(orc);
        orc.takeDamage(orc.getMaxHp());
        int before = player.getHp();

        simulate(1.0);

        assertEquals(before, player.getHp(), "Olen orkun kolu inmemeli");
    }
}
