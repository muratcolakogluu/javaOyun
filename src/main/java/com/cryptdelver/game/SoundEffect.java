package com.cryptdelver.game;

/**
 * Oyunun çıkardığı ses olayları.
 *
 * <p>Oyun mantığı "hangi dosya çalsın" bilmiyor, yalnızca <em>ne olduğunu</em>
 * söylüyor. Dosya adları ve çalma işi {@code ui} katmanında; bu ayrım sayesinde
 * kurallar hâlâ ses kütüphanesi olmadan test edilebiliyor.</p>
 */
public enum SoundEffect {

    /** Oyuncu silahını savurdu (isabet olsun olmasın). */
    SWING("swing"),

    /** Oyuncunun vuruşu bir düşmana isabet etti. */
    HIT("hit"),

    /** Oyuncu hasar aldı. */
    HURT("hurt"),

    /** Bir düşman öldü. */
    KILL("kill"),

    /** Yerden eşya alındı. */
    PICKUP("pickup"),

    /** İksir içildi. */
    POTION("potion"),

    /** Silah ya da zırh kuşanıldı. */
    EQUIP("equip"),

    /** Bir alt kata inildi. */
    STAIRS("stairs"),

    /** Boss belirdi. */
    BOSS("boss"),

    /** Oyuncu öldü. */
    DEATH("death");

    private final String fileName;

    SoundEffect(String fileName) {
        this.fileName = fileName;
    }

    /** Uzantısız dosya adı; {@code ui} katmanı buna göre yüklüyor. */
    public String getFileName() {
        return fileName;
    }
}
