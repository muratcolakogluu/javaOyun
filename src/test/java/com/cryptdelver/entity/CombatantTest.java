package com.cryptdelver.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Can ve savunmanin ortak davranisi. Player ile Enemy bunlari artik tek yerden
 * miras aldigi icin burada bir kez test etmek yetiyor.
 */
class CombatantTest {

    /** Testte kullanilan en sade dovuscu. */
    private static class Dummy extends Combatant {

        private final int attack;
        private final int defense;

        Dummy(int maxHp, int attack, int defense) {
            super(0, 0, "Kukla", maxHp);
            this.attack = attack;
            this.defense = defense;
        }

        @Override
        public int getAttackPower() {
            return attack;
        }

        @Override
        public int getDefense() {
            return defense;
        }

        @Override
        public String getSpriteName() {
            return "unknown";
        }
    }

    @Test
    @DisplayName("Yeni dovuscu dolu canla baslar")
    void startsAtFullHealth() {
        Dummy dummy = new Dummy(12, 3, 0);

        assertEquals(12, dummy.getHp());
        assertEquals(12, dummy.getMaxHp());
        assertTrue(dummy.isAlive());
    }

    @Test
    @DisplayName("Can sifirin altina inmez")
    void healthNeverGoesBelowZero() {
        Dummy dummy = new Dummy(10, 3, 0);

        dummy.takeDamage(25);

        assertEquals(0, dummy.getHp());
        assertFalse(dummy.isAlive());
    }

    @Test
    @DisplayName("Iyilesme azami cani asmaz")
    void healingStopsAtMaxHealth() {
        Dummy dummy = new Dummy(10, 3, 0);
        dummy.takeDamage(3);

        dummy.heal(99);

        assertEquals(10, dummy.getHp());
    }

    @Test
    @DisplayName("Guclenince kazanilan can hemen dolar")
    void raisingMaxHealthAlsoFillsIt() {
        Dummy dummy = new Dummy(10, 3, 0);

        dummy.increaseMaxHp(5);

        assertEquals(15, dummy.getMaxHp());
        assertEquals(15, dummy.getHp(), "Yeni can yarim dolu birakilmamali");
    }

    @Test
    @DisplayName("Savunma turden gelir: ciplak imp 0, kemikli iskelet 1")
    void defenseComesFromTheType() {
        assertEquals(0, new Player(0, 0).getDefense(), "Zirhsiz oyuncunun savunmasi yok");
        assertEquals(0, new Imp(0, 0).getDefense());
        assertEquals(1, new Skeleton(0, 0).getDefense());
    }

    @Test
    @DisplayName("Dusman guclendirmesi can, guc ve savunmayi birlikte artirir")
    void strengthenRaisesAllThreeStats() {
        Imp imp = new Imp(0, 0);
        int baseHp = imp.getMaxHp();
        int baseAttack = imp.getAttackPower();
        int baseDefense = imp.getDefense();

        imp.strengthen(4, 2, 1);

        assertEquals(baseHp + 4, imp.getMaxHp());
        assertEquals(baseHp + 4, imp.getHp());
        assertEquals(baseAttack + 2, imp.getAttackPower());
        assertEquals(baseDefense + 1, imp.getDefense());
    }
}
