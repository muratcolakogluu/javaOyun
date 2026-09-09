package com.cryptdelver.ui;

import com.cryptdelver.entity.Enchantment;
import com.cryptdelver.game.Game;
import com.cryptdelver.game.Records;
import com.cryptdelver.game.Settings;
import com.cryptdelver.persistence.RecordsFile;
import com.cryptdelver.persistence.SaveData;
import com.cryptdelver.persistence.SaveFile;
import com.cryptdelver.persistence.SettingsFile;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.transform.Scale;
import javafx.stage.Screen;

/**
 * Oyun ekranı: oyun döngüsünü çevirir, klavye girdisini yön komutuna
 * dönüştürür ve her karede yeniden çizer.
 *
 * <p>Girdiyi olay bazlı değil <em>durum bazlı</em> okuyoruz: basılı tuşlar bir
 * kümede tutulur ve her karede hangilerinin basılı olduğuna bakılır. Klavyenin
 * tuş tekrarı gecikmesi böylece devreye girmiyor.</p>
 *
 * <p>Yön tuşları ayrıca <em>basılma sırasıyla</em> tutuluyor: iki yön birden
 * basılıysa en son basılan kazanır, o bırakılınca diğerine geri dönülür. Izgara
 * hareketinde çapraz gidiş olmadığı için bu, köşe dönerken tuş sırasının
 * beklendiği gibi davranmasını sağlıyor.</p>
 */
public class GameScreen {

    /**
     * Bir karede işlenecek azami süre. Pencere sürüklenir ya da bilgisayar
     * takılırsa arada saniyeler geçebilir; sınırlamazsak varlıklar tek karede
     * birkaç kare birden atlar.
     */
    private static final double MAX_DELTA = 0.05;

    /**
     * Pencere çerçevesi ve başlık çubuğu için ayrılan pay.
     *
     * <p>Tuvalin yanına eklenen bu payı ölçemiyoruz — pencere daha
     * gösterilmediği için yüksekliği belli değil. Cömert bir tahmin
     * kullanıyoruz: fazladan bırakılan birkaç piksel görünmüyor, eksik
     * bırakılan pikseller ise ekranın dışında kalıyor.</p>
     */
    private static final double WINDOW_CHROME = 48;

    private final Game game;
    private final Canvas canvas;
    private final GameRenderer renderer = new GameRenderer();
    private final Group root;
    private final double scale;
    private final Set<KeyCode> pressedKeys = EnumSet.noneOf(KeyCode.class);
    private final Deque<KeyCode> heldDirections = new ArrayDeque<>();
    private final SaveFile saveFile = new SaveFile();
    private final SettingsFile settingsFile = new SettingsFile();
    private final StartMenu menu;
    private final Records records = new Records();
    private final RecordsFile recordsFile = new RecordsFile();

    private AnimationTimer loop;
    private long lastFrameNanos;
    private boolean runRecorded;

    public GameScreen(Game game) {
        this.game = game;

        double width = game.getDungeon().getWidth() * (double) GameRenderer.TILE_SIZE;
        double height = game.getDungeon().getHeight() * (double) GameRenderer.TILE_SIZE
                + GameRenderer.HUD_HEIGHT;

        this.canvas = new Canvas(width, height);
        this.scale = fitToScreen(width, height);

        // Tuval sabit boyutta kalıyor, onu saran düğüm küçülüyor.
        Group scaledCanvas = new Group(canvas);
        scaledCanvas.getTransforms().add(new Scale(scale, scale));

        // Kök düğüm Group: hiçbir yerleşim yapmıyor, tuvali (0, 0)'da bırakıyor.
        // Önce StackPane vardı ve ortalıyordu; ölçeklenmiş düğümün yerleşim
        // boyutu ölçeklenmemiş hâli olduğu için tuvali eksi koordinata itip
        // sol ve üst kenarı ekranın dışında bırakıyordu — bilgi şeridinin sol
        // sütunu kırpılıyor, boss can çubuğu da hiç görünmüyordu.
        this.root = new Group(scaledCanvas);

        // "Devam et" satırı yalnızca gerçekten kayıt varsa görünsün.
        this.menu = new StartMenu(saveFile.exists());
        recordsFile.load(records);
    }

