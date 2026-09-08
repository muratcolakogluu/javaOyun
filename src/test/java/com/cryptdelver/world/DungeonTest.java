package com.cryptdelver.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DungeonTest {

    @Test
    @DisplayName("Yeni zindan bastan sona duvarla dolu olur")
    void newDungeonIsAllWalls() {
        Dungeon dungeon = new Dungeon(5, 5);

        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                assertEquals(Tile.WALL, dungeon.getTile(x, y));
                assertFalse(dungeon.isWalkable(x, y));
            }
        }
    }

    @Test
    @DisplayName("Zemin yapilan kare yurunebilir hale gelir")
    void floorTileIsWalkable() {
        Dungeon dungeon = new Dungeon(5, 5);

        dungeon.setTile(2, 3, Tile.FLOOR);

        assertTrue(dungeon.isWalkable(2, 3));
        assertFalse(dungeon.isWalkable(2, 2));
    }

    @Test
    @DisplayName("Harita disi koordinatlar duvar sayilir")
    void outOfBoundsIsWall() {
        Dungeon dungeon = new Dungeon(5, 5);
        dungeon.fill(Tile.FLOOR);

        assertFalse(dungeon.contains(-1, 0));
        assertFalse(dungeon.isWalkable(-1, 0));
        assertFalse(dungeon.isWalkable(5, 0));
        assertEquals(Tile.WALL, dungeon.getTile(0, 5));
    }

    @Test
    @DisplayName("En uzak kare aramasi koridorun ucunu bulur")
    void findsFarthestTileAlongACorridor() {
        Dungeon dungeon = new Dungeon(10, 5);
        // Tek satirlik koridor: (1,2) ile (8,2) arasi.
        for (int x = 1; x <= 8; x++) {
            dungeon.setTile(x, 2, Tile.FLOOR);
        }

        assertEquals(new Position(8, 2), dungeon.findFarthestWalkableFrom(new Position(1, 2)));
        assertEquals(new Position(1, 2), dungeon.findFarthestWalkableFrom(new Position(8, 2)));
    }

    @Test
    @DisplayName("En uzak kare aramasi duvarin arkasina gecmez")
    void farthestSearchStaysInTheReachableArea() {
        Dungeon dungeon = new Dungeon(10, 5);
        for (int x = 1; x <= 3; x++) {
            dungeon.setTile(x, 2, Tile.FLOOR);
        }
        // Duvarla ayrilmis, ulasilamayan ikinci bir bosluk.
        dungeon.setTile(8, 2, Tile.FLOOR);

        assertEquals(new Position(3, 2), dungeon.findFarthestWalkableFrom(new Position(1, 2)));
    }

    @Test
    @DisplayName("Dogma noktasi aramasi en yakin yurunebilir kareyi bulur")
    void findsNearestWalkableTile() {
        Dungeon dungeon = new Dungeon(9, 9);
        dungeon.setTile(6, 4, Tile.FLOOR);

        assertEquals(new Position(6, 4), dungeon.findWalkableNear(4, 4));
    }
}
