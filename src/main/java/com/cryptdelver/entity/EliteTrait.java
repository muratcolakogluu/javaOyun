package com.cryptdelver.entity;

import com.cryptdelver.game.Text;

/**
 * Sıradan bir düşmanı "elit" yapan tek özellik.
 *
 * <p>Bir kat, başka bir katın aynısıydı — yalnızca sayıları büyüktü. Kalabalık
 * sertleşiyordu ama kalabalığın <em>içinde</em> hiçbir şey olmuyordu: yirmi
 * imp, yirmi impti. Elit, katın ortasında "bu farklı" diyeceğin bir an
 * açıyor.</p>
 *
 * <h2>Neden tek özellik</h2>
 * <p>Üç özelliği birden taşıyan bir elit, oyuncunun okuyamayacağı bir yaratık
 * olurdu: neden ölmediğini de neden yetişemediğini de aynı anda anlaman
 * gerekirdi. Tek özellik, aurasının rengine bakıp <b>ne yapman gerektiğini</b>
 * bilmen demek — üçü de farklı bir cevap istiyor.</p>
 *
 * <p>Yeni çizim gerekmiyor: gövde aynı, adının önüne bir sıfat geliyor ve
 * ayağının dibinde renkli bir halka yanıyor. Ucuz olması özellikle önemli,
 * çünkü elitin işi yeni bir düşman türü olmak değil — var olanı bir anlığına
 * başka bir soruya çevirmek.</p>
 */
public enum EliteTrait {

    /**
     * Zırhlı: kabuğu kalın, adımı ağır.
     *
     * <p>Cevabı silah kademesi ya da arkadan vuruş. Yavaş olması kasıtlı:
     * hem kaçabilirsin hem de etrafından dolanabilirsin, yani "vuramıyorum"
     * bir çıkmaz değil.</p>
     */
    ZIRHLI(Text.ELITE_ARMOURED, 0, 0, 4, 0.8, 1.0, 25),

    /**
     * Çevik: hızlı ve sık vuran.
     *
     * <p>Cevabı kaçmak değil — kaçamıyorsun. Ya sıkıştığın yerden sıçrayıp
     * çıkacaksın ya da göğüs göğüse bitireceksin. Canı ve kabuğu sıradan,
     * yani hızlı bitiyor.</p>
     */
    CEVIK(Text.ELITE_SWIFT, 0, 1, 0, 1.6, 0.7, 25),

    /**
     * Kanlı: duvar gibi.
     *
     * <p>Cevabı sabır ve iksir. Hızı ve kabuğu sıradan; tehdidi yalnızca
     * "bitmiyor" olmasından geliyor, yani dövüşü uzatıyor ve zindanın sabrını
     * tüketiyor. Bu yüzden en çok altını o bırakıyor.</p>
     */
    KANLI(Text.ELITE_BLOODY, 14, 3, 0, 1.0, 1.0, 45);

    private final Text label;
    private final int bonusHp;
    private final int bonusAttack;
    private final int bonusDefense;
    private final double speedScale;
    private final double cooldownScale;
    private final int gold;

    EliteTrait(Text label, int bonusHp, int bonusAttack, int bonusDefense,
               double speedScale, double cooldownScale, int gold) {
        this.label = label;
        this.bonusHp = bonusHp;
        this.bonusAttack = bonusAttack;
        this.bonusDefense = bonusDefense;
        this.speedScale = speedScale;
        this.cooldownScale = cooldownScale;
        this.gold = gold;
    }

    /** Adın önüne gelen sıfat: "Zırhlı Ork". */
    public String getLabel() {
        return label.get();
    }

    /**
     * Bıraktığı altın.
     *
     * <p>Elit bir ceza değil bir <em>fırsat</em> olmalı: kaçmak da bir cevap
     * ama kalıp devirmenin karşılığı var. Altın miktarı da özelliğe göre —
     * en uzun dövüşü Kanlı çıkarıyor, en çok ödülü de o veriyor.</p>
     */
    public int getGold() {
        return gold;
    }

    /** Sıradan değerlerin üstüne binen güç. */
    void strengthen(Enemy enemy) {
        enemy.strengthen(bonusHp, bonusAttack, bonusDefense);
    }

    double scaleSpeed(double speed) {
        return speed * speedScale;
    }

    double scaleCooldown(double cooldown) {
        return cooldown * cooldownScale;
    }
}
