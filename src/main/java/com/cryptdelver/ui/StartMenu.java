package com.cryptdelver.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Oyun açılınca karşılayan menü.
 *
 * <p>Önce pencere açılır açılmaz zindanın ortasına düşüyordun. Hem oyunun bir
 * başlangıcı yoktu hem de kayıtlı oyunu yüklemek için önce ölmeyi göze alıp
 * {@code F9}'a basman gerekiyordu. Menü bu ikisini de çözüyor.</p>
 *
 * <p>Menü yalnızca <em>seçimi</em> tutuyor; seçilen şeyin ne yaptığını
 * {@link GameScreen} biliyor. Bu ayrım sayesinde burası pencere, kayıt ya da
 * oyun durumu hakkında hiçbir şey bilmiyor ve tek başına sınanabiliyor.</p>
 */
public class StartMenu {

    /** Menüdeki bir satır. */
    public enum Option {
        NEW_GAME("Yeni Oyun"),
        CONTINUE("Kayitli Oyuna Devam Et"),
        QUIT("Cikis");

        private final String label;

        Option(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private final List<Option> options = new ArrayList<>();

    private int index;
    private boolean open = true;

    /**
     * @param saveExists kayıtlı oyun var mı; yoksa "devam et" satırı hiç
     *                   görünmüyor. Seçilemeyen soluk bir satır göstermektense
     *                   listeden çıkarmak daha az kafa karıştırıyor.
     */
    public StartMenu(boolean saveExists) {
        options.add(Option.NEW_GAME);
        if (saveExists) {
            options.add(Option.CONTINUE);
        }
        options.add(Option.QUIT);
    }

    public List<Option> getOptions() {
        return List.copyOf(options);
    }

    public int getIndex() {
        return index;
    }

    public Option getSelected() {
        return options.get(index);
    }

    /** Menü ekranda mı; kapandıktan sonra oyun akmaya başlıyor. */
    public boolean isOpen() {
        return open;
    }

    public void close() {
        open = false;
    }

    /** Seçim listenin başına ve sonuna sarıyor: son satırdan aşağı ilki. */
    public void moveDown() {
        index = (index + 1) % options.size();
    }

    public void moveUp() {
        index = (index - 1 + options.size()) % options.size();
    }
}
