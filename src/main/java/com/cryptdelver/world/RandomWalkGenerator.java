package com.cryptdelver.world;

import java.util.Random;

/**
 * Rastgele yürüyüş (drunkard walk) ile mağara benzeri zindan üretir.
 *
 * <p>Algoritma: haritanın ortasından başlayan tek bir kazıcı, her adımda dört
 * yönden birini rastgele seçip bulunduğu kareyi zemine çevirir. Hedeflenen
 * zemin oranına ulaşılana kadar sürer.</p>
 *
 * <p>Tek bir kazıcı kullanmak, üretilen alanın tek parça olmasını garanti eder:
 * kazıcının izlediği yol zaten kesintisiz bir yürüyüş yoludur. Dışarı taşmayı,
 * her adımı harita sınırlarına kırparak engelliyoruz.</p>
 */
public class RandomWalkGenerator implements DungeonGenerator {

    /** Haritanın iç alanının ne kadarının zemin olacağı. */
    private static final double DEFAULT_FLOOR_RATIO = 0.50;

    /** Sonsuz döngüye karşı güvenlik freni: hedeflenen zemin başına en fazla adım. */
    private static final int MAX_STEPS_PER_TILE = 40;

    private final double floorRatio;

    public RandomWalkGenerator() {
        this(DEFAULT_FLOOR_RATIO);
    }

    public RandomWalkGenerator(double floorRatio) {
        if (floorRatio <= 0 || floorRatio >= 1) {
            throw new IllegalArgumentException("Zemin oranı 0 ile 1 arasında olmalı: " + floorRatio);
        }
        this.floorRatio = floorRatio;
    }

    @Override
    public Dungeon generate(int width, int height, long seed) {
        Dungeon dungeon = new Dungeon(width, height);
        Random random = new Random(seed);

        int innerTiles = (width - 2) * (height - 2);
        int target = Math.max(1, (int) (innerTiles * floorRatio));
        int maxSteps = target * MAX_STEPS_PER_TILE;

        int x = width / 2;
        int y = height / 2;
        dungeon.setTile(x, y, Tile.FLOOR);
        int carved = 1;

        for (int step = 0; step < maxSteps && carved < target; step++) {
            switch (random.nextInt(4)) {
                case 0 -> y--;
                case 1 -> y++;
                case 2 -> x--;
                default -> x++;
            }

            // Kenarda bir karelik duvar şeridi her zaman kalsın.
            x = Math.clamp(x, 1, width - 2);
            y = Math.clamp(y, 1, height - 2);

            if (dungeon.getTile(x, y) != Tile.FLOOR) {
                dungeon.setTile(x, y, Tile.FLOOR);
                carved++;
            }
        }

        return dungeon;
    }

    @Override
    public String getName() {
        return "Rastgele yürüyüş (mağara)";
    }
}
