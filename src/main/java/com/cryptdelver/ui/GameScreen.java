package com.cryptdelver.ui;

import com.cryptdelver.game.Game;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.Set;
import javafx.animation.AnimationTimer;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;

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

    private final Game game;
    private final Canvas canvas;
    private final GameRenderer renderer = new GameRenderer();
    private final StackPane root;
    private final Set<KeyCode> pressedKeys = EnumSet.noneOf(KeyCode.class);
    private final Deque<KeyCode> heldDirections = new ArrayDeque<>();

    private AnimationTimer loop;
    private long lastFrameNanos;

    public GameScreen(Game game) {
        this.game = game;

        double width = game.getDungeon().getWidth() * (double) GameRenderer.TILE_SIZE;
        double height = game.getDungeon().getHeight() * (double) GameRenderer.TILE_SIZE
                + GameRenderer.HUD_HEIGHT;

        this.canvas = new Canvas(width, height);
        this.root = new StackPane(canvas);
    }

    public Parent getRoot() {
        return root;
    }

    public double getWidth() {
        return canvas.getWidth();
    }

    public double getHeight() {
        return canvas.getHeight();
    }

    /** Klavye dinleyicilerini sahneye bağlar. */
    public void attachInput(Scene scene) {
        scene.setOnKeyPressed(this::onKeyPressed);
        scene.setOnKeyReleased(this::onKeyReleased);
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

        // Tek seferlik komutlar; hareket ve saldırı her karede durumdan okunuyor.
        switch (code) {
            case E -> game.descend();
            case R -> game.regenerateFloor();
            case G -> game.cycleGenerator();
            case ENTER -> {
                if (game.isOver()) {
                    game.restart();
                }
            }
            case DIGIT1, DIGIT2, DIGIT3, DIGIT4, DIGIT5, DIGIT6, DIGIT7, DIGIT8 ->
                    game.useItem(slotOf(code));
            default -> {
                // Diğer tuşlar yalnızca basılı tuşlar kümesini ilgilendiriyor.
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
