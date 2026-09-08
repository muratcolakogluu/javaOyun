package com.cryptdelver.ui;

import java.util.List;
import javafx.scene.canvas.GraphicsContext;

/**
 * Birden çok kareyi sırayla gösteren sprite.
 *
 * <p>Zamanı dışarıdan almıyor, sistem saatinden okuyor. Böylece hangi varlığın
 * hangi karede olduğunu kimsenin takip etmesi gerekmiyor — aynı animasyon aynı
 * anda ekrandaki bütün impler için aynı kareyi gösteriyor. Tek tek faz kaymış
 * animasyonlar daha canlı dururdu ama her varlığa zamanlayıcı taşımak
 * gerekirdi; bu ölçekte gereksiz.</p>
 */
public class AnimatedSprite implements Sprite {

    private static final double NANOS_PER_SECOND = 1_000_000_000.0;

    private final List<Sprite> frames;
    private final double frameDuration;

    /**
     * @param frames        sırayla gösterilecek kareler, en az bir tane
     * @param frameDuration bir karenin ekranda kalma süresi, saniye
     */
    public AnimatedSprite(List<Sprite> frames, double frameDuration) {
        if (frames.isEmpty()) {
            throw new IllegalArgumentException("Animasyonun en az bir karesi olmalı");
        }
        this.frames = List.copyOf(frames);
        this.frameDuration = frameDuration;
    }

    @Override
    public void draw(GraphicsContext gc, double centerX, double centerY, double size) {
        double seconds = System.nanoTime() / NANOS_PER_SECOND;
        int index = (int) (seconds / frameDuration) % frames.size();

        frames.get(index).draw(gc, centerX, centerY, size);
    }
}
