package com.cryptdelver.ui;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * Sprite adlarını çizilebilir sprite'lara bağlar.
 *
 * <p>Her ad için önce {@code resources/assets/sprites/<ad>.png} aranır; dosya
 * varsa {@link ImageSprite}, yoksa {@link ShapeSprites} içindeki şekil çizimi
 * kullanılır. Yani hazır bir paket bulduğumuzda yapılacak tek iş, dosyaları
 * doğru adlarla o klasöre koymak — kodda hiçbir değişiklik gerekmiyor.</p>
 *
 * <p>Beklenen dosya adları — zindan: {@code floor}, {@code wall},
 * {@code stairs}; karakterler: {@code player}, {@code imp}, {@code skeleton},
 * {@code boss}; eşyalar: {@code potion}, {@code gold}, {@code sword},
 * {@code sword_steel}, {@code axe}, {@code sword_crypt},
 * {@code armor_leather}, {@code armor_chain}, {@code armor_plate},
 * {@code armor_crypt}.</p>
 */
public class SpriteRegistry {

    private static final String SPRITE_PATH = "/assets/sprites/";

    private final Map<String, Sprite> sprites = new HashMap<>();
    private final Sprite unknown = ShapeSprites.unknown();

    public SpriteRegistry() {
        // Zindanın kendisi de sprite: hazır bir tileset gelince duvar ve zemin
        // de dosyadan çizilebilsin diye.
        register("floor", ShapeSprites.floorTile());
        register("wall", ShapeSprites.wallTile());
        register("stairs", ShapeSprites.stairsTile());

        register("player", ShapeSprites.player());
        register("imp", ShapeSprites.imp());
        register("skeleton", ShapeSprites.skeleton());
        register("boss", ShapeSprites.boss());
        register("potion", ShapeSprites.potion());
        register("gold", ShapeSprites.gold());

        // Silah kademeleri: aynı çizim, yükselen kalite hissi renklerde.
        register("sword", ShapeSprites.sword(Color.web("#9a8f7c"), Color.web("#6b5a3c")));
        register("sword_steel", ShapeSprites.sword(Color.web("#cfd3dc"), Color.web("#b08a3a")));
        register("axe", ShapeSprites.axe());
        register("sword_crypt", ShapeSprites.sword(Color.web("#b9a6f0"), Color.web("#e8c46a")));

        // Zırh kademeleri.
        // Kask kademeleri; pakette kask çizimi de yok.
        register("helmet_leather", ShapeSprites.helmet(Color.web("#7d5a3c"), Color.web("#a8794f")));
        register("helmet_chain", ShapeSprites.helmet(Color.web("#6f7482"), Color.web("#9aa0ad")));
        register("helmet_steel", ShapeSprites.helmet(Color.web("#8b98b4"), Color.web("#d6dcea")));
        register("helmet_crypt", ShapeSprites.helmet(Color.web("#5f5090"), Color.web("#e8c46a")));

        // Kalkan kademeleri; pakette kalkan çizimi yok, hepsi şekilden geliyor.
        register("shield_wood", ShapeSprites.shield(Color.web("#7d5a3c"), Color.web("#a8794f")));
        register("shield_iron", ShapeSprites.shield(Color.web("#6f7482"), Color.web("#9aa0ad")));
        register("shield_steel", ShapeSprites.shield(Color.web("#8b98b4"), Color.web("#d6dcea")));
        register("shield_crypt", ShapeSprites.shield(Color.web("#5f5090"), Color.web("#e8c46a")));

        register("armor_leather", ShapeSprites.armor(Color.web("#7d5a3c"), Color.web("#a8794f")));
        register("armor_chain", ShapeSprites.armor(Color.web("#7f8492"), Color.web("#aeb3c0")));
        register("armor_plate", ShapeSprites.armor(Color.web("#93a1bd"), Color.web("#d6dcea")));
        register("armor_crypt", ShapeSprites.armor(Color.web("#5f5090"), Color.web("#e8c46a")));
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
