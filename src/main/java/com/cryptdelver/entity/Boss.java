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
 *   <li><b>Yaratık çağırır:</b> belli aralıklarla etrafına imp doğurur, bu
 *       yüzden onu görmezden gelip beklemek işe yaramaz.</li>
 *   <li><b>Ganimet bırakır:</b> öldüğünde altın ve bir balta düşürür.</li>
 * </ul>
 *
 * <p>Bu üç davranışın üçü de {@link Enemy} sınıfındaki genişleme noktalarıyla
 * eklendi; ortak tur akışının tek satırı değişmedi.</p>
 */
public class Boss extends Enemy {

    /**
     * Boss değerleri, büyücü geldikten sonra yeniden ayarlandı.
     *
     * <p>Artık oyuncunun 5. kata iki farklı hâlde gelmesi mümkün: altınını
     * harcamadan (8 vuruş, 2 savunma) ya da büyücüde takımını tavana
     * yükselterek (10 vuruş, 4 savunma). Değerler ikincisine göre seçildi —
     * yükseltilmiş takımla dövüş kabaca 4 saniye, yükseltmesiz 7 saniye
     * sürüyor. Yani <b>altını harcamamak artık bir seçim, ihmal değil</b>:
     * boss eskisi gibi ayakta durup vuruşmayı affetmiyor.</p>
     *
     * <p>Asıl kaçış yolu yine hız farkı: oyuncu saniyede 6 kare, boss 2.4.
     * Vurup geri çekilerek dövüşürsen hiç hasar almadan da bitirebilirsin.</p>
     */
    private static final EnemyStats STATS = new EnemyStats(
            55,     // can
            6,      // vuruş gücü
            4,      // savunma: kalın zırh, kılıcın kademesi önemli
            2.4,    // hız (kare/saniye) — yavaş ama durmak bilmez
            1.3,    // vuruş arası bekleme: tek hatada ölmeyesin
            60);    // fark etme menzili: pratikte tüm harita

    /**
     * Her yeni bossun bir öncekine göre kazandığı değerler.
     *
     * <p>Katın kendi derinlik bonusu ({@code Game.applyDepthBonus}) tüm
     * düşmanlara ortak ve yavaş artıyor; boss için yeterli değil. Oyuncunun
     * takımı her kademede sıçradığı için bossun da sıçraması gerekiyordu,
     * yoksa 15. kattaki boss 5. kattakinden kolay geliyordu.</p>
     */
    private static final int HP_PER_BOSS = 16;
    private static final int ATTACK_PER_BOSS = 2;
    private static final int DEFENSE_PER_BOSS = 1;

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

    /**
     * Ganimet altını büyücüyle birlikte yükseltildi: altının harcanacağı bir
     * yer olduğu için bossu geçmek artık doğrudan bir sonraki yükseltmeyi
     * ödüyor.
     */
    private static final int BASE_GOLD_DROP = 50;
    private static final int GOLD_DROP_PER_DEPTH = 18;

    /**
     * Bossu geçmenin kalıcı ödülü: azami can.
     *
     * <p>Azami can artık yalnızca buradan geliyor — eskiden her inişte +2
     * geliyordu ve beş katta toplam +10 ediyordu. Ödülü 2'de bıraksak oyuncu
     * derin katlara neredeyse taban canla inerdi; 5 olunca boss başına kazanç
     * eskisinin yarısı oluyor: hâlâ belirgin bir düşüş, ama sertlik cezaya
     * dönmüyor.</p>
     */
    private static final int MAX_HP_REWARD = 5;

    private static final int[][] SUMMON_SPOTS = {
            {0, -1}, {0, 1}, {-1, 0}, {1, 0}, {-1, -1}, {1, -1}, {-1, 1}, {1, 1}};

    private final Random random = new Random();
    private double summonTimer = FIRST_SUMMON_DELAY;

