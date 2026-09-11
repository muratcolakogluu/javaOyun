package com.cryptdelver.entity;

import com.cryptdelver.game.Text;

/**
 * Canı olan, vuran ve vurulabilen varlıkların ortak atası.
 *
 * <p>{@link Player} ile {@link Enemy} arasındaki tek gerçek ortaklık buydu:
 * can, hasar alma, iyileşme, ölüm. Önceden ikisinde de ayrı ayrı yazılıydı;
 * burada birleştirildi. Kazanç sadece tekrar temizliği değil — savunma
 * (zırh) gibi yeni bir kavram eklendiğinde yazılacak <em>tek</em> yer artık
 * belli.</p>
 *
 * <p>Eşyalar bu sınıftan türemez: {@link Item} doğrudan {@link Entity}'dir,
 * çünkü canı yoktur.</p>
 */
public abstract class Combatant extends Entity {

    private int maxHp;
    private int hp;

    protected Combatant(int tileX, int tileY, Text name, int maxHp) {
        super(tileX, tileY, name);
        this.maxHp = maxHp;
        this.hp = maxHp;
    }

    /** Testler icin: adi dogrudan veriyor. */
    protected Combatant(int tileX, int tileY, String name, int maxHp) {
        super(tileX, tileY, name);
        this.maxHp = maxHp;
        this.hp = maxHp;
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public boolean isAlive() {
        return hp > 0;
    }

    /** Hasar alır; can 0'ın altına düşmez. */
    public void takeDamage(int amount) {
        hp = Math.max(0, hp - amount);
        triggerHitFlash();
    }

    /** İyileşir; can en fazla azami cana kadar çıkar. */
    public void heal(int amount) {
        hp = Math.min(maxHp, hp + amount);
    }

    /**
     * Azami canı büyütür ve kazanılan kadarını hemen doldurur.
     *
     * <p>Düşmanları doğarken güçlendirmek için: taze bir düşman yarı canlı
     * doğmamalı.</p>
     */
    protected void increaseMaxHp(int extra) {
        maxHp += extra;
        hp += extra;
    }

    /**
     * Yalnızca azami canı büyütür; mevcut can olduğu gibi kalır.
     *
     * <p>Oyuncunun kat inerken kazandığı can böyle veriliyor: tavan yükseliyor
     * ama inmek <em>iyileştirmiyor</em>. Yaralı indiysen yaralı devam
     * ediyorsun; iksiri hâlâ içmen gerekiyor.</p>
     */
    protected void raiseMaxHp(int extra) {
        maxHp += extra;
    }

    /**
     * Azami canı küçültür; mevcut can yeni tavanı aşarsa oraya çekilir.
     *
     * <p>Kader taşı bunu kullanıyor: verilen şeyin bedeli canın <em>tavanı</em>,
     * o anki canın değil. Yalnızca canını düşürseydi bir iksirle geri
     * alınabilir olurdu ve bedel diye bir şey kalmazdı.</p>
     */
    protected void lowerMaxHp(int less) {
        maxHp = Math.max(1, maxHp - less);
        hp = Math.min(hp, maxHp);
    }

    /** Canı tamamen doldurur. */
    protected void restoreFullHealth() {
        hp = maxHp;
    }

    /**
     * Azami canı verilen tabana döndürür ve canı doldurur.
     *
     * <p>Kazanılmış can tavanını <em>silmek</em> için var. Ölümden sonra yeni
     * oyuna başlarken gerekiyor: bossları yenerek kazandığın can seninle
     * mezara gidiyor, bir sonraki denemeye taşınmıyor.</p>
     */
    protected void resetMaxHp(int base) {
        maxHp = base;
        hp = base;
    }

    /** Vuruş gücü. */
    public abstract int getAttackPower();

    /**
     * Gelen hasarı azaltan savunma.
     *
     * <p>Şimdilik herkes için 0. Zırh sistemi geldiğinde oyuncu kuşandığı
     * zırhtan, düşmanlar da türlerinden bir savunma değeri döndürecek; hasar
     * hesabı zaten bu metodu çağırdığı için başka hiçbir yer değişmeyecek.</p>
     */
    public int getDefense() {
        return 0;
    }
}