    /**
     * Pencerenin ekrana sığması için gereken küçültme oranı.
     *
     * <p>Oyun sabit boyutlu bir tuvale çiziliyor: 40x22 kare, artı bilgi
     * şeridi. Ekran ölçeklemesi %150 olan bir dizüstünde JavaFX'in mantıksal
     * alanı 1280x800'e iniyor, pencere çerçevesi de eklenince tuvalin altı
     * ekranın dışında kalıyordu — bilgi şeridinin son satırı görünmüyordu.</p>
     *
     * <p>Çözüm kat boyutunu küçültmek değil: o hem oynanışı değiştirirdi hem
     * de kayıtları makineye bağlardı. Bunun yerine tüm sahne aynı oranda
     * ölçekleniyor. Oyun koordinatları değişmiyor, çizim kodu bundan
     * habersiz. Ekran yeterince büyükse oran 1 kalıyor ve hiçbir şey olmuyor.</p>
     */
    private static double fitToScreen(double width, double height) {
        Rectangle2D visual = Screen.getPrimary().getVisualBounds();
        double usableHeight = visual.getHeight() - WINDOW_CHROME;

        return Math.min(1, Math.min(visual.getWidth() / width, usableHeight / height));
    }

    public Parent getRoot() {
        return root;
    }

    /** Pencerenin isteyeceği genişlik: tuvalin ölçeklenmiş hâli. */
    public double getWidth() {
        return canvas.getWidth() * scale;
    }

    public double getHeight() {
        return canvas.getHeight() * scale;
    }

    /**
     * Klavye ve fare dinleyicilerini bağlar.
     *
     * <p>Klavye sahneye, fare <em>tuvale</em> bağlanıyor. Sebebi koordinatlar:
     * pencere ekrana sığsın diye ölçeklenmiş olabiliyor ve tuvale gelen fare
     * olayları tuvalin kendi koordinatlarında geliyor — dönüşümü JavaFX
     * yapıyor, bizim ölçek çarpanıyla uğraşmamız gerekmiyor.</p>
     */
    public void attachInput(Scene scene) {
        scene.setOnKeyPressed(this::onKeyPressed);
        scene.setOnKeyReleased(this::onKeyReleased);

        canvas.setOnMouseMoved(this::onMouseMoved);
        canvas.setOnMouseExited(event -> renderer.getClicks().clearMouse());
        canvas.setOnMousePressed(this::onMousePressed);
    }

    /**
     * Fare hareketi: imleç nerede, menüde seçili satır da orada.
     *
     * <p>Menüde imleci taşımak, klavye ve fareyi tek bir seçim üstünde
     * buluşturuyor. İkisi ayrı olsaydı ekranda iki vurgulu satır dururdu ve
     * Enter'ın hangisini seçeceği belirsiz olurdu.</p>
     */
    private void onMouseMoved(MouseEvent event) {
        renderer.getClicks().setMouse(event.getX(), event.getY());

        if (!menu.isOpen()) {
            return;
        }

        UiAction hovered = renderer.getClicks().hovered();
        if (hovered instanceof UiAction.Menu(StartMenu.Option option)) {
            menu.select(option);
        } else if (hovered instanceof UiAction.Setting(StartMenu.SettingRow row)) {
            menu.selectSetting(row);
        }
    }

    /** Tıklama: farenin altındaki eylemi çalıştırır. */
    private void onMousePressed(MouseEvent event) {
        renderer.getClicks().setMouse(event.getX(), event.getY());

        UiAction action = renderer.getClicks().hit(event.getX(), event.getY());
        if (action != null && isReachable(action)) {
            perform(action, event.isShiftDown());
        }
    }

