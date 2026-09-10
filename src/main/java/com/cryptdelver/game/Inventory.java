package com.cryptdelver.game;

import com.cryptdelver.entity.Item;
import java.util.ArrayList;
import java.util.List;

/**
 * Oyuncunun çantası: sınırlı sayıda <em>slot</em> taşır.
 *
 * <p>Aynı türden yığılabilen eşyalar tek slotu paylaşır — üç iksir bir slotta
 * "3x" olarak durur. Böylece kapasite sınırı hâlâ anlamlı (neyi bırakacağına
 * karar vermen gerekiyor) ama iksir toplamak çantayı tıkamıyor.</p>
 *
 * <p>Yığın, aynı eşyadan birden çok nesneyi gerçekten saklıyor; "sayı" tutup
 * eşyayı çoğaltmıyoruz. Böylece yere bırakırken elde gerçek bir nesne oluyor
 * ve kaydetme kodu her parçayı ayrı ayrı yazabiliyor.</p>
 */
public class Inventory {

    /** Taşınabilecek azami slot sayısı; ekrandaki slot sayısıyla aynı. */
    public static final int CAPACITY = 8;

    /** Aynı slotu paylaşan, birbirinin aynısı eşyalar. */
    private static final class Stack {

        private final String key;
        private final List<Item> items = new ArrayList<>();

        private Stack(String key, Item first) {
            this.key = key;
            this.items.add(first);
        }

        private Item top() {
            return items.get(items.size() - 1);
        }
    }

    private final List<Stack> stacks = new ArrayList<>();

    /**
     * Eşyayı çantaya koyar.
     *
     * <p>Yığılabilen bir eşyanın açık yığını varsa çanta dolu olsa bile kabul
     * edilir: yeni slot açılmıyor ki.</p>
     *
     * @return çanta doluysa ve yeni slot gerekiyorsa {@code false}
     */
    public boolean add(Item item) {
        Stack existing = stackFor(item);
        if (existing != null) {
            existing.items.add(item);
            return true;
        }

        if (isFull()) {
            return false;
        }
        return stacks.add(new Stack(stackKey(item), item));
    }

    /** Eşyanın tek bir örneğini çantadan çıkarır; yığın boşalırsa slot kapanır. */
    public boolean remove(Item item) {
        for (int i = 0; i < stacks.size(); i++) {
            Stack stack = stacks.get(i);
            if (stack.items.remove(item)) {
                if (stack.items.isEmpty()) {
                    stacks.remove(i);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Slottaki eşya; slot boş ya da geçersizse {@code null}.
     *
     * <p>Yığının en üstteki örneğini verir — kullanmak, kuşanmak ve bırakmak
     * hep tek bir örneği ilgilendirdiği için çağıranlar yığından habersiz
     * çalışabiliyor.</p>
     */
    public Item get(int slot) {
        return slot >= 0 && slot < stacks.size() ? stacks.get(slot).top() : null;
    }

    /** Slottaki eşya sayısı; boş slot için 0. */
    public int getCount(int slot) {
        return slot >= 0 && slot < stacks.size() ? stacks.get(slot).items.size() : 0;
    }

    /** Eşyanın bulunduğu slot; çantada değilse -1. */
    public int slotOf(Item item) {
        for (int i = 0; i < stacks.size(); i++) {
            if (stacks.get(i).items.contains(item)) {
                return i;
            }
        }
        return -1;
    }

    /** Çantadaki bütün eşyalar, slot sırasıyla düzleştirilmiş halde. */
    public List<Item> getItems() {
        List<Item> all = new ArrayList<>();
        for (Stack stack : stacks) {
            all.addAll(stack.items);
        }
        return List.copyOf(all);
    }

    /** Dolu slot sayısı. */
    public int size() {
        return stacks.size();
    }

    /** Çantadaki toplam eşya sayısı; yığınlar dahil. */
    public int totalItems() {
        int total = 0;
        for (Stack stack : stacks) {
            total += stack.items.size();
        }
        return total;
    }

    public boolean isFull() {
        return stacks.size() >= CAPACITY;
    }

    public boolean isEmpty() {
        return stacks.isEmpty();
    }

    public void clear() {
        stacks.clear();
    }

    /** Eşyanın katılabileceği açık yığın; yoksa {@code null}. */
    private Stack stackFor(Item item) {
        if (!item.isStackable()) {
            return null;
        }

        String key = stackKey(item);
        for (Stack stack : stacks) {
            if (stack.key.equals(key)) {
                return stack;
            }
        }
        return null;
    }

    /**
     * İki eşyanın aynı yığına girip giremeyeceğini belirleyen anahtar.
     *
     * <p>Tür <em>ve</em> ad birlikte bakılıyor: bütün iksirler tek yığın olur
     * ama ileride "büyük iksir" gibi bir varyant eklenirse ayrı yığında
     * durur.</p>
     */
    private String stackKey(Item item) {
        return item.getKind() + "|" + item.getName();
    }
}
