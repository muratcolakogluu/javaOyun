package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Boss;
import com.cryptdelver.entity.Player;
import com.cryptdelver.world.BspGenerator;
import com.cryptdelver.world.RandomWalkGenerator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Katlarin sirasi.
 *
 * <p>Uretici secimi eskiden oyuncudaydi ({@code G} tusu) ve bu bir hata
 * ayiklama kolayligiydi: zindan bir yol olmaktan cikip ayar penceresine
 * donuyordu. Artik sira sabit ve derinlige bagli; bu testler o sirayi
 * kilitliyor.</p>
 */
class FloorOrderTest {

    private static final int WIDTH = 40;
    private static final int HEIGHT = 22;

    private Player player;
    private Game game;

    @BeforeEach
    void setUp() {
        player = new Player(0, 0);
        game = new Game(List.of(new BspGenerator(), new RandomWalkGenerator()), WIDTH, HEIGHT, player);
    }

    /** Bir kat asagi iner; boss varsa once onu indirir. */
    private void goDownOneFloor() {
        Boss boss = game.getBoss();
        if (boss != null) {
            player.setTile(boss.getTileX() + 1, boss.getTileY());
            for (int i = 0; i < 600 && boss.isAlive(); i++) {
                game.playerAttacks();
            }
        }

        player.setTile(game.getStairs());
        assertTrue(game.descend(), "Inis calismali");
    }

    /** 1. kattan baslayip verilen derinlige kadar uretici adlarini toplar. */
    private List<String> generatorNamesDownTo(int depth) {
        List<String> names = new ArrayList<>();
        names.add(game.getCurrentGenerator().getName());

        while (game.getDepth() < depth) {
            goDownOneFloor();
            names.add(game.getCurrentGenerator().getName());
        }
        return names;
    }

    /**
     * Katin sekli artik bolgenin kimligi.
     *
     * <p>Once tek/cift diye degisiyordu ve bu hicbir sey anlatmiyordu --
     * yalnizca ayni goruntude ust uste inmeni engelliyordu. Kural derinligin
     * saf bir fonksiyonu oldugu icin dogrudan sinaniyor: yirmi kat inmek
     * ayni seyi cok daha yavas dogrulardi.</p>
     */
    @Test
    @DisplayName("Katin sekli bolgesine gore")
    void floorShapeFollowsTheRegion() {
        FloorBuilder builder = new FloorBuilder(
                List.of(new BspGenerator(), new RandomWalkGenerator()), WIDTH, HEIGHT);

        // Mahzen orulmus odalar, Kript yine yapilmis salonlar.
        for (int depth : new int[] {1, 2, 3, 4, 16, 17, 18, 19}) {
            assertEquals(0, builder.generatorForDepth(depth), depth + ". kat odali olmali");
        }

        // Sarnic bastan sona oyulmus magara.
        for (int depth : new int[] {6, 7, 8, 9}) {
            assertEquals(1, builder.generatorForDepth(depth), depth + ". kat magara olmali");
        }
    }

    /** Korluk cokmekte olan bir yer: bazi katlari hala oda, bazilari magara. */
    @Test
    @DisplayName("Korluk iki sekli birden tasiyor")
    void theEmbersMixBothShapes() {
        FloorBuilder builder = new FloorBuilder(
                List.of(new BspGenerator(), new RandomWalkGenerator()), WIDTH, HEIGHT);

        assertEquals(0, builder.generatorForDepth(11));
        assertEquals(1, builder.generatorForDepth(12));
        assertEquals(0, builder.generatorForDepth(13));
        assertEquals(1, builder.generatorForDepth(14));
    }

    /**
     * Boss yavas ama durmak bilmez; vurup geri cekilerek dovusmek icin alan
     * gerekiyor. Magara koridorlarinda sikisip kaliyordun.
     */
    @Test
    @DisplayName("Boss katlari her zaman odali")
    void bossFloorsAlwaysUseRooms() {
        List<String> names = generatorNamesDownTo(5);

        assertTrue(game.isBossFloor(), "5. kat boss kati");
        assertEquals(names.get(0), names.get(4), "Boss kati 1. katla ayni bicimde olmali");
    }

    /** Ayni derinlikte yeniden uretmek bicimi degistirmiyor. */
    @Test
    @DisplayName("Kat yeniden uretilince bicimi degismiyor")
    void regeneratingKeepsTheFloorStyle() {
        goDownOneFloor();
        String before = game.getCurrentGenerator().getName();

        game.regenerateFloor();

        assertEquals(before, game.getCurrentGenerator().getName());
    }
}
