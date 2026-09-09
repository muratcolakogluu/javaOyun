package com.cryptdelver.entity;

/**
 * Bir parçaya basılabilen büyü.
 *
 * <p><b>Bir parçada bir büyü durur.</b> Yeni bir büyü basmak eskisinin yerine
 * geçiyor — tezgâhta üst üste büyü yığıp parçayı sınırsız güçlendirmek yok.</p>
 *
 * <p>Büyülerin hiçbiri vuruş ya da savunmaya sayı eklemiyor; bu bilinçli.
 * Kuralı hatırlamak gerekirse: <em>büyülü bir parça bossun bırakacağından iyi
 * olamaz.</em> Sayı ekleseydi bu kural bozulurdu. Onun yerine büyüler <em>başka
 * bir eksende</em> çalışıyor: can emmek, hasar yansıtmak, geç yıpranmak. Bu
 * sayede altını nereye harcayacağın gerçek bir seçim oluyor — yükseltme seni
 * tavana yaklaştırıyor, büyü ise tavanı değil oynayış tarzını değiştiriyor.</p>
 */
public enum Enchantment {

    /** Öldürdüğün her düşman biraz can veriyor; saldırgan oynayışın ödülü. */
    VAMPIRLIK("Vampirlik", "her öldürmede 2 can", 220),

    /** Sana vuran düşman da hasar alıyor; kalabalığın ortasında işe yarıyor. */
    DIKEN("Diken", "sana vurana 1 hasar", 200),

    /** Parça yarı hızda yıpranıyor; büyücüye daha az altın bırakmanın yolu. */
    SAGLAMLIK("Sağlamlık", "yıpranma yarı hızda", 160);

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
