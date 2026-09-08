package com.cryptdelver.game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Oyuncuya ne olduğunu anlatan kısa mesajların kaydı.
 *
 * <p>Sıra tabanlı bir oyunda geri bildirim yazıyla verilir: kimin kime kaç
 * hasar vurduğu ekranda görünmezse savaş anlaşılmaz olur. Son
 * {@value #CAPACITY} mesaj tutulur, eskisi düşer.</p>
 */
public class MessageLog {

    private static final int CAPACITY = 50;

    private final Deque<String> messages = new ArrayDeque<>();

    public void add(String message) {
        messages.addLast(message);
        if (messages.size() > CAPACITY) {
            messages.removeFirst();
        }
    }

    /** En son eklenen mesaj; kayıt boşsa boş metin. */
    public String last() {
        return messages.isEmpty() ? "" : messages.peekLast();
    }

    /** En yeniden eskiye doğru en fazla {@code count} mesaj. */
    public List<String> latest(int count) {
        List<String> all = new ArrayList<>(messages);
        List<String> result = new ArrayList<>();

        for (int i = all.size() - 1; i >= 0 && result.size() < count; i--) {
            result.add(all.get(i));
        }
        return result;
    }

    public void clear() {
        messages.clear();
    }
}
