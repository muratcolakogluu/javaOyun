package com.cryptdelver.entity;

import com.cryptdelver.game.Text;

/**
 * Bir parçaya basılabilen büyü.
 *
 * <p><b>Bir parçada bir büyü durur.</b> Yeni bir büyü basmak eskisinin yerine
 * geçiyor — tezgâhta üst üste büyü yığıp parçayı sınırsız güçlendirmek yok.</p>
 *
 * <p>Büyülerin hiçbiri vuruşa ya da savunmaya sayı eklemiyor; bu bilinçli.
 * Kuralı hatırlamak gerekirse: <em>büyülü bir parça bossun bırakacağından iyi
 * olamaz.</em> Sayı ekleseydi bu kural bozulurdu. Onun yerine büyüler <em>başka
 * eksenlerde</em> çalışıyor: menzil, hız, can, yansıma, dayanıklılık. Bu
 * sayede altını nereye harcayacağın gerçek bir seçim oluyor — yükseltme seni
 * tavana yaklaştırıyor, büyü ise tavanı değil oynayış tarzını değiştiriyor.</p>
 *
 * <p>Büyüler ikiye ayrılıyor: kılıca basılanlar saldırıyı, zırha basılanlar
 * ayakta kalmayı değiştiriyor. Sağlamlık ikisinde de var çünkü yıpranma da
 * ikisinde ortak.</p>
 */
public enum Enchantment {

    // ------------------------------------------------------------ kılıç

    /** Öldürdüğün her düşman biraz can veriyor; saldırgan oynayışın ödülü. */
    VAMPIRLIK(Text.ENCHANT_VAMPIRISM, Text.ENCHANT_VAMPIRISM_INFO, 220),

    /**
     * Vuruş bir kare daha uzağa ulaşıyor.
     *
     * <p>Menzil, hasardan bağımsız bir eksen: aynı vuruşu daha güvenli yerden
     * yapıyorsun. Kaçarak dövüşen oyuncunun en çok işine yarayan büyü bu.</p>
     */
    YILDIRIM(Text.ENCHANT_LIGHTNING, Text.ENCHANT_LIGHTNING_INFO, 260),

    /** Vuruşlar arası bekleme kısalıyor; aynı sürede daha çok savuruş. */
    ACELE(Text.ENCHANT_HASTE, Text.ENCHANT_HASTE_INFO, 240),

    // ------------------------------------------------------------- zırh

    /** Sana vuran düşman da hasar alıyor; kalabalığın ortasında işe yarıyor. */
    DIKEN(Text.ENCHANT_THORNS, Text.ENCHANT_THORNS_INFO, 200),

    /** Yavaş yavaş can doluyor; iksir bulamadığın katlarda hayat kurtarıyor. */
    YENILENME(Text.ENCHANT_REGEN, Text.ENCHANT_REGEN_INFO, 280),

    /** Daha hızlı yürüyorsun; vur-kaç oynayışın omurgası. */
    CEVIKLIK(Text.ENCHANT_AGILITY, Text.ENCHANT_AGILITY_INFO, 240),

    // ------------------------------------------------------------- ortak

    /** Parça yarı hızda yıpranıyor; büyücüye daha az altın bırakmanın yolu. */
    SAGLAMLIK(Text.ENCHANT_STURDY, Text.ENCHANT_STURDY_INFO, 160);

    private final Text label;
    private final Text description;
    private final int cost;

    Enchantment(Text label, Text description, int cost) {
        this.label = label;
        this.description = description;
        this.cost = cost;
    }

    /** Ekranda ve mesajlarda görünen ad. */
    public String getLabel() {
        return label.get();
    }

    /** Tezgâhta büyünün yanında yazan tek satırlık açıklama. */
    public String getDescription() {
        return description.get();
    }

    public int getCost() {
        return cost;
    }
}
