package com.cryptdelver.entity;

import com.cryptdelver.game.Text;

import com.cryptdelver.game.Game;
import com.cryptdelver.world.Position;

/**
 * Kript Lordu: yanına ışınlanır. Oyunun son bossu.
 *
 * <p>Yirmi kat boyunca öğrendiğin tek evrensel doğru şuydu: <b>kaçabilirsin.</b>
 * Oyuncu saniyede 6 kare gidiyor, hiçbir düşman yetişemiyor; okçu bunu bir
 * hatta, Kor Şeytanı bir eşikte kırıyordu ama ikisinde de yeterince koşarsan
 * kurtuluyordun. Lort o kapıyı kapatıyor: uzaklaştığın anda yanında
 * beliriyor.</p>
 *
 * <p>Bu yüzden son dövüş bir <em>takas</em> dövüşü. Kaçış bir hamle olmaktan
 * çıkıyor, geriye üç şey kalıyor: iksirlerini doğru anda içmek, bombanı
 * saklamış olmak ve takımını büyücüde hazırlamış olmak. Yani oyunun bütün
 * sistemlerinin aynı anda sorulduğu tek yer burası.</p>
 *
 * <p>Işınlanma ücretsiz bir vuruş değil: yan yana geldiğinde vuruş beklemesi
 * sıfırlanmıyor, yani belirdiği anda vurmuyor. Bir de {@value #BLINK_INTERVAL}
 * saniyede bir olabiliyor — kaçmak seni kurtarmıyor ama <em>kısa</em> bir soluk
 * hâlâ alabiliyorsun.</p>
 */
public class Lort extends Boss {

    /** İki ışınlanma arasındaki en kısa süre, saniye. */
    private static final double BLINK_INTERVAL = 4.0;

    /** Oyuncu bu kadar kare uzaklaşırsa ışınlanma tetikleniyor. */
    private static final int BLINK_DISTANCE = 5;

    /** Dövüşün başında bir soluk payı. */
    private static final double FIRST_BLINK_DELAY = 5.0;

    /** Oyuncunun çevresinde belirebileceği kareler; sırayla deneniyor. */
    private static final int[][] LANDING_SPOTS = {
            {0, -1}, {0, 1}, {-1, 0}, {1, 0}, {-1, -1}, {1, -1}, {-1, 1}, {1, 1}};

    private double blinkTimer = FIRST_BLINK_DELAY;

    public Lort(int tileX, int tileY) {
        super(tileX, tileY, Text.BOSS_LORT, "boss");
    }

    @Override
    protected void onUpdate(Game game, double delta) {
        blinkTimer -= delta;

        Player player = game.getPlayer();
        if (blinkTimer > 0 || tileDistanceTo(player) < BLINK_DISTANCE) {
            return;
        }

        // Adım ortasında olmak engel değil: ışınlanmak zaten adımı iptal ediyor
        // ({@code setTile}). Beklemeyi denedik ama boss kovalarken neredeyse
        // her karede adım hâlinde oluyor, yani ışınlanma hiç gerçekleşmiyordu.
        Position landing = findLanding(game, player);
        if (landing == null) {
            return;
        }

        blinkTimer = BLINK_INTERVAL;
        setTile(landing);
        game.getMessageLog().combat(Text.MSG_BOSS_BLINK.get(getName()));
    }

    /** Oyuncunun çevresinde boş bir kare; hepsi doluysa ışınlanma iptal. */
    private Position findLanding(Game game, Player player) {
        for (int[] spot : LANDING_SPOTS) {
            int x = player.getTileX() + spot[0];
            int y = player.getTileY() + spot[1];
            if (game.isTileFree(x, y, this)) {
                return new Position(x, y);
            }
        }
        return null;
    }
}
