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

    /**
     * Saniyede kaç kare ilerlediği.
     *
     * <p>Türün değeri varsayılan ama <em>dövüş sırasında değişebiliyor</em>:
     * Kor Şeytanı canı yarılanınca hızlanıyor. Bu yüzden hareket bütçesi
     * doğrudan {@link EnemyStats#speed()} yerine buradan okunuyor — yoksa
     * öfkelenme kodu yazılabilir ama hiçbir etkisi olmazdı.</p>
     */
    public double getSpeed() {
        return stats.speed();
    }

    /**
     * İki vuruş arasındaki bekleme, saniye.
     *
     * <p>{@link #getSpeed()} ile aynı gerekçe: öfkelenen bir düşmanın yalnızca
     * hızlı yürüyüp aynı tempoda vurması yarım bir değişiklik olurdu.</p>
     */
    public double getAttackCooldown() {
        return stats.attackCooldown();
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
        Stance stance = stanceTowards(game);

        // Kaçan düşman vurmaz; canını kurtarmaya çalışır. Duran düşman ise
        // yanına gelirsen vurur — durmasının sebebi korkmak değil, mesafeyi
        // kendi seçmek.
        if (!isMoving() && isAdjacentTo(player) && stance != Stance.FLEE) {
            if (attackCooldown <= 0) {
                game.enemyAttacksPlayer(this);
                attackCooldown = getAttackCooldown();
            }
            return;
        }

        double budget = getSpeed() * delta;
        int guard = 0;
        while (budget > 0 && guard++ < MAX_STEPS_PER_FRAME) {
            if (!isMoving() && !startStep(game, player, stance)) {
                break;
            }
            budget = advance(budget);
        }
    }

    /** Sıradaki adımı seçip başlatır. */
    private boolean startStep(Game game, Player player, Stance stance) {
        if (tileDistanceTo(player) > stats.aggroRange()) {
            return startIdleStep(game);
        }

        return switch (stance) {
            case FLEE -> startFleeStep(game, player);
            case HOLD -> false;
            case CHASE -> startChaseStep(game, player);
        };
    }

    private boolean startChaseStep(Game game, Player player) {
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
     * Şu an ne yapmalı: yaklaşmalı mı, durmalı mı, kaçmalı mı.
     *
     * <p>Varsayılan yaklaşmak — oyundaki çoğu düşman sonuna kadar üstüne
     * geliyor. Goblin canı azalınca {@link Stance#FLEE}'ye, okçu seni
     * nişan hattında görünce {@link Stance#HOLD}'a geçiyor. Sayıları
     * değiştirmeden gerçek davranış farkı yaratmanın yolu bu tek karar.</p>
     *
     * <p>Karar her karede bir kez soruluyor ve hem vuruşta hem adımda aynı
     * cevap kullanılıyor: ikisi ayrı ayrı sorulsaydı, arada durum değişince
     * "kaçıyorum ama vuruyorum" gibi tutarsız bir kare çıkabilirdi.</p>
     */
    protected Stance stanceTowards(Game game) {
        return Stance.CHASE;
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
