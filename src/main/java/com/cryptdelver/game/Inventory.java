package com.cryptdelver.game;

import com.cryptdelver.entity.Item;
import java.util.ArrayList;
import java.util.List;

/**
 * Oyuncunun çantası: sınırlı sayıda eşya taşır.
 *
 * <p>Kapasitenin sınırlı olması bilinçli bir oyun kararı — her şeyi toplayıp
 * gezemezsin, neyi bırakacağına karar vermen gerekir. Slotlar ekranda 1-8
 * tuşlarıyla eşleşiyor, bu yüzden sıra korunuyor.</p>
 */
public class Inventory {

    /** Taşınabilecek azami eşya sayısı; ekrandaki slot sayısıyla aynı. */
    public static final int CAPACITY = 8;

    private final List<Item> items = new ArrayList<>();

    /**
     * Eşyayı çantaya koyar.
     *
     * @return çanta doluysa {@code false}
     */
    public boolean add(Item item) {
        if (isFull()) {
            return false;
        }
        return items.add(item);
    }

    public boolean remove(Item item) {
        return items.remove(item);
    }

    /** Slottaki eşya; slot boş ya da geçersizse {@code null}. */
    public Item get(int slot) {
        return slot >= 0 && slot < items.size() ? items.get(slot) : null;
    }

    public List<Item> getItems() {
        return List.copyOf(items);
    }

    public int size() {
        return items.size();
    }

    public boolean isFull() {
        return items.size() >= CAPACITY;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public void clear() {
        items.clear();
    }
}
