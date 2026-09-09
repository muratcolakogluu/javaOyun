package com.cryptdelver.entity;


/**
 * Yıldızkıran: zindanda bulunabilecek en iyi kılıç.
 *
 * <p>Sıradan silahlardan iki şeyle ayrılıyor. <b>Vuruşu tavanın üstünde:</b>
 * en üst kademe Kript Kılıcı +9 verirken bu +14 veriyor, yani bulunduğun katta
 * bossun bırakacağı ödülden de iyi. Bu, oyunun her yerinde geçerli olan
 * "büyülü parça bossunkinden iyi olamaz" kuralının bilinçli tek istisnası —
 * kural yükseltmeyle <em>satın alınan</em> güç için var, şansla bulunan bir
 * efsane için değil.</p>
 *
 * <p><b>İki büyü yuvası var:</b> tek yuva "hangisi dursun" diye seçtiriyor,
 * bu kılıç seçtirmiyor. Vampirlik ve Acele'yi aynı anda taşımak, oyunun geri
 * kalanında mümkün olmayan bir şey.</p>
 *
 * <p>Yükseltilemiyor ve buna gerek de yok: bonusu zaten her katın tavanının
 * üstünde, {@link #canUpgrade(int)} bu yüzden hep {@code false} dönüyor.
 * Yıpranıyor ama çok yavaş — dayanıklılığı en üst kademenin iki katı.</p>
 */
public class LegendWeapon extends Weapon {

    private static final String NAME = "Yıldızkıran";
    private static final int ATTACK_BONUS = 14;
    private static final int DURABILITY = 360;
    private static final int ENCHANT_SLOTS = 2;

    public LegendWeapon(int tileX, int tileY) {
        super(tileX, tileY, NAME, ATTACK_BONUS, "sword_legend", DURABILITY);
    }

    @Override
    public int getEnchantSlots() {
        return ENCHANT_SLOTS;
    }

    @Override
    public String getSaveKind() {
        return "LEGEND";
    }
}
