package com.cryptdelver.game;

import com.cryptdelver.entity.Boss;
import com.cryptdelver.entity.Enemy;
import com.cryptdelver.entity.Item;
import com.cryptdelver.entity.Wizard;
import com.cryptdelver.world.Dungeon;
import com.cryptdelver.world.Position;
import java.util.List;

/**
 * Kurulmuş bir kat: harita ve üstündeki her şey.
 *
 * <p>{@link FloorBuilder} bunu üretiyor, {@link Game} devralıyor. Aradaki bu
 * kayıt sayesinde kat kurulumu oyunun durumuna hiç dokunmuyor: üretici tarafta
 * "şunu şuraya koy" diye bir yan etki yok, elde edilen kat bir <em>değer</em>
 * olarak dönüyor.</p>
 *
 * <p>Katın iki hâli var. <b>Döşeme</b> ({@link FloorBuilder#layout}) yalnızca
 * haritayı, doğulan yeri, merdiveni ve büyücüyü içeriyor — bunlar tohumdan
 * belirlenimci olarak çıkıyor, o yüzden kayıt yüklerken de aynısı kuruluyor.
 * <b>Dolu kat</b> ({@link FloorBuilder#build}) buna boss, düşmanlar ve eşyalar
 * ekliyor; kayıt yüklerken bunlar dosyadan geldiği için o adım atlanıyor.</p>
 *
 * @param boss merdiveni tutan boss; boss katı değilse {@code null}
 * @param wizard kattaki büyücü; yoksa {@code null}
 */
public record Floor(long seed,
                    Dungeon dungeon,
                    Position spawn,
                    Position stairs,
                    Wizard wizard,
                    Boss boss,
                    List<Enemy> enemies,
                    List<Item> groundItems) {

    public Floor {
        enemies = List.copyOf(enemies);
        groundItems = List.copyOf(groundItems);
    }

    /** Döşemeye boss, düşman ve eşya ekleyip dolu katı verir. */
    Floor filledWith(Boss boss, List<Enemy> enemies, List<Item> groundItems) {
        return new Floor(seed, dungeon, spawn, stairs, wizard, boss, enemies, groundItems);
    }
}
