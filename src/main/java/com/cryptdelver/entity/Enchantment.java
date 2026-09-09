package com.cryptdelver.entity;

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
    VAMPIRLIK("Vampirlik", "her öldürmede 2 can", 220),

    /**
     * Vuruş bir kare daha uzağa ulaşıyor.
     *
     * <p>Menzil, hasardan bağımsız bir eksen: aynı vuruşu daha güvenli yerden
     * yapıyorsun. Kaçarak dövüşen oyuncunun en çok işine yarayan büyü bu.</p>
     */
    YILDIRIM("Yildirim", "vurus bir kare uzaga daha erisir", 260),

    /** Vuruşlar arası bekleme kısalıyor; aynı sürede daha çok savuruş. */
    ACELE("Acele", "daha hizli savuruyorsun", 240),

    // ------------------------------------------------------------- zırh

    /** Sana vuran düşman da hasar alıyor; kalabalığın ortasında işe yarıyor. */
    DIKEN("Diken", "sana vurana 1 hasar", 200),

    /** Yavaş yavaş can doluyor; iksir bulamadığın katlarda hayat kurtarıyor. */
    YENILENME("Yenilenme", "birkac saniyede bir 1 can", 280),

    /** Daha hızlı yürüyorsun; vur-kaç oynayışın omurgası. */
    CEVIKLIK("Ceviklik", "daha hizli yuruyorsun", 240),

    // ------------------------------------------------------------- ortak

    /** Parça yarı hızda yıpranıyor; büyücüye daha az altın bırakmanın yolu. */
    SAGLAMLIK("Saglamlik", "yipranma yari hizda", 160);

    private final String label;
    private final String description;
    private final int cost;

    Enchantment(String label, String description, int cost) {
        this.label = label;
        this.description = description;
        this.cost = cost;
    }

    /** Ekranda ve mesajlarda görünen ad. */
    public String getLabel() {
        return label;
    }

    /** Tezgâhta büyünün yanında yazan tek satırlık açıklama. */
    public String getDescription() {
        return description;
    }

    public int getCost() {
        return cost;
    }
}
