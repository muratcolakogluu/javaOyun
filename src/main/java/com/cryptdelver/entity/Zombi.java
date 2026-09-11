package com.cryptdelver.entity;

import com.cryptdelver.game.Text;

import com.cryptdelver.ai.GreedyPathfinder;

/**
 * Zombi: yavaş ama dayanıklı; Sarnıç katlarında beliriyor.
 *
 * <p>Oyundaki diğer düşmanlar hız üstünden tehdit ediyor: goblin koşup vuruyor,
 * imp sürü hâlinde geliyor. Zombi tersini yapıyor — çok yavaş ama canı yüksek
 * ve vuruşu sert. Kaçarak geçmek kolay, ama kalabalıkta yolunu kapatıyor ve
 * onu indirmek kılıcının dayanıklılığından ciddi bir pay alıyor.</p>
 *
 * <p>Böylece "her düşmanı öldürmeli miyim" sorusu yıpranma tarafından da
 * sorulmuş oluyor.</p>
 */
public class Zombi extends Enemy {

    private static final EnemyStats STATS = new EnemyStats(
            18,     // can — oyundaki en dayanıklı sıradan düşman
            4,      // vuruş gücü
            1,      // savunma
            1.8,    // hız (kare/saniye) — oyuncunun üçte biri
            1.5,    // vuruş arası bekleme: yavaş ama ağır
            8);     // fark etme menzili

    public Zombi(int tileX, int tileY) {
        super(tileX, tileY, Text.ENEMY_ZOMBI, STATS, new GreedyPathfinder());
    }

    @Override
    public String getKind() {
        return "ZOMBI";
    }

    @Override
    public String getSpriteName() {
        return "zombi";
    }
}
