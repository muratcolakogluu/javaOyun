package com.cryptdelver.game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Oyuncuya ne olduğunu anlatan kısa mesajların kaydı.
 *
 * <p>Gerçek zamanlı bir oyunda geri bildirim yazıyla veriliyor: kimin kime kaç
 * hasar vurduğu ekranda görünmezse savaş anlaşılmaz oluyor.</p>
 *
 * <h2>Neden kanallara ayrıldı</h2>
 * <p>Bütün mesajlar tek bir listede akarken üç satırlık pencerede hepsi
 * birbirini kovalıyordu: iksiri içtiğini görmek için dövüş satırlarının
 * arasından okumak, altını aldığını anlamak için gözle satır taramak
 * gerekiyordu. Oysa bunlar <em>farklı sorulara</em> cevap veriyor — "dövüş
 * nasıl gidiyor", "elime ne geçti", "üstümde ne var".</p>
 *
 * <p>Bu yüzden üç ayrı kanal var ve her biri ekranda kendi sütununda duruyor.
 * Her kanalın kendi tarihçesi olduğu için dövüş gürültüsü artık ganimet
 * satırını dışarı itemiyor: eskiden kalabalık bir kavga "Altın topladın"ı
 * ekrandan silerdi.</p>
 *
 * <h2>Katlama ve önem</h2>
 * <p>Arka arkaya gelen <b>aynı mesaj katlanıyor</b> ("İmp 3 hasar aldı ×4"):
 * kalabalıkta beş düşmana vurmak beş satır değil bir satır. Katlama kanal
 * içinde bakılıyor — araya giren bir ganimet satırı dövüş sayacını
 * bölmüyor, çünkü ikisi zaten ayrı sütunlarda.</p>
 *
 * <p>Mesajlar ayrıca <b>önemli/sıradan</b> diye ayrılıyor; ekran önemlileri
 * farklı renkte gösteriyor.</p>
 */
public class MessageLog {

    /** Her kanalda tutulan mesaj sayısı. */
    private static final int CAPACITY = 24;

    /**
     * Bir mesajın hangi soruya cevap verdiği.
     *
     * <p>Kanalın adı ekranda hangi sütuna düşeceğini de belirliyor; başka bir
     * yerde eşleme tablosu yok.</p>
     */
    public enum Channel {

        /** Dövüş: verilen ve alınan hasar, ölen düşmanlar, büyülerin vuruşa etkisi. */
        COMBAT,

        /** Eşya: yerden alınan, bırakılan, kuşanılan parçalar ve altın. */
        ITEM,

        /** Üstündeki durum: iksirler, büyücü işleri, kat değişimi, uyarılar. */
        STATUS
    }

    /** Tek bir kayıt: metni, kanalı, kaç kez tekrarlandığı ve önemli olup olmadığı. */
    public static final class Entry {

        private final String text;
        private final Channel channel;
        private final boolean important;

        /**
         * Kaçıncı sırada eklendiği.
         *
         * <p>Kanallar ayrı listelerde durduğu için "hepsi birlikte, en yeniden
         * eskiye" sıralaması ancak ortak bir sayaçla kurulabiliyor.</p>
         */
        private final long sequence;

        private int count = 1;

        private Entry(String text, Channel channel, boolean important, long sequence) {
            this.text = text;
            this.channel = channel;
            this.important = important;
            this.sequence = sequence;
        }

        /** Ham metin, tekrar sayısı olmadan. */
        public String getText() {
            return text;
        }

        public Channel getChannel() {
            return channel;
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

    private final Map<Channel, Deque<Entry>> channels = new EnumMap<>(Channel.class);
    private long nextSequence;

    public MessageLog() {
        for (Channel channel : Channel.values()) {
            channels.put(channel, new ArrayDeque<>());
        }
    }

    /**
     * Genel bir olay: iksir, büyücü işi, kat değişimi, kayıt.
     *
     * <p>Kanalı belirtmeyen çağrılar buraya düşüyor. Varsayılanın "durum"
     * olması bilinçli: sınıflandırılmamış bir mesajın dövüş ya da ganimet
     * sütununa düşüp oradaki takibi bozması, sağdaki genel sütunda görünmesinden
     * daha kötü.</p>
     */
    public void add(String message) {
        append(message, Channel.STATUS, false);
    }

    /**
     * Kaçırılmaması gereken bir olay: kırılan parça, uyanan zindan, ölüm.
     *
     * <p>Önemli mesajlar da katlanıyor ama gürültünün arasında rengiyle
     * ayrılıyor.</p>
     */
    public void addImportant(String message) {
        append(message, Channel.STATUS, true);
    }

    /** Dövüş satırı: hasar, ölüm, ıskalama. */
    public void combat(String message) {
        append(message, Channel.COMBAT, false);
    }

    /** Eşya satırı: toplanan, bırakılan, kuşanılan parça ve altın. */
    public void item(String message) {
        append(message, Channel.ITEM, false);
    }

    /** Eşya kanalında kaçırılmaması gereken bir satır. */
    public void importantItem(String message) {
        append(message, Channel.ITEM, true);
    }

    private void append(String message, Channel channel, boolean important) {
        Deque<Entry> entries = channels.get(channel);
        Entry last = entries.peekLast();

        // Yalnızca aynı kanalda arka arkaya gelen aynı mesaj katlanıyor: başka
        // bir kanalda olan bir şey bu sütunda görünmediği için sayacı bölmesi
        // için de bir neden yok.
        if (last != null && last.text.equals(message) && last.important == important) {
            last.count++;
            return;
        }

        entries.addLast(new Entry(message, channel, important, nextSequence++));
        if (entries.size() > CAPACITY) {
            entries.removeFirst();
        }
    }

    /** En son eklenen mesaj, kanal farkı gözetmeden; kayıt boşsa boş metin. */
    public String last() {
        List<Entry> newest = latestEntries(1);
        return newest.isEmpty() ? "" : newest.get(0).getDisplay();
    }

    /** Bütün kanallardan, en yeniden eskiye en fazla {@code count} mesaj. */
    public List<String> latest(int count) {
        List<String> result = new ArrayList<>();
        for (Entry entry : latestEntries(count)) {
            result.add(entry.getDisplay());
        }
        return result;
    }

    /** Aynı liste, ama rengini seçebilmesi için ekrana kayıtların kendisi. */
    public List<Entry> latestEntries(int count) {
        List<Entry> all = new ArrayList<>();
        for (Deque<Entry> entries : channels.values()) {
            all.addAll(entries);
        }

        all.sort(Comparator.comparingLong((Entry entry) -> entry.sequence).reversed());
        return all.subList(0, Math.min(count, all.size()));
    }

    /** Tek bir kanaldan, en yeniden eskiye en fazla {@code count} mesaj. */
    public List<Entry> latestEntries(Channel channel, int count) {
        List<Entry> all = new ArrayList<>(channels.get(channel));
        List<Entry> result = new ArrayList<>();

        for (int i = all.size() - 1; i >= 0 && result.size() < count; i--) {
            result.add(all.get(i));
        }
        return result;
    }

    public void clear() {
        for (Deque<Entry> entries : channels.values()) {
            entries.clear();
        }
    }
}
