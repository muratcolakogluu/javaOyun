package com.cryptdelver.entity;

import com.cryptdelver.game.Text;

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

    /**
     * Başlamış bir vuruşun kalan hazırlık süresi.
     *
     * <p>Sıfırsa düşman ya vuruyor ya yürüyor; sıfırdan büyükse kolunu
     * kaldırmış, inmesini bekliyor.</p>
     */
    private double windupLeft;

    /** Bu düşmanı elit yapan özellik; sıradansa {@code null}. */
    private EliteTrait elite;

    /** Sersemlik: bittiğinde düşman yeniden hareket ediyor. */
    private double staggerLeft;

    /** Kilitli mahzenin anahtarını taşıyor mu. */
    private boolean carriesKey;

    /** Korku: bittiğinde düşman yeniden üstüne geliyor. */
    private double frightLeft;

    protected Enemy(int tileX, int tileY, Text name, EnemyStats stats, Pathfinder pathfinder) {
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
        return elite == null ? stats.speed() : elite.scaleSpeed(stats.speed());
    }

    /**
     * İki vuruş arasındaki bekleme, saniye.
     *
     * <p>{@link #getSpeed()} ile aynı gerekçe: öfkelenen bir düşmanın yalnızca
     * hızlı yürüyüp aynı tempoda vurması yarım bir değişiklik olurdu.</p>
     */
    public double getAttackCooldown() {
        return elite == null
                ? stats.attackCooldown()
                : elite.scaleCooldown(stats.attackCooldown());
    }

    public Pathfinder getPathfinder() {
        return pathfinder;
    }

    /**
     * Bu düşmanı elit yapar.
     *
     * <p>Doğarken bir kez çağrılıyor ve geri alınmıyor: elitlik bir durum
     * değil, o yaratığın <em>ne olduğu</em>. Dövüşün ortasında elitleşen bir
     * düşman, oyuncunun okuduğu her şeyi geçersiz kılardı.</p>
     */
    public void makeElite(EliteTrait trait) {
        this.elite = trait;
        trait.strengthen(this);
    }

    public boolean isElite() {
        return elite != null;
    }

    /** Elitlik özelliği; ekran aurasının rengini buradan seçiyor. */
    public EliteTrait getElite() {
        return elite;
    }

    /**
     * Adı; elitse önünde sıfatı var.
     *
     * <p>"Zırhlı Ork" ile "Ork" dövüş kaydında yan yana göründüğünde hangi
     * vuruşun hangisine indiği karışmıyor. Ad üretimi burada çünkü sıfat
     * düşmanın kendi bilgisi.</p>
     */
    @Override
    public String getName() {
        String name = elite == null ? super.getName() : elite.getLabel() + " " + super.getName();

        // Anahtarcı sıfatı en başta: dövüş kaydında "hangisine vuruyorum"
        // sorusunun cevabı ilk kelime olsun. Ekrandaki işaret haritada
        // söylüyor, ad ise kayıtta.
        return carriesKey ? Text.ENEMY_KEEPER.get() + " " + name : name;
    }

    /**
     * Vuruştan önceki hazırlık süresi, saniye; 0 ise işaret yok.
     *
     * <p>{@link #getSpeed()} gibi tür değerinden okunuyor ama alt sınıfın
     * değiştirmesine açık: dövüşün ortasında işareti kısaltan bir yetenek
     * yazılabilsin diye.</p>
     */
    public double getWindup() {
        return stats.windup();
    }

    /**
     * Bu düşmana mahzenin anahtarını verir.
     *
     * <p>Kat kurulurken bir kez çağrılıyor. Anahtarı hangi düşmanın taşıdığı
     * ekranda görünüyor ({@link #hasKey()}), yoksa "kalabalığın içinden doğru
     * olanı bul" bir hedef değil bir tarama olurdu.</p>
     */
    public void giveKey() {
        this.carriesKey = true;
    }

    public boolean hasKey() {
        return carriesKey;
    }

    /**
     * Düşmanı sersemletir: verilen süre boyunca ne yürüyor ne vuruyor.
     *
     * <p>Savuşturmanın ödülü bu. Savuşturmak yalnızca vuruşu <em>engelleseydi</em>
     * bir "hasar almama" tuşu olurdu ve doğru anı tutmanın karşılığı olmazdı.
     * Sersemlik o anı bir <b>saldırı fırsatına</b> çeviriyor — hem bedava
     * vuruş, hem arkasına geçmek için vakit.</p>
     *
     * <p>Başlamış hazırlık da iptal ediliyor: karşılanan vuruş inmiş
     * sayılıyor, aynı kol ikinci kez inmiyor.</p>
     */
    public void stagger(double seconds) {
        staggerLeft = Math.max(staggerLeft, seconds);
        windupLeft = 0;
    }

    /**
     * Düşmanı korkutur: verilen süre boyunca oyuncudan kaçıyor.
     *
     * <p>Sersemlikten farkı hareket etmesi: sersemlemiş düşman duruyor,
     * korkmuş düşman <em>uzaklaşıyor</em>. İkisi ayrı çünkü çözdükleri şey
     * ayrı — sersemlik vurmak için pencere açıyor, korku aradaki mesafeyi
     * açıyor.</p>
     */
    public void frighten(double seconds) {
        frightLeft = Math.max(frightLeft, seconds);
    }

    /** Şu an korkmuş mu; ekran bunu okuyup işaretini çiziyor. */
    public boolean isFrightened() {
        return frightLeft > 0;
    }

    /** Şu an sersemlemiş mi; ekran bunu okuyup işaretini çiziyor. */
    public boolean isStaggered() {
        return staggerLeft > 0;
    }

    /**
     * Şu an vuruşa hazırlanıyor mu.
     *
     * <p>Ekran bunu okuyup işareti çiziyor. Uyarının <em>görülmesi</em>
     * mekaniğin kendisi kadar önemli — görünmeyen bir hazırlık, oyuncu için
     * yalnızca gecikmiş bir vuruş olurdu.</p>
     */
    public boolean isWindingUp() {
        return windupLeft > 0;
    }

    /** Hazırlığın ne kadarının geçtiği: 0 kolun kalktığı an, 1 vuruş anı. */
    public double getWindupProgress() {
        double windup = getWindup();
        return windupLeft <= 0 || windup <= 0 ? 0 : 1 - windupLeft / windup;
    }

    /**
     * Türün kısa etiketi ({@code "IMP"}, {@code "SKELETON"}...).
     *
     * <p>{@link Item#getKind()} ile aynı gerekçe: türü soran kod
     * {@code instanceof} zinciri kurmasın, düşman kendini adlandırsın.</p>
     */
    public abstract String getKind();

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

        // Sersemlik her şeyin önünde: sersemlemiş düşman ne yürüyor ne
        // vuruyor. Yetenek sayaçları da ilerlemiyor, yoksa boss sersemken
        // salvo biriktirip ayılınca hepsini birden atardı.
        if (staggerLeft > 0) {
            staggerLeft -= delta;
            return;
        }

        onUpdate(game, delta);

        // Başlamış vuruş her şeyin önünde: kol kalktıysa iniyor. Kaçsan da
        // iniyor -- ama boşluğa. Kaçınmanın anlamı bu.
        if (windupLeft > 0) {
            windupLeft -= delta;
            if (windupLeft <= 0) {
                releaseSwing(game, player);
            }
            return;
        }

        frightLeft = Math.max(0, frightLeft - delta);

        // Korku türün kendi kararını eziyor: kaçan bir düşman ne kovalıyor ne
        // hat tutuyor. Boss da kaçıyor -- rüşvet bossa da işliyor ve bunun
        // özel bir kuralı yok.
        Stance stance = frightLeft > 0 ? Stance.FLEE : stanceTowards(game);

        // Kaçan düşman vurmaz; canını kurtarmaya çalışır. Duran düşman ise
        // yanına gelirsen vurur — durmasının sebebi korkmak değil, mesafeyi
        // kendi seçmek.
        if (!isMoving() && isAdjacentTo(player) && stance != Stance.FLEE) {
            if (attackCooldown <= 0) {
                beginSwing(game);
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

    /**
     * Vuruşu başlatır: işareti olan bekler, olmayan hemen vurur.
     *
     * <p>Ağır vuranlara işaret koymanın sebebi, dövüşü bir <em>ritme</em>
     * çevirmek. İşaretsiz haliyle orkun yanında durmak saf bir hesaptı:
     * canın yetiyorsa vuruşurdun, yetmiyorsa ölürdün ve arada verilecek bir
     * karar yoktu. Kalkan kol, o kararı geri veriyor — bir adım geri çekil,
     * vuruş boşa gitsin, sonra geri gir.</p>
     *
     * <p>Bedelsiz değil: işaretli vuruş daha sert. Yani "sürekli geri çekil"
     * de bir çözüm değil, zamanlamayı tutturmak gerekiyor.</p>
     */
    private void beginSwing(Game game) {
        // Vurmadan önce dönüyor: bir düşman sana vururken sana bakıyordur.
        // Dönme anı aynı zamanda arkadan vuruş penceresinin kapandığı an --
        // ve hazırlık başladıktan sonra bir daha dönmüyor, yani kolunu
        // kaldırmış düşmanın arkasına geçmek işe yarıyor.
        faceTowards(game.getPlayer());

        if (getWindup() <= 0) {
            game.enemyAttacksPlayer(this);
            attackCooldown = getAttackCooldown();
            return;
        }

        windupLeft = getWindup();
    }

    /**
     * Hazırlığı biten vuruşu indirir.
     *
     * <p>Oyuncu hâlâ yanındaysa isabet, değilse boşluk. İkisinde de bekleme
     * başlıyor: boşa giden vuruş düşmanı <em>açık</em> bırakıyor ve karşılık
     * vermenin penceresi bu.</p>
     */
    private void releaseSwing(Game game, Player player) {
        attackCooldown = getAttackCooldown();

        if (player.isAlive() && isAdjacentTo(player)) {
            game.enemyAttacksPlayer(this);
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
        // Sıradan düşmanlar ganimet bırakmaz; elit olan kesesini bırakıyor.
        // Elit bir ceza değil bir fırsat olmalı: kaçmak da bir cevap ama
        // kalıp devirmenin bir karşılığı var.
        if (elite != null) {
            game.addGroundItem(new Gold(getTileX(), getTileY(), elite.getGold()));
        }

        // Anahtar düştüğü yerde duruyor: taşıyanı nerede devirdiysen mahzene
        // oradan yürüyorsun. Doğrudan çantaya koymak "öldür ve al" olurdu,
        // oysa kattaki mesafe de kararın parçası.
        if (carriesKey) {
            game.addGroundItem(new Key(getTileX(), getTileY()));
            game.getMessageLog().importantItem(Text.MSG_KEY_DROPPED.get(getName()));
        }
    }
}
