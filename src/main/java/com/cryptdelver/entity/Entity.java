package com.cryptdelver.entity;

import com.cryptdelver.game.Text;
import com.cryptdelver.world.Position;

/**
 * Zindanda yeri olan her şeyin ortak atası: oyuncu, düşmanlar, eşyalar.
 *
 * <p>Hareket ızgaraya kilitli: bir varlık her zaman bir karenin üstündedir ya
 * da komşu bir kareye <em>geçmektedir</em>. Adım başlayınca hedef kare
 * ({@code targetTile}) belirlenir ve {@code progress} 0'dan 1'e giderken varlık
 * iki karenin arasında çizilir. Adım ortasında yön değiştirilemez — kare kare
 * hareketin hissi buradan geliyor.</p>
 *
 * <p>Mantık ızgarada (tam sayı), görüntü arada (kesirli): oyun kuralları
 * {@link #getTile()} ile, çizim {@link #getRenderX()} ile çalışır.</p>
 */
public abstract class Entity {

    /** Vuruş anındaki beyaz parlamanın süresi (saniye). */
    private static final double HIT_FLASH_DURATION = 0.16;

    /**
     * Varlığın adı, çevrilebilir bir anahtar olarak.
     *
     * <p>Ad ham dizge olarak saklanmıyor çünkü dil oyunun ortasında
     * değişebiliyor: kurulurken çözülmüş bir ad, dil değişince eski dilde
     * kalırdı.</p>
     */
    private final Text nameKey;

    /**
     * Anahtarı olmayan adlar için.
     *
     * <p>Yalnızca testler kullanıyor: "Test Kılıcı" gibi uydurma adlara sözlükte
     * yer açmanın anlamı yok. Oyundaki her adın bir anahtarı var.</p>
     */
    private final String rawName;

    private int tileX;
    private int tileY;
    private int targetX;
    private int targetY;
    private boolean moving;
    private double progress;
    private double hitFlash;

    protected Entity(int tileX, int tileY, Text name) {
        this.nameKey = name;
        this.rawName = null;
        setTile(tileX, tileY);
    }

    /** Testler için: adı doğrudan veriyor, çeviriden geçmiyor. */
    protected Entity(int tileX, int tileY, String name) {
        this.nameKey = null;
        this.rawName = name;
        setTile(tileX, tileY);
    }

    /** Ekranda görünen ad; dil değişince bu da değişiyor. */
    public String getName() {
        return nameKey == null ? rawName : nameKey.get();
    }

    /** Varlığın mantıksal olarak üstünde durduğu kare. */
    public int getTileX() {
        return tileX;
    }

    public int getTileY() {
        return tileY;
    }

    public Position getTile() {
        return new Position(tileX, tileY);
    }

    /** Adım halindeyse gittiği kare; değilse bulunduğu kare. */
    public Position getTargetTile() {
        return new Position(targetX, targetY);
    }

    public boolean isMoving() {
        return moving;
    }

    /** Çizim için kesirli konum: iki karenin arasındaki gerçek yer. */
    public double getRenderX() {
        return tileX + 0.5 + (targetX - tileX) * progress;
    }

    public double getRenderY() {
        return tileY + 0.5 + (targetY - tileY) * progress;
    }

    /** Varlığı bir kareye oturtur ve devam eden adımı iptal eder. */
    public final void setTile(int x, int y) {
        this.tileX = x;
        this.tileY = y;
        this.targetX = x;
        this.targetY = y;
        this.moving = false;
        this.progress = 0;
    }

    /** Baktığı yön; başlangıçta sağa. */
    private int facingX = 1;
    private int facingY;

    public void setTile(Position tile) {
        setTile(tile.x(), tile.y());
    }

    /** Komşu kareye adımı başlatır. Uygunluk kontrolü çağıranın işi. */
    public void beginStep(int x, int y) {
        this.targetX = x;
        this.targetY = y;
        this.moving = true;
        this.progress = 0;
        face(x - tileX, y - tileY);
    }

    /**
     * Baktığı yön: son attığı adımın yönü.
     *
     * <p>Oyuncuda tuşlar belirliyordu ve yalnızca oyuncuda vardı. Artık
     * herkeste var, çünkü <b>bir şeyin arkasında olmak</b> bir kural hâline
     * geldi: arkadan inen vuruş daha sert. Yön hareketten çıkıyor, yani
     * ayrıca beslenmesi gereken bir alan değil — adım atan zaten dönmüş
     * oluyor.</p>
     */
    public int getFacingX() {
        return facingX;
    }

