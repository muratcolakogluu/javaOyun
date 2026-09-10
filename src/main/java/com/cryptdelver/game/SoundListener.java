package com.cryptdelver.game;

/**
 * Oyunun ses olaylarını dinleyen taraf.
 *
 * <p>Varsayılanı hiçbir şey yapmayan {@link #SILENT}: testler ve başsız
 * çalıştırmalar ses kütüphanesine hiç dokunmuyor. Pencere açıldığında
 * {@code ui} katmanı gerçek çalıcıyı takıyor.</p>
 *
 * <p>Ortam sesi metotlarının gövdesi boş bırakıldı, {@code abstract}
 * yapılmadı: ses <em>oynanışın çalışması için gerekli değil</em> ve bunu
 * dinlemek istemeyen bir taraf sırf derlensin diye iki boş metot yazmak
 * zorunda kalmasın. Aynı gerekçe {@link #SILENT}'in var oluş sebebi.</p>
 */
public interface SoundListener {

    /** Hiçbir şey çalmayan dinleyici; oyunun varsayılanı. */
    SoundListener SILENT = new SoundListener() {
        @Override
        public void play(SoundEffect effect) {
            // Sessiz.
        }
    };

    /** Tek seferlik bir olay: vuruş, ölüm, eşya. */
    void play(SoundEffect effect);

    /**
     * Kat değişti; altta dönen ses bu olsun.
     *
     * <p>Aynı ses ikinci kez istenirse çalan taraf onu baştan başlatmıyor:
     * aynı bölgede kat değiştirmek müziği kesmemeli.</p>
     */
    default void playAmbience(Ambience ambience) {
        // Sessiz.
    }

    /** Altta dönen sesi susturur: ölüm, zafer, menüye dönüş. */
    default void stopAmbience() {
        // Sessiz.
    }
}
