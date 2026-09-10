package com.cryptdelver.entity;

import com.cryptdelver.ai.GreedyPathfinder;
import com.cryptdelver.game.Game;

/**
 * Goblin: hızlı, cesareti canı kadar.
 *
 * <p>İmpten sert vurur ama canı azalınca <em>kaçar</em> — vurup kaçan bir
 * düşman. Bitirmek istiyorsan peşinden gitmen gerekiyor; peşine düşmezsen
 * köşede toparlanıp geri gelmiyor (canı dolmuyor) ama yolunu kesmeye devam
 * ediyor.</p>
 *
 * <p>Oyuna kattığı şey sayı değil karar: her düşmanı kovalamaya değer mi,
 * yoksa merdivene mi koşsan?</p>
 */
public class Goblin extends Enemy {

    private static final EnemyStats STATS = new EnemyStats(
            7,      // can
            3,      // vuruş gücü
            0,      // savunma
            5.5,    // hız (kare/saniye) — oyuncudan biraz yavaş
            0.9,    // vuruş arası bekleme
            9);     // fark etme menzili

    /** Canının bu oranının altına düşünce kaçmaya başlar. */
    private static final double FLEE_HEALTH_RATIO = 0.35;

    public Goblin(int tileX, int tileY) {
        super(tileX, tileY, "Goblin", STATS, new GreedyPathfinder());
    }

    @Override
    protected Stance stanceTowards(Game game) {
        return getHp() < getMaxHp() * FLEE_HEALTH_RATIO ? Stance.FLEE : Stance.CHASE;
    }

    @Override
    public String getKind() {
        return "GOBLIN";
    }

    @Override
    public String getSpriteName() {
        return "goblin";
    }
}
