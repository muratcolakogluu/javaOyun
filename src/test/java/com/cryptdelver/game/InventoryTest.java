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
        Item first = new Potion(0, 0);
        Item second = new Potion(1, 0);

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
        for (int i = 0; i < Inventory.CAPACITY; i++) {
            assertTrue(inventory.add(new Potion(0, 0)));
        }

        assertTrue(inventory.isFull());
        assertFalse(inventory.add(new Potion(0, 0)));
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
        Item first = new Potion(0, 0);
        Item second = new Potion(1, 0);
        inventory.add(first);
        inventory.add(second);

        inventory.remove(first);

        assertSame(second, inventory.get(0), "Ikinci esya bosalan slota kaymali");
        assertEquals(1, inventory.size());
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
