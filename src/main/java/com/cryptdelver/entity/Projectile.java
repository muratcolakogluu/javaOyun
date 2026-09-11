package com.cryptdelver.entity;

import com.cryptdelver.game.Text;

import com.cryptdelver.game.Game;

/**
 * Havada uçan bir ok.
 *
 * <p>Oyunun ilk menzilli tehdidi. Anında isabet eden bir "uzaktan vuruş"
 * yazmak çok daha kolay olurdu ama <em>kaçınılamaz</em> olurdu: gerçek zamanlı
 * bir oyunda göremediğin bir hasar, hasar değil vergidir. Ok havada gerçekten
 * yol alıyor, yani görüyorsun ve yoldan çekilebiliyorsun.</p>
 *
 * <p>Hareket herkesle aynı kurallara tabi: ızgaraya kilitli, dört yönlü, kare
 * kare. Farkı yalnızca hızı — saniyede {@value #SPEED} kare, oyuncunun iki
 * katından fazla. Yani okun önünden çekilmek mümkün ama <b>anında karar
 * vermen</b> gerekiyor.</p>
 *
 * <p>Hasarı kendi taşımıyor, <em>atanı</em> taşıyor: böylece zırh, savunma ve
 * Diken gibi kurallar tek bir yerde kalıyor ({@code Game.resolveDamage}) ve ok
 * bunları bilmek zorunda olmuyor. Atan ok havadayken ölse bile isabet geçerli;
 * yayı bırakmışsın, ok yolda.</p>
 */
public class Projectile extends Entity {

    /** Saniyede kaç kare gider. */
    private static final double SPEED = 13.0;

    private final int stepX;
    private final int stepY;
    private final Enemy shooter;

    private int tilesLeft;
    private boolean spent;

    /**
     * @param stepX    yatay yön: -1, 0 ya da 1
     * @param stepY    dikey yön: -1, 0 ya da 1
     * @param range    kaç kare gittikten sonra düşeceği
     */
    public Projectile(int tileX, int tileY, int stepX, int stepY, Enemy shooter, int range) {
        super(tileX, tileY, Text.ITEM_ARROW);
        this.stepX = stepX;
        this.stepY = stepY;
        this.shooter = shooter;
        this.tilesLeft = range;
    }

    /** Atan düşman; hasar onun vuruş gücünden hesaplanıyor. */
    public Enemy getShooter() {
        return shooter;
    }

    /** Menzili bitti, duvara çarptı ya da isabet etti. */
    public boolean isSpent() {
        return spent;
    }

    /** Okun baktığı yön; çizim tarafı ucu buna göre döndürüyor. */
    public int getStepX() {
        return stepX;
    }

    public int getStepY() {
        return stepY;
    }

    /**
     * Oku bir kare ilerletir.
     *
     * <p>Çarpışma <em>kare başına</em> bakılıyor, her çerçevede değil: oyunun
     * geri kalanı gibi ok da ızgarada yaşıyor ve "hangi karedeydi" sorusunun
     * tek bir cevabı oluyor. Oyuncu adım hâlindeyken iki kareyi birden tuttuğu
     * için ({@code occupies}) arada kalıp okun içinden geçmesi de mümkün
     * değil.</p>
     */
    public void update(Game game, double delta) {
        if (spent) {
            return;
        }

        double budget = SPEED * delta;
        while (budget > 0 && !spent) {
            if (!isMoving() && !launchNextTile(game)) {
                return;
            }

            budget = advance(budget);
            if (!isMoving()) {
                arriveAt(game);
            }
        }
    }

    /** Bir sonraki kareye doğru yola çıkar; duvar ya da menzil sonu ise düşer. */
    private boolean launchNextTile(Game game) {
        int nextX = getTileX() + stepX;
        int nextY = getTileY() + stepY;

        if (tilesLeft <= 0 || !game.getDungeon().isWalkable(nextX, nextY)) {
            spent = true;
            return false;
        }

        tilesLeft--;
        beginStep(nextX, nextY);
        return true;
    }

    /** Vardığı karede oyuncu varsa isabet. */
    private void arriveAt(Game game) {
        if (game.getPlayer().occupies(getTileX(), getTileY())) {
            game.projectileHitsPlayer(this);
            spent = true;
        }
    }

    @Override
    public String getSpriteName() {
        return "arrow";
    }
}
