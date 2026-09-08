package com.cryptdelver.ui;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

/**
 * Hazır bir PNG dosyasından çizen sprite.
 *
 * <p>Şu an kullanılmıyor: {@code resources/assets/sprites} klasörü boş olduğu
 * için {@link SpriteRegistry} şekil çizimlerine düşüyor. Klasöre
 * {@code rat.png}, {@code player.png} gibi dosyalar konduğu anda bu sınıf
 * devreye girer — kodun başka hiçbir yeri değişmez.</p>
 */
public class ImageSprite implements Sprite {

    private final Image image;

    public ImageSprite(Image image) {
        this.image = image;
    }

    @Override
    public void draw(GraphicsContext gc, double centerX, double centerY, double size) {
        gc.drawImage(image, centerX - size / 2, centerY - size / 2, size, size);
    }
}