    /**
     * O an açık olan ekranın gerçekten sahip olduğu bir eylem mi.
     *
     * <p>Çanta slotları haritayla birlikte her karede çiziliyor, dolayısıyla
     * menü ya da tezgâh perdesinin <em>altında</em> kalıyorlar. Bu kontrol
     * olmasaydı menüdeyken perdenin ardındaki bir slota tıklamak eşya
     * kullanırdı — görünmeyen bir şeye basmış olurdun.</p>
     */
    private boolean isReachable(UiAction action) {
        if (menu.isOpen()) {
            return action instanceof UiAction.Menu || action instanceof UiAction.Setting;
        }
        if (game.isForgeOpen()) {
            return action instanceof UiAction.Forge || action instanceof UiAction.Enchant;
        }
        return action instanceof UiAction.Slot && !game.isFrozen();
    }

    /**
     * Bir arayüz eylemini oyuna uygular.
     *
     * <p>Mühürlü arayüz sayesinde bu {@code switch} bütün durumları kapsamak
     * zorunda: yeni bir tıklanabilir öğe eklendiğinde derleyici burayı
     * gösteriyor.</p>
     */
    private void perform(UiAction action, boolean shiftDown) {
        switch (action) {
            case UiAction.Menu(StartMenu.Option option) -> {
                menu.select(option);
                chooseFromMenu();
            }
            case UiAction.Setting(StartMenu.SettingRow row) -> {
                menu.selectSetting(row);
                if (row == StartMenu.SettingRow.BACK) {
                    menu.back();
                } else {
                    adjustSetting(1);
                }
            }
            case UiAction.Forge(UiAction.Bench bench) -> performBench(bench);
            case UiAction.Enchant(boolean onWeapon, var enchantment) -> {
                if (onWeapon) {
                    game.enchantWeapon(enchantment);
                } else {
                    game.enchantArmor(enchantment);
                }
            }
            case UiAction.Slot(int index) -> {
                // Çantada tıklama kullanıyor, Shift ile yere bırakıyor; tuş
                // takımındaki 1-8 ve Shift+1-8 ile aynı kural.
                if (shiftDown) {
                    game.dropItem(index);
                } else {
                    game.useItem(index);
                }
            }
        }
    }

    private void performBench(UiAction.Bench bench) {
        switch (bench) {
            case REPAIR_WEAPON -> game.repairWeapon();
            case REPAIR_ARMOR -> game.repairArmor();
            case UPGRADE_WEAPON -> game.upgradeWeapon();
            case UPGRADE_ARMOR -> game.upgradeArmor();
        }
    }

    /**
     * Kayıtlı ayarları yükleyip ses çalıcıyı takar; pencere açıldığında
     * çağrılıyor.
     */
    public void enableSound() {
        settingsFile.load(game.getSettings());
        game.setSoundListener(new SoundPlayer(game.getSettings()));
    }

    /** Oyun döngüsünü başlatır. */
    public void start() {
        loop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastFrameNanos == 0) {
                    lastFrameNanos = now;
                    return;
                }

                double delta = Math.min((now - lastFrameNanos) / 1_000_000_000.0, MAX_DELTA);
                lastFrameNanos = now;

