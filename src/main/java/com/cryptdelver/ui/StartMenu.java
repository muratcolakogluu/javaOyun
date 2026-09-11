package com.cryptdelver.ui;

import com.cryptdelver.game.StartPath;
import com.cryptdelver.game.Text;
import java.util.ArrayList;
import java.util.List;

/**
 * Oyun açılınca karşılayan menü ve alt sayfaları.
 *
 * <p>Önce pencere açılır açılmaz zindanın ortasına düşüyordun: oyunun bir
 * başlangıcı yoktu, ayarlara da ancak oyunun içinden ulaşılıyordu.</p>
 *
 * <p>Menü yalnızca <em>nerede olduğunu ve neyin seçili olduğunu</em> tutuyor.
 * Seçilen şeyin ne yaptığını {@link GameScreen}, nasıl göründüğünü
 * {@link GameRenderer} biliyor. Bu ayrım sayesinde burası pencere ya da oyun
 * durumu hakkında hiçbir şey bilmiyor ve JavaFX açmadan sınanabiliyor.</p>
 */
public class StartMenu {

    /** Menünün hangi sayfası açık. */
    public enum Pane { MAIN, PATHS, SETTINGS, HELP }

    /** Ana sayfadaki satırlar. */
    public enum Option {
        NEW_GAME(Text.MENU_NEW_GAME),
        SETTINGS(Text.MENU_SETTINGS),
        HELP(Text.MENU_HELP),
        QUIT(Text.MENU_QUIT);

        private final Text label;

        Option(Text label) {
            this.label = label;
        }

        public String getLabel() {
            return label.get();
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
        LANGUAGE(Text.SETTING_LANGUAGE),
        VOLUME(Text.SETTING_EFFECTS),
        MUSIC(Text.SETTING_MUSIC),
        MUTE(Text.SETTING_MUTE),
        DIFFICULTY(Text.SETTING_DIFFICULTY),
        BACK(Text.SETTING_BACK);

        private final Text label;

        SettingRow(Text label) {
            this.label = label;
        }

        public String getLabel() {
            return label.get();
        }
    }

    private final List<Option> options = new ArrayList<>();

    private Pane pane = Pane.MAIN;
    private int mainIndex;
    private int settingIndex;
    private int pathIndex;
    private boolean open = true;

    public StartMenu() {
        options.add(Option.NEW_GAME);
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

    /**
     * Başlangıç yolları; koşuya nasıl indiğini seçtiğin sayfa.
     *
     * <p>Sıra {@link StartPath}'in kendi sırası: hangi yolun listenin başında
     * duracağına oyunun kuralları karar veriyor, menü değil.</p>
     */
    public List<StartPath> getPaths() {
        return List.of(StartPath.values());
    }

    public StartPath getSelectedPath() {
        return StartPath.values()[pathIndex];
    }

    public void selectPath(StartPath path) {
        pathIndex = path.ordinal();
    }

    /** Açık sayfadaki seçili satırın sırası; çizim bunu vurguluyor. */
    public int getIndex() {
        return switch (pane) {
            case SETTINGS -> settingIndex;
            case PATHS -> pathIndex;
            case MAIN, HELP -> mainIndex;
        };
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
     * <p>Listede olmayan bir satır sessizce yok sayılıyor: eksik bir satır
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
        switch (pane) {
            case SETTINGS -> {
                int count = SettingRow.values().length;
                settingIndex = (settingIndex + step + count) % count;
            }
            case PATHS -> {
                int count = StartPath.values().length;
                pathIndex = (pathIndex + step + count) % count;
            }
            case MAIN, HELP ->
                    mainIndex = (mainIndex + step + options.size()) % options.size();
        }
    }
}
