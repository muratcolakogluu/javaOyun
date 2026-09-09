package com.cryptdelver.world;

/**
 * Zindan haritasındaki tek bir karenin türü.
 *
 * <p>Her tür, üzerinden geçilip geçilemediğini ve ekranda hangi karakterle
 * temsil edildiğini kendisi bilir; böylece "bu kare yürünebilir mi?" sorusunu
 * her yerde {@code if (tile == WALL)} diye sormak yerine polimorfik olarak
 * {@link #isWalkable()} ile soruyoruz. Yeni bir tür (kapı, su, tuzak) eklemek,
 * çağıran kodu değiştirmeden buraya bir satır eklemek demek — nitekim merdiven
 * de tam olarak böyle eklendi.</p>
 */
public enum Tile {

    /** Üzerinde yürünebilen zemin. */
    FLOOR(true, '.'),

    /** Geçilemeyen duvar. */
    WALL(false, '#'),

    /** Bir alt kata inen merdiven; üzerinde durulabilir. */
    STAIRS_DOWN(true, '>'),

    /**
     * Bir üst kata çıkan merdiven; kata indiğin nokta.
     *
     * <p>Geri dönebilmek altına bir anlam kazandırdı: kesende para birikince
     * yukarıdaki büyücüye dönüp takımına büyü bastırabiliyorsun. Öncesinde
     * altın yalnızca bulunduğun katta büyücü varsa işe yarıyordu.</p>
     */
    STAIRS_UP(true, '<');

    private final boolean walkable;
    private final char glyph;

    Tile(boolean walkable, char glyph) {
        this.walkable = walkable;
        this.glyph = glyph;
    }

    /** Bir varlığın bu karenin üzerine geçebilip geçemeyeceği. */
    public boolean isWalkable() {
        return walkable;
    }

    /** Karenin metinsel gösterimi (hata ayıklama ve olası ASCII modu için). */
    public char getGlyph() {
        return glyph;
    }
}
