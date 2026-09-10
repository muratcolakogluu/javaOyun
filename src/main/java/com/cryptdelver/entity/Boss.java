package com.cryptdelver.entity;

import com.cryptdelver.ai.AStarPathfinder;
import com.cryptdelver.game.Game;
import com.cryptdelver.game.LootTable;

/**
 * Bir bölgenin sahibi: belirli katlarda merdiveni tutan boss.
 *
 * <p>Öldürülmeden aşağı inilemez — inişin bir bedeli olsun diye. Sıradan
 * düşmanlardan üç şeyle ayrılıyor: menzili tüm harita (nerede olursan ol peşine
 * düşer), ganimet bırakır, ve <b>her birinin kendine ait bir yeteneği
 * var.</b></p>
 *
 * <h2>Neden dört sınıf</h2>
 * <p>Uzun süre tek sınıftı ve dört bossun farkı yalnızca adı, gövdesi ve
 * sayılarıydı. Yani 20. kattaki boss, 5. kattakinin büyütülmüş hâliydi:
 * on beş kat inip aynı dövüşü tekrar veriyordun. Sayı büyütmek bir bossu
 * <em>sertleştiriyor</em> ama <em>değiştirmiyor</em>.</p>
 *
 * <p>Şimdi dördü de başka bir soru soruyor:</p>
 * <ul>
 *   <li>{@link Bekci} — yaratık çağırır: kuşatılmadan dövüşmeyi öğretir.</li>
 *   <li>{@link Bogucu} — dört yöne salvo atar: hizadan çıkmayı öğretir.</li>
 *   <li>{@link Seytan} — canı yarılanınca öfkelenir: bitirme anını öğretir.</li>
 *   <li>{@link Lort} — yanına ışınlanır: kaçmanın bittiği yer.</li>
 * </ul>
 *
 * <p>Ortak olan her şey (değerler, ölçekleme, ganimet, ödül) burada duruyor;
 * alt sınıflar yalnızca {@link #onUpdate} ve gerekiyorsa hız/bekleme
 * geçersiz kılıyor. Yani dört sınıf, aynı kodun dört kopyası değil.</p>
 */
public abstract class Boss extends Enemy {

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
     * <p>Taban hız oyuncunun yarısı: vurup geri çekilerek dövüşmek mümkün
     * kalsın. Bunu kıran tek boss Kript Lordu, ve kırma biçimi hız değil
     * ışınlanma — çünkü "daha hızlı boss" oyuncuya bir şey öğretmiyor.</p>
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
     * <p>Katın kendi derinlik bonusu ({@code FloorBuilder.applyDepthBonus}) tüm
     * düşmanlara ortak ve yavaş artıyor; boss için yeterli değil. Oyuncunun
     * takımı her kademede sıçradığı için bossun da sıçraması gerekiyordu,
     * yoksa 15. kattaki boss 5. kattakinden kolay geliyordu.</p>
     */
    private static final int HP_PER_BOSS = 16;
    private static final int ATTACK_PER_BOSS = 2;
    private static final int DEFENSE_PER_BOSS = 1;

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

    private final String spriteName;

    protected Boss(int tileX, int tileY, String name, String spriteName) {
        super(tileX, tileY, name, STATS, new AStarPathfinder());
        this.spriteName = spriteName;
    }

    /**
     * Kaçıncı bossa hangi bölge sahibinin düştüğü.
     *
     * <p>Sırayı boss kendi bilmiyor, katı kuran taraf söylüyor: "ben kaçıncıyım"
     * derinliğin bilgisi, bossun değil. Kayıttan dönerken de aynı yerden
     * çıkıyor, o yüzden ayrı bir alan saklanmıyor.</p>
     *
     * @param bossNumber 1 ilk boss (5. kat), 4 sonuncu (20. kat)
     */
    public static Boss forNumber(int tileX, int tileY, int bossNumber) {
        return switch (Math.clamp(bossNumber, 1, 4)) {
            case 1 -> new Bekci(tileX, tileY);
            case 2 -> new Bogucu(tileX, tileY);
            case 3 -> new Seytan(tileX, tileY);
            default -> new Lort(tileX, tileY);
        };
    }

    /**
     * Kaçıncı boss olduğuna göre güçlenir.
     *
     * @param bossNumber 1 ilk boss (5. kat), 2 ikinci (10. kat)...
     */
    public void scaleTo(int bossNumber) {
        int steps = Math.max(0, bossNumber - 1);
        strengthen(HP_PER_BOSS * steps, ATTACK_PER_BOSS * steps, DEFENSE_PER_BOSS * steps);
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

        game.getMessageLog().combat(getName() + " düştü! " + reward.getName() + " bıraktı.");
        game.getMessageLog().addImportant("Gücü sana geçti: +" + MAX_HP_REWARD + " azami can.");
    }

    @Override
    public String getSaveKind() {
        return "BOSS";
    }

    @Override
    public String getSpriteName() {
        return spriteName;
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
