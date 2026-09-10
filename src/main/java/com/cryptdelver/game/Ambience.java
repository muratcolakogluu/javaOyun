package com.cryptdelver.game;

/**
 * Katın altında sürekli dönen ortam sesi.
 *
 * <p>{@link SoundEffect} tek seferlik olayları anlatıyor: vuruş, ölüm, eşya.
 * Burası tam tersi — bir <em>durum</em>. Zindanın yirmi katı boyunca hiç ses
 * yoktu ve bu, bölgelerin renk dışında birbirinden ayrılmadığı anlamına
 * geliyordu: Sarnıç yeşil bir Mahzendi.</p>
 *
 * <p>Ses o farkı renkten daha güçlü taşıyor. Sarnıçta damla, Korlukta ateş
 * çatırtısı, Kriptte doğaüstü bir çınlama var; hepsi aynı alçak uğultunun
 * üstüne biniyor, yani bölgeler ayrı ama oyun tek bir yer olmaya devam
 * ediyor.</p>
 *
 * <p>Boss katlarının kendi sesi var ve bölgeninkinin <em>yerine</em> geçiyor.
 * Müziğin değişmesi, oyuncunun "burası başka bir yer" diye anladığı ilk şey —
 * merdivenden inip bunu duyduğunda ne olacağını daha bossu görmeden
 * biliyorsun.</p>
 */
public enum Ambience {

    MAHZEN("amb_mahzen"),
    SARNIC("amb_sarnic"),
    KORLUK("amb_korluk"),
    KRIPT("amb_kript"),

    /** Boss katı: gerilim; bölge sesinin yerine geçiyor. */
    BOSS("amb_boss");

    private final String fileName;

    Ambience(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    /**
     * Bir bölgenin ortam sesi.
     *
     * <p>Eşleme burada, {@link FloorTheme}'de değil: bölge <em>görüntünün</em>
     * sorusu, bu ise sesin. İkisini aynı yerde tutmak, ilerideki bir ses
     * değişikliğinin renk tablosunu kurcalamasını gerektirirdi.</p>
     */
    public static Ambience forTheme(FloorTheme theme) {
        return switch (theme) {
            case MAHZEN -> MAHZEN;
            case SARNIC -> SARNIC;
            case KORLUK -> KORLUK;
            case KRIPT -> KRIPT;
        };
    }
}
