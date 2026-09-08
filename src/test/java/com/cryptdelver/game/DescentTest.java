package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Item;
import com.cryptdelver.entity.Player;
import com.cryptdelver.entity.Potion;
import com.cryptdelver.world.BspGenerator;
import com.cryptdelver.world.Position;
import com.cryptdelver.world.Tile;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Kat inisi ve derinlikle artan zorluk.
 *
 * <p>Burada gercek uretici kullaniliyor: kat uretimi, merdiven yerlestirme ve
 * doldurma zincirinin tamami calisiyor — yine de JavaFX gerekmiyor.</p>
 */
class DescentTest {

    private static final int WIDTH = 40;
    private static final int HEIGHT = 24;

    private Player player;
    private Game game;

    @BeforeEach
    void setUp() {
        player = new Player(0, 0);
        game = new Game(List.of(new BspGenerator()), WIDTH, HEIGHT, player);
    }

    /** Oyuncuyu merdivene tasiyip indirir. */
    private void goDownOneFloor() {
        player.setTile(game.getStairs());
        assertTrue(game.descend(), "Merdivenin ustunde inis calismali");
    }

    @Test
    @DisplayName("Oyun ilk katta baslar")
    void gameStartsAtFirstFloor() {
        assertEquals(1, game.getDepth());
    }

    @Test
    @DisplayName("Her katta bir inis merdiveni var")
    void everyFloorHasStairs() {
        Position stairs = game.getStairs();

        assertNotNull(stairs);
        assertEquals(Tile.STAIRS_DOWN, game.getDungeon().getTile(stairs.x(), stairs.y()));
        assertTrue(game.getDungeon().isWalkable(stairs.x(), stairs.y()), "Merdivene basilabilmeli");
        assertNotEquals(player.getTile(), stairs, "Merdiven dogdugun karede olmamali");
    }

    /**
     * Merdiven, dogma noktasindan yuruyerek gidilebilen <em>en uzak</em> kareye
     * konur.
     *
     * <p>Onceki hali "uzaklik > 10" diye sabit bir esik ariyordu ve zaman zaman
     * patliyordu: rastgele tohum bazen derli toplu bir harita uretiyor ve
     * tasarimda bu esigi garanti eden bir kural yok. Simdi tasarimin gercekten
     * soz verdigi sey olculuyor: baska hicbir kare merdivenden uzak degil.</p>
     */
    @Test
    @DisplayName("Merdiven, dogma noktasindan gidilebilen en uzak kareye konur")
    void stairsSitAtTheFarthestReachableTile() {
        Map<Position, Integer> distances = walkingDistancesFrom(player.getTile());

        Integer stairsDistance = distances.get(game.getStairs());
        assertNotNull(stairsDistance, "Merdivene yuruyerek ulasilabilmeli");

        int farthest = distances.values().stream().max(Integer::compareTo).orElseThrow();
        assertEquals(farthest, stairsDistance.intValue(),
                "Merdiven en uzak karede olmali");
        assertTrue(stairsDistance > 1, "Merdiven dogdugun karenin dibinde olmamali");
    }

    /** Baslangictan her yurunebilir kareye kac adimda gidildigini hesaplar (BFS). */
    private Map<Position, Integer> walkingDistancesFrom(Position start) {
        int[][] directions = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        Map<Position, Integer> distances = new HashMap<>();
        Deque<Position> queue = new ArrayDeque<>();

        distances.put(start, 0);
        queue.add(start);

        while (!queue.isEmpty()) {
            Position current = queue.poll();
            for (int[] direction : directions) {
                Position next = current.offset(direction[0], direction[1]);
                if (game.getDungeon().isWalkable(next.x(), next.y()) && !distances.containsKey(next)) {
                    distances.put(next, distances.get(current) + 1);
                    queue.add(next);
                }
            }
        }
        return distances;
    }

    @Test
    @DisplayName("Merdivende degilken inilemez")
    void cannotDescendAwayFromStairs() {
        Position stairs = game.getStairs();
        player.setTile(stairs.x() == 1 ? stairs.offset(1, 0) : stairs.offset(-1, 0));

        assertFalse(game.descend());
        assertEquals(1, game.getDepth());
    }

    @Test
    @DisplayName("Inince derinlik artar ve yeni bir kat uretilir")
    void descendingBuildsANewFloor() {
        Position oldStairs = game.getStairs();

        goDownOneFloor();

        assertEquals(2, game.getDepth());
        assertNotEquals(oldStairs, game.getStairs(), "Yeni katin merdiveni yeni yerde olmali");
        assertFalse(game.getEnemies().isEmpty(), "Yeni kat da dusmanlarla dolmali");
    }

    @Test
    @DisplayName("Inerken can, canta ve kese korunur")
    void progressSurvivesTheDescent() {
        player.takeDamage(6);
        game.addGold(40);
        Item potion = new Potion(0, 0);
        game.getInventory().add(potion);
        int hpBefore = player.getHp();

        int maxHpBefore = player.getMaxHp();

        goDownOneFloor();

        assertEquals(hpBefore, player.getHp(), "Inmek can doldurmamali");
        assertTrue(player.getMaxHp() > maxHpBefore, "Inmek azami cani buyutmeli");
        assertEquals(40, game.getGold());
        assertEquals(1, game.getInventory().size());
    }

    @Test
    @DisplayName("Derin katlarda daha cok dusman var")
    void deeperFloorsAreMoreCrowded() {
        int firstFloorEnemies = game.getEnemies().size();

        // 5. kat boss kati; orada siradan dusman sayisi bilerek dusuruluyor,
        // o yuzden karsilastirmayi 4. katta yapiyoruz.
        for (int i = 0; i < 3; i++) {
            goDownOneFloor();
        }

        assertEquals(4, game.getDepth());
        assertFalse(game.isBossFloor());
        assertTrue(game.getEnemies().size() > firstFloorEnemies,
                "4. katta 1. kattan cok dusman olmali");
    }

    @Test
    @DisplayName("Boss katinda siradan dusman kalabaligi azalir")
    void bossFloorHasFewerRegularEnemies() {
        for (int i = 0; i < 3; i++) {
            goDownOneFloor();
        }
        int fourthFloorEnemies = game.getEnemies().size();

        goDownOneFloor();

        assertTrue(game.isBossFloor());
        assertTrue(game.getEnemies().size() < fourthFloorEnemies,
                "Boss katinda kalabalik daha az olmali");
    }

    @Test
    @DisplayName("Merdivenin ustu esyayla kapanmaz")
    void stairsTileStaysClear() {
        for (int floor = 0; floor < 3; floor++) {
            Position stairs = game.getStairs();
            for (Item item : game.getGroundItems()) {
                assertNotEquals(stairs, item.getTile(), "Merdivenin ustune esya konmamali");
            }
            goDownOneFloor();
        }
    }

    @Test
    @DisplayName("Yeniden baslayinca ilk kata donulur")
    void restartReturnsToFirstFloor() {
        goDownOneFloor();
        goDownOneFloor();
        assertEquals(3, game.getDepth());

        game.restart();

        assertEquals(1, game.getDepth());
    }

    @Test
    @DisplayName("Olu oyuncu inemez")
    void deadPlayerCannotDescend() {
        player.setTile(game.getStairs());
        player.takeDamage(player.getMaxHp());

        assertFalse(game.descend());
        assertEquals(1, game.getDepth());
    }
}
