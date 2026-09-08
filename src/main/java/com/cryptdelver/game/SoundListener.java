package com.cryptdelver.game;

/**
 * Oyunun ses olaylarını dinleyen taraf.
 *
 * <p>Varsayılanı hiçbir şey yapmayan {@link #SILENT}: testler ve başsız
 * çalıştırmalar ses kütüphanesine hiç dokunmuyor. Pencere açıldığında
 * {@code ui} katmanı gerçek çalıcıyı takıyor.</p>
 */
@FunctionalInterface
public interface SoundListener {

    /** Hiçbir şey çalmayan dinleyici; oyunun varsayılanı. */
    SoundListener SILENT = effect -> {
    };

    void play(SoundEffect effect);
}
