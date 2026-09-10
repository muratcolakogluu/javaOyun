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
 *
 * <p><b>Gezilmiş katlar da kaydediliyor.</b> Merdivenden geri çıkabilmek
 * geldiğinden beri kat hafızası oyunun bir kuralı: çıktığın kat düşmanları ve
 * ganimetiyle olduğu gibi bekliyor. Kayıt yalnızca bulunduğun katı saklarsa bu
 * kural kaydet-yükle ile deliniyordu — çık, kaydet, yükle, in; kat yepyeni
 * ganimetle karşına geliyordu. Yani kattaki durumu saklama gerekçesinin bir üst
 * katmanı.</p>
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
        int playerMaxHp,
        int equippedWeaponSlot,
        int equippedArmorSlot,
        int returns,
        String visionMask,
        List<ItemData> inventory,
        List<ItemData> groundItems,
        List<EnemyData> enemies,
        List<FloorData> visitedFloors) {

    /** Kuşanılmış parça yoksa slot alanına yazılan değer. */
    public static final int NO_SLOT = -1;

    /**
     * Sürüm 6 öncesi kayıtlarda dayanıklılık alanı yok.
     *
     * <p>Bu değerle karşılaşan taraf parçayı tam dolu kabul ediyor: eski kaydı
     * yükleyen oyuncuyu kırık kılıçla cezalandırmak yanlış olurdu.</p>
     */
    public static final int UNKNOWN_DURABILITY = -1;

    public SaveData {
        visionMask = visionMask == null ? "" : visionMask;
        inventory = List.copyOf(inventory);
        groundItems = List.copyOf(groundItems);
        enemies = List.copyOf(enemies);
        visitedFloors = List.copyOf(visitedFloors);
    }

    /**
     * Keşfi, kat hafızası ve geri dönüş sayacı olmayan kayıt: sürüm 8 öncesi
     * dosyalar.
     *
     * <p>Boş maske "hiçbir yeri görmedin" demek, yani eski bir kaydı açan
     * oyuncu katı yeniden keşfediyor. Uydurulmuş bir haritayı doğru sanmasından
     * iyi — {@link #UNKNOWN_DURABILITY} ile aynı yaklaşım.</p>
     */
    public SaveData(int depth, long seed, int generatorIndex, int gold, double elapsedSeconds,
                    int playerX, int playerY, int playerHp, int playerMaxHp,
                    int equippedWeaponSlot, int equippedArmorSlot,
                    List<ItemData> inventory, List<ItemData> groundItems,
                    List<EnemyData> enemies) {
        this(depth, seed, generatorIndex, gold, elapsedSeconds, playerX, playerY, playerHp,
                playerMaxHp, equippedWeaponSlot, equippedArmorSlot, 0, "",
                inventory, groundItems, enemies, List.of());
    }

    /**
     * Bırakıldığı hâliyle saklanan bir kat.
     *
     * <p>Bulunduğun kattan tek farkı oyuncunun orada olmaması: harita yine
     * tohumdan üretiliyor, üstündeki düşmanlar ve eşyalar yine tek tek
     * yazılıyor. Zindanın sabrı da geliyor, çünkü uyanmış bir kata döndüğünde
     * takviyelerin kaldığı yerden devam etmesi gerekiyor.</p>
     *
     * @param visionMask o katta keşfedilmiş kareler
     * @param awake      zindan o katta oyuncuyu fark etmiş miydi
     */
    public record FloorData(int depth, long seed, int generatorIndex, double floorSeconds,
                            boolean awake, String visionMask,
                            List<ItemData> groundItems, List<EnemyData> enemies) {

        public FloorData {
            visionMask = visionMask == null ? "" : visionMask;
            groundItems = List.copyOf(groundItems);
            enemies = List.copyOf(enemies);
        }
    }

    /**
     * Bir eşyanın kaydı.
     *
     * <p>Dayanıklılık ve yükseltme yalnızca silah ve zırh için anlamlı; iksir
     * ve altın bu alanlara sıfır yazıyor. Her eşya türüne ayrı kayıt biçimi
     * yazmaktansa iki alanı boş geçmek daha ucuz.</p>
     *
     * @param kind         POTION, GOLD, WEAPON ya da ARMOR
     * @param value        altın miktarı ya da <em>taban</em> vuruş/savunma bonusu
     * @param durability   kalan dayanıklılık
     * @param upgradeLevel büyücüde kaç kademe yükseltildiği
     * @param enchantment  basılı büyünün etiketi; büyü yoksa boş dizge
     */
    public record ItemData(String kind, int x, int y, String name, int value, String spriteName,
                           int durability, int upgradeLevel, String enchantment) {

        /** Yıpranma bilinmeyen eski kayıtlar için: parça sağlam sayılıyor. */
        public ItemData(String kind, int x, int y, String name, int value, String spriteName) {
            this(kind, x, y, name, value, spriteName, UNKNOWN_DURABILITY, 0, "");
        }
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