    /**
     * Bölgelerin sahipleri.
     *
     * <p>Dört boss, dört ayrı gövde ve ad. Davranışları aynı — hepsi peşine
     * düşüyor ve yaratık çağırıyor — ama her bölgenin sonunda başka bir şeyle
     * karşılaşmak yolculuğun ilerlediğini gösteriyor. Ayrı sınıf yazmadım:
     * fark eden şey görüntü ve ad, davranış değil; dört sınıf yazmak aynı kodu
     * dört kez kopyalamak olurdu.</p>
     */
    private enum Kind {
        BEKCI("Mahzen Bekcisi", "boss_bekci"),
        BOGUCU("Sarnic Bogucusu", "boss_bogucu"),
        SEYTAN("Kor Seytani", "boss_seytan"),
        LORT("Kript Lordu", "boss");

        private final String label;
        private final String sprite;

        Kind(String label, String sprite) {
            this.label = label;
            this.sprite = sprite;
        }
    }

    private final Kind kind;

    /**
     * @param bossNumber kaçıncı boss: 1 ilk (5. kat), 4 sonuncu (20. kat).
     *                   Gövdesini ve adını buradan alıyor.
     */
    public Boss(int tileX, int tileY, int bossNumber) {
        this(tileX, tileY, kindFor(bossNumber));
    }

    private Boss(int tileX, int tileY, Kind kind) {
        super(tileX, tileY, kind.label, STATS, new AStarPathfinder());
        this.kind = kind;
    }

    /** Kayıttan dönen boss; sırası bilinmiyorsa son bölgenin sahibi sayılıyor. */
    public Boss(int tileX, int tileY) {
        this(tileX, tileY, Kind.LORT);
    }

    private static Kind kindFor(int bossNumber) {
        Kind[] kinds = Kind.values();
        return kinds[Math.clamp(bossNumber, 1, kinds.length) - 1];
    }

    /**
     * Kaçıncı boss olduğuna göre güçlenir.
     *
     * <p>Sayacı bossun kendisi tutmuyor, katı kuran taraf söylüyor: aynı sınıf
     * her derinlikte kullanılıyor, "ben kaçıncıyım" bilgisi ona ait değil.</p>
     *
     * @param bossNumber 1 ilk boss (5. kat), 2 ikinci (10. kat)...
     */
    public void scaleTo(int bossNumber) {
        int steps = Math.max(0, bossNumber - 1);
        strengthen(HP_PER_BOSS * steps, ATTACK_PER_BOSS * steps, DEFENSE_PER_BOSS * steps);
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

            game.addEnemy(new Imp(x, y));
            summoned++;
        }

        if (summoned > 0) {
            game.getMessageLog().add(getName() + " " + summoned + " yaratık çağırdı!");
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
     * Öldüğünde altın, <em>katın bir üst kademesinden</em> silah ve kalıcı
     * azami can bırakır.
     *
     * <p>Bossu geçmek bu yüzden sıradan katları soymaktan hızlı güçlendiriyor:
     * normalde birkaç kat daha inmeden bulamayacağın silahı erken veriyor.</p>
     *
     * <p>Azami can eskiden her inişte artıyordu. Artık yalnızca burada
     * artıyor: can tavanı oyuncunun tek kalıcı büyümesi ve onu merdivenden
     * inmeye değil <em>bossu yenmeye</em> bağlamak, ilerlemeyi beceriyle
     * ilişkilendiriyor. Kaçarak inen oyuncu artık güçlenmiyor.</p>
     */
    @Override
    public void onDeath(Game game) {
        int depth = game.getDepth();
        int gold = BASE_GOLD_DROP + GOLD_DROP_PER_DEPTH * depth;

        game.addGroundItem(new Gold(getTileX(), getTileY(), gold));

        Weapon reward = LootTable.weaponForTier(LootTable.bossTierForDepth(depth),
                getTileX(), getTileY());
        game.addGroundItem(reward);

        game.getPlayer().gainMaxHp(MAX_HP_REWARD);

        game.getMessageLog().add(getName() + " düştü! " + reward.getName() + " bıraktı.");
        game.getMessageLog().addImportant("Gücü sana geçti: +" + MAX_HP_REWARD + " azami can.");
    }

    @Override
    public String getSaveKind() {
        return "BOSS";
    }

    @Override
    public String getSpriteName() {
        return kind.sprite;
    }

    /**
     * Ölçek büyütülmüyor: boss sprite'ı zaten iki kare eninde (32×36 piksel),
     * bir de çarpan uygulasak koridora sığmayacak kadar büyürdü.
     */
    @Override
    public double getDrawScale() {
        return 1.0;
    }
}
