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
    private boolean muted;
    private Difficulty difficulty = Difficulty.NORMAL;
    private boolean autoSave = true;

    /** Ayarlanmış ses seviyesi, 0 ile 1 arası (sessize almadan bağımsız). */
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

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty == null ? Difficulty.NORMAL : difficulty;
    }

    /**
     * Her kat inişinde kendiliğinden kaydedilsin mi.
     *
     * <p>Varsayılan açık: {@code F5} tuşunun varlığını bilmeyen oyuncunun ilk
     * ölümünde bir saatlik ilerlemeyi kaybetmesi kötü bir karşılama olurdu.
     * Kaydetmeyi bir <em>karar</em> olarak yaşamak isteyen kapatabiliyor.</p>
     */
    public boolean isAutoSave() {
        return autoSave;
    }

    public void setAutoSave(boolean autoSave) {
        this.autoSave = autoSave;
    }

    public void toggleAutoSave() {
        autoSave = !autoSave;
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }
}