                // Menü açıkken dünya donuyor ama çizim sürüyor: arkada duran
                // zindan menünün fonu oluyor ve animasyonlar akmaya devam
                // ediyor.
                if (!menu.isOpen()) {
                    applyInput();
                    game.update(delta);
                    noteFinishedRun();
                }
                render();
            }
        };
        loop.start();
    }

    public void stop() {
        if (loop != null) {
            loop.stop();
        }
    }

    /**
     * Koşu yeni bittiyse rekorlara işler.
     *
     * <p>Bitişi oyun bildirmiyor, ekran <em>fark ediyor</em>: {@code isOver} ya
     * da {@code isWon} ilk kez doğru olduğunda bir kez sayılıyor. Bayrak
     * olmasaydı her karede yeniden kaydedilirdi — koşu sayısı saniyede altmış
     * artardı.</p>
     *
     * <p>Rekorları oyunun kendisine koymadım: {@code Game} bir koşu, rekorlar
     * ise koşular <em>arası</em> bir şey. Oyun onları bilmek zorunda değil.</p>
     */
    private void noteFinishedRun() {
        if (runRecorded || (!game.isOver() && !game.isWon())) {
            return;
        }

        runRecorded = true;
        records.recordRun(game.getDepth(), game.getGold(), game.isWon());
        recordsFile.save(records);
    }

    /** Ekranı oyunun güncel durumuna göre çizer. */
    public void render() {
        renderer.render(canvas.getGraphicsContext2D(), game, menu, records);
    }

    /** Basılı yön tuşunu oyuncunun yönüne, boşluğu saldırı isteğine çevirir. */
    private void applyInput() {
        // Dünya durmuşken basılı tuşları okumuyoruz; yoksa duraklatma ya da
        // büyücü ekranı kapanır kapanmaz birikmiş bir saldırı boşalıyordu.
        if (game.isFrozen()) {
            return;
        }

        KeyCode direction = heldDirections.peekLast();

        int dx = 0;
        int dy = 0;
        if (direction != null) {
            switch (direction) {
                case W, UP -> dy = -1;
                case S, DOWN -> dy = 1;
                case A, LEFT -> dx = -1;
                case D, RIGHT -> dx = 1;
                default -> {
                    // Yön tuşu değil; kuyruğa zaten girmiyor.
                }
            }
        }

        game.getPlayer().setMoveInput(dx, dy);

        if (pressedKeys.contains(KeyCode.SPACE)) {
            game.getPlayer().requestAttack();
        }
    }

    private void onKeyPressed(KeyEvent event) {
        KeyCode code = event.getCode();
        pressedKeys.add(code);

        if (isDirection(code) && !heldDirections.contains(code)) {
            heldDirections.addLast(code);
        }

        // Menü açıkken başka hiçbir tuş işlemiyor.
        if (menu.isOpen()) {
            handleMenuCommand(code);
            return;
        }

        // Duraklatmayı açıp kapatmak, ayarlar, kaydetmek ve yüklemek her zaman
        // serbest; oynanışa dokunan komutlar duraklatmada geçersiz.
        switch (code) {
            case ESCAPE -> game.togglePause();
            // Tezgâh açıkken F bir büyü tuşu; tezgâhtan ESC ile çıkılıyor.
            case F -> {
                if (game.isForgeOpen()) {
                    handleForgeCommand(code);
                } else {
                    game.toggleForge();
                }
            }
            case F5 -> saveGame();
            case F9 -> loadGame();
            case MINUS, SUBTRACT -> changeVolume(-Settings.VOLUME_STEP);
            case PLUS, ADD, EQUALS -> changeVolume(Settings.VOLUME_STEP);
            case M -> toggleMute();
            default -> handlePlayCommand(code, event);
        }
    }

    /**
     * Başlangıç menüsünün tuşları.
     *
     * <p>Ok tuşları ya da W/S ile geziniyor, Enter ya da boşlukla
     * seçiliyor. Rakam tuşu vermedim: menüde satır sayısı kayda göre
     * değişiyor, sabit rakam ezberletmek yanlış olurdu.</p>
     */
    private void handleMenuCommand(KeyCode code) {
        switch (code) {
            case UP, W -> menu.moveUp();
            case DOWN, S -> menu.moveDown();
            case LEFT, A -> adjustSetting(-1);
            case RIGHT, D -> adjustSetting(1);
            case ENTER, SPACE -> chooseFromMenu();
            case ESCAPE -> menu.back();
            default -> {
                // Menüde başka tuşun işi yok.
            }
        }
    }

    private void chooseFromMenu() {
        if (menu.getPane() != StartMenu.Pane.MAIN) {
            // Alt sayfalarda Enter yalnızca "Geri" satırında bir şey yapıyor;
            // değerler sağ/sol ile değişiyor.
            if (menu.getPane() != StartMenu.Pane.SETTINGS
                    || menu.getSelectedSetting() == StartMenu.SettingRow.BACK) {
                menu.back();
            } else {
                adjustSetting(1);
            }
            return;
        }

        switch (menu.getSelected()) {
            case NEW_GAME -> startPlaying();
            case CONTINUE -> {
                loadGame();
                startPlaying();
            }
            case SETTINGS -> menu.openPane(StartMenu.Pane.SETTINGS);
            case HELP -> menu.openPane(StartMenu.Pane.HELP);
            case QUIT -> {
                stop();
                Platform.exit();
            }
        }
    }

    private void startPlaying() {
        menu.close();

        // Menüde basılı kalan tuşlar oyuna sarkmasın.
        pressedKeys.clear();
        heldDirections.clear();
    }

    /**
     * Ayarlar sayfasında sağ/sol.
     *
     * <p>Her değişiklik anında diske yazılıyor. Dosya dört satır; "ayarları
     * kaydet" diye ayrı bir adım istemek, kazandırdığından çok götürürdü.</p>
     */
    private void adjustSetting(int step) {
        if (menu.getPane() != StartMenu.Pane.SETTINGS || step == 0) {
            return;
        }

        Settings settings = game.getSettings();
        switch (menu.getSelectedSetting()) {
            case VOLUME -> settings.adjustVolume(step * Settings.VOLUME_STEP);
            case MUTE -> settings.toggleMuted();
            case DIFFICULTY -> settings.setDifficulty(step > 0
                    ? settings.getDifficulty().next()
                    : settings.getDifficulty().previous());
            case AUTO_SAVE -> settings.toggleAutoSave();
            case BACK -> {
                return;
            }
        }

        settingsFile.save(settings);
    }

    /**
     * Ses seviyesini değiştirir ve tercihi diske yazar.
     *
     * <p>Her değişiklikte kaydediyoruz: dosya iki satır, ama oyuncunun ayarı
     * bir sonraki açılışta yerinde duruyor. "Ayarları kaydet" diye ayrı bir
     * adım istememek daha doğru.</p>
     */
    private void changeVolume(double delta) {
        game.getSettings().adjustVolume(delta);
        settingsFile.save(game.getSettings());
        game.getMessageLog().add("Ses: %" + game.getSettings().getVolumePercent());
    }

    private void toggleMute() {
        game.getSettings().toggleMuted();
        settingsFile.save(game.getSettings());
        game.getMessageLog().add(game.getSettings().isMuted() ? "Ses kapatıldı." : "Ses açıldı.");
    }

    /** Yalnızca oyun akarken işleyen tek seferlik komutlar. */
    private void handlePlayCommand(KeyCode code, KeyEvent event) {
        // Büyücü ekranı açıkken rakamlar çantayı değil tezgâhı yönetiyor.
        if (game.isForgeOpen()) {
            handleForgeCommand(code);
            return;
        }

        if (game.isPaused()) {
            return;
        }

        switch (code) {
            case E -> descend();
            case ENTER -> {
                // Ölünce de kazanınca da aynı tuş yeniden başlatıyor: iki
                // perde de aynı yerde aynı şeyi yazıyor.
                if (game.isOver() || game.isWon()) {
                    game.restart();
                    runRecorded = false;
                }
            }
            // Shift basılıysa eşya kullanılmaz, yere bırakılır.
            case DIGIT1, DIGIT2, DIGIT3, DIGIT4, DIGIT5, DIGIT6, DIGIT7, DIGIT8 -> {
                if (event.isShiftDown()) {
                    game.dropItem(slotOf(code));
                } else {
                    game.useItem(slotOf(code));
                }
            }
            default -> {
                // Diğer tuşlar yalnızca basılı tuşlar kümesini ilgilendiriyor.
            }
        }
    }

    /**
     * Bir alt kata iner; ayar açıksa iniş sonrası kendiliğinden kaydeder.
     *
     * <p>Kaydetme anı olarak kat inişi seçildi: kat sınırı oyunun doğal
     * kontrol noktası, hem oyuncunun kafasında hem kayıt biçiminde. Her
     * saniye kaydetmek diski yorar, ölümde kaydetmek de anlamsız olurdu.</p>
     */
    private void descend() {
        if (game.descend() && game.getSettings().isAutoSave()) {
            saveGame();
        }
    }

    /**
     * Oyunu diske yazar.
     *
     * <p>Dosya işlemleri patlarsa oyun düşmesin: hata mesaj kaydına yazılıyor,
     * oyuncu ekranda görüyor ve oynamaya devam ediyor.</p>
     */
    private void saveGame() {
        try {
            saveFile.write(game.captureSave());
            game.getMessageLog().add("Oyun kaydedildi.");
        } catch (IOException e) {
            game.getMessageLog().add("Kaydedilemedi: " + e.getMessage());
        }
    }

    /** Diskteki kaydı yükler; kayıt yoksa uyarır. */
    private void loadGame() {
        try {
            Optional<SaveData> data = saveFile.read();
            if (data.isEmpty()) {
                game.getMessageLog().add("Kayıtlı oyun yok.");
                return;
            }
            game.applySave(data.get());
        } catch (IOException | RuntimeException e) {
            game.getMessageLog().add("Kayıt yüklenemedi: " + e.getMessage());
        }
    }

    /**
     * Büyücü tezgâhının tuşları.
     *
     * <p>Çanta ile aynı rakamları kullanıyor ama karışmıyor: tezgâh açıkken
     * çanta komutları hiç çalışmıyor, kapalıyken de tezgâh komutları. Ekranda
     * hangi rakamın ne yaptığı yazılı olduğu için ayrı tuş takımı ezberletmeye
     * gerek yok.</p>
     */
    private void handleForgeCommand(KeyCode code) {
        switch (code) {
            case DIGIT1 -> game.repairWeapon();
            case DIGIT2 -> game.repairArmor();
            case DIGIT3 -> game.upgradeWeapon();
            case DIGIT4 -> game.upgradeArmor();

            // Üst sıra kılıcın, ana sıra zırhın; ekrandaki düzenle aynı.
            case Q -> game.enchantWeapon(Enchantment.VAMPIRLIK);
            case W -> game.enchantWeapon(Enchantment.YILDIRIM);
            case E -> game.enchantWeapon(Enchantment.ACELE);
            case R -> game.enchantWeapon(Enchantment.SAGLAMLIK);

            case A -> game.enchantArmor(Enchantment.DIKEN);
            case S -> game.enchantArmor(Enchantment.YENILENME);
            case D -> game.enchantArmor(Enchantment.CEVIKLIK);
            case F -> game.enchantArmor(Enchantment.SAGLAMLIK);

            default -> {
                // Diğer tuşlar tezgâhta bir şey yapmıyor.
            }
        }
    }

    /** 1-8 tuşlarını çanta slot numarasına (0-7) çevirir. */
    private static int slotOf(KeyCode digit) {
        return switch (digit) {
            case DIGIT1 -> 0;
            case DIGIT2 -> 1;
            case DIGIT3 -> 2;
            case DIGIT4 -> 3;
            case DIGIT5 -> 4;
            case DIGIT6 -> 5;
            case DIGIT7 -> 6;
            case DIGIT8 -> 7;
            default -> -1;
        };
    }

    private void onKeyReleased(KeyEvent event) {
        pressedKeys.remove(event.getCode());
        heldDirections.remove(event.getCode());
    }

    private static boolean isDirection(KeyCode code) {
        return switch (code) {
            case W, A, S, D, UP, DOWN, LEFT, RIGHT -> true;
            default -> false;
        };
    }
}
