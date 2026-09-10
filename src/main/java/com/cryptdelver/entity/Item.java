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
     * Çantadan yere bırakıldığı anda çalışır.
     *
     * <p>{@link #onPickup} ile simetrik: kuşanılan eşyalar burada üstünden
     * çıkarılıyor, yoksa yere attığın kılıcın bonusu üstünde kalırdı.</p>
     */
    public void onDrop(Game game) {
        // Kuşanılmayan eşyalar için yapacak bir şey yok.
    }

    /**
     * Çantadan kullanıldığında çalışır.
     *
     * @return eşya tükendiyse {@code true} — çantadan silinir. Silah gibi kalıcı
     *         eşyalar {@code false} döner.
     */
    public abstract boolean use(Game game);

    /**
     * Kayıt dosyasındaki tür etiketi ({@code "POTION"}, {@code "GOLD"}...).
     *
     * <p>Kaydetme kodunun {@code instanceof} zinciri yazmasını önlüyor: eşya
     * kendini nasıl adlandıracağını kendi biliyor. Karşılığında model sınıfları
     * kayıt biçiminden az da olsa haberdar oluyor — bilinçli bir takas.</p>
     */
    public abstract String getSaveKind();

    /** Kayıtta saklanan sayısal değer: altın miktarı, vuruş ya da savunma bonusu. */
    public int getSaveValue() {
        return 0;
    }

    /**
     * Kayıtta saklanan dayanıklılık ve yükseltme kademesi.
     *
     * <p>Yalnızca kuşanılan parçalar için anlamlı; iksir ve altın sıfır
     * döndürüyor. Bunu {@code instanceof Equipment} ile sormak yerine soruyu
     * eşyanın kendisine bırakmak, kaydetme kodunu tür bilmekten kurtarıyor —
     * {@link #getSaveValue()} ile aynı gerekçe.</p>
     */
    public int getSaveDurability() {
        return 0;
    }

    public int getSaveUpgradeLevel() {
        return 0;
    }

    /**
     * Balonda görünen ad; ekipman yükseltmesini ve büyülerini de gösteriyor.
     *
     * <p>Varsayılan olarak eşyanın adı. {@link Equipment} bunu geçersiz kılıp
     * "+2" ve büyü etiketlerini ekliyor, çünkü aynı adı taşıyan iki kılıç
     * birbirinden ancak böyle ayrılıyor.</p>
     */
    public String getFullTooltipName() {
        return getName();
    }

    /**
     * Çantada üstüne gelince görünen tek satırlık açıklama.
     *
     * <p>Slotta yalnızca ikon vardı: dört nadir eşyanın üçü aynı şekilde şişe,
     * yalnızca renkleri farklı. Elinde yeşil ve sarı şişe varken hangisinin
     * hız hangisinin öfke olduğu tamamen ezberdi. Cevabı eşyanın kendisi
     * veriyor, çizim katmanı tür kontrolü yapmıyor.</p>
     */
    public String getDescription() {
        return "";
    }

    /** Kayıtta saklanan büyünün etiketi; büyü yoksa boş dizge. */
    public String getSaveEnchantment() {
        return "";
    }

    /**
     * Üstünde büyü var mı.
     *
     * <p>Çizim katmanı büyülü parçaları parlatmak için soruyor. Cevabı eşyanın
     * kendisi veriyor: iksirin ya da altının büyüsü olamaz, o yüzden onlar için
     * soru {@code false} ile bitiyor ve çizim tarafında tür kontrolü
     * gerekmiyor.</p>
     */
    public boolean isEnchanted() {
        return false;
    }

    /**
     * Aynı türden eşyalar çantada tek slotu paylaşabilir mi.
     *
     * <p>İksir gibi tüketilenler yığılır ("3x"), ekipman yığılmaz: her kılıcın
     * ve zırhın kendi kimliği var, kuşanılan parçanın hangisi olduğu önemli.</p>
     */
    public boolean isStackable() {
        return false;
    }

    /**
     * Üstüne basınca kendiliğinden alınır mı.
     *
     * <p>Toplamayı F tuşuna bağlarken kuralı <em>her</em> eşyaya uygulamıştık;
     * yanlıştı. Toplamanın tuşa bağlanmasının nedeni çanta yönetimiydi: yerde
     * bıraktığın kılıcın üstünden geçmek onu geri almasın, kaçarken bastığın
     * kötü zırh slot doldurmasın. İksirin ve altının böyle bir sorunu yok —
     * iksirler tek slotta yığılıyor, altın çantaya hiç girmiyor. Onlar için tuş
     * beklemek, hiçbir karara karşılık gelmeyen fazladan bir iş demekti.</p>
     *
     * <p>Sınır tam da bu: <b>çantanı sıkıştırabilen şeyler tuş istiyor,
     * sıkıştıramayanlar istemiyor.</b> Cevabı eşyanın kendisi veriyor, toplama
     * kodu tür kontrolü yapmıyor.</p>
     */
    public boolean isAutoPickedUp() {
        return false;
    }
}
