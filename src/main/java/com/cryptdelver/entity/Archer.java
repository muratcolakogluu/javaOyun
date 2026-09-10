package com.cryptdelver.entity;

import com.cryptdelver.ai.AStarPathfinder;
import com.cryptdelver.game.Game;

/**
 * Okçu: uzaktan vuran, yaklaşınca geri çekilen düşman.
 *
 * <p>Oyuna geldiği ana kadar <b>kaçmak her zaman doğru hamleydi.</b> Oyuncu
 * saniyede 6 kare gidiyor, en hızlı düşman 5.5; yani "vur, geri çekil, tekrar
 * vur" hiçbir zaman cezalandırılmıyordu. Kalabalıktan bile yürüyerek
 * çıkabiliyordun. Bu, oyundaki bütün dövüşlerin tek bir doğru cevabı olması
 * demekti.</p>
 *
 * <p>Okçu o cevabı bozuyor: mesafe onun lehine. Geri çekilirsen ok yersin,
 * durursan ok yersin — tek çare üstüne gitmek, ama üstüne giderken de
 * vuruluyorsun. Karşılığında canı çok az: yanına varabilirsen iki vuruşta
 * düşüyor.</p>
 *
 * <h2>Nişan hattı</h2>
 * <p>Yalnızca <em>aynı satır ya da sütunda</em> ve arada duvar yokken atış
 * yapıyor. Bunun sebebi teknik değil, okunabilirlik: oyuncunun "neden
 * vuruluyorum" sorusuna bakışta cevap verebilmesi gerekiyor. Kaçınma yolu da
 * bu kuraldan doğrudan çıkıyor — <b>hattan çık.</b> Yan bir adım, koşarak
 * uzaklaşmaktan iyi bir hamle oluyor; oyunun ilk kez yana hareket etmeyi
 * ödüllendirdiği yer burası.</p>
 */
public class Archer extends Enemy {

    private static final EnemyStats STATS = new EnemyStats(
            8,      // can: yanına varabilirsen çabuk düşer
            4,      // vuruş gücü — oku da bu güçten hesaplanıyor
            0,      // savunma: zırhsız
            3.4,    // hız (kare/saniye): kaçarken yakalanabilsin
            1.4,    // yakın dövüşte vuruş arası bekleme
            12);    // fark etme menzili: attığı mesafeden biraz uzak

    /** Kaç kare öteye ok atabiliyor. */
    private static final int SHOT_RANGE = 8;

    /** İki atış arasındaki bekleme, saniye. */
    private static final double SHOT_COOLDOWN = 2.0;

    /**
     * İlk atıştan önceki hazırlık.
     *
     * <p>Köşeyi döndüğün anda ok yemek "kaçınılmaz" hissettiriyordu; yarım
     * saniye, oku görüp hattan çıkmaya yetiyor.</p>
     */
    private static final double DRAW_TIME = 0.55;

    private double shotCooldown = DRAW_TIME;

    public Archer(int tileX, int tileY) {
        super(tileX, tileY, "Okçu", STATS, new AStarPathfinder());
    }

    /**
     * Duruşu tamamen mesafeye bağlı.
     *
     * <p>Yanına geldiysen kaçıyor (yayını çekecek yeri yok), nişan hattındaysan
     * duruyor ve atıyor, ikisi de değilse hattı kurmak için yaklaşıyor.</p>
     */
    @Override
    protected Stance stanceTowards(Game game) {
        if (isAdjacentTo(game.getPlayer())) {
            return Stance.FLEE;
        }
        return hasShotAt(game) ? Stance.HOLD : Stance.CHASE;
    }

    @Override
    protected void onUpdate(Game game, double delta) {
        shotCooldown = Math.max(0, shotCooldown - delta);

        // Adım ortasında ok atmıyor: iki karenin arasından çıkan bir ok hangi
        // kareden geldiği belirsiz olurdu.
        if (shotCooldown > 0 || isMoving()) {
            return;
        }

        Player player = game.getPlayer();
        if (isAdjacentTo(player) || !hasShotAt(game)) {
            return;
        }

        shotCooldown = SHOT_COOLDOWN;
        game.addProjectile(new Projectile(getTileX(), getTileY(),
                Integer.signum(player.getTileX() - getTileX()),
                Integer.signum(player.getTileY() - getTileY()),
                this, SHOT_RANGE));
    }

    /**
     * Oyuncu nişan hattında ve menzilde mi.
     *
     * <p>Hat, aynı satır ya da aynı sütun demek. Arada duvar olup olmadığına
     * kareler tek tek yürünerek bakılıyor; bir görüş hesabı çağırmıyoruz çünkü
     * sorulan şey farklı: <em>ok geçebilir mi</em>.</p>
     */
    private boolean hasShotAt(Game game) {
        Player player = game.getPlayer();
        int dx = player.getTileX() - getTileX();
        int dy = player.getTileY() - getTileY();

        if (dx != 0 && dy != 0) {
            return false;
        }
        int distance = Math.abs(dx) + Math.abs(dy);
        if (distance == 0 || distance > SHOT_RANGE) {
            return false;
        }

        return lineIsClear(game, Integer.signum(dx), Integer.signum(dy), distance);
    }

    /**
     * Arada duvar var mı.
     *
     * <p>Yalnızca duvara bakıyor, aradaki düşmanlara değil: onlar da oku
     * kesseydi okçular kendi sürülerinin arkasında tamamen etkisiz kalırdı ve
     * kalabalıkta hiç tehdit olmazlardı — oysa asıl tehlikeli oldukları yer
     * orası.</p>
     */
    private boolean lineIsClear(Game game, int stepX, int stepY, int distance) {
        for (int i = 1; i < distance; i++) {
            if (!game.getDungeon().isWalkable(getTileX() + stepX * i, getTileY() + stepY * i)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String getKind() {
        return "ARCHER";
    }

    @Override
    public String getSpriteName() {
        return "archer";
    }
}
