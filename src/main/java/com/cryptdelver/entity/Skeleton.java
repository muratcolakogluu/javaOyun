package com.cryptdelver.entity;

import com.cryptdelver.game.Text;

import com.cryptdelver.ai.AStarPathfinder;

/**
 * İskelet: yavaş, dayanıklı, sert vurur ve <em>akıllı</em>. Uzaktan fark eder,
 * peşini bırakmaz; boştayken nöbet tutar, yerinden kıpırdamaz.
 *
 * <p>Yol bulucusu A*: duvarın arkasına saklanmak işe yaramaz, koridorun
 * etrafından dolaşıp gelir. İmpten farkı yalnızca sayılar değil, kafası —
 * ve bu fark, {@code Enemy} sınıfının tek satırına dokunmadan sadece kurucuya
 * verilen nesneyle sağlanıyor.</p>
 *
 * <p>Oyuncudan belirgin şekilde yavaş olması bilinçli: peşini bırakmayan bir
 * düşmandan kaçıp toparlanmak mümkün olsun, ama dar koridorda sıkışırsan
 * bedelini ödeyesin.</p>
 */
public class Skeleton extends Enemy {

    private static final EnemyStats STATS = new EnemyStats(
            10,     // can
            4,      // vuruş gücü
            1,      // savunma: kemik kalkan gibi çalışıyor
            3.0,    // hız (kare/saniye)
            1.25,   // vuruş arası bekleme
            11);    // fark etme menzili (kare)

    public Skeleton(int tileX, int tileY) {
        super(tileX, tileY, Text.ENEMY_SKELETON, STATS, new AStarPathfinder());
    }

    @Override
    public String getKind() {
        return "SKELETON";
    }

    @Override
    public String getSpriteName() {
        return "skeleton";
    }
}
