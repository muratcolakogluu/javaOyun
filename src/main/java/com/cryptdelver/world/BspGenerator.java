package com.cryptdelver.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * İkili alan bölme (Binary Space Partitioning) ile oda-koridor zindanı üretir.
 *
 * <p>Algoritma: haritanın tamamını tek bir dikdörtgen kabul et, onu rastgele
 * bir çizgiyle ikiye böl, parçalar yeterince küçülene kadar aynı şekilde
 * bölmeye devam et. En küçük parçalara (yapraklara) birer oda oyulur;
 * özyineleme geri sararken her düğüm, iki çocuğundan gelen odaları bir
 * koridorla birleştirir.</p>
 *
 * <p>Haritanın tek parça olması bu yapının doğal sonucudur: her birleşme adımı
 * iki bağlı bölgeyi tek bağlı bölge haline getirir, dolayısıyla kopuk kalan
 * oda olamaz.</p>
 */
public class BspGenerator implements DungeonGenerator {

    /** Daha fazla bölünemeyecek en küçük parça kenarı. */
    private static final int MIN_LEAF = 8;

    /** Bir odanın en küçük kenarı. */
    private static final int MIN_ROOM = 4;

    /** Özyinelemenin en fazla ineceği derinlik. */
    private static final int MAX_DEPTH = 5;

    /** Kenar oranı bu eşiği aşarsa, parçayı uzun kenarından bölmeye zorlarız. */
    private static final double ASPECT_LIMIT = 1.25;

    private final List<Room> lastRooms = new ArrayList<>();

    @Override
    public Dungeon generate(int width, int height, long seed) {
        Dungeon dungeon = new Dungeon(width, height);
        Random random = new Random(seed);

        lastRooms.clear();
        split(dungeon, 0, 0, width, height, 0, random);

        return dungeon;
    }

    @Override
    public String getName() {
        return "BSP (oda + koridor)";
    }

    /** Son üretimde oluşan odalar; doğma noktası ve eşya yerleşimi için. */
    public List<Room> getLastRooms() {
        return List.copyOf(lastRooms);
    }

    /**
     * Verilen alanı böler ve içine oda(lar) oyar.
     *
     * @return bu alt ağacı temsil eden oda; üst düğüm koridoru buraya bağlar
     */
    private Room split(Dungeon dungeon, int x, int y, int width, int height, int depth, Random random) {
        boolean canSplitVertically = width >= MIN_LEAF * 2;
        boolean canSplitHorizontally = height >= MIN_LEAF * 2;

        if (depth >= MAX_DEPTH || (!canSplitVertically && !canSplitHorizontally)) {
            Room room = createRoom(x, y, width, height, random);
            room.carveInto(dungeon);
            lastRooms.add(room);
            return room;
        }

        boolean horizontal = chooseHorizontal(width, height, canSplitVertically, canSplitHorizontally, random);

        Room first;
        Room second;
        if (horizontal) {
            int cut = MIN_LEAF + random.nextInt(height - MIN_LEAF * 2 + 1);
            first = split(dungeon, x, y, width, cut, depth + 1, random);
            second = split(dungeon, x, y + cut, width, height - cut, depth + 1, random);
        } else {
            int cut = MIN_LEAF + random.nextInt(width - MIN_LEAF * 2 + 1);
            first = split(dungeon, x, y, cut, height, depth + 1, random);
            second = split(dungeon, x + cut, y, width - cut, height, depth + 1, random);
        }

        carveCorridor(dungeon, first.center(), second.center(), random);
        return random.nextBoolean() ? first : second;
    }

    /** Bölmenin yatay mı dikey mi olacağına karar verir. */
    private boolean chooseHorizontal(int width, int height, boolean canSplitVertically,
                                     boolean canSplitHorizontally, Random random) {
        if (!canSplitVertically) {
            return true;
        }
        if (!canSplitHorizontally) {
            return false;
        }
        // Uzun ve ince parçaları kısa kenardan bölmek, odaları kareye yaklaştırır.
        if (height > width * ASPECT_LIMIT) {
            return true;
        }
        if (width > height * ASPECT_LIMIT) {
            return false;
        }
        return random.nextBoolean();
    }

    /** Parçanın içine, kenarlarında en az birer karelik duvar payı bırakan bir oda yerleştirir. */
    private Room createRoom(int leafX, int leafY, int leafWidth, int leafHeight, Random random) {
        int maxWidth = Math.max(MIN_ROOM, leafWidth - 2);
        int maxHeight = Math.max(MIN_ROOM, leafHeight - 2);

        int roomWidth = MIN_ROOM + random.nextInt(maxWidth - MIN_ROOM + 1);
        int roomHeight = MIN_ROOM + random.nextInt(maxHeight - MIN_ROOM + 1);

        int slackX = Math.max(1, leafWidth - roomWidth - 1);
        int slackY = Math.max(1, leafHeight - roomHeight - 1);

        int roomX = leafX + 1 + random.nextInt(slackX);
        int roomY = leafY + 1 + random.nextInt(slackY);

        return new Room(roomX, roomY, roomWidth, roomHeight);
    }

    /**
     * İki nokta arasına L biçiminde koridor oyar. Önce yatay sonra dikey mi,
     * yoksa tersi mi gideceğine rastgele karar verilir; bu, haritaya çeşitlilik
     * katar.
     */
    private void carveCorridor(Dungeon dungeon, Position from, Position to, Random random) {
        if (random.nextBoolean()) {
            carveHorizontal(dungeon, from.x(), to.x(), from.y());
            carveVertical(dungeon, from.y(), to.y(), to.x());
        } else {
            carveVertical(dungeon, from.y(), to.y(), from.x());
            carveHorizontal(dungeon, from.x(), to.x(), to.y());
        }
    }

    private void carveHorizontal(Dungeon dungeon, int fromX, int toX, int y) {
        for (int x = Math.min(fromX, toX); x <= Math.max(fromX, toX); x++) {
            dungeon.setTile(x, y, Tile.FLOOR);
        }
    }

    private void carveVertical(Dungeon dungeon, int fromY, int toY, int x) {
        for (int y = Math.min(fromY, toY); y <= Math.max(fromY, toY); y++) {
            dungeon.setTile(x, y, Tile.FLOOR);
        }
    }
}
