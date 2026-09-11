package com.cryptdelver.entity;

import com.cryptdelver.game.Text;

import com.cryptdelver.game.Game;

/**
 * Kaçış iksiri: seni doğrudan merdivenin başına ışınlar.
 *
 * <p>Zindanın en sinir bozucu anı, katın öbür ucunda canın bitmek üzereyken
 * merdivene kadar bütün yolu kalabalığın içinden geri yürümek. Bu iksir o anı
 * çözüyor.</p>
 *
 * <p>Merdiven boss tarafından tutuluyorsa da çalışıyor — ama seni bossun
 * kucağına bırakıyor. Yani kurtarıcı olmaktan çıkıp riskli bir hamleye
 * dönüşüyor; "her durumda güvenli" bir eşya olmasını istemedim.</p>
 */
public class EscapePotion extends Item {

    public EscapePotion(int tileX, int tileY) {
        super(tileX, tileY, Text.ITEM_ESCAPE);
    }

    @Override
    public boolean use(Game game) {
        return game.teleportToStairs();
    }

    @Override
    public String getDescription() {
        return Text.ITEM_ESCAPE_INFO.get();
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
        return "ESCAPE";
    }

    @Override
    public String getSpriteName() {
        return "potion_escape";
    }
}
