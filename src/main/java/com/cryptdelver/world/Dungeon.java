package com.cryptdelver.world;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Zindanın tek bir katı: {@link Tile} karelerinden oluşan iki boyutlu ızgara.
 *
 * <p>Bu sınıf haritayı yalnızca <em>tutar</em>; nasıl üretildiğini bilmez.
 * Üretim işi {@link DungeonGenerator} uygulamalarına ait. Koordinatlar
 * {@code [x][y]} sırasıyla, sol üst köşe {@code (0, 0)} olacak şekilde
 * tutulur.</p>
 */
public class Dungeon {

    private final int width;
    private final int height;
    private final Tile[][] tiles;

    /** Tamamı duvarla dolu, verilen ölçülerde boş bir kat oluşturur. */
    public Dungeon(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Zindan ölçüleri pozitif olmalı: " + width + "x" + height);
        }
        this.width = width;
        this.height = height;
        this.tiles = new Tile[width][height];
        fill(Tile.WALL);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /** Verilen koordinatın harita sınırları içinde kalıp kalmadığı. */
    public boolean contains(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    /**
     * Koordinattaki kareyi verir. Harita dışı istekler için {@link Tile#WALL}
     * döner; böylece sınır kontrolünü her çağıran yerde tekrarlamak gerekmez.
     */
    public Tile getTile(int x, int y) {
        return contains(x, y) ? tiles[x][y] : Tile.WALL;
    }

    public void setTile(int x, int y, Tile tile) {
        if (contains(x, y)) {
            tiles[x][y] = tile;
        }
    }

    /** Haritanın tamamını tek bir kare türüyle doldurur. */
    public final void fill(Tile tile) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles[x][y] = tile;
            }
        }
    }

    /** Bir varlığın bu koordinata hareket edip edemeyeceği. */
    public boolean isWalkable(int x, int y) {
        return getTile(x, y).isWalkable();
    }

    /** Haritadaki yürünebilir kare sayısı. */
    public int countWalkable() {
        int count = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (tiles[x][y].isWalkable()) {
                    count++;
                }
            }
        }
        return count;
    }

    /** Haritadaki tüm yürünebilir kareler; doğma noktası seçmek için. */
    public List<Position> walkablePositions() {
        List<Position> positions = new ArrayList<>();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (tiles[x][y].isWalkable()) {
                    positions.add(new Position(x, y));
                }
            }
        }
        return positions;
    }

    /**
     * Başlangıçtan yürüyerek gidilebilen <em>en uzak</em> kareyi bulur.
     *
     * <p>Genişlik öncelikli arama (BFS) ile dalga dalga yayılıyoruz; komşu
     * kareler hep eşit maliyetli olduğu için kuyruğa en son giren kare aynı
     * zamanda en uzaktaki karedir. Merdiveni buraya koymak, katı baştan sona
     * yürümeyi gerektiriyor — hemen yanı başında merdiven bulup geçmek yerine
     * katı gerçekten keşfetmen gerekiyor.</p>
     *
     * @return en uzak yürünebilir kare; başlangıç yürünebilir değilse kendisi
     */
    public Position findFarthestWalkableFrom(Position start) {
        Position farthest = start;

        // Haritalama BFS sırasını koruduğu için son giren kare en uzaktakidir.
        for (Position spot : walkableDistancesFrom(start).keySet()) {
            farthest = spot;
        }
        return farthest;
    }

    /**
     * Başlangıçtan yürüyerek ulaşılan her karenin kaç adım uzakta olduğu.
     *
     * <p>Kuş uçuşu mesafe burada işe yaramıyor: duvarın öbür yanındaki kare
     * iki kare ötede görünür ama oraya varmak koridoru dolaşmayı gerektirebilir.
     * Büyücüyü doğulan yerin <em>yürüme</em> mesafesine göre koyabilmemiz için
     * bu ayrım gerekiyordu.</p>
     *
     * <p>Dönen eşleme BFS sırasında: ilk giren en yakın, son giren en uzak
     * kare. Sıra korunduğu için üstünde gezinmek de belirlenimci — aynı harita
     * hep aynı yanıtı veriyor.</p>
     *
     * @param blocked geçilmez sayılacak fazladan kareler; boş küme verilebilir
     */
    public Map<Position, Integer> walkableDistancesFrom(Position start, Set<Position> blocked) {
        Map<Position, Integer> distances = new LinkedHashMap<>();
        if (!isWalkable(start.x(), start.y()) || blocked.contains(start)) {
            return distances;
        }

        int[][] directions = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        Deque<Position> queue = new ArrayDeque<>();

        distances.put(start, 0);
        queue.add(start);

        while (!queue.isEmpty()) {
            Position current = queue.poll();
            int next = distances.get(current) + 1;

            for (int[] direction : directions) {
                Position neighbour = current.offset(direction[0], direction[1]);
                if (!isWalkable(neighbour.x(), neighbour.y()) || blocked.contains(neighbour)) {
                    continue;
                }
                if (distances.putIfAbsent(neighbour, next) == null) {
                    queue.add(neighbour);
                }
            }
        }

        return distances;
    }

    public Map<Position, Integer> walkableDistancesFrom(Position start) {
        return walkableDistancesFrom(start, Set.of());
    }

    /**
     * Verilen karenin çevresinde yürünebilir en yakın kareyi arar.
     *
     * <p>Merkezden dışa doğru genişleyen kare halkalar tarar; oyuncunun ya da
     * düşmanın duvarın içinde doğmasını engellemek için kullanılır.</p>
     *
     * @throws IllegalStateException haritada hiç yürünebilir kare yoksa
     */
    public Position findWalkableNear(int centerX, int centerY) {
        int maxRadius = Math.max(width, height);

        for (int radius = 0; radius <= maxRadius; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    // Yalnızca halkanın kenarına bak; içi önceki turlarda tarandı.
                    if (Math.abs(dx) != radius && Math.abs(dy) != radius) {
                        continue;
                    }
                    int x = centerX + dx;
                    int y = centerY + dy;
                    if (isWalkable(x, y)) {
                        return new Position(x, y);
                    }
                }
            }
        }

        throw new IllegalStateException("Haritada yürünebilir hiçbir kare yok");
    }
}
