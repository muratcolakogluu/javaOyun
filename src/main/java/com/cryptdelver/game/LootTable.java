package com.cryptdelver.game;

import com.cryptdelver.entity.Armor;
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
 *
 * <p>Bu tablo aynı zamanda <b>yükseltmenin tavanını</b> belirliyor: büyücüde
 * bir parçayı en fazla, o katta bossun bırakacağı parçanın seviyesine kadar
 * çıkarabiliyorsun. Altınla bossu atlamak yok.</p>
 */
public final class LootTable {

    /** Kaç katta bir kademe atlanır. */
    private static final int FLOORS_PER_TIER = 3;

    /** En yüksek kademe (1 tabanlı). */
    public static final int MAX_TIER = 4;

    /**
     * Bir ekipman kademesinin verisi.
     *
     * @param durability kaç kullanım dayanır; üst kademeler hem daha güçlü hem
     *                   daha uzun ömürlü, yoksa iyi parça bulmak yükü artırırdı
     */
    private record Gear(Text name, int bonus, String spriteName, int durability) {
    }

    private static final Gear[] WEAPONS = {
            new Gear(Text.WEAPON_RUSTY, 2, "sword", 90),
            new Gear(Text.WEAPON_STEEL, 4, "sword_steel", 120),
            new Gear(Text.WEAPON_AXE, 6, "axe", 150),
            new Gear(Text.WEAPON_CRYPT, 9, "sword_crypt", 180),
    };

    private static final Gear[] ARMORS = {
            new Gear(Text.ARMOR_LEATHER, 1, "armor_leather", 60),
            new Gear(Text.ARMOR_CHAIN, 2, "armor_chain", 80),
            new Gear(Text.ARMOR_PLATE, 4, "armor_plate", 100),
            new Gear(Text.ARMOR_CRYPT, 6, "armor_crypt", 120),
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
        return new Weapon(tileX, tileY, gear.name(), gear.bonus(), gear.spriteName(),
                gear.durability());
    }

    public static Armor armorForTier(int tier, int tileX, int tileY) {
        Gear gear = ARMORS[clampIndex(tier)];
        return new Armor(tileX, tileY, gear.name(), gear.bonus(), gear.spriteName(),
                gear.durability());
    }

    /** Kademenin silah bonusu; yükseltme tavanı buradan okunuyor. */
    public static int weaponBonusForTier(int tier) {
        return WEAPONS[clampIndex(tier)].bonus();
    }

    /** Kademenin zırh bonusu; yükseltme tavanı buradan okunuyor. */
    public static int armorBonusForTier(int tier) {
        return ARMORS[clampIndex(tier)].bonus();
    }

    /**
     * Tabloda olmayan bir bonusa dayanıklılık uydurur.
     *
     * <p>Eski kayıtlardan ya da testlerden elle üretilmiş parçalar için gerekli:
     * bonusu aşmayan en yüksek kademenin dayanıklılığını veriyoruz. Tabloya yeni
     * kademe eklendiğinde burası kendiliğinden doğru kalıyor.</p>
     */
    public static int weaponDurabilityFor(int bonus) {
        return durabilityFor(WEAPONS, bonus);
    }

    public static int armorDurabilityFor(int bonus) {
        return durabilityFor(ARMORS, bonus);
    }

    private static int durabilityFor(Gear[] table, int bonus) {
        int durability = table[0].durability();
        for (Gear gear : table) {
            if (gear.bonus() <= bonus) {
                durability = gear.durability();
            }
        }
        return durability;
    }

    /** Kademeyi dizi indisine çevirir ve sınırların içinde tutar. */
    private static int clampIndex(int tier) {
        return Math.clamp(tier, 1, MAX_TIER) - 1;
    }
}
