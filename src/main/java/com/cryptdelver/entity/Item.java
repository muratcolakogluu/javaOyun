package com.cryptdelver.entity;

import com.cryptdelver.game.Game;

/**
 * Yerden toplanabilen eşyaların ortak atası.
 *
 * <p>Eşyalar da birer {@link Entity}: haritada bir kareleri ve sprite'ları var.
 * Ama kendi başlarına hareket etmedikleri için {@link Actor} arayüzünü
 * uygulamazlar — davranışı arayüzle ayırmanın karşılığı burada görünüyor,
 * eşyalar oyun döngüsünde hiç güncellenmiyor.</p>
 *
 * <p>Her eşya türü "toplanınca ne olur" ve "kullanılınca ne olur" sorularını
 * kendi yanıtlıyor; {@code Game} tarafında {@code if (item instanceof Potion)}
 * gibi bir kontrol yok.</p>
 */
public abstract class Item extends Entity {

    protected Item(int tileX, int tileY, String name) {
        super(tileX, tileY, name);
    }

    /**
     * Toplanınca çantaya girer mi.
     *
     * <p>Altın girmez: doğrudan keseye yazılır. Bu yüzden çanta dolu olsa bile
     * altın toplanabilir.</p>
     */
    public boolean goesToInventory() {
        return true;
    }

    /** Yerden alındığı anda çalışır. */
    public void onPickup(Game game) {
        // Çoğu eşya için yapacak bir şey yok; altın bunu geçersiz kılıyor.
    }

    /**
     * Çantadan kullanıldığında çalışır.
     *
     * @return eşya tükendiyse {@code true} — çantadan silinir. Silah gibi kalıcı
     *         eşyalar {@code false} döner.
     */
    public abstract boolean use(Game game);
}
