package com.cryptdelver.game;

import com.cryptdelver.entity.Armor;
import com.cryptdelver.entity.Shield;
import com.cryptdelver.entity.Weapon;

/**
 * Hangi katta hangi kalitede ekipman çıkacağını belirleyen tablo.
 *
 * <p>Temel kural: <b>ganimet kademesi yalnızca derinliğe bağlı ve asla
 * düşmez.</b> 2. katta bulduğun zırh 4. katta bulacağından iyi olamaz —
 * ilerlemenin his olarak da doğru olması için rastgelelik kademe seçimine hiç
 * karışmıyor. Her {@value #FLOORS_PER_TIER} katta bir kademe atlanıyor.</p>
 *
 * <p>Boss ganimeti bunun bir istisnası: bulunduğun katın <em>bir üst</em>
 * kademesinden silah bırakır. Yani bossu geçmek, sıradan katları soymaktan
 * daha hızlı güçlendiriyor.</p>
 */
public final class LootTable {

    /** Kaç katta bir kademe atlanır. */
    private static final int FLOORS_PER_TIER = 3;

    /** En yüksek kademe (1 tabanlı). */
    public static final int MAX_TIER = 4;

    /** Bir ekipman kademesinin verisi. */
    private record Gear(String name, int bonus, String spriteName) {
    }

    private static final Gear[] WEAPONS = {
            new Gear("Paslı Kılıç", 2, "sword"),
            new Gear("Çelik Kılıç", 4, "sword_steel"),
            new Gear("Savaş Baltası", 6, "axe"),
            new Gear("Kript Kılıcı", 9, "sword_crypt"),
    };

    private static final Gear[] ARMORS = {
            new Gear("Deri Zırh", 1, "armor_leather"),
            new Gear("Zincir Zırh", 2, "armor_chain"),
            new Gear("Plaka Zırh", 4, "armor_plate"),
            new Gear("Kript Plakası", 6, "armor_crypt"),
    };

    /**
     * Kalkanlar zırhtan az koruyor: savunma iki slottan toplandığı için
     * kalkanı da zırh kadar güçlü yapsaydık derin katlarda gelen hasar
     * tamamen alt sınıra yapışırdı.
     */
    private static final Gear[] SHIELDS = {
            new Gear("Tahta Kalkan", 1, "shield_wood"),
            new Gear("Demir Kalkan", 1, "shield_iron"),
            new Gear("Çelik Kalkan", 2, "shield_steel"),
            new Gear("Kript Kalkanı", 3, "shield_crypt"),
    };

    private LootTable() {
    }

    /** Verilen derinlikte çıkan ekipman kademesi; 1 ile {@link #MAX_TIER} arası. */
    public static int tierForDepth(int depth) {
        int tier = 1 + (Math.max(1, depth) - 1) / FLOORS_PER_TIER;
        return Math.min(MAX_TIER, tier);
    }

    /** Boss ganimetinin kademesi: kattakinin bir üstü. */
    public static int bossTierForDepth(int depth) {
        return Math.min(MAX_TIER, tierForDepth(depth) + 1);
    }

    public static Weapon weaponForTier(int tier, int tileX, int tileY) {
        Gear gear = WEAPONS[clampIndex(tier)];
        return new Weapon(tileX, tileY, gear.name(), gear.bonus(), gear.spriteName());
    }

    public static Armor armorForTier(int tier, int tileX, int tileY) {
        Gear gear = ARMORS[clampIndex(tier)];
        return new Armor(tileX, tileY, gear.name(), gear.bonus(), gear.spriteName());
    }

    public static Shield shieldForTier(int tier, int tileX, int tileY) {
        Gear gear = SHIELDS[clampIndex(tier)];
        return new Shield(tileX, tileY, gear.name(), gear.bonus(), gear.spriteName());
    }

    /** Kademeyi dizi indisine çevirir ve sınırların içinde tutar. */
    private static int clampIndex(int tier) {
        return Math.clamp(tier, 1, MAX_TIER) - 1;
    }
}
