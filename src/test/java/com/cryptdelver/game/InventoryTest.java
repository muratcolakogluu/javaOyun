package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Item;
import com.cryptdelver.entity.Potion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InventoryTest {

    @Test
    @DisplayName("Eklenen esya slot sirasini korur")
    void slotsKeepInsertionOrder() {
        Inventory inventory = new Inventory();
        Item first = LootTable.weaponForTier(1, 0, 0);
        Item second = LootTable.armorForTier(1, 0, 0);

        inventory.add(first);
        inventory.add(second);

        assertSame(first, inventory.get(0));
        assertSame(second, inventory.get(1));
        assertEquals(2, inventory.size());
    }

    @Test
    @DisplayName("Kapasite dolunca ekleme reddedilir")
    void addingBeyondCapacityFails() {
        Inventory inventory = new Inventory();
        // Yigilmayan parcalarla dolduruyoruz; iksirler tek slotu paylasirdi.

        for (int i = 0; i < Inventory.CAPACITY; i++) {
            assertTrue(inventory.add(LootTable.weaponForTier(1, 0, 0)));
        }

        assertTrue(inventory.isFull());
        assertFalse(inventory.add(LootTable.armorForTier(1, 0, 0)));
        assertEquals(Inventory.CAPACITY, inventory.size());
    }

    @Test
    @DisplayName("Gecersiz slot null doner")
    void invalidSlotReturnsNull() {
        Inventory inventory = new Inventory();
        inventory.add(new Potion(0, 0));

        assertNull(inventory.get(-1));
        assertNull(inventory.get(1));
        assertNull(inventory.get(Inventory.CAPACITY));
    }

    @Test
    @DisplayName("Silinen esya sonraki slotlari kaydirir")
    void removingShiftsLaterSlots() {
        Inventory inventory = new Inventory();
        Item first = LootTable.weaponForTier(1, 0, 0);
        Item second = LootTable.armorForTier(1, 0, 0);
        inventory.add(first);
        inventory.add(second);

        inventory.remove(first);

        assertSame(second, inventory.get(0), "Ikinci esya bosalan slota kaymali");
        assertEquals(1, inventory.size());
    }

    @Test
    @DisplayName("Ayni iksirler tek slotta yigilir")
    void identicalPotionsShareASlot() {
        Inventory inventory = new Inventory();

        inventory.add(new Potion(0, 0));
        inventory.add(new Potion(0, 0));
        inventory.add(new Potion(0, 0));

        assertEquals(1, inventory.size(), "Uc iksir tek slot kaplamali");
        assertEquals(3, inventory.getCount(0));
        assertEquals(3, inventory.totalItems());
    }

    @Test
    @DisplayName("Ekipman yigilmaz, her parca kendi slotunda")
    void gearDoesNotStack() {
        Inventory inventory = new Inventory();

        inventory.add(LootTable.weaponForTier(1, 0, 0));
        inventory.add(LootTable.weaponForTier(1, 0, 0));

        assertEquals(2, inventory.size(), "Iki kilic iki slot kaplamali");
        assertEquals(1, inventory.getCount(0));
    }

    @Test
    @DisplayName("Yigindan tek tek harcanir")
    void removingTakesOneFromTheStack() {
        Inventory inventory = new Inventory();
        inventory.add(new Potion(0, 0));
        inventory.add(new Potion(0, 0));

        inventory.remove(inventory.get(0));

        assertEquals(1, inventory.size(), "Slot hala duruyor");
        assertEquals(1, inventory.getCount(0));

        inventory.remove(inventory.get(0));

        assertTrue(inventory.isEmpty(), "Yigin bosalinca slot kapanmali");
    }

    @Test
    @DisplayName("Canta doluyken bile acik yigina eklenebilir")
    void fullBagStillAcceptsStackableItems() {
        Inventory inventory = new Inventory();
        inventory.add(new Potion(0, 0));
        for (int i = 1; i < Inventory.CAPACITY; i++) {
            inventory.add(LootTable.weaponForTier(1, 0, 0));
        }
        assertTrue(inventory.isFull());

        assertTrue(inventory.add(new Potion(0, 0)), "Iksir acik yigina girmeli");
        assertFalse(inventory.add(LootTable.armorForTier(1, 0, 0)), "Yeni slot acilamaz");
        assertEquals(2, inventory.getCount(0));
    }

    @Test
    @DisplayName("Slot arama yigindaki her parcayi bulur")
    void slotLookupFindsItemsInsideStacks() {
        Inventory inventory = new Inventory();
        Item weapon = LootTable.weaponForTier(2, 0, 0);
        Item potion = new Potion(0, 0);
        inventory.add(weapon);
        inventory.add(potion);
        inventory.add(new Potion(0, 0));

        assertEquals(0, inventory.slotOf(weapon));
        assertEquals(1, inventory.slotOf(potion), "Yiginin icindeki parca da bulunmali");
        assertEquals(-1, inventory.slotOf(null), "Kusanilmamis parca icin -1");
    }

    @Test
    @DisplayName("Temizlenen canta bos kalir")
    void clearEmptiesTheBag() {
        Inventory inventory = new Inventory();
        inventory.add(new Potion(0, 0));

        inventory.clear();

        assertTrue(inventory.isEmpty());
        assertEquals(0, inventory.size());
    }
}
