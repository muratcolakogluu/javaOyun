package com.cryptdelver.game;

/**
 * Katın altında sürekli dönen müzik.
 *
 * <p>{@link SoundEffect} tek seferlik olayları anlatıyor: vuruş, ölüm, eşya.
 * Burası tam tersi — çalmayı hiç bırakmayan bir parça. Zindanın yirmi katı
 * boyunca hiç müzik yoktu ve bu, bölgelerin renk dışında birbirinden
 * ayrılmadığı anlamına geliyordu: Sarnıç yeşil bir Mahzendi.</p>
 *
 * <p>Her bölgenin kendi parçası var ve hepsi aynı çalgıdan çıkıyor: Mahzen la
 * minörde sakin, Sarnıç dorian modda akan, Korluk fa majörde sıcak, Kript mi
 * minörde gizemli. Yani bölgeler ayrı ama oyun tek bir yer olmaya devam
 * ediyor.</p>
 *
 * <p><b>Melodi, uğultu değil.</b> İlk denemede her bölge uzayan bir akordu ve
 * sonuç korkutucuydu; akorları akortlu yapmak da kurtarmadı, çünkü sorun akort
 * değil türdü — değişmeyen, uzayan bir ses korku filminin dili. Müzik hissi
 * notaların <em>hareket etmesinden</em> geliyor.</p>
 *
 * <p>Boss katlarının kendi parçası var ve bölgeninkinin <em>yerine</em>
 * geçiyor. Müziğin değişmesi, oyuncunun "burası başka bir yer" diye anladığı
 * ilk şey — merdivenden inip bunu duyduğunda ne olacağını daha bossu görmeden
 * biliyorsun. Gerilimi de ritimden alıyor, akortsuzluktan değil.</p>
 */
public enum Ambience {

    MAHZEN("amb_mahzen"),
    SARNIC("amb_sarnic"),
    KORLUK("amb_korluk"),
    KRIPT("amb_kript"),

    /** Boss katı: re minörde nabızlı; bölge parçasının yerine geçiyor. */
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
