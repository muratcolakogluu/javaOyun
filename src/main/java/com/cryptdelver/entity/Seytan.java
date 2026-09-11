package com.cryptdelver.entity;

import com.cryptdelver.game.Text;

import com.cryptdelver.game.Game;

/**
 * Kor Şeytanı: canı yarılanınca öfkelenir.
 *
 * <p>Dövüşün ikinci yarısı birinci yarısıyla aynı değil. Taban hâlinde her boss
 * gibi yavaş, yani vur-kaç ile hiç hasar almadan eritilebilir. Ama canı yarıya
 * inince hızı neredeyse oyuncununkine yetişiyor ({@value #ENRAGED_SPEED} kare/sn,
 * oyuncu 6) ve iki katı tempoda vuruyor.</p>
 *
 * <p>Öğrettiği şey <b>bitirme anı</b>: dövüşün ilk yarısında rahat rahat
 * yıprattığın boss, tam "az kaldı" dediğin anda seni kovalamaya başlıyor.
 * Yarıya inmeden önce iksirini içmiş, konumunu almış olman gerekiyor —
 * sonrasında geri çekilmek hâlâ mümkün ama artık ucu ucuna.</p>
 *
 * <p>Hız oyuncununkinin <em>altında</em> bırakıldı, bilerek: kaçış tamamen
 * kapansaydı bu bir dövüş değil bir zar atışı olurdu. Fark 0.8 kare/saniye —
 * koridorda soluklanmaya yetiyor, oyalanmaya yetmiyor.</p>
 */
public class Seytan extends Boss {

    /** Canın bu oranının altında öfkeleniyor. */
    private static final double ENRAGE_RATIO = 0.5;

    private static final double ENRAGED_SPEED = 5.2;

    /** Öfkeliyken vuruş arası bekleme; taban 1.3 saniye. */
    private static final double ENRAGED_COOLDOWN = 0.65;

    private boolean enraged;

    public Seytan(int tileX, int tileY) {
        super(tileX, tileY, Text.BOSS_SEYTAN, "boss_seytan");
    }

    /** Öfkelendi mi; ekran çevresine kızıl bir halka çiziyor. */
    public boolean isEnraged() {
        return enraged;
    }

    /**
     * Öfke bir kez açılıyor ve bir daha kapanmıyor.
     *
     * <p>Canı iyileşse bile geri dönmüyor — dönseydi oyuncu bossu eşiğin
     * etrafında tutmayı öğrenir, dövüş de bir mekanik yerine bir hileye
     * dönüşürdü.</p>
     */
    @Override
    protected void onUpdate(Game game, double delta) {
        if (enraged || getHp() > getMaxHp() * ENRAGE_RATIO) {
            return;
        }

        enraged = true;
        game.getMessageLog().addImportant(Text.MSG_BOSS_ENRAGE.get(getName()));
    }

    @Override
    public double getSpeed() {
        return enraged ? ENRAGED_SPEED : super.getSpeed();
    }

    @Override
    public double getAttackCooldown() {
        return enraged ? ENRAGED_COOLDOWN : super.getAttackCooldown();
    }
}
