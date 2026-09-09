package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Player;
import com.cryptdelver.world.BspGenerator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Katta oyalanmanin bedeli.
 *
 * <p>Bu olmadan kati tamamen temizlemek her zaman en dogru hamleydi: dusmanlar
 * yeniden dogmuyor, ganimet sinirli, yani kalmanin hicbir riski yoktu. Bu da
 * butun ekonomiyi zayiflatiyordu -- yipranma, altin, iksir saklamak, hepsi
 * "zaten hepsini alirim" diye cozuluyordu.</p>
 */
class ReinforcementTest {

    private static final double FRAME = 1.0 / 60;

    private Player player;
    private Game game;

    @BeforeEach
    void setUp() {
        player = new Player(0, 0);
        game = new Game(List.of(new BspGenerator()), 40, 22, player);
    }

    /**
     * Oyunu belirtilen saniye kadar ilerletir ve oyuncuyu ayakta tutar.
     *
     * <p>Can doldurmak sarttir: takviyeler ancak dakikalar sonra geliyor,
     * oysa taban canli ve zirhsiz bir oyuncu sekiz dusmanin ortasinda bir
     * dakika yasamiyor. Olen oyuncuda sayac zaten durduğu icin test hicbir
     * sey olcemezdi.</p>
     */
    private void simulate(double seconds) {
        for (int i = 0; i < (int) (seconds * 60); i++) {
            player.heal(player.getMaxHp());
            game.update(FRAME);
        }
    }

    @Test
    @DisplayName("Kat yeni acilmisken zindan uykuda")
    void theDungeonStartsAsleep() {
        simulate(5);

        assertFalse(game.isDungeonAwake(), "Bes saniyede uyanmamali");
    }

    @Test
    @DisplayName("Uzun sure kalinca zindan uyaniyor")
    void lingeringWakesTheDungeon() {
        simulate(95);

        assertTrue(game.isDungeonAwake());
        assertTrue(game.getMessageLog().latest(10).stream()
                        .anyMatch(line -> line.contains("fark etti")),
                "Oyuncu uyarilmali");
    }

    @Test
    @DisplayName("Uyanik zindan takviye gonderiyor")
    void anAwakeDungeonSendsReinforcements() {
        int before = game.getEnemies().size();

        simulate(140);

        assertTrue(game.getEnemies().size() > before,
                "Dusman sayisi artmali: " + before + " -> " + game.getEnemies().size());
    }

    /** Takviyeler oyuncunun tepesinde belirmiyor; tehdit uzaktan geliyor. */
    @Test
    @DisplayName("Takviyeler uzakta doguyor")
    void reinforcementsArriveFromAfar() {
        simulate(140);

        game.getEnemies().forEach(enemy ->
                assertTrue(enemy.getTile().manhattanDistance(player.getTile()) > 0,
                        "Dusman oyuncunun uzerinde dogmamali"));
    }

    /** Asagi inmek sayaci sifirliyor: ilerlemek gercekten rahatlatiyor. */
    @Test
    @DisplayName("Kat degistirince zindan yeniden uykuya daliyor")
    void descendingResetsThePatience() {
        simulate(95);
        assertTrue(game.isDungeonAwake());

        player.setTile(game.getStairs());
        assertTrue(game.descend());

        assertFalse(game.isDungeonAwake(), "Yeni kat bastan sayiyor");
        assertEquals(0, game.getFloorSeconds(), 1e-6);
    }

    /** Kalabalik sinirsiz buyumuyor; kat oynanamaz hale gelmemeli. */
    @Test
    @DisplayName("Takviyelerin bir siniri var")
    void reinforcementsHaveACeiling() {
        simulate(600);

        assertTrue(game.getEnemies().size() <= 24,
                "Sinir asilmamali: " + game.getEnemies().size());
    }

    @Test
    @DisplayName("Duraklatilmisken sayac islemiyor")
    void pausingStopsTheClock() {
        game.togglePause();

        simulate(120);

        assertFalse(game.isDungeonAwake(), "Duraklatma zindani uyandirmamali");
    }
}
