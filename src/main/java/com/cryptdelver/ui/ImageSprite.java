package com.cryptdelver.ui;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

/**
 * Hazır bir PNG dosyasından çizen sprite.
 *
 * <p>Ölçek, resmin kutuya sığdırılmasıyla değil <em>kaynak piksel boyutuyla</em>
 * hesaplanıyor: pakette bir zindan karesi {@value #SOURCE_TILE} piksel, o yüzden
 * her kaynak piksel {@code size / 16} ekran pikseline karşılık geliyor. Bunun
 * sonucu: 6×7'lik sikke küçücük, 16×28'lik şövalye bir kare eninde ama boyu
 * karenin dışına taşacak kadar uzun, 32×36'lık boss iki kare eninde çiziliyor —
 * yani paketin kendi oran duygusu korunuyor.</p>
 *
 * <p>Dikeyde figür karenin <em>tabanına</em> oturuyor: uzun karakterler karenin
 * içinde ayakta duruyor, taşan kısım yukarı çıkıyor. Ortalasaydık ayakları
 * havada kalırdı.</p>
 */
public class ImageSprite implements Sprite {

    /** Paketin kendi kare boyutu: 16×16'lık bir resim tam bir zindan karesi. */
    private static final double SOURCE_TILE = 16.0;

    private final Image image;

    public ImageSprite(Image image) {
        this.image = image;
    }

    @Override
    public void draw(GraphicsContext gc, double centerX, double centerY, double size) {
        double scale = size / SOURCE_TILE;
        double width = image.getWidth() * scale;
        double height = image.getHeight() * scale;

        double x = centerX - width / 2;
        double y = centerY + size / 2 - height;

        gc.drawImage(image, x, y, width, height);
    }
}
