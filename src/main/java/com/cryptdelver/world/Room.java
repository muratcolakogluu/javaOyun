package com.cryptdelver.world;

/**
 * Zindandaki dikdörtgen bir oda.
 *
 * <p>Koordinatlar odanın <em>iç</em> alanını tarif eder: {@code (x, y)} sol üst
 * kare, {@code width}/{@code height} ise kaç kare kapladığıdır. Odanın kendi
 * duvarları yoktur — duvarlar, etrafındaki oyulmamış karelerdir.</p>
 */
public record Room(int x, int y, int width, int height) {

    public Room {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Oda ölçüleri pozitif olmalı: " + width + "x" + height);
        }
    }

    /** Odanın en sağdaki dolu sütunu. */
    public int right() {
        return x + width - 1;
    }

    /** Odanın en alttaki dolu satırı. */
    public int bottom() {
        return y + height - 1;
    }

    /** Odanın orta karesi; koridorlar buradan bağlanır. */
    public Position center() {
        return new Position(x + width / 2, y + height / 2);
    }

    public boolean contains(int px, int py) {
        return px >= x && px <= right() && py >= y && py <= bottom();
    }

    /**
     * İki odanın çakışıp çakışmadığı. Aralarında en az bir duvar kalsın diye
     * bir karelik pay bırakarak kontrol ediyoruz.
     */
    public boolean overlaps(Room other) {
        return x <= other.right() + 1
                && right() + 1 >= other.x
                && y <= other.bottom() + 1
                && bottom() + 1 >= other.y;
    }

    /** Odanın kapladığı alanı haritada zemine çevirir. */
    public void carveInto(Dungeon dungeon) {
        for (int px = x; px <= right(); px++) {
            for (int py = y; py <= bottom(); py++) {
                dungeon.setTile(px, py, Tile.FLOOR);
            }
        }
    }
}
