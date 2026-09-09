package com.cryptdelver.game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Oyuncuya ne olduğunu anlatan kısa mesajların kaydı.
 *
 * <p>Gerçek zamanlı bir oyunda geri bildirim yazıyla veriliyor: kimin kime kaç
 * hasar vurduğu ekranda görünmezse savaş anlaşılmaz oluyor. Son
 * {@value #CAPACITY} mesaj tutuluyor, eskisi düşüyor.</p>
 *
 * <h2>Dövüş gürültüsü</h2>
 * <p>Ekranda üç satır var ve dövüşte "İmp 3 hasar aldı" akıyor. <em>Kılıcın
 * kırıldı</em>, <em>zindan seni fark etti</em> gibi önemli olaylar bir saniyede
 * kayboluyordu; bunu bir testte bizzat gördük, uyarı mesajı son on satırdan
 * taşmıştı.</p>
 *
 * <p>İki şey yapılıyor. Arka arkaya gelen <b>aynı mesaj katlanıyor</b>
 * ("İmp 3 hasar aldı ×4"): kalabalıkta beş düşmana vurmak beş satır değil bir
 * satır. Ve mesajlar <b>önemli/sıradan</b> diye ayrılıyor; ekran önemlileri
 * farklı renkte gösteriyor.</p>
 */
public class MessageLog {

    private static final int CAPACITY = 50;

    /** Tek bir kayıt: metni, kaç kez tekrarlandığı ve önemli olup olmadığı. */
    public static final class Entry {

        private final String text;
        private final boolean important;
        private int count = 1;

        private Entry(String text, boolean important) {
            this.text = text;
            this.important = important;
        }

        /** Ham metin, tekrar sayısı olmadan. */
        public String getText() {
            return text;
        }

        public boolean isImportant() {
            return important;
        }

        public int getCount() {
            return count;
        }

        /** Ekranda görünen hâli: tekrarlandıysa sonunda çarpanı var. */
        public String getDisplay() {
            return count > 1 ? text + " x" + count : text;
        }
    }

    private final Deque<Entry> entries = new ArrayDeque<>();

    /** Sıradan bir olay: dövüş satırları, eşya toplama. */
    public void add(String message) {
        append(message, false);
    }

    /**
     * Kaçırılmaması gereken bir olay: kırılan parça, uyanan zindan, ölüm.
     *
     * <p>Önemli mesajlar da katlanıyor ama gürültünün arasında rengiyle
     * ayrılıyor.</p>
     */
    public void addImportant(String message) {
        append(message, true);
    }

    private void append(String message, boolean important) {
        Entry last = entries.peekLast();

        // Yalnızca arka arkaya gelen aynı mesaj katlanıyor: araya başka bir
        // olay girdiyse ikisi ayrı olay, birleştirmek yanıltıcı olurdu.
        if (last != null && last.text.equals(message) && last.important == important) {
            last.count++;
            return;
        }

        entries.addLast(new Entry(message, important));
        if (entries.size() > CAPACITY) {
            entries.removeFirst();
        }
    }

    /** En son eklenen mesaj; kayıt boşsa boş metin. */
    public String last() {
        Entry entry = entries.peekLast();
        return entry == null ? "" : entry.getDisplay();
    }

    /** En yeniden eskiye doğru en fazla {@code count} mesaj, gösterim hâliyle. */
    public List<String> latest(int count) {
        List<String> result = new ArrayList<>();
        for (Entry entry : latestEntries(count)) {
            result.add(entry.getDisplay());
        }
        return result;
    }

    /** Aynı liste, ama rengini seçebilmesi için ekrana kayıtların kendisi. */
    public List<Entry> latestEntries(int count) {
        List<Entry> all = new ArrayList<>(entries);
        List<Entry> result = new ArrayList<>();

        for (int i = all.size() - 1; i >= 0 && result.size() < count; i--) {
            result.add(all.get(i));
        }
        return result;
    }

    public void clear() {
        entries.clear();
    }
}
