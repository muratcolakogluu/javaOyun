package com.cryptdelver.game;

/**
 * Zorluk kademesi.
 *
 * <p>İki kaldıraca dokunuyor: kattaki <b>kalabalık</b> ve düşmanların
 * <b>derinlikle güçlenme hızı</b>. Üçüncü bir yol daha vardı — düşmanların
 * taban değerlerini çarpmak — ama o, tür değerlerini ({@code EnemyStats})
 * anlamsızlaştırırdı: iskeletin ne kadar sert olduğu türün tanımında yazılı
 * olmalı, ayarlar penceresinde değil.</p>
 *
 * <p>Zorluk oyun durumunun değil <em>tercihlerin</em> parçası: yeniden
 * başlamak onu sıfırlamıyor ve kayıt dosyasında değil ayar dosyasında
 * duruyor. Oyun ortasında değiştirmek de serbest; bir sonraki kat yeni
 * kademeye göre kuruluyor.</p>
 */
public enum Difficulty {

    /** Daha seyrek kalabalık, yavaş sertleşen düşmanlar. */
    KOLAY("Kolay", 0.7, 0.5),

    /** Oyunun dengelendiği kademe. */
    NORMAL("Normal", 1.0, 1.0),

    /** Kalabalık kat, derinlikle hızla sertleşen düşmanlar. */
    ZOR("Zor", 1.3, 1.6);

    private final String label;
    private final double crowdScale;
    private final double depthScale;

    Difficulty(String label, double crowdScale, double depthScale) {
        this.label = label;
        this.crowdScale = crowdScale;
        this.depthScale = depthScale;
    }

    public String getLabel() {
        return label;
    }

    /** Kattaki düşman sayısını bu oranda ölçekler. */
    public int scaleCrowd(int count) {
        return Math.max(1, (int) Math.round(count * crowdScale));
    }

    /** Derinlikle kazanılan bonusu bu oranda ölçekler. */
    public int scaleDepthBonus(int bonus) {
        return (int) Math.round(bonus * depthScale);
    }

    /** Sıradaki kademe; ayarlar ekranında sağ/sol ile geziliyor. */
    public Difficulty next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public Difficulty previous() {
        return values()[(ordinal() - 1 + values().length) % values().length];
    }
}