    public int getFacingY() {
        return facingY;
    }

    /** Yönü değiştirir; sıfır vektör yok sayılıyor, yani yerinde duran dönmez. */
    protected void face(int dx, int dy) {
        if (dx != 0 || dy != 0) {
            facingX = dx;
            facingY = dy;
        }
    }

    /**
     * Yüzünü verilen varlığa döner.
     *
     * <p>Çapraz duruyorsa baskın eksene dönüyor: yönler dört yönlü kalmalı,
     * yoksa "arkası" kavramı bulanıklaşır.</p>
     */
    public void faceTowards(Entity other) {
        int dx = other.tileX - tileX;
        int dy = other.tileY - tileY;

        if (Math.abs(dx) >= Math.abs(dy)) {
            face(Integer.signum(dx), 0);
        } else {
            face(0, Integer.signum(dy));
        }
    }

    /**
     * Verilen varlık bu varlığın <em>arkasında</em> mı.
     *
     * <p>Yalnızca tam arka sayılıyor, yan taraf değil: baktığı yönle ona
     * doğru olan yön birbirine ters bakıyorsa evet. Yanına geçmek kolay,
     * arkasına geçmek bir hamle gerektiriyor — ödül de ona göre.</p>
     */
    public boolean isBehind(Entity other) {
        int towardsX = other.tileX - tileX;
        int towardsY = other.tileY - tileY;
        return facingX * towardsX + facingY * towardsY < 0;
    }

    /**
     * Devam eden adımı ilerletir.
     *
     * @param amount kare cinsinden ilerleme payı
     * @return adım bittiyse artan pay, bitmediyse 0
     */
    public double advance(double amount) {
        if (!moving) {
            return 0;
        }

        progress += amount;
        if (progress < 1) {
            return 0;
        }

        // Adım tamamlandı; artan pay bir sonraki adıma taşınır ki hız sabit kalsın.
        double leftover = progress - 1;
        setTile(targetX, targetY);
        return leftover;
    }

    /**
     * Bu varlık verilen kareyi tutuyor mu. Adım halindeyken hem çıktığı hem
     * girdiği kare doludur; iki varlığın aynı kareye girmesini bu engelliyor.
     */
    public boolean occupies(int x, int y) {
        return (tileX == x && tileY == y) || (moving && targetX == x && targetY == y);
    }

    /** İki varlık arasındaki ızgara (Manhattan) uzaklığı. */
    public int tileDistanceTo(Entity other) {
        return Math.abs(tileX - other.tileX) + Math.abs(tileY - other.tileY);
    }

    /** Yan yana mı; çapraz komşuluk sayılmaz. */
    public boolean isAdjacentTo(Entity other) {
        return tileDistanceTo(other) == 1;
    }

    public void triggerHitFlash() {
        hitFlash = HIT_FLASH_DURATION;
    }

    public boolean isFlashing() {
        return hitFlash > 0;
    }

    /** Zamanlayıcıları ilerletir; her karede çağrılır. */
    protected void tickTimers(double delta) {
        if (hitFlash > 0) {
            hitFlash = Math.max(0, hitFlash - delta);
        }
    }

    /**
     * Ekranda hangi sprite ile çizileceğinin adı ({@code "imp"}, {@code "player"}...).
     *
     * <p>Varlık nasıl çizildiğini bilmez, yalnızca kim olduğunu söyler; çizimi
     * {@code ui} katmanındaki sprite kaydı seçer. Bu yüzden şekilden PNG'ye
     * geçmek model sınıflarının hiçbirini ilgilendirmiyor.</p>
     */
    public abstract String getSpriteName();

    /**
     * Sprite'ın kare boyutuna göre ölçeği. Boss gibi iri varlıklar bunu
     * büyüterek tehdidi görünür kılıyor; çizim kodu kimin iri olduğunu
     * bilmek zorunda kalmıyor.
     */
    public double getDrawScale() {
        return 1.0;
    }

    @Override
    public String toString() {
        return getName() + "(" + tileX + ", " + tileY + ")";
    }
}
