package com.cryptdelver.game;

/**
 * Zindanın yirmi katının tasarımı: her beş kat bir bölge.
 *
 * <p>Bölgeler bossla bitiyor, yani oyunun ritmi zaten var olan "beş katta bir
 * boss" kuralıyla aynı hizada: dört kat inersin, beşincide bölgenin sahibiyle
 * hesaplaşırsın, sonrası yeni bir yer. Zindan böylece rastgele üretilmiş
 * katlar dizisi olmaktan çıkıp bir yolculuğa dönüşüyor.</p>
 *
 * <p>Bölge yalnızca <em>görüntüyü</em> değiştiriyor; zorluk derinlikten
 * geliyor. Bölgeye ayrıca zorluk yüklemek iki kaldıracı birbirine karıştırır
 * ve dengeyi okumayı zorlaştırırdı.</p>
 *
 * <p>Renkler haritanın üstüne ince bir perde olarak seriliyor. Karo
 * çizimlerini boyamak denenebilirdi ama taş neredeyse gri: doygunluğu artırmak
 * griyi renklendirmiyor. Perde ise hem işe yarıyor hem de varlıkların üstüne
 * binmediği için oyuncu ve düşmanlar okunaklı kalıyor.</p>
 */
public enum FloorTheme {

    /** 1-5: giriş katları. Kuru taş, soğuk ve nötr. */
    MAHZEN("Mahzen", "#3a4a6b", 0.10, "#243044", 0.20),

    /** 6-10: su sızmış, yosun tutmuş katlar. */
    SARNIC("Sarnic", "#2f6b57", 0.14, "#155a6b", 0.24),

    /** 11-15: derindeki sıcak damarlar. */
    KORLUK("Korluk", "#8a3a22", 0.16, "#6b2438", 0.26),

    /** 16-20: Kript Lordunun kendi katları. */
    KRIPT("Kript", "#5a2f7a", 0.20, "#33245e", 0.30);

    /** Kaç katta bir bölge değişir. */
    public static final int FLOORS_PER_THEME = 5;

    /** Oyunun son katı; buradan aşağısı yok. */
    public static final int MAX_DEPTH = FLOORS_PER_THEME * 4;

    private final String label;
    private final String roomTint;
    private final double roomAlpha;
    private final String caveTint;
    private final double caveAlpha;

    FloorTheme(String label, String roomTint, double roomAlpha,
               String caveTint, double caveAlpha) {
        this.label = label;
        this.roomTint = roomTint;
        this.roomAlpha = roomAlpha;
        this.caveTint = caveTint;
        this.caveAlpha = caveAlpha;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Haritanın üstüne serilen perdenin rengi, {@code #rrggbb}.
     *
     * <p>Bölgenin rengi tek başına yetmiyordu: odalı ve mağara katlar aynı
     * görünüyor, tek fark harita şekli oluyordu. Mağaralar artık kendi
     * rengini alıyor — aynı bölgenin daha ham, daha ıslak, daha kapalı hâli.
     * Böylece kat değiştirdiğinde nereye girdiğini renkten de anlıyorsun.</p>
     *
     * @param cave kat mağara mı (koridorlu değil, oyulmuş)
     */
    public String getTint(boolean cave) {
        return cave ? caveTint : roomTint;
    }

    /**
     * Perdenin yoğunluğu.
     *
     * <p>Derin bölgelerde biraz daha koyu: aşağı indikçe zindanın kapandığı
     * hissi renkten de okunuyor. Mağaralar her bölgede odalardan koyu —
     * dar ve kapalı olmaları renge de yansıyor.</p>
     */
    public double getTintAlpha(boolean cave) {
        return cave ? caveAlpha : roomAlpha;
    }

    /** Bu bölgenin son katı; boss orada bekliyor. */
    public int getLastDepth() {
        return (ordinal() + 1) * FLOORS_PER_THEME;
    }

    /**
     * Verilen derinliğin bölgesi.
     *
     * <p>Yirminin altındaki katlar diziyi taşırmıyor: son bölge son kata kadar
     * sürüyor ve zaten oradan aşağı inilemiyor.</p>
     */
    public static FloorTheme forDepth(int depth) {
        int index = (Math.max(1, depth) - 1) / FLOORS_PER_THEME;
        return values()[Math.min(index, values().length - 1)];
    }
}
