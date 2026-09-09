package com.cryptdelver.entity;

import com.cryptdelver.ai.AStarPathfinder;

/**
 * Şaman: uzaktan fark eden, ısrarcı takipçi; Korluk katlarında beliriyor.
 *
 * <p>Ayırt edici yanı canı ya da vuruşu değil, <em>aklı</em>: A* ile yol
 * buluyor, yani duvarların arkasından dolanıp geliyor ve menzili neredeyse
 * bütün kat. Şimdiye kadar bu davranış yalnızca bossta vardı.</p>
 *
 * <p>Kaçarak oynayan oyuncuyu cezalandıran ilk sıradan düşman bu: köşeye
 * çekilip beklemek artık işe yaramıyor, gelip buluyor.</p>
 */
public class Saman extends Enemy {

    private static final EnemyStats STATS = new EnemyStats(
            10,     // can
            5,      // vuruş gücü — sert vuruyor
            1,      // savunma
            4.2,    // hız (kare/saniye)
            1.1,    // vuruş arası bekleme
            40);    // fark etme menzili: pratikte bütün kat

    public Saman(int tileX, int tileY) {
        super(tileX, tileY, "Sahin Saman", STATS, new AStarPathfinder());
    }

    @Override
    public String getSaveKind() {
        return "SAMAN";
    }

    @Override
    public String getSpriteName() {
        return "saman";
    }
}
