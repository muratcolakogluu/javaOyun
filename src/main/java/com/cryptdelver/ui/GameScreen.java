package com.cryptdelver.ui;

import com.cryptdelver.entity.Enchantment;
import com.cryptdelver.game.Game;
import com.cryptdelver.game.Settings;
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
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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

    private AnimationTimer loop;
    private long lastFrameNanos;

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

    /** Klavye dinleyicilerini sahneye bağlar. */
    public void attachInput(Scene scene) {
        scene.setOnKeyPressed(this::onKeyPressed);
        scene.setOnKeyReleased(this::onKeyReleased);
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

                applyInput();
                game.update(delta);
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

    /** Ekranı oyunun güncel durumuna göre çizer. */
    public void render() {
        renderer.render(canvas.getGraphicsContext2D(), game);
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

        // Duraklatmayı açıp kapatmak, ayarlar, kaydetmek ve yüklemek her zaman
        // serbest; oynanışa dokunan komutlar duraklatmada geçersiz.
        switch (code) {
            case ESCAPE -> game.togglePause();
            case F -> game.toggleForge();
            case F5 -> saveGame();
            case F9 -> loadGame();
            case MINUS, SUBTRACT -> changeVolume(-Settings.VOLUME_STEP);
            case PLUS, ADD, EQUALS -> changeVolume(Settings.VOLUME_STEP);
            case M -> toggleMute();
            default -> handlePlayCommand(code, event);
        }
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
            case E -> game.descend();
            case R -> game.regenerateFloor();
            case G -> game.cycleGenerator();
            case ENTER -> {
                if (game.isOver()) {
                    game.restart();
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
            case DIGIT5 -> game.enchantWeapon(Enchantment.VAMPIRLIK);
            case DIGIT6 -> game.enchantWeapon(Enchantment.SAGLAMLIK);
            case DIGIT7 -> game.enchantArmor(Enchantment.DIKEN);
            case DIGIT8 -> game.enchantArmor(Enchantment.SAGLAMLIK);
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
