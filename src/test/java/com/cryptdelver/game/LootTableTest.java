package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Armor;
import com.cryptdelver.entity.Weapon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Ganimet kademelerinin derinlikle iliskisi.
 *
 * <p>Buradaki en onemli test {@code lootNeverGetsWorseWithDepth}: oyunun
 * ilerleme hissi buna dayaniyor, 2. katta bulunan 4. kattakinden iyi olamaz.</p>
 */
class LootTableTest {

    private static final int DEEPEST_TESTED_FLOOR = 40;

    @Test
    @DisplayName("Kademe derinlikle artar, ilk katta en dusuk")
    void tierRisesWithDepth() {
        assertEquals(1, LootTable.tierForDepth(1));
        assertEquals(1, LootTable.tierForDepth(3));
        assertEquals(2, LootTable.tierForDepth(4));
        assertEquals(3, LootTable.tierForDepth(7));
        assertEquals(4, LootTable.tierForDepth(10));
    }

    @Test
    @DisplayName("Kademe tavanda kalir, tasmaz")
    void tierStopsAtTheCap() {
        assertEquals(LootTable.MAX_TIER, LootTable.tierForDepth(100));
        assertEquals(LootTable.MAX_TIER, LootTable.bossTierForDepth(100));
    }

    @Test
    @DisplayName("Ganimet derinlikle asla kotulesmez")
    void lootNeverGetsWorseWithDepth() {
        int previousAttack = 0;
        int previousDefense = 0;

        for (int depth = 1; depth <= DEEPEST_TESTED_FLOOR; depth++) {
            int tier = LootTable.tierForDepth(depth);
            Weapon weapon = LootTable.weaponForTier(tier, 0, 0);
            Armor armor = LootTable.armorForTier(tier, 0, 0);

            assertTrue(weapon.getAttackBonus() >= previousAttack,
                    depth + ". kattaki silah oncekinden kotu olmamali");
            assertTrue(armor.getDefenseBonus() >= previousDefense,
                    depth + ". kattaki zirh oncekinden kotu olmamali");

            previousAttack = weapon.getAttackBonus();
            previousDefense = armor.getDefenseBonus();
        }
    }

    @Test
    @DisplayName("Derin katta bulunan sig kattakinden kesin iyi")
    void deepFloorsBeatShallowOnes() {
        Weapon shallow = LootTable.weaponForTier(LootTable.tierForDepth(2), 0, 0);
        Weapon deep = LootTable.weaponForTier(LootTable.tierForDepth(4), 0, 0);

        assertTrue(deep.getAttackBonus() > shallow.getAttackBonus(),
                "4. kat silahi 2. kattakinden iyi olmali");
    }

    @Test
    @DisplayName("Boss ganimeti kattakinin bir ustu")
    void bossLootIsOneTierBetter() {
        for (int depth = 1; depth <= 9; depth++) {
            assertEquals(LootTable.tierForDepth(depth) + 1, LootTable.bossTierForDepth(depth),
                    depth + ". katta boss bir ust kademe vermeli");
        }

        Weapon floorWeapon = LootTable.weaponForTier(LootTable.tierForDepth(5), 0, 0);
        Weapon bossWeapon = LootTable.weaponForTier(LootTable.bossTierForDepth(5), 0, 0);
        assertTrue(bossWeapon.getAttackBonus() > floorWeapon.getAttackBonus());
    }

    @Test
    @DisplayName("Gecersiz kademe istekleri sinirlarda kalir")
    void tierRequestsAreClamped() {
        assertEquals(LootTable.weaponForTier(1, 0, 0).getAttackBonus(),
                LootTable.weaponForTier(0, 0, 0).getAttackBonus());
        assertEquals(LootTable.armorForTier(LootTable.MAX_TIER, 0, 0).getDefenseBonus(),
                LootTable.armorForTier(99, 0, 0).getDefenseBonus());
    }
}
