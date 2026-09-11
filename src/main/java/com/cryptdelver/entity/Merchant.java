package com.cryptdelver.entity;

import com.cryptdelver.game.Text;
import com.cryptdelver.world.Position;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Gezgin satıcı: altının ikinci müşterisi.
 *
 * <p>Uzun süre altını yalnızca büyücüye harcayabiliyordun ve büyücü beş katta
 * bir çıkıyordu. Yani <b>arada dört kat boyunca kese ölü bir kaynaktı</b> —
 * topluyordun ama yapacak bir şey yoktu. Büyü fiyatlarını kademelendirince
 * bekleyen birikim daha da büyüdü.</p>
 *
 * <p>Satıcı küçük ve sık olanı satıyor: iksir, bomba, hız. Büyücü "büyük
 * alışveriş" olarak özel kalıyor — takımını orada hazırlıyorsun, yolda
 * hayatta kalmanı satıcıdan alıyorsun.</p>
 *
 * <h2>Neden tezgâhı sınırlı</h2>
 * <p>Her şeyden sınırsız satsaydı altın bir <em>karar</em> olmaktan çıkar,
 * yalnızca bir sayaç olurdu: yeterince altınla her kata tam dolu inersin.
 * Üç parça, her birinden bir tane — "hangisini alayım" sorusu gerçek kalıyor
 * ve bir sonraki satıcıya kadar elindekiyle idare ediyorsun.</p>
 */
public class Merchant extends Entity {

    /** Tezgâhta kaç parça durur. */
    public static final int STOCK_SIZE = 3;

    /**
     * Tezgâhtaki tek bir parça ve fiyatı.
     *
     * <p>Fiyat eşyanın kendisinde değil burada: aynı iksirin yerde bulunanı
     * bedava, tezgâhtakinin bir bedeli var. Fiyat eşyanın değil <em>satışın</em>
     * özelliği.</p>
     */
    public record Offer(Item item, int price) {
    }

    /**
     * Fiyatlar.
     *
     * <p>Kat başına ortalama 75 altın topluyorsun. İksir 30, yani her katta
     * bir tane alınabilir; bomba ve nadir iksirler bir kat biriktirmeyi
     * gerektiriyor. Tam tezgâhı boşaltmak iki kat sürüyor — yani satıcıyı
     * görmek "her şeyi al" değil "neye ihtiyacım var" sorusu.</p>
     */
    private static final int POTION_PRICE = 30;
    private static final int BOMB_PRICE = 55;
    private static final int RARE_PRICE = 75;

    private final List<Offer> stock = new ArrayList<>();

    public Merchant(int tileX, int tileY) {
        super(tileX, tileY, Text.MERCHANT_NAME);
    }

    /**
     * Katın tohumundan tezgâh kurar.
     *
     * <p>Zar tohumdan atılıyor, genel rastgelelikten değil: kat kurulumu
     * tohumun saf bir fonksiyonu kalsın diye — büyücünün varlığında izlediğimiz
     * yolun aynısı.</p>
     *
     * <p>İlk sıra her zaman iksir: satıcıyı görmek en azından bir kez can
     * satın alabilmek demek olmalı. Kalan iki sıra değişiyor, yani iki farklı
     * satıcı aynı tezgâhı açmıyor.</p>
     */
    public static Merchant stocked(Position spot, long seed) {
        Merchant merchant = new Merchant(spot.x(), spot.y());
        Random random = new Random(seed);

        merchant.stock.add(new Offer(new Potion(0, 0), POTION_PRICE));
        for (int i = 1; i < STOCK_SIZE; i++) {
            merchant.stock.add(rollOffer(random));
        }
        return merchant;
    }

    private static Offer rollOffer(Random random) {
        return switch (random.nextInt(4)) {
            case 0 -> new Offer(new Bomb(0, 0), BOMB_PRICE);
            case 1 -> new Offer(new HastePotion(0, 0), RARE_PRICE);
            case 2 -> new Offer(new FuryPotion(0, 0), RARE_PRICE);
            default -> new Offer(new EscapePotion(0, 0), RARE_PRICE);
        };
    }

    /** Tezgâhta duranlar; satılanlar listeden düşüyor. */
    public List<Offer> getStock() {
        return List.copyOf(stock);
    }

    /** Verilen sıradaki parça; sıra boşsa ya da satılmışsa {@code null}. */
    public Offer offerAt(int index) {
        return index < 0 || index >= stock.size() ? null : stock.get(index);
    }

    /** Parçayı tezgâhtan düşürür; satış tamamlandığında çağrılıyor. */
    public void take(Offer offer) {
        stock.remove(offer);
    }

    public boolean isSoldOut() {
        return stock.isEmpty();
    }

    /**
     * Tezgâhın üstünde görünen tek satırlık laf.
     *
     * <p>Büyücüde olduğu gibi: cümleyi satıcının kendisi seçiyor, böylece
     * durumu okuyan bir NPC oluyor. Kesesi boş oyuncuya "al" demek anlamsız.</p>
     */
    public String greetingFor(int gold) {
        if (isSoldOut()) {
            return Text.MERCHANT_SOLD_OUT.get();
        }
        return gold < POTION_PRICE
                ? Text.MERCHANT_BROKE.get()
                : Text.MERCHANT_IDLE.get();
    }

    @Override
    public String getSpriteName() {
        return "merchant";
    }
}
