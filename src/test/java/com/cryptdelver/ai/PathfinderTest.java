package com.cryptdelver.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.world.Dungeon;
import com.cryptdelver.world.Position;
import com.cryptdelver.world.Tile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Iki yol bulucunun ortak sozlesmesi ve aralarindaki fark.
 *
 * <p>Test haritasi 12x10: ortada x=5 sutununda bir duvar var, tek gecit
 * asagida (5, 8). Sol taraftan sag tarafa gitmek icin bu gecitten dolasmak
 * gerekiyor — acgozlu yol bulucunun kaldigi, A* nin gectigi yer burasi.</p>
 */
class PathfinderTest {

    private static final int WIDTH = 12;
    private static final int HEIGHT = 10;

    private Dungeon dungeon;

    @BeforeEach
    void setUp() {
        dungeon = new Dungeon(WIDTH, HEIGHT);
        dungeon.fill(Tile.FLOOR);

        for (int x = 0; x < WIDTH; x++) {
            dungeon.setTile(x, 0, Tile.WALL);
            dungeon.setTile(x, HEIGHT - 1, Tile.WALL);
        }
        for (int y = 0; y < HEIGHT; y++) {
            dungeon.setTile(0, y, Tile.WALL);
            dungeon.setTile(WIDTH - 1, y, Tile.WALL);
        }

        // Ortadaki bolme duvari; (5, 8) acik birakiliyor.
        for (int y = 1; y <= 7; y++) {
            dungeon.setTile(5, y, Tile.WALL);
        }
    }

    /**
     * Yol bulucuyu tekrar tekrar cagirarak hedefe yurur.
     *
     * @return atilan adim sayisi; hedefe varilamazsa -1
     */
    private int walk(Pathfinder pathfinder, Position from, Position to, int maxSteps) {
        Position current = from;
        for (int steps = 1; steps <= maxSteps; steps++) {
            Position next = pathfinder.nextStep(dungeon, current, to);
            if (next == null) {
                return -1;
            }
            assertEquals(1, current.manhattanDistance(next), "Adim komsu kareye olmali");
            assertTrue(dungeon.isWalkable(next.x(), next.y()), "Duvara adim atilmamali");

            current = next;
            if (current.equals(to)) {
                return steps;
            }
        }
        return -1;
    }

    @Nested
    @DisplayName("A*")
    class AStar {

        private final Pathfinder pathfinder = new AStarPathfinder();

        @Test
        @DisplayName("Duvarin etrafindan dolasip hedefe varir")
        void goesAroundTheWall() {
            int steps = walk(pathfinder, new Position(2, 2), new Position(8, 2), 60);

            assertTrue(steps > 0, "A* gecidi bulup hedefe varmali");
        }

        @Test
        @DisplayName("Acik alanda en kisa yolu bulur")
        void findsShortestPathInOpenSpace() {
            // Bolme duvarinin solunda kalan acik alan.
            Position from = new Position(1, 1);
            Position to = new Position(4, 5);

            int steps = walk(pathfinder, from, to, 60);

            assertEquals(from.manhattanDistance(to), steps,
                    "Engelsiz alanda adim sayisi Manhattan uzakligina esit olmali");
        }

        @Test
        @DisplayName("Ulasilamayan hedef icin null doner")
        void unreachableTargetReturnsNull() {
            // (10, 5) karesini duvarla tamamen cevir.
            dungeon.setTile(9, 5, Tile.WALL);
            dungeon.setTile(11, 5, Tile.WALL);
            dungeon.setTile(10, 4, Tile.WALL);
            dungeon.setTile(10, 6, Tile.WALL);

            assertNull(pathfinder.nextStep(dungeon, new Position(2, 2), new Position(10, 5)));
        }

        @Test
        @DisplayName("Hedef duvarsa null doner")
        void wallTargetReturnsNull() {
            assertNull(pathfinder.nextStep(dungeon, new Position(2, 2), new Position(5, 3)));
        }

        @Test
        @DisplayName("Ayni karedeyse adim yok")
        void sameTileReturnsNull() {
            assertNull(pathfinder.nextStep(dungeon, new Position(2, 2), new Position(2, 2)));
        }
    }

    @Nested
    @DisplayName("Acgozlu")
    class Greedy {

        private final Pathfinder pathfinder = new GreedyPathfinder();

        @Test
        @DisplayName("Acik alanda hedefe dogru ilerler")
        void movesTowardTargetInOpenSpace() {
            Position from = new Position(1, 1);
            Position to = new Position(4, 1);

            Position step = pathfinder.nextStep(dungeon, from, to);

            assertNotNull(step);
            assertEquals(new Position(2, 1), step);
        }

        @Test
        @DisplayName("Duvarin onunde takilir; A* nin varlik sebebi bu")
        void getsStuckAtTheWall() {
            // (4, 2) den (8, 2) ye: tam karsida duvar var, dikeyde fark yok.
            assertNull(pathfinder.nextStep(dungeon, new Position(4, 2), new Position(8, 2)));

            // Ayni durumda A* dolasmayi buluyor.
            assertNotNull(new AStarPathfinder()
                    .nextStep(dungeon, new Position(4, 2), new Position(8, 2)));
        }

        @Test
        @DisplayName("Kose donmeyi bilmez ama yan eksende yol varsa kullanir")
        void slidesAlongTheOtherAxis() {
            // Hedef capraz: yatay kapali, dikey acik.
            Position step = pathfinder.nextStep(dungeon, new Position(4, 2), new Position(8, 5));

            assertEquals(new Position(4, 3), step, "Yatay kapaliysa dikey eksene kaymali");
        }
    }
}
