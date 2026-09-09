package com.cryptdelver.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * O karede ekranda duran tıklanabilir bölgeler.
 *
 * <p>Çizim katmanı bir satırı çizerken aynı anda buraya kaydediyor. Kayıt
 * çizimle <em>aynı satırda</em> yapıldığı için yerleşim koordinatları tek bir
 * yerde kalıyor; ayrı bir "tıklama alanları" tablosu tutsaydık ikisi er geç
 * birbirinden ayrı düşerdi.</p>
 *
 * <p>{@link #add} hem kaydediyor hem de farenin o an o bölgenin üstünde olup
 * olmadığını söylüyor. Tek çağrıyla iki iş: çizen kod dönen değere bakıp satırı
 * vurgulu çiziyor, yani üstüne gelme efekti fazladan bir hesap
 * gerektirmiyor.</p>
 */
public class ClickMap {

    private record Region(UiAction action, double x, double y, double width, double height) {

        boolean contains(double px, double py) {
            return px >= x && py >= y && px < x + width && py < y + height;
        }
    }

    private final List<Region> regions = new ArrayList<>();

    private double mouseX = -1;
    private double mouseY = -1;

    /** Yeni kareye başlarken önceki karenin bölgeleri siliniyor. */
    public void clear() {
        regions.clear();
    }

    /**
     * Farenin tuval üzerindeki yeri.
     *
     * <p>Pencere ölçeklenmiş olabilir ama buraya gelen koordinatlar tuvalin
     * kendi koordinatları — fare olayları tuvale bağlandığı için dönüşümü
     * JavaFX yapıyor.</p>
     */
    public void setMouse(double x, double y) {
        this.mouseX = x;
        this.mouseY = y;
    }

    /** Fare tuvalin dışına çıktı; hiçbir şeyin üstünde değil. */
    public void clearMouse() {
        setMouse(-1, -1);
    }

    /**
     * Bir bölgeyi kaydeder.
     *
     * @return fare şu anda bu bölgenin üstündeyse {@code true}
     */
    public boolean add(UiAction action, double x, double y, double width, double height) {
        Region region = new Region(action, x, y, width, height);
        regions.add(region);
        return region.contains(mouseX, mouseY);
    }

    /**
     * Verilen noktadaki eylem; hiçbir bölge yoksa {@code null}.
     *
     * <p>Sondan başa bakılıyor: sonra çizilen üstte duruyor, dolayısıyla
     * üst üste binen bölgelerde tıklama görünen öğeye gidiyor.</p>
     */
    public UiAction hit(double x, double y) {
        for (int i = regions.size() - 1; i >= 0; i--) {
            Region region = regions.get(i);
            if (region.contains(x, y)) {
                return region.action();
            }
        }
        return null;
    }

    /** Farenin altındaki eylem; menüde imleci taşımak için kullanılıyor. */
    public UiAction hovered() {
        return hit(mouseX, mouseY);
    }
}
