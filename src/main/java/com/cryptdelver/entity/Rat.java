package com.cryptdelver.entity;

import com.cryptdelver.ai.GreedyPathfinder;
import com.cryptdelver.game.Game;
import java.util.Random;

/**
 * Fare: hızlı ama zayıf. Oyuncuyu geç fark eder, buna karşılık boş durmaz —
 * kare kare dolaştığı için koridorda beklenmedik anda karşına çıkar.
 *
 * <p>Oyuncudan biraz yavaş koşar; kaçmak mümkün, ama sürüsüne yakalanırsan
 * sıkıntı.</p>
 */
public class Rat extends Enemy {

    private static final EnemyStats STATS = new EnemyStats(
            5,      // can
            2,      // vuruş gücü
            0,      // savunma: çıplak
            5.0,    // hız (kare/saniye)
            0.85,   // vuruş arası bekleme
            7);     // fark etme menzili (kare)

    /** Dolaşırken iki adım arasında beklediği en kısa ve en uzun süre. */
    private static final double MIN_IDLE_PAUSE = 0.25;
    private static final double MAX_IDLE_PAUSE = 1.1;

    /** Dolaşırken yön değiştirme olasılığı; düşük tutmak gidişi düzgünleştiriyor. */
    private static final double TURN_CHANCE = 0.3;

    private static final int[][] DIRECTIONS = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};

    private final Random random = new Random();
    private int[] wanderDirection = DIRECTIONS[0];

    public Rat(int tileX, int tileY) {
        super(tileX, tileY, "Fare", STATS, new GreedyPathfinder());
        pickNewDirection();
    }

    /**
     * Oyuncu menzil dışındayken kısa molalarla dolaşır.
     *
     * <p>Yönü her adımda değil ara sıra değiştiriyoruz; yoksa yerinde titreyip
     * duruyor. Duvara toslarsa yeni bir yön deneniyor.</p>
     */
    @Override
    protected boolean startIdleStep(Game game) {
        if (getIdleTimer() > 0) {
            return false;
        }

        if (random.nextDouble() < TURN_CHANCE) {
            pickNewDirection();
        }

        if (!game.tryStartStep(this, wanderDirection[0], wanderDirection[1])) {
            pickNewDirection();
            if (!game.tryStartStep(this, wanderDirection[0], wanderDirection[1])) {
                setIdleTimer(MIN_IDLE_PAUSE);
                return false;
            }
        }

        setIdleTimer(MIN_IDLE_PAUSE + random.nextDouble() * (MAX_IDLE_PAUSE - MIN_IDLE_PAUSE));
        return true;
    }

    private void pickNewDirection() {
        wanderDirection = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
    }

    @Override
    public String getSaveKind() {
        return "RAT";
    }

    @Override
    public String getSpriteName() {
        return "rat";
    }
}
