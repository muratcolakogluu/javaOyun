package com.cryptdelver.entity;

import com.cryptdelver.game.Game;

/**
 * Sarnıç Boğucusu: dört yöne birden salvo atar.
 *
 * <p>Okçunun dersini boss ölçeğinde soruyor. Okçu tek bir hattı tutuyordu ve
 * cevabı hattan çıkmaktı; Boğucu <b>dört hattı birden</b> tutuyor, yani
 * cevabı yan tarafa değil <em>köşeye</em> geçmek: salvo çıkarken çapraz
 * durursan hiçbir ok sana değmiyor.</p>
 *
 * <h2>Neden önce hazırlanıyor</h2>
 * <p>Salvo habersiz gelseydi kaçınılamazdı ve bu bir mekanik değil vergi
 * olurdu. Boğucu {@value #WINDUP} saniye şişiyor ve ekran o sırada dört kolu
 * kırmızı boyuyor — yani dövüş bir <em>ritme</em> dönüşüyor: vur, vur, işaret
 * gelince çapraza kay, geri dön. Bossla yan yana durup vuruşmak artık
 * çalışmıyor ama bossa vurmak da hâlâ mümkün.</p>
 *
 * <p>Salvo yakın dövüşün yerine geçmiyor, üstüne biniyor: yanına gelirsen
 * ayrıca pençeliyor. Yani "uzak dur" da bir cevap değil.</p>
 */
public class Bogucu extends Boss {

    /** İki salvo arasındaki süre, saniye. */
    private static final double VOLLEY_INTERVAL = 5.0;

    /** İşaretin ekranda kaldığı, yani kaçmak için verilen süre. */
    static final double WINDUP = 0.9;

    /** Salvonun kaç kare uzağa ulaştığı. */
    private static final int VOLLEY_RANGE = 9;

    private static final int[][] DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private double volleyTimer = VOLLEY_INTERVAL;
    private double windupLeft;

    public Bogucu(int tileX, int tileY) {
        super(tileX, tileY, "Sarnic Bogucusu", "boss_bogucu");
    }

    /**
     * Şu an salvoya hazırlanıyor mu; ekran işareti buna bakarak çiziyor.
     *
     * <p>Uyarının <em>görülmesi</em> mekaniğin kendisi kadar önemli, o yüzden
     * durum dışarıya açık.</p>
     */
    public boolean isWindingUp() {
        return windupLeft > 0;
    }

    /** İşaretin ne kadarının geçtiği (0 başlangıç, 1 salvo anı). */
    public double getWindupProgress() {
        return windupLeft <= 0 ? 0 : 1 - windupLeft / WINDUP;
    }

    /** Salvonun ulaştığı kare sayısı; işaret kolları da bu kadar uzanıyor. */
    public int getVolleyRange() {
        return VOLLEY_RANGE;
    }

    /**
     * Şişerken yerinden kıpırdamıyor.
     *
     * <p>İki sebeple. İşaretin çizildiği yerle salvonun çıktığı yer aynı kare
     * olmalı, yoksa uyarı yalan söylerdi. Bir de duruşun kendisi bir uyarı:
     * durmak bilmeyen bossun aniden durması, oyuncunun bir şey olacağını
     * anladığı ilk işaret.</p>
     */
    @Override
    protected Stance stanceTowards(Game game) {
        return isWindingUp() ? Stance.HOLD : super.stanceTowards(game);
    }

    @Override
    protected void onUpdate(Game game, double delta) {
        if (windupLeft > 0) {
            windupLeft -= delta;
            if (windupLeft <= 0) {
                fireVolley(game);
            }
            return;
        }

        volleyTimer -= delta;
        if (volleyTimer > 0) {
            return;
        }

        volleyTimer = VOLLEY_INTERVAL;
        windupLeft = WINDUP;
        game.getMessageLog().combat(getName() + " şişiyor — köşeye kay!");
    }

    private void fireVolley(Game game) {
        for (int[] direction : DIRECTIONS) {
            game.addProjectile(new Projectile(getTileX(), getTileY(),
                    direction[0], direction[1], this, VOLLEY_RANGE));
        }
    }
}
