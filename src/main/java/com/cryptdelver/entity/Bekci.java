package com.cryptdelver.entity;

import com.cryptdelver.game.Text;

import com.cryptdelver.game.Game;
import java.util.Random;

/**
 * Mahzen Bekçisi: ilk boss, yaratık çağırır.
 *
 * <p>Karşılaştığın ilk boss ve öğrettiği şey <b>kuşatılmamak</b>. Tek başına
 * yavaş ve kaçılabilir; tehlikeli olan, etrafına doğan impleri görmezden gelip
 * ona vurmaya devam etmen. Bu, bossu vurmak için doğru anı seçmeyi öğretiyor —
 * sonraki üç bossun üçü de aynı beceriyi başka bir dille soruyor.</p>
 *
 * <p>Çağırma dövüşün ilk saniyelerinde başlamıyor: hem boss hem sürü aynı anda
 * üstüne binerse kaçacak yer kalmıyordu.</p>
 */
public class Bekci extends Boss {

    /** İki çağırma arasındaki süre, saniye. */
    private static final double SUMMON_INTERVAL = 6.0;

    /** İlk çağırmadan önceki hazırlık süresi. */
    private static final double FIRST_SUMMON_DELAY = 4.5;

    /** Her çağırmada kaç yaratık gelir. */
    private static final int MINIONS_PER_SUMMON = 2;

    /** Kattaki düşman sayısı bunu aşarsa çağırmayı bırakır. */
    private static final int ENEMY_LIMIT = 16;

    private static final int[][] SUMMON_SPOTS = {
            {0, -1}, {0, 1}, {-1, 0}, {1, 0}, {-1, -1}, {1, -1}, {-1, 1}, {1, 1}};

    private final Random random = new Random();
    private double summonTimer = FIRST_SUMMON_DELAY;

    public Bekci(int tileX, int tileY) {
        super(tileX, tileY, Text.BOSS_BEKCI, "boss_bekci");
    }

    @Override
    protected void onUpdate(Game game, double delta) {
        summonTimer -= delta;
        if (summonTimer > 0) {
            return;
        }

        summonTimer = SUMMON_INTERVAL;
        summonMinions(game);
    }

    private void summonMinions(Game game) {
        if (game.getEnemies().size() >= ENEMY_LIMIT) {
            return;
        }

        int summoned = 0;
        for (int[] spot : shuffledSpots()) {
            if (summoned >= MINIONS_PER_SUMMON) {
                break;
            }

            int x = getTileX() + spot[0];
            int y = getTileY() + spot[1];
            if (!game.isTileFree(x, y, this)) {
                continue;
            }

            game.addEnemy(new Imp(x, y));
            summoned++;
        }

        if (summoned > 0) {
            game.getMessageLog().combat(Text.MSG_BOSS_SUMMON.get(getName(), summoned));
        }
    }

    /** Yaratıklar hep aynı yönde belirmesin diye komşu kareleri karıştırır. */
    private int[][] shuffledSpots() {
        int[][] spots = SUMMON_SPOTS.clone();
        for (int i = spots.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int[] temp = spots[i];
            spots[i] = spots[j];
            spots[j] = temp;
        }
        return spots;
    }
}
