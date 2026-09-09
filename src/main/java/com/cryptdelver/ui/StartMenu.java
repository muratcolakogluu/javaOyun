package com.cryptdelver.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Oyun açılınca karşılayan menü ve alt sayfaları.
 *
 * <p>Önce pencere açılır açılmaz zindanın ortasına düşüyordun. Hem oyunun bir
 * başlangıcı yoktu hem de kayıtlı oyunu yüklemek için önce ölmeyi göze alıp
 * {@code F9}'a basman gerekiyordu.</p>
 *
 * <p>Menü yalnızca <em>nerede olduğunu ve neyin seçili olduğunu</em> tutuyor.
 * Seçilen şeyin ne yaptığını {@link GameScreen}, nasıl göründüğünü
 * {@link GameRenderer} biliyor. Bu ayrım sayesinde burası pencere, kayıt ya da
 * oyun durumu hakkında hiçbir şey bilmiyor ve JavaFX açmadan
 * sınanabiliyor.</p>
 */
public class StartMenu {

    /** Menünün hangi sayfası açık. */
    public enum Pane { MAIN, SETTINGS, HELP }

    /** Ana sayfadaki satırlar. */
    public enum Option {
        NEW_GAME("Yeni Oyun"),
        CONTINUE("Kayitli Oyuna Devam Et"),
        SETTINGS("Ayarlar"),
        HELP("Nasil Oynanir"),
        QUIT("Cikis");

        private final String label;

        Option(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    /**
     * Ayarlar sayfasındaki satırlar.
     *
     * <p>{@code BACK} de bir satır: "geri dönmek için ESC" diye ayrı bir kural
     * ezberletmek yerine listede görünür bir çıkış duruyor. ESC de çalışıyor,
     * ama bilmen gerekmiyor.</p>
     */
    public enum SettingRow {
        VOLUME("Ses seviyesi"),
        MUTE("Sessiz"),
        DIFFICULTY("Zorluk"),
        AUTO_SAVE("Otomatik kaydetme"),
        BACK("Geri");

        private final String label;

        SettingRow(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private final List<Option> options = new ArrayList<>();

    private Pane pane = Pane.MAIN;
    private int mainIndex;
    private int settingIndex;
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
        options.add(Option.SETTINGS);
        options.add(Option.HELP);
        options.add(Option.QUIT);
    }

    public Pane getPane() {
        return pane;
    }

    public List<Option> getOptions() {
        return List.copyOf(options);
    }

    public List<SettingRow> getSettingRows() {
        return List.of(SettingRow.values());
    }

    /** Açık sayfadaki seçili satırın sırası; çizim bunu vurguluyor. */
    public int getIndex() {
        return pane == Pane.SETTINGS ? settingIndex : mainIndex;
    }

    public Option getSelected() {
        return options.get(mainIndex);
    }

    public SettingRow getSelectedSetting() {
        return SettingRow.values()[settingIndex];
    }

    /** Menü ekranda mı; kapandıktan sonra oyun akmaya başlıyor. */
    public boolean isOpen() {
        return open;
    }

    public void close() {
        open = false;
    }

    public void openPane(Pane target) {
        this.pane = target;
    }

    /**
     * Bir adım geri: alt sayfadan ana sayfaya, ana sayfadan hiçbir yere.
     *
     * <p>Ana sayfada ESC'nin oyunu kapatmaması bilinçli: yanlışlıkla basınca
     * pencerenin kapanması, kazanılabilecek en ucuz sinir bozukluğu olurdu.
     * Çıkış listede duruyor.</p>
     */
    public void back() {
        pane = Pane.MAIN;
    }

    /**
     * Verilen satırı seçili yapar; fare imlecin altındaki satıra geldiğinde
     * çağrılıyor.
     *
     * <p>Listede olmayan bir satır sessizce yok sayılıyor: kayıt yokken
     * "devam et" listede olmadığı için böyle bir istek gelebilir.</p>
     */
    public void select(Option option) {
        int index = options.indexOf(option);
        if (index >= 0) {
            mainIndex = index;
        }
    }

    public void selectSetting(SettingRow row) {
        settingIndex = row.ordinal();
    }

    /** Seçim listenin başına ve sonuna sarıyor: son satırdan aşağı ilki. */
    public void moveDown() {
        move(1);
    }

    public void moveUp() {
        move(-1);
    }

    private void move(int step) {
        if (pane == Pane.SETTINGS) {
            int count = SettingRow.values().length;
            settingIndex = (settingIndex + step + count) % count;
        } else {
            mainIndex = (mainIndex + step + options.size()) % options.size();
        }
    }
}
