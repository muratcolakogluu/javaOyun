package com.cryptdelver.game;

import com.cryptdelver.world.Dungeon;
import com.cryptdelver.world.Position;
import com.cryptdelver.world.Tile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Kilitli mahzen: katın sorduğu tek soru.
 *
 * <p>Harita şimdiye kadar bir <em>kap</em>tı — içinden geçiyordun, sana bir şey
 * sormuyordu. Mahzen kilitli bir kapının arkasında duruyor, anahtarı kattaki
 * bir düşman taşıyor. Yani kat artık bir hedef veriyor: kalabalığın içinden
 * <b>doğru olanı</b> bul, sonra kapıya dön.</p>
 *
 * <p>İsteğe bağlı olması önemli. Merdiven hâlâ açık; mahzeni görüp "buna
 * vaktim yok" demek de bir cevap. Zorunlu olsaydı bir hedef değil bir vergi
 * olurdu.</p>
 *
 * <h2>Neden kayaya oyuluyor</h2>
 * <p>İlk düşünce var olan bir odayı duvarla kapatmaktı. O yol tehlikeli:
 * duvar <em>eklemek</em>, katı ikiye bölüp merdiveni ulaşılmaz bırakabilir ve
 * bunu her seferinde ayrıca sınamak gerekir. Mahzen bunun yerine sağlam
 * kayanın içine oyuluyor: hiçbir yürünebilir kare kapanmıyor, yalnızca yeni
 * kareler açılıyor. Bağlantı bozulması <em>imkânsız</em> — sınanacak bir şey
 * değil, kurulumun kendisinden çıkan bir sonuç.</p>
 */
public final class Vault {

    /** Mahzenin içi: iki kareye iki kare. */
    private static final int SIZE = 2;

    /**
     * Kapının çevresinde bozulmadan kalması gereken kaya payı.
     *
     * <p>Oyulan alanın etrafında bir kare duvar kalmalı, yoksa mahzen komşu
     * bir koridora açılır ve kilit anlamsız olur: kapıyı hiç açmadan yandan
     * girebilirsin.</p>
     */
    private static final int ROCK_MARGIN = 1;

    private final Position door;
    private final List<Position> inside;

    private Vault(Position door, List<Position> inside) {
        this.door = door;
        this.inside = List.copyOf(inside);
    }

    /** Kapının karesi; oyuncu buraya komşu olunca açabiliyor. */
    public Position getDoor() {
        return door;
    }

    /** Mahzenin içindeki kareler; ganimet buraya konuyor. */
    public List<Position> getInside() {
        return inside;
    }

    /**
     * Kata bir mahzen oymayı dener ve haritayı değiştirir.
     *
     * <p>Aranan şey şu: yürünebilir bir kare (sundurma), ona komşu bir duvar
     * karesi (kapı) ve kapının ardında oyulabilecek kadar sağlam kaya.
     * Bulunamazsa kat mahzensiz kalıyor — zorlamak için haritayı bozmaktan
     * iyidir.</p>
     *
     * @return kurulan mahzen; yer bulunamadıysa {@code null}
     */
    public static Vault carve(Dungeon dungeon, Position spawn, long seed) {
        List<Position> porches = new ArrayList<>(dungeon.walkablePositions());
        Collections.shuffle(porches, new Random(seed));

        for (Position porch : porches) {
            // Girişin dibindeki bir mahzen sürpriz olmaz; biraz uzakta olsun.
            if (porch.manhattanDistance(spawn) < MIN_DISTANCE_FROM_SPAWN) {
                continue;
            }

            for (int[] direction : DIRECTIONS) {
                Vault vault = tryDirection(dungeon, porch, direction[0], direction[1]);
                if (vault != null) {
                    return vault;
                }
            }
        }
        return null;
    }

    /** Mahzen doğulan yerden en az bu kadar uzağa oyuluyor. */
    private static final int MIN_DISTANCE_FROM_SPAWN = 10;

    private static final int[][] DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    /**
     * Sundurmadan verilen yöne doğru bir mahzen oymayı dener.
     *
     * <p>Kapı sundurmanın hemen yanındaki duvar karesi; mahzenin içi kapının
     * bir adım ötesinden başlıyor. Önce bütün kareler ölçülüyor, sonra
     * <em>hepsi birden</em> oyuluyor: yarısını oyup vazgeçmek haritada bir
     * yara bırakırdı.</p>
     */
    private static Vault tryDirection(Dungeon dungeon, Position porch, int dx, int dy) {
        Position door = porch.offset(dx, dy);
        if (dungeon.getTile(door.x(), door.y()) != Tile.WALL) {
            return null;
        }

        // Mahzenin içi ve çevresindeki kaya payı: hepsi hâlâ duvar olmalı.
        List<Position> inside = new ArrayList<>();
        for (int along = 1; along <= SIZE; along++) {
            for (int across = 0; across < SIZE; across++) {
                inside.add(sideStep(door, dx, dy, along, across));
            }
        }

        for (Position cell : inside) {
            if (!isSolidWithMargin(dungeon, cell, porch)) {
                return null;
            }
        }

        dungeon.setTile(door.x(), door.y(), Tile.DOOR_LOCKED);
        for (Position cell : inside) {
            dungeon.setTile(cell.x(), cell.y(), Tile.FLOOR);
        }
        return new Vault(door, inside);
    }

    /**
     * Kapıdan {@code along} adım ileri, {@code across} adım yana.
     *
     * <p>Yön dört yönden biri olduğu için "yan", yönün dik bileşeni:
     * {@code (dy, dx)}. Böylece dört yön için ayrı ayrı koordinat yazmak
     * gerekmiyor.</p>
     */
    private static Position sideStep(Position door, int dx, int dy, int along, int across) {
        return new Position(door.x() + dx * along + dy * across,
                door.y() + dy * along + dx * across);
    }

    /**
     * Bu kare oyulabilir mi: kendisi ve çevresindeki pay sağlam kaya mı.
     *
     * <p>Sundurma bu kontrolün dışında: kapının önündeki kare zaten
     * yürünebilir ve mahzenin girişi orası.</p>
     */
    private static boolean isSolidWithMargin(Dungeon dungeon, Position cell, Position porch) {
        for (int ox = -ROCK_MARGIN; ox <= ROCK_MARGIN; ox++) {
            for (int oy = -ROCK_MARGIN; oy <= ROCK_MARGIN; oy++) {
                Position around = cell.offset(ox, oy);
                if (around.equals(porch)) {
                    continue;
                }
                if (!dungeon.contains(around.x(), around.y())
                        || dungeon.getTile(around.x(), around.y()) != Tile.WALL) {
                    return false;
                }
            }
        }
        return true;
    }
}
