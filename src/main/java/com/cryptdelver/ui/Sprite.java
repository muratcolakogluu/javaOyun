package com.cryptdelver.ui;

import javafx.scene.canvas.GraphicsContext;

/**
 * Bir varlığın ekrandaki görüntüsü.
 *
 * <p>Çizim, merkezi verilen ve kenarı {@code size} piksel olan bir kutunun
 * içine yapılır. Bu sayede aynı sprite hem haritada (bir tile boyutunda) hem
 * envanter kutucuğunda (daha küçük) kullanılabiliyor.</p>
 *
 * <p>Arayüz olmasının sebebi: bugün şekillerle çiziyoruz ({@link ShapeSprites}),
 * yarın hazır bir PNG paketi bulunca {@link ImageSprite} devreye girecek ve
 * çağıran kodun tek satırı bile değişmeyecek.</p>
 */
@FunctionalInterface
public interface Sprite {

    /**
     * Sprite'ı çizer.
     *
     * @param centerX kutunun merkezinin piksel x'i
     * @param centerY kutunun merkezinin piksel y'si
     * @param size    kutunun kenar uzunluğu, piksel
     */
    void draw(GraphicsContext gc, double centerX, double centerY, double size);
}
