package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Enchantment;
import com.cryptdelver.entity.LegendWeapon;
import com.cryptdelver.entity.Player;
import com.cryptdelver.entity.Weapon;
import com.cryptdelver.world.BspGenerator;
import com.cryptdelver.world.Dungeon;
import com.cryptdelver.world.Tile;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Efsanevi kilic ve ikinci buyu yuvasi.
 *
 * <p>Yildizkiran oyunun iki kuralinin bilincli istisnasi: vurusu bossun
 * birakacagi odulun de ustunde ve iki buyu tasiyabiliyor. Testler bu iki
 * istisnanin gercekten calistigini ve siradan parcalara sizmadigini
 * kovaliyor.</p>
 */
class LegendWeaponTest {

    private Player player;
    private Game game;

    @BeforeEach
    void setUp() {
        Dungeon dungeon = new Dungeon(11, 9);
        dungeon.fill(Tile.FLOOR);
        player = new Player(4, 4);
        game = new Game(dungeon, player);
    }

    @Test
    @DisplayName("Efsanevi kilic en ust kademeden de guclu")
    void theLegendBeatsEveryTier() {
        int best = LootTable.weaponBonusForTier(LootTable.MAX_TIER);

        assertTrue(new LegendWeapon(0, 0).getBonus() > best,
                "Yildizkiran Kript Kilicini gecmeli");
    }

    /** Bonusu her katin tavaninin ustunde oldugu icin yukseltilemiyor. */
    @Test
    @DisplayName("Efsanevi kilic yukseltilemiyor")
    void theLegendCannotBeUpgraded() {
        LegendWeapon legend = new LegendWeapon(0, 0);

        assertFalse(legend.canUpgrade(1));
        assertFalse(legend.canUpgrade(FloorTheme.MAX_DEPTH), "Son katta bile tavanin ustunde");
    }

    @Test
    @DisplayName("Efsanevi kilicta iki buyu yuvasi var")
    void theLegendHasTwoSlots() {
        LegendWeapon legend = new LegendWeapon(0, 0);

        assertEquals(2, legend.getEnchantSlots());
        assertEquals(1, new Weapon(0, 0, "Siradan", 4, "sword").getEnchantSlots(),
                "Siradan kilicta tek yuva");
    }

    @Test
    @DisplayName("Iki buyu ayni anda tasinabiliyor")
    void bothSlotsWorkAtOnce() {
        LegendWeapon legend = new LegendWeapon(0, 0);
        player.equip(legend);

        legend.enchant(Enchantment.VAMPIRLIK);
        legend.enchant(Enchantment.ACELE);

        assertTrue(legend.hasEnchantment(Enchantment.VAMPIRLIK));
        assertTrue(legend.hasEnchantment(Enchantment.ACELE));
        assertEquals(2, legend.getEnchantments().size());
        assertTrue(player.getAttackCooldown() < 0.35, "Acele gercekten isliyor");
    }

    /** Yuvalar dolunca en eski buyu gidiyor, en yeni degil. */
    @Test
    @DisplayName("Ucuncu buyu en eskinin yerine geciyor")
    void theThirdSpellReplacesTheOldest() {
        LegendWeapon legend = new LegendWeapon(0, 0);
        legend.enchant(Enchantment.VAMPIRLIK);
        legend.enchant(Enchantment.ACELE);

        Enchantment replaced = legend.enchant(Enchantment.YILDIRIM);

        assertEquals(Enchantment.VAMPIRLIK, replaced, "En eski gitmeli");
        assertFalse(legend.hasEnchantment(Enchantment.VAMPIRLIK));
        assertTrue(legend.hasEnchantment(Enchantment.ACELE), "Sonra basilan kalmali");
        assertTrue(legend.hasEnchantment(Enchantment.YILDIRIM));
    }

    @Test
    @DisplayName("Siradan kilicta ikinci buyu birincinin yerine geciyor")
    void anOrdinaryWeaponStillHoldsOne() {
        Weapon plain = new Weapon(0, 0, "Siradan", 4, "sword");

        plain.enchant(Enchantment.VAMPIRLIK);
        Enchantment replaced = plain.enchant(Enchantment.ACELE);

        assertEquals(Enchantment.VAMPIRLIK, replaced);
        assertEquals(1, plain.getEnchantments().size());
    }

    @Test
    @DisplayName("Ayni buyu ikinci kez yuva harcamiyor")
    void repeatingASpellChangesNothing() {
        LegendWeapon legend = new LegendWeapon(0, 0);
        legend.enchant(Enchantment.VAMPIRLIK);

        assertNull(legend.enchant(Enchantment.VAMPIRLIK), "Zaten var, kimse silinmemeli");
        assertEquals(1, legend.getEnchantments().size());
    }

    @Test
    @DisplayName("Adinda iki buyu de goruunuyor")
    void theNameListsBothSpells() {
        LegendWeapon legend = new LegendWeapon(0, 0);
        legend.enchant(Enchantment.VAMPIRLIK);
        legend.enchant(Enchantment.ACELE);

        String name = legend.getFullName();

        assertTrue(name.contains(Enchantment.VAMPIRLIK.getLabel()), name);
        assertTrue(name.contains(Enchantment.ACELE.getLabel()), name);
    }
}
