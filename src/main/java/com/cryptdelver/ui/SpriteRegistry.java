package com.cryptdelver.ui;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javafx.scene.image.Image;

/**
 * Sprite adlarını çizilebilir sprite'lara bağlar.
 *
 * <p>Her ad için önce {@code resources/assets/sprites/<ad>.png} aranır; dosya
 * varsa {@link ImageSprite}, yoksa {@link ShapeSprites} içindeki şekil çizimi
 * kullanılır. Yani hazır bir paket bulduğumuzda yapılacak tek iş, dosyaları
 * doğru adlarla o klasöre koymak — kodda hiçbir değişiklik gerekmiyor.</p>
 *
 * <p>Beklenen dosya adları: {@code player}, {@code rat}, {@code skeleton},
 * {@code boss}, {@code potion}, {@code gold}, {@code sword}, {@code axe}.</p>
 */
public class SpriteRegistry {

    private static final String SPRITE_PATH = "/assets/sprites/";

    private final Map<String, Sprite> sprites = new HashMap<>();
    private final Sprite unknown = ShapeSprites.unknown();

    public SpriteRegistry() {
        register("player", ShapeSprites.player());
        register("rat", ShapeSprites.rat());
        register("skeleton", ShapeSprites.skeleton());
        register("boss", ShapeSprites.boss());
        register("potion", ShapeSprites.potion());
        register("gold", ShapeSprites.gold());
        register("sword", ShapeSprites.sword());
        register("axe", ShapeSprites.axe());
    }

    /** Ada karşılık gelen sprite; tanınmayan ad için göze batan yer tutucu. */
    public Sprite get(String name) {
        return sprites.getOrDefault(name, unknown);
    }

    /** PNG varsa onu, yoksa verilen şekil çizimini kaydeder. */
    private void register(String name, Sprite fallback) {
        Sprite loaded = loadImage(name);
        sprites.put(name, loaded != null ? loaded : fallback);
    }

    private Sprite loadImage(String name) {
        try (InputStream stream = getClass().getResourceAsStream(SPRITE_PATH + name + ".png")) {
            if (stream == null) {
                return null;
            }
            return new ImageSprite(new Image(stream));
        } catch (IOException | RuntimeException e) {
            // Bozuk ya da okunamayan dosya oyunu düşürmesin; şekle geri dönülür.
            System.err.println("Sprite yüklenemedi, şekil çizimine dönülüyor: " + name);
            return null;
        }
    }
}
