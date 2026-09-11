package com.cryptdelver.game;

/**
 * Oyuncunun tercihleri: şimdilik ses.
 *
 * <p>Oyun durumundan (can, kat, çanta) ayrı tutuluyor çünkü yaşam döngüsü
 * farklı: yeniden başlamak ayarları sıfırlamaz, ölmek ayarları etkilemez ve
 * ayarlar kayıt dosyasından bağımsız olarak kendi dosyasında saklanır.</p>
 *
 * <p>Ses seviyesi 0 ile 1 arasında tutuluyor; {@link #getEffectiveVolume()}
 * sessize alınmışsa 0 döndürüyor, böylece çalan tarafın iki ayrı alanı
 * birleştirmesi gerekmiyor.</p>
 */
public class Settings {

    /** Ses seviyesi bu adımlarla değişiyor: on kademe. */
    public static final double VOLUME_STEP = 0.1;

    private static final double DEFAULT_VOLUME = 0.5;

    private double volume = DEFAULT_VOLUME;
    private double musicVolume = DEFAULT_VOLUME;
    private boolean muted;
    private Difficulty difficulty = Difficulty.NORMAL;
    private Language language = Language.TURKCE;

    /** Ayarlanmış efekt seviyesi, 0 ile 1 arası (sessize almadan bağımsız). */
    public double getVolume() {
        return volume;
    }

    public void setVolume(double value) {
        this.volume = clamp(value);
    }

    /** Sesi bir kademe artırır ya da azaltır; sınırların dışına taşmaz. */
    public void adjustVolume(double delta) {
        setVolume(volume + delta);
    }

    /**
     * Ortam sesinin kendi seviyesi.
     *
     * <p>Efektlerden ayrı tutuluyor çünkü ikisi farklı şeyler istiyor: vuruşun
     * duyulması <em>gerekiyor</em> — o bir geri bildirim — ama altta dönen
     * müzik bir tercih. Tek bir seviye olsaydı müzikten rahatsız olan oyuncu
     * dövüşün sesini de kısmak zorunda kalırdı.</p>
     *
     * <p>Sıfıra çekmek müziği tamamen kapatıyor; ayrı bir "müzik kapalı"
     * anahtarı eklemedik, çünkü sıfır zaten bunu söylüyor.</p>
     */
    public double getMusicVolume() {
        return musicVolume;
    }

    public void setMusicVolume(double value) {
        this.musicVolume = clamp(value);
    }

    public void adjustMusicVolume(double delta) {
        setMusicVolume(musicVolume + delta);
    }

    /** Çalma anında kullanılacak müzik seviyesi: sessizdeyse 0. */
    public double getEffectiveMusicVolume() {
        return muted ? 0 : musicVolume;
    }

    public int getMusicPercent() {
        return (int) Math.round(musicVolume * 100);
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public void toggleMuted() {
        muted = !muted;
    }

    /** Çalma anında kullanılacak seviye: sessizdeyse 0. */
    public double getEffectiveVolume() {
        return muted ? 0 : volume;
    }

    /** Ekranda gösterilen yüzde. */
    public int getVolumePercent() {
        return (int) Math.round(volume * 100);
    }

    /**
     * Oyunun konustugu dil.
     *
     * <p>Ayarlanan dil ayni anda {@link Text}e de bildiriliyor: metin isteyen
     * taraflarin ayarlari tasimasi gerekmesin diye tek bir sahip var ve o da
     * burasi.</p>
     */
    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language chosen) {
        this.language = chosen == null ? Language.TURKCE : chosen;
        Text.use(this.language);
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty == null ? Difficulty.NORMAL : difficulty;
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }
}
