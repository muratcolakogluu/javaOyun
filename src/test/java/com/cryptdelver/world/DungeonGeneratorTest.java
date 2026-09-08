package com.cryptdelver.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Her iki uretici de ayni sozlesmeyi saglamali; bu yuzden testleri tek yerde
 * yazip parametre olarak iki uretici de veriyoruz. Yeni bir uretici eklendiginde
 * asagidaki listeye bir satir eklemek, onu da bu kurallara tabi tutmaya yeter.
 */
class DungeonGeneratorTest {

    private static final int WIDTH = 48;
    private static final int HEIGHT = 30;

    static Stream<Arguments> generators() {
        return Stream.of(
                Arguments.of(new BspGenerator()),
                Arguments.of(new RandomWalkGenerator()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("generators")
    void producesConnectedFloor(DungeonGenerator generator) {
        // Tek bir tohum sansli gelebilir; birkac tohumu birden deniyoruz.
        for (long seed = 0; seed < 25; seed++) {
            Dungeon dungeon = generator.generate(WIDTH, HEIGHT, seed);

            int reachable = countReachableFrom(dungeon, firstWalkable(dungeon));

            assertEquals(dungeon.countWalkable(), reachable,
                    generator.getName() + " tohum " + seed + ": kopuk bolge var");
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("generators")
    void sameSeedProducesSameDungeon(DungeonGenerator generator) {
        Dungeon first = generator.generate(WIDTH, HEIGHT, 12345L);
        Dungeon second = generator.generate(WIDTH, HEIGHT, 12345L);

        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                assertEquals(first.getTile(x, y), second.getTile(x, y),
                        "Ayni tohum farkli harita uretti: (" + x + ", " + y + ")");
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("generators")
    void bordersStayWalls(DungeonGenerator generator) {
        Dungeon dungeon = generator.generate(WIDTH, HEIGHT, 7L);

        for (int x = 0; x < WIDTH; x++) {
            assertEquals(Tile.WALL, dungeon.getTile(x, 0), "Ust kenar acik");
            assertEquals(Tile.WALL, dungeon.getTile(x, HEIGHT - 1), "Alt kenar acik");
        }
        for (int y = 0; y < HEIGHT; y++) {
            assertEquals(Tile.WALL, dungeon.getTile(0, y), "Sol kenar acik");
            assertEquals(Tile.WALL, dungeon.getTile(WIDTH - 1, y), "Sag kenar acik");
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("generators")
    void carvesEnoughFloorToBePlayable(DungeonGenerator generator) {
        Dungeon dungeon = generator.generate(WIDTH, HEIGHT, 99L);

        int total = WIDTH * HEIGHT;
        int walkable = dungeon.countWalkable();

        assertTrue(walkable > total * 0.15,
                generator.getName() + ": harita fazla dar, zemin orani " + (walkable / (double) total));
        assertTrue(walkable < total * 0.75,
                generator.getName() + ": harita fazla bos, zemin orani " + (walkable / (double) total));
    }

    /** Haritadaki ilk yurunebilir kare; tarama buradan baslar. */
    private Position firstWalkable(Dungeon dungeon) {
        for (int x = 0; x < dungeon.getWidth(); x++) {
            for (int y = 0; y < dungeon.getHeight(); y++) {
                if (dungeon.isWalkable(x, y)) {
                    return new Position(x, y);
                }
            }
        }
        throw new AssertionError("Uretici bos harita dondurdu");
    }

    /**
     * Genislik oncelikli arama (BFS) ile baslangictan yuruyerek ulasilabilen
     * kare sayisini bulur. Bu sayi toplam zemin sayisina esitse harita tek
     * parcadir; kucukse oyuncunun asla goremeyecegi bir bolge var demektir.
     */
    private int countReachableFrom(Dungeon dungeon, Position start) {
        Set<Position> visited = new HashSet<>();
        Deque<Position> queue = new ArrayDeque<>();

        visited.add(start);
        queue.add(start);

        int[][] directions = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        while (!queue.isEmpty()) {
            Position current = queue.poll();
            for (int[] direction : directions) {
                Position next = current.offset(direction[0], direction[1]);
                if (dungeon.isWalkable(next.x(), next.y()) && visited.add(next)) {
                    queue.add(next);
                }
            }
        }

        return visited.size();
    }
}
