package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Imp;
import com.cryptdelver.entity.Orc;
import com.cryptdelver.entity.Player;
import com.cryptdelver.entity.Skeleton;
import com.cryptdelver.world.Dungeon;
import com.cryptdelver.world.Tile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Arkadan vurus: kacis adiminin saldiri tarafi.
 *
 * <p>Sicrama simdiye kadar tamamen savunmaydi -- kaciyordun, o kadar. Arkadan
 * inen vurus onu bir <em>hamleye</em> ceviriyor: dusmanin yanindan sicrayip
 * arkasina dusuyorsun ve daha donemeden vuruyorsun. Buradaki sinavlar
 * "arka"nin gercekten arka oldugunu ve odulun yalnizca orada verildigini
 * kovaliyor.</p>
 */
class BackstabTest {

    private static final double FRAME = 1.0 / 60;

    /** Hasar karsilastirmasinda kac vurus toplanacak. */
    private static final int ROUNDS = 40;

    private Player player;
    private Game game;

    @BeforeEach
    void setUp() {
        Dungeon dungeon = new Dungeon(21, 11);
        dungeon.fill(Tile.FLOOR);
        player = new Player(10, 5);
        game = new Game(dungeon, player);
    }

    /**
     * Dusmani verilen yone baktirir: bir adim attirmak yonu de degistiriyor,
     * cunku yon hareketin kendisinden cikiyor.
     */
    private void turn(com.cryptdelver.entity.Enemy enemy, int dx, int dy) {
        enemy.beginStep(enemy.getTileX() + dx, enemy.getTileY() + dy);
        enemy.setTile(enemy.getTileX(), enemy.getTileY());
    }

    /**
     * Hic adim atmamis bir dusmanin yonu keyfi olurdu ve oyuncu yari yariya
     * bedava arkadan vurus kazanirdi. Bedava olan sey hamle degildir.
     */
    @Test
    @DisplayName("Kata giren dusman oyuncuya bakiyor")
    void arrivingEnemiesLookAtYou() {
        Imp imp = new Imp(14, 5);
        game.addEnemy(imp);

        assertEquals(-1, imp.getFacingX(), "Oyuncu solunda kaldi");
        assertFalse(game.hasOpeningOn(imp));
    }

    @Test
    @DisplayName("Adim atan dusman o yone bakiyor")
    void steppingTurnsYou() {
        Imp imp = new Imp(5, 5);

        turn(imp, -1, 0);

        assertEquals(-1, imp.getFacingX());
        assertEquals(0, imp.getFacingY());
    }

    @Test
    @DisplayName("Sirtini dondugu sey arkasinda sayiliyor")
    void whatIsBehindIsBehind() {
        Imp imp = new Imp(5, 5);
        turn(imp, 1, 0);

        player.setTile(4, 5);
        assertTrue(imp.isBehind(player), "Sagi bakan impin solu arkasidir");

        player.setTile(6, 5);
        assertFalse(imp.isBehind(player), "Baktigi yon arkasi degil");
    }

    /**
     * Yan taraf arka sayilmiyor. Yanina gecmek kolay, arkasina gecmek bir
     * hamle gerektiriyor -- odul de ona gore.
     */
    @Test
    @DisplayName("Yan taraf arka sayilmiyor")
    void theFlankIsNotTheBack() {
        Imp imp = new Imp(5, 5);
        turn(imp, 1, 0);

        player.setTile(5, 4);
        assertFalse(imp.isBehind(player));

        player.setTile(5, 6);
        assertFalse(imp.isBehind(player));
    }

    /**
     * Hasarda sapma var, yani tek bir vurus esitlik verebiliyor: onden gelen
     * yuksek zar, arkadan gelen dusuk zarla ayni sayiyi tutturabilir. Kirk
     * vurusun toplaminda boyle bir tesaduf yok.
     */
    @Test
    @DisplayName("Arkadan vurus daha sert iniyor")
    void theBackstabHitsHarder() {
        int fromFront = totalDamage(11, -1, ROUNDS);
        int fromBehind = totalDamage(9, -1, ROUNDS);

        assertTrue(fromBehind > fromFront,
                "Arkadan vurulan daha cok kaybetmeli: " + fromBehind + " / " + fromFront);
    }

    /**
     * Ayni vurusu defalarca tekrarlayip toplam hasari verir.
     *
     * <p>Her turda taze bir iskelet: ayni iskelete vurmak onu oldururdu ve
     * olcum yarida kalirdi.</p>
     *
     * @param x dusmanin oyuncuya gore konumu
     * @param facingX dusmanin baktigi yon
     */
    private int totalDamage(int x, int facingX, int rounds) {
        int total = 0;

        for (int round = 0; round < rounds; round++) {
            Skeleton bones = new Skeleton(x, 5);
            game.addEnemy(bones);
            turn(bones, facingX, 0);

            game.playerAttacks();
            total += bones.getMaxHp() - bones.getHp();
            game.removeEnemy(bones);
        }
        return total;
    }

    @Test
    @DisplayName("Acik sirt oyuna sorulabiliyor")
    void theOpeningIsVisibleToTheScreen() {
        Orc orc = new Orc(11, 5);
        game.addEnemy(orc);
        turn(orc, 1, 0);

        assertTrue(game.hasOpeningOn(orc), "Oyuncu orkun arkasinda");

        turn(orc, -1, 0);
        assertFalse(game.hasOpeningOn(orc), "Ork donunce firsat kapaniyor");
    }

    /** Uzaktan arkadan vurmak da sayilsaydi "arkasina gecmek" diye bir hamle kalmazdi. */
    @Test
    @DisplayName("Uzaktaki dusmanin sirti acik sayilmiyor")
    void distanceClosesTheOpening() {
        Imp imp = new Imp(15, 5);
        game.addEnemy(imp);
        turn(imp, 1, 0);

        assertFalse(game.hasOpeningOn(imp), "Bes kare oteden arkadan vurus yok");
    }

    /**
     * Kolunu kaldirmis ork yerinden kipirdamiyor, yani arkasi acik kaliyor.
     * Iki mekanigin bulustugu yer burasi.
     */
    @Test
    @DisplayName("Hazirlanan orkun arkasina gecilebiliyor")
    void thewindupLeavesTheBackOpen() {
        Orc orc = new Orc(11, 5);
        game.addEnemy(orc);

        // Ork oyuncuya dogru donup kolunu kaldiriyor.
        turn(orc, -1, 0);
        for (int i = 0; i < 60 && !orc.isWindingUp(); i++) {
            game.update(FRAME);
        }
        assertTrue(orc.isWindingUp());

        // Oyuncu sicrayip arkasina gecince ork donemiyor: hazirlanirken
        // yurumuyor.
        player.setTile(12, 5);
        game.update(FRAME);

        assertTrue(orc.isWindingUp(), "Isaret hala surmeli");
        assertTrue(game.hasOpeningOn(orc), "Hazirlanan dusman donemez");
    }

    @Test
    @DisplayName("Oyuncu tusa dokununca adim atmadan doniyor")
    void thePlayerTurnsInPlace() {
        player.setMoveInput(0, -1);
        player.setMoveInput(0, 0);

        assertEquals(0, player.getFacingX());
        assertEquals(-1, player.getFacingY(), "Tus birakilinca yon korunmali");
    }
}
