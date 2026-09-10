package com.cryptdelver.entity;

import com.cryptdelver.game.Game;

/**
 * Hız iksiri: bir süre daha hızlı yürürsün.
 *
 * <p>Şifa iksiri canını geri veriyor, bu ise <em>durumdan kaçmanı</em>
 * sağlıyor. İkisi farklı sorunun cevabı: biri "canım azaldı", diğeri "buradan
 * çıkmam lazım". Kalabalık bir odada ya da bossun ortasında sıkıştığında
 * açılan tek kapı bu.</p>
 *
 * <p>Süresi kısa tutuldu: uzun sürseydi zindanı koşarak geçmenin karşılığı
 * olurdu, oysa amaç kaçış anını kurtarmak.</p>
 */
public class HastePotion extends Item {

    /** Etkinin süresi, saniye. */
    public static final double DURATION = 8.0;

    public HastePotion(int tileX, int tileY) {
        super(tileX, tileY, "Hız İksiri");
    }

    @Override
    public boolean use(Game game) {
        game.getPlayer().applyHaste(DURATION);
        game.getMessageLog().add("Hız iksiri: ayakların hafifledi.");
        return true;
    }

    @Override
    public String getDescription() {
        return (int) DURATION + " saniye daha hizli yurursun";
    }

    /** Tek slotta yigildigi icin cantani sikistirmaz: uzerine basmak yeter. */
    @Override
    public boolean isAutoPickedUp() {
        return true;
    }

    @Override
    public boolean isStackable() {
        return true;
    }

    @Override
    public String getKind() {
        return "HASTE";
    }

    @Override
    public String getSpriteName() {
        return "potion_haste";
    }
}
