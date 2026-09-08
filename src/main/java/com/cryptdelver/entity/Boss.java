package com.cryptdelver.entity;

import com.cryptdelver.ai.AStarPathfinder;
import com.cryptdelver.game.Game;
import com.cryptdelver.game.LootTable;
import java.util.Random;

/**
 * Kript Lordu: belirli katlarda merdiveni tutan boss.
 *
 * <p>Öldürülmeden aşağı inilemez — inişin bir bedeli olsun diye. Sıradan
 * düşmanlardan üç şeyle ayrılıyor:</p>
 * <ul>
 *   <li><b>Menzili tüm harita:</b> nerede olursan ol peşine düşer, A* ile.</li>
 *   <li><b>Yaratık çağırır:</b> belli aralıklarla etrafına fare doğurur, bu
 *       yüzden onu görmezden gelip beklemek işe yaramaz.</li>
 *   <li><b>Ganimet bırakır:</b> öldüğünde altın ve bir balta düşürür.</li>
 * </ul>
 *
 * <p>Bu üç davranışın üçü de {@link Enemy} sınıfındaki genişleme noktalarıyla
 * eklendi; ortak tur akışının tek satırı değişmedi.</p>
 */
public class Boss extends Enemy {

    /**
     * Boss değerleri, ilk karşılaşmanın (5. kat) kazanılabilir olmasına göre
     * ayarlandı.
     *
     * <p>5. katta oyuncu 2. kademe takımla geliyor: 8 vuruş, 2 savunma, 20 can.
     * Bu değerlerle dövüş kabaca "boss 4 saniyede düşer, oyuncu 7 saniyede
     * ölür" dengesinde — yani ayakta durup vuruşmak <em>yetiyor</em>, ama hata
     * payı dar. Önceki hâlinde ikisi de 4 saniyeydi, yani yazı tura atıyordun.</p>
     *
     * <p>Asıl kaçış yolu hız farkı: oyuncu saniyede 6 kare, boss 2.2. Vurup
     * geri çekilerek dövüşürsen hiç hasar almadan da bitirebilirsin.</p>
     */
    private static final EnemyStats STATS = new EnemyStats(
            40,     // can
            5,      // vuruş gücü
            3,      // savunma: kalın zırh, kılıcın kademesi önemli
            2.2,    // hız (kare/saniye) — yavaş ama durmak bilmez
            1.4,    // vuruş arası bekleme: tek hatada ölmeyesin
            60);    // fark etme menzili: pratikte tüm harita

    /** İki çağırma arasındaki süre, saniye. */
    private static final double SUMMON_INTERVAL = 6.0;

    /**
     * İlk çağırmadan önceki hazırlık süresi.
     *
     * <p>Dövüşün ilk saniyelerinde yaratık gelmiyor: hem boss hem sürü aynı
     * anda üstüne binerse kaçacak yer kalmıyordu.</p>
     */
    private static final double FIRST_SUMMON_DELAY = 4.5;

    /** Her çağırmada kaç yaratık gelir. */
    private static final int MINIONS_PER_SUMMON = 2;

    /** Kattaki düşman sayısı bunu aşarsa çağırmayı bırakır. */
    private static final int ENEMY_LIMIT = 16;

    private static final int BASE_GOLD_DROP = 40;
    private static final int GOLD_DROP_PER_DEPTH = 15;

    private static final int[][] SUMMON_SPOTS = {
            {0, -1}, {0, 1}, {-1, 0}, {1, 0}, {-1, -1}, {1, -1}, {-1, 1}, {1, 1}};

    private final Random random = new Random();
    private double summonTimer = FIRST_SUMMON_DELAY;

    public Boss(int tileX, int tileY) {
        super(tileX, tileY, "Kript Lordu", STATS, new AStarPathfinder());
    }

    /** Sayacı işletir ve zamanı gelince yaratık çağırır. */
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

            game.addEnemy(new Rat(x, y));
            summoned++;
        }

        if (summoned > 0) {
            game.getMessageLog().add("Kript Lordu " + summoned + " yaratık çağırdı!");
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

    /**
     * Öldüğünde altın ve <em>katın bir üst kademesinden</em> silah bırakır.
     *
     * <p>Bossu geçmek bu yüzden sıradan katları soymaktan hızlı güçlendiriyor:
     * normalde birkaç kat daha inmeden bulamayacağın silahı erken veriyor.</p>
     */
    @Override
    public void onDeath(Game game) {
        int depth = game.getDepth();
        int gold = BASE_GOLD_DROP + GOLD_DROP_PER_DEPTH * depth;

        game.addGroundItem(new Gold(getTileX(), getTileY(), gold));

        Weapon reward = LootTable.weaponForTier(LootTable.bossTierForDepth(depth),
                getTileX(), getTileY());
        game.addGroundItem(reward);

        game.getMessageLog().add("Kript Lordu düştü! " + reward.getName() + " bıraktı.");
    }

    @Override
    public String getSaveKind() {
        return "BOSS";
    }

    @Override
    public String getSpriteName() {
        return "boss";
    }

    @Override
    public double getDrawScale() {
        return 1.35;
    }
}
