package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Enemy;
import com.cryptdelver.entity.Player;
import com.cryptdelver.world.BspGenerator;
import com.cryptdelver.world.DungeonGenerator;
import com.cryptdelver.world.RandomWalkGenerator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Katin altinda donen ortam sesi.
 *
 * <p>Bolgeler renk disinda birbirinden ayrilmiyordu: Sarnic yesil bir
 * Mahzendi. Ses o farki renkten daha guclu tasiyor, o yuzden hangi katta
 * hangi sesin istendigi bir kural ve testi hak ediyor.</p>
 *
 * <p>Ses kutuphanesine hic dokunulmuyor: sahte bir dinleyici yalnizca ne
 * istendigini not ediyor. Kurallarin pencere acmadan sinanabilmesi
 * {@link SoundListener}'in var olus sebebi.</p>
 */
class AmbienceTest {

    /** Istenen ortam seslerini sirasiyla not eden sahte dinleyici. */
    private static final class Ear implements SoundListener {

        private final List<Ambience> requested = new ArrayList<>();
        private boolean stopped;

        @Override
        public void play(SoundEffect effect) {
            // Efektler bu testin konusu degil.
        }

        @Override
        public void playAmbience(Ambience ambience) {
            requested.add(ambience);
            stopped = false;
        }

        @Override
        public void stopAmbience() {
            stopped = true;
        }

        private Ambience last() {
            return requested.isEmpty() ? null : requested.get(requested.size() - 1);
        }
    }

    private Player player;
    private Game game;
    private Ear ear;

    private static List<DungeonGenerator> generators() {
        return List.of(new BspGenerator(), new RandomWalkGenerator());
    }

    @BeforeEach
    void setUp() {
        player = new Player(0, 0);
        game = new Game(generators(), 40, 22, player);
        ear = new Ear();
        game.setSoundListener(ear);
    }

    /** Kati temizleyip iner; boss katlarinda merdiven kilitli oldugu icin gerekli. */
    private void clearAndDescend() {
        while (!game.getEnemies().isEmpty()) {
            Enemy target = game.getEnemies().get(0);
            player.setTile(target.getTileX() + 1, target.getTileY());
            for (int i = 0; i < 600 && target.isAlive(); i++) {
                game.playerAttacks();
            }
        }

        player.setTile(game.getStairs());
        assertTrue(game.descend(), "Inis calismali");
    }

    private void descendTo(int depth) {
        while (game.getDepth() < depth) {
            clearAndDescend();
        }
    }

    /**
     * Dinleyici oyun kurulduktan sonra takiliyor. Ilk katin sesi burada
     * baslatilmazsa oyuncu ikinci kata inene kadar sessizlik duyar.
     */
    @Test
    @DisplayName("Dinleyici takilinca ilk katin sesi basliyor")
    void attachingTheListenerStartsTheFirstFloor() {
        assertEquals(Ambience.MAHZEN, ear.last());
    }

    @Test
    @DisplayName("Her bolgenin kendi sesi var")
    void everyRegionHasItsOwnSound() {
        assertEquals(Ambience.MAHZEN, Ambience.forTheme(FloorTheme.MAHZEN));
        assertEquals(Ambience.SARNIC, Ambience.forTheme(FloorTheme.SARNIC));
        assertEquals(Ambience.KORLUK, Ambience.forTheme(FloorTheme.KORLUK));
        assertEquals(Ambience.KRIPT, Ambience.forTheme(FloorTheme.KRIPT));
    }

    /**
     * Muzigin degismesi, oyuncunun "burasi baska bir yer" diye anladigi ilk
     * sey: merdivenden inip bunu duydugunda ne olacagini daha bossu gormeden
     * biliyorsun.
     */
    @Test
    @DisplayName("Boss katinda bolge sesinin yerine gerilim geciyor")
    void bossFloorsReplaceTheRegionSound() {
        descendTo(5);

        assertEquals(Ambience.BOSS, ear.last());
    }

    @Test
    @DisplayName("Boss kati gecilince bolge sesine donuluyor")
    void theRegionSoundReturnsAfterTheBoss() {
        descendTo(6);

        assertEquals(Ambience.SARNIC, ear.last(), "6. kat Sarnic");
    }

    /** Geri donerken de dogru ses: kat hafizasi sesi de kapsamali. */
    @Test
    @DisplayName("Yukari cikinca da ses kata uyuyor")
    void goingBackUpRestoresTheSound() {
        descendTo(5);
        assertEquals(Ambience.BOSS, ear.last());

        assertTrue(game.ascend());

        assertEquals(Ambience.MAHZEN, ear.last(), "4. kat Mahzen");
    }

    /** Sessizlik, olumu ekrandaki yazidan daha net anlatiyor. */
    @Test
    @DisplayName("Olunce zemin susuyor")
    void deathSilencesTheFloor() {
        player.takeDamage(player.getMaxHp());
        game.enemyAttacksPlayer(game.getEnemies().get(0));

        assertTrue(ear.stopped, "Olumde zemin durmali");
    }

    @Test
    @DisplayName("Sessiz dinleyici hicbir sey istemiyor")
    void theSilentListenerAsksForNothing() {
        Game quiet = new Game(generators(), 40, 22, new Player(0, 0));
        quiet.setSoundListener(null);

        // Sessize dusen oyun yine calisiyor; burada aranan sey cokmemesi.
        assertNull(new Ear().last());
        assertEquals(1, quiet.getDepth());
    }
}
