package com.cryptdelver.world;

/**
 * Oyuncunun kattan ne gördüğü ve neyi hatırladığı.
 *
 * <p>Önce kata iner inmez bütün harita görünüyordu: merdiven nerede, düşman
 * nerede, eşya nerede — hepsi ilk saniyede belliydi. Bu yüzden zindanda
 * <em>keşif</em> yoktu, yalnızca "merdivene git" vardı. Oyun bir zindandan çok
 * içinde koridorlar olan bir arenaya benziyordu.</p>
 *
 * <p>Her karenin üç hâli var:</p>
 * <ul>
 *   <li><b>Görünür:</b> şu anda ışık altında. Düşmanlar ve eşyalar yalnızca
 *       burada çiziliyor.</li>
 *   <li><b>Hatırlanan:</b> daha önce görüldü. Zemin ve duvar soluk çiziliyor;
 *       üstünde ne olduğunu bilmiyorsun. Merdivenin yerini hatırlaman bu
 *       sayede.</li>
 *   <li><b>Karanlık:</b> hiç görülmedi, hiç çizilmiyor.</li>
 * </ul>
 *
 * <p>Görüş, duvarlarla kesiliyor: her kareye oyuncudan bir çizgi çekiliyor,
 * arada duvar varsa kare görünmüyor. Yarıçapla sınırlı olması hem oyun için
 * ("fener kadar görüyorsun") hem hesap için gerekli — kat başına birkaç yüz
 * kare bakmak, tüm haritayı taramaktan çok daha ucuz.</p>
 *
 * <p>Hesap yalnızca oyuncu <em>kare değiştirince</em> yapılıyor. Her karede
 * yapmak boşa iş olurdu: aynı karede duran oyuncunun gördüğü değişmiyor.</p>
 */
public class Vision {

    /**
     * Kaç kare öteyi görebiliyorsun.
     *
     * <p>Sekizde başladı ama dar geliyordu: 40 karelik bir katta kendi
     * etrafında küçük bir cepten bakıyor gibisin ve odanın karşı duvarını bile
     * göremiyorsun. On bir kare, bir odayı bir bakışta görmene yetiyor ama
     * katın tamamını hâlâ göstermiyor — keşif duruyor, klostrofobi gidiyor.</p>
     */
    public static final int RADIUS = 11;

    private final int width;
    private final int height;
    private final boolean[][] visible;
    private final boolean[][] remembered;

    private int lastX = Integer.MIN_VALUE;
    private int lastY = Integer.MIN_VALUE;

    public Vision(int width, int height) {
        this.width = width;
        this.height = height;
        this.visible = new boolean[width][height];
        this.remembered = new boolean[width][height];
    }

    /** Şu anda ışık altında mı. */
    public boolean isVisible(int x, int y) {
        return contains(x, y) && visible[x][y];
    }

    /** Görülmüş mü: ya şu anda görünüyor ya da daha önce görüldü. */
    public boolean isRemembered(int x, int y) {
        return contains(x, y) && remembered[x][y];
    }

    /**
     * Görüşü oyuncunun bulunduğu kareye göre yeniler.
     *
     * <p>Aynı kareden ikinci kez çağrılırsa hiçbir şey yapmıyor; çağıran
     * tarafın "kare değişti mi" diye ayrıca bakması gerekmesin diye kontrol
     * burada.</p>
     */
    public void update(Dungeon dungeon, int fromX, int fromY) {
        if (fromX == lastX && fromY == lastY) {
            return;
        }
        lastX = fromX;
        lastY = fromY;

        for (boolean[] column : visible) {
            java.util.Arrays.fill(column, false);
        }

        for (int x = fromX - RADIUS; x <= fromX + RADIUS; x++) {
            for (int y = fromY - RADIUS; y <= fromY + RADIUS; y++) {
                if (!contains(x, y) || distanceSquared(fromX, fromY, x, y) > RADIUS * RADIUS) {
                    continue;
                }
                if (hasLineOfSight(dungeon, fromX, fromY, x, y)) {
                    visible[x][y] = true;
                    remembered[x][y] = true;
                }
            }
        }
    }

    /** Kat değişince her şey unutuluyor; yeni kat baştan karanlık. */
    public void reset() {
        for (int x = 0; x < width; x++) {
            java.util.Arrays.fill(visible[x], false);
            java.util.Arrays.fill(remembered[x], false);
        }
        lastX = Integer.MIN_VALUE;
        lastY = Integer.MIN_VALUE;
    }

    /**
     * İki kare arasında duvarsız bir çizgi var mı.
     *
     * <p>Bresenham çizgisiyle yürüyoruz. Hedefin kendisi duvar olabilir —
     * duvarı görmen gerekiyor, arkasını değil; o yüzden son kare kontrol
     * dışında.</p>
     */
    private boolean hasLineOfSight(Dungeon dungeon, int fromX, int fromY, int toX, int toY) {
        int dx = Math.abs(toX - fromX);
        int dy = Math.abs(toY - fromY);
        int stepX = fromX < toX ? 1 : -1;
        int stepY = fromY < toY ? 1 : -1;
        int error = dx - dy;

        int x = fromX;
        int y = fromY;

        while (x != toX || y != toY) {
            int doubled = error * 2;
            if (doubled > -dy) {
                error -= dy;
                x += stepX;
            }
            if (doubled < dx) {
                error += dx;
                y += stepY;
            }

            if (x == toX && y == toY) {
                return true;
            }
            if (!dungeon.isWalkable(x, y)) {
                return false;
            }
        }
        return true;
    }

    private int distanceSquared(int fromX, int fromY, int toX, int toY) {
        int dx = toX - fromX;
        int dy = toY - fromY;
        return dx * dx + dy * dy;
    }

    private boolean contains(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }
}
