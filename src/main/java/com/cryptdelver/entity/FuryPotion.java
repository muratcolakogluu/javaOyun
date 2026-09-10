package com.cryptdelver.entity;

import com.cryptdelver.game.Game;

/**
 * Öfke iksiri: bir süre daha sert vurursun.
 *
 * <p>Bombanın tersi bir araç: bomba kalabalığı dağıtıyor, öfke tek bir şeyi
 * hızla indirmeyi sağlıyor. Pratikte boss iksiri — dövüşün ortasında içmek
 * dört-beş saniyelik bir pencere açıyor.</p>
 *
 * <p>Verdiği vuruş bonusu ekipmandan gelmiyor, yani "büyülü parça bossunkinden
 * iyi olamaz" kuralına dokunmuyor: iksir tükeniyor, ekipman kalıcı.</p>
 */
public class FuryPotion extends Item {

    /** Etkinin süresi, saniye. */
    public static final double DURATION = 10.0;

    /** Süre boyunca eklenen vuruş gücü. */
    public static final int ATTACK_BONUS = 4;

    public FuryPotion(int tileX, int tileY) {
        super(tileX, tileY, "Öfke İksiri");
    }

    @Override
    public boolean use(Game game) {
        game.getPlayer().applyFury(DURATION);
        game.getMessageLog().add("Öfke iksiri: vuruşun sertleşti (+" + ATTACK_BONUS + ").");
        return true;
    }

    @Override
    public String getDescription() {
        return (int) DURATION + " saniye +" + ATTACK_BONUS + " vurus";
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
    public String getSaveKind() {
        return "FURY";
    }

    @Override
    public String getSpriteName() {
        return "potion_fury";
    }
}
