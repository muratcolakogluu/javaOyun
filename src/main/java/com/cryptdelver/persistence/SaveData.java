package com.cryptdelver.persistence;

import java.util.List;

/**
 * Kaydedilmiş bir oyunun tam durumu.
 *
 * <p>Oyun nesnelerine değil, sade değerlere bakar: {@code Player}, {@code Imp}
 * gibi sınıfları tanımaz, onları {@code "IMP"} gibi etiketlerle anar. Bu ayrım
 * sayesinde dosya biçimi oyun sınıflarının iç yapısına bağlı kalmıyor — bir
 * düşmana yeni bir alan eklemek kayıt dosyasını bozmuyor.</p>
 *
 * <p><b>Harita kaydedilmiyor:</b> yalnızca {@code seed} ve
 * {@code generatorIndex} saklanıyor. Üreticiler aynı tohumla aynı haritayı
 * ürettiği için (bu 3. adımdan beri testle garanti) 1440 kareyi diske yazmaya
 * gerek yok. Ama harita üstündeki <em>durum</em> — düşmanlar, yerdeki eşyalar —
 * kaydediliyor; yoksa kaydedip yüklemek katı yeniden doldurup altın ve iksir
 * çiftliğine dönüştürürdü.</p>
 */
public record SaveData(
        int depth,
        long seed,
        int generatorIndex,
        int gold,
        double elapsedSeconds,
        int playerX,
        int playerY,
        int playerHp,
        int equippedWeaponSlot,
        int equippedArmorSlot,
        int equippedShieldSlot,
        int equippedHelmetSlot,
        List<ItemData> inventory,
        List<ItemData> groundItems,
        List<EnemyData> enemies) {

    /** Kuşanılmış parça yoksa slot alanına yazılan değer. */
    public static final int NO_SLOT = -1;

    public SaveData {
        inventory = List.copyOf(inventory);
        groundItems = List.copyOf(groundItems);
        enemies = List.copyOf(enemies);
    }

    /**
     * Bir eşyanın kaydı.
     *
     * @param kind  POTION, GOLD, WEAPON ya da ARMOR
     * @param value altın miktarı, vuruş bonusu ya da savunma bonusu
     */
    public record ItemData(String kind, int x, int y, String name, int value, String spriteName) {
    }

    /**
     * Bir düşmanın kaydı.
     *
     * <p>Bonusları değil <em>toplam</em> değerleri saklıyoruz: yüklerken taze
     * bir düşman üretip aradaki farkı ekliyoruz. Böylece tür değerlerini
     * ({@code EnemyStats}) sonradan dengelemek eski kayıtları bozmuyor.</p>
     *
     * @param kind RAT, SKELETON ya da BOSS
     */
    public record EnemyData(String kind, int x, int y, int hp, int maxHp, int attack, int defense) {
    }
}
