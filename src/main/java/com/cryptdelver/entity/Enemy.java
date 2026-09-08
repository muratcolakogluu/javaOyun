package com.cryptdelver.entity;

import com.cryptdelver.ai.Pathfinder;
import com.cryptdelver.game.Game;
import com.cryptdelver.world.Position;

/**
 * Kendi başına hareket eden düşmanların ortak atası.
 *
 * <p>Davranış akışı burada bir kez yazıldı ve {@code final}: yan yanaysa vur,
 * fark ettiyse kare kare kovala, etmediyse kendi işine bak. Alt sınıflar akışı
 * değil <em>değerlerini</em> ({@link EnemyStats}) ve boştaki davranışlarını
 * ({@link #startIdleStep}) belirler.</p>
 *
 * <p>Hareket oyuncuyla aynı kurallara tabi: ızgaraya kilitli, dört yönlü, adım
 * ortasında yön değişmiyor. Fark yalnızca hızda ve adımı kimin seçtiğinde —
 * oyuncuda tuş, burada {@link Pathfinder}. 5. adımda A* eklendiğinde değişecek
 * tek şey kurucuya verilen yol bulucu olacak.</p>
 */
public abstract class Enemy extends Combatant implements Actor {

    private static final int MAX_STEPS_PER_FRAME = 8;

    private final EnemyStats stats;
    private final Pathfinder pathfinder;
    private int bonusAttack;
    private int bonusDefense;
    private double attackCooldown;
    private double idleTimer;

    protected Enemy(int tileX, int tileY, String name, EnemyStats stats, Pathfinder pathfinder) {
        super(tileX, tileY, name, stats.maxHp());
        this.stats = stats;
        this.pathfinder = pathfinder;
    }

    public EnemyStats getStats() {
        return stats;
    }

    @Override
    public int getAttackPower() {
        return stats.attackPower() + bonusAttack;
    }

    /** Türün doğal savunması, üstüne derinlik bonusu. */
    @Override
    public int getDefense() {
        return stats.defense() + bonusDefense;
    }

    /**
     * Düşmanı kalıcı olarak güçlendirir.
     *
     * <p>Derin katlarda aynı türden daha sert düşmanlar çıkarmak için var:
     * tür başına yeni sınıf yazmak yerine, doğarken güçlendiriliyorlar.
     * Kazanılan can anında da yansıyor, yoksa yarı canlı doğardı.</p>
     */
    public void strengthen(int extraHp, int extraAttack, int extraDefense) {
        increaseMaxHp(extraHp);
        bonusAttack += extraAttack;
        bonusDefense += extraDefense;
    }

    public double getSpeed() {
        return stats.speed();
    }

    public Pathfinder getPathfinder() {
        return pathfinder;
    }

    /**
     * Kayıt dosyasındaki tür etiketi ({@code "RAT"}, {@code "SKELETON"}...).
     *
     * <p>{@code Item.getSaveKind()} ile aynı gerekçe: kaydetme kodu
     * {@code instanceof} zinciri kurmasın, düşman kendini adlandırsın.</p>
     */
    public abstract String getSaveKind();

    /** Boşta gezinen düşmanların adımları arasında bekleyeceği süre. */
    protected double getIdleTimer() {
        return idleTimer;
    }

    protected void setIdleTimer(double seconds) {
        this.idleTimer = seconds;
    }

    @Override
    public final void update(Game game, double delta) {
        tickTimers(delta);
        attackCooldown = Math.max(0, attackCooldown - delta);
        idleTimer = Math.max(0, idleTimer - delta);

        Player player = game.getPlayer();
        if (!isAlive() || !player.isAlive()) {
            return;
        }

        onUpdate(game, delta);

        // Kaçan düşman vurmaz; canını kurtarmaya çalışır.
        if (!isMoving() && isAdjacentTo(player) && !shouldFlee()) {
            if (attackCooldown <= 0) {
                game.enemyAttacksPlayer(this);
                attackCooldown = stats.attackCooldown();
            }
            return;
        }

        double budget = stats.speed() * delta;
        int guard = 0;
        while (budget > 0 && guard++ < MAX_STEPS_PER_FRAME) {
            if (!isMoving() && !startStep(game, player)) {
                break;
            }
            budget = advance(budget);
        }
    }

    /** Sıradaki adımı seçip başlatır. */
    private boolean startStep(Game game, Player player) {
        if (tileDistanceTo(player) > stats.aggroRange()) {
            return startIdleStep(game);
        }
        if (shouldFlee()) {
            return startFleeStep(game, player);
        }

        Position step = pathfinder.nextStep(game.getDungeon(), getTile(), player.getTile());
        if (step == null) {
            return false;
        }
        return game.tryStartStep(this, step.x() - getTileX(), step.y() - getTileY());
    }

    /**
     * Oyuncu menzil dışındayken atılacak adım. Varsayılan: kıpırdama.
     *
     * @return adım başlatıldıysa {@code true}
     */
    protected boolean startIdleStep(Game game) {
        return false;
    }

    /**
     * Şu an kaçmalı mı. Varsayılan: hayır, sonuna kadar dövüşür.
     *
     * <p>Goblin bunu canı azalınca açıyor. Kaçan düşman ne vuruyor ne
     * kovalıyor; oyuncudan uzaklaşan bir kare arıyor. Bu, sayıları
     * değiştirmeden gerçek bir davranış farkı yaratıyor: goblini bitirmek
     * istiyorsan peşinden gitmen gerekiyor.</p>
     */
    protected boolean shouldFlee() {
        return false;
    }

    /**
     * Oyuncudan uzaklaşan bir kareye adım atar.
     *
     * <p>Uzaklaşma yönünü önce büyük eksende deniyoruz, olmazsa diğerinde;
     * ikisi de kapalıysa köşeye sıkışmış demektir ve olduğu yerde kalır.</p>
     */
    private boolean startFleeStep(Game game, Player player) {
        int awayX = Integer.signum(getTileX() - player.getTileX());
        int awayY = Integer.signum(getTileY() - player.getTileY());

        boolean horizontalFirst = Math.abs(getTileX() - player.getTileX())
                >= Math.abs(getTileY() - player.getTileY());

        int firstX = horizontalFirst ? awayX : 0;
        int firstY = horizontalFirst ? 0 : awayY;

        if (game.tryStartStep(this, firstX, firstY)) {
            return true;
        }
        return game.tryStartStep(this, horizontalFirst ? 0 : awayX, horizontalFirst ? awayY : 0);
    }

    /**
     * Her karede, hareket ve saldırı kararlarından önce çalışır.
     *
     * <p>Yeteneği olan düşmanlar için genişleme noktası: boss bunu kullanarak
     * belli aralıklarla yaratık çağırıyor. Sıradan düşmanların yeteneği yok,
     * o yüzden varsayılan davranış boş.</p>
     */
    protected void onUpdate(Game game, double delta) {
        // Yeteneği olan alt sınıflar burayı doldurur.
    }

    /**
     * Düşman öldüğünde, listeden çıkarıldıktan sonra çalışır.
     *
     * <p>Ganimet düşürmenin yeri burası: ne bırakacağını {@code Game} değil,
     * düşmanın kendisi biliyor.</p>
     */
    public void onDeath(Game game) {
        // Sıradan düşmanlar ganimet bırakmaz.
    }
}
