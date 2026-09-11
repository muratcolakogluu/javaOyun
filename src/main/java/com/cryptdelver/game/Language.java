package com.cryptdelver.game;

/**
 * Oyunun konuştuğu dil.
 *
 * <p>Varsayılan Türkçe: oyun Türkçe yazıldı, metinlerin aslı bu dilde ve
 * çeviri İngilizceye yapıldı. Bunu tersine çevirmek metnin nüansını kaybettirir
 * — özellikle büyücünün ağzından çıkan satırlarda.</p>
 *
 * <p>Ayarlar sayfasında tek satır. Dil değişince ekrandaki her şey anında
 * yeni dile geçiyor; yeniden başlatmak gerekmiyor çünkü hiçbir metin
 * saklanmıyor, hepsi çizim anında {@link Text}'ten okunuyor.</p>
 */
public enum Language {

    TURKCE("Türkçe"),
    ENGLISH("English");

    private final String label;

    Language(String label) {
        this.label = label;
    }

    /**
     * Dilin kendi adı, kendi dilinde.
     *
     * <p>Çevrilmiyor: "Türkçe" satırını arayan biri ekranda "Turkish" yazsa
     * bulamaz. Dil listeleri her zaman böyle yazılır.</p>
     */
    public String getLabel() {
        return label;
    }

    public Language next() {
        Language[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    public Language previous() {
        Language[] all = values();
        return all[(ordinal() - 1 + all.length) % all.length];
    }
}
