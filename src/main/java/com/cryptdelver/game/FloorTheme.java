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
    MAHZEN(Text.REGION_MAHZEN, "#3a4a6b", 0.10, "#243044", 0.20,
            new String[] {"floor_mahzen_a", "floor_mahzen_b", "floor_mahzen_c"},
            new String[] {"wall_mahzen"}, "wallmark_mahzen"),

    /** 6-10: su sızmış, yosun tutmuş katlar. */
    SARNIC(Text.REGION_SARNIC, "#2f6b57", 0.14, "#155a6b", 0.24,
            new String[] {"floor_sarnic_a", "floor_sarnic_b", "floor_sarnic_c"},
            new String[] {"wall_sarnic", "wall_mahzen"}, "wallmark_sarnic"),

    /** 11-15: derindeki sıcak damarlar. */
    KORLUK(Text.REGION_KORLUK, "#8a3a22", 0.16, "#6b2438", 0.26,
            new String[] {"floor_korluk_a", "floor_korluk_b", "floor_korluk_c"},
            new String[] {"wall_korluk", "wall_korluk_b", "wall_mahzen"}, "wallmark_korluk"),

    /** 16-20: Kript Lordunun kendi katları. */
    KRIPT(Text.REGION_KRIPT, "#5a2f7a", 0.20, "#33245e", 0.30,
            new String[] {"floor_kript_a", "floor_kript_b", "floor_kript_c"},
            new String[] {"wall_kript"}, "wallmark_kript");

    /** Kaç katta bir bölge değişir. */
    public static final int FLOORS_PER_THEME = 5;

    /** Oyunun son katı; buradan aşağısı yok. */
    public static final int MAX_DEPTH = FLOORS_PER_THEME * 4;

    private final Text label;
    private final String roomTint;
    private final double roomAlpha;
    private final String caveTint;
    private final double caveAlpha;
    private final String[] floorSprites;
    private final String[] wallSprites;
    private final String wallMarkSprite;

    FloorTheme(Text label, String roomTint, double roomAlpha,
               String caveTint, double caveAlpha,
               String[] floorSprites, String[] wallSprites, String wallMarkSprite) {
        this.label = label;
        this.roomTint = roomTint;
        this.roomAlpha = roomAlpha;
        this.caveTint = caveTint;
        this.caveAlpha = caveAlpha;
        this.floorSprites = floorSprites;
        this.wallSprites = wallSprites;
        this.wallMarkSprite = wallMarkSprite;
    }

    public String getLabel() {
        return label.get();
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

    /**
     * Bölgenin zemin karoları.
     *
     * <p>Tek bir karo değil birkaçı: aynı resmi kırk kere yan yana koymak
     * zemini duvar kâğıdına çeviriyordu, göz tekrarı hemen yakalıyor. Üç
     * çeşidi karıştırmak aynı taşı düzensiz döşenmiş gibi gösteriyor.</p>
     *
     * <p>Çeşitler bölgeye göre seçildi: Mahzen'de düzgün taş, Sarnıç'ta
     * çatlamış ve oyulmuş, Korluk'ta molozlu, Kript'te yine düzgün — son
     * bölge yapılmış bir yer, çökmüş değil.</p>
     */
    public String[] getFloorSprites() {
        return floorSprites.clone();
    }

    /**
     * Bölgenin duvar karosu.
     *
     * <p>Asıl kimliği bu taşıyor. Renk perdesi tek başına yetmiyordu: dört
     * bölge de aynı tuğla duvarı farklı renkte gösteriyordu, yani "başka bir
     * yerdeyim" hissi yalnızca renkten geliyordu. Sarnıç'ta duvar yosun
     * akıtıyor, Korluk'ta delik deşik — ikisi de bakışta anlaşılıyor.</p>
     *
     * <p>Bölgenin kaç çeşidi olduğu da bir şey anlatıyor: Mahzen ve Kript tek
     * bir düzgün duvar (yapılmış, bakımlı yerler), Korluk üç çeşit (çökmüş bir
     * yer, her duvarı başka türlü yıkılmış).</p>
     */
    public String[] getWallSprites() {
        return wallSprites.clone();
    }

    /**
     * Arada bir duvara konan işaret: sancak, çeşme, kaynak.
     *
     * <p>Seyrek olması önemli — her duvarda olsa süs değil desen olurdu.
     * Duvarın <em>üstünde</em> durduğu için yanıltmıyor: zaten geçilemeyen bir
     * kareye bir şey eklemek oyuncuya yanlış bir şey vaat etmiyor. Zeminlere
     * sütun ya da sandık koymayı denemedik; yürünebilen bir kareye engel gibi
     * duran bir şey koymak tam da o yanlış vaat olurdu.</p>
     */
    public String getWallMarkSprite() {
        return wallMarkSprite;
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
