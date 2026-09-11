package com.cryptdelver.entity;

import com.cryptdelver.game.Text;

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
 *
 * <h2>Kısa işaret</h2>
 * <p>Orkun kalkan kolu gibi vuruşundan önce hazırlanıyor ama yalnızca
 * {@code 0.4} saniye — orkunkinin üçte ikisi. Fark kasıtlı: ork ağır ve
 * okunaklı, şaman hızlı ve zor. İkisi de aynı dili konuşuyor (duran düşman
 * vuracak demek) ama şamanın cümlesi daha kısa, yani aynı refleks daha
 * keskin bir zamanlama istiyor.</p>
 */
public class Saman extends Enemy {

    private static final EnemyStats STATS = new EnemyStats(
            10,     // can
            6,      // vuruş gücü — sert vuruyor
            1,      // savunma
            4.2,    // hız (kare/saniye)
            1.1,    // vuruş arası bekleme
            40,     // fark etme menzili: pratikte bütün kat
            0.4);   // hazırlık: hızlı gövde, kısa işaret

    public Saman(int tileX, int tileY) {
        super(tileX, tileY, Text.ENEMY_SAMAN, STATS, new AStarPathfinder());
    }

    @Override
    public String getKind() {
        return "SAMAN";
    }

    @Override
    public String getSpriteName() {
        return "saman";
    }
}
