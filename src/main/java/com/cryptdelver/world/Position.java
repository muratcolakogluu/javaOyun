package com.cryptdelver.world;

/**
 * Izgara üzerinde bir kare koordinatı.
 *
 * <p>Değişmez (immutable) bir record; harita üretiminde, doğma noktalarında ve
 * ileride A* yol bulmada düğüm olarak kullanılacak. {@code equals}/{@code hashCode}
 * hazır geldiği için doğrudan {@code Set} ve {@code Map} anahtarı olabiliyor.</p>
 */
public record Position(int x, int y) {

    /** Bu karenin verilen kadar ötelenmiş hali. */
    public Position offset(int dx, int dy) {
        return new Position(x + dx, y + dy);
    }

    /**
     * Izgara (Manhattan) uzaklığı: yalnızca dört yöne hareket edildiğinde
     * iki kare arasındaki en kısa adım sayısı.
     */
    public int manhattanDistance(Position other) {
        return Math.abs(x - other.x) + Math.abs(y - other.y);
    }
}
