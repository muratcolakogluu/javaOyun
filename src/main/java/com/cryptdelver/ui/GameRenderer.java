package com.cryptdelver.ui;

import com.cryptdelver.entity.Enemy;
import com.cryptdelver.entity.Entity;
import com.cryptdelver.entity.Item;
import com.cryptdelver.entity.Player;
import com.cryptdelver.game.Game;
import com.cryptdelver.game.Inventory;
import com.cryptdelver.world.Dungeon;
import com.cryptdelver.world.DungeonGenerator;
import com.cryptdelver.world.Tile;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * Oyun durumunu Canvas'a çizen sınıf.
 *
 * <p>Varlıkların mantıksal yeri ızgarada tam sayı, ama çizim
 * {@code getRenderX()} ile kesirli konumu kullanıyor: adım süresince varlık iki
 * karenin arasında görünüyor.</p>
 *
 * <p>Hangi varlığın nasıl çizileceğini bu sınıf bilmiyor: varlık adını söylüyor
 * ({@code "rat"}), {@link SpriteRegistry} çizimi veriyor. Tür kontrolü
 * ({@code instanceof}) hiçbir yerde yok.</p>
 */
public class GameRenderer {

    /** Bir tile'ın piksel cinsinden kenar uzunluğu. */
    public static final int TILE_SIZE = 20;

    /** Haritanın altındaki bilgi ve çanta şeridinin yüksekliği. */
    public static final int HUD_HEIGHT = 88;

    /** Yerdeki eşyalar biraz küçük çiziliyor ki karakterlerden ayırt edilsin. */
    private static final double GROUND_ITEM_SCALE = 0.78;

    private static final double SWING_RADIUS = 1.1;

    private static final int SLOT_SIZE = 30;
    private static final int SLOT_GAP = 4;
    private static final int SLOT_ORIGIN_X = 10;

    private static final Color BACKGROUND = Color.web("#0d0d12");
    private static final Color FLOOR_COLOR = Color.web("#1b1b26");
    private static final Color FLOOR_SPECK = Color.web("#262634");
    private static final Color WALL_COLOR = Color.web("#3f3a56");
    private static final Color WALL_EDGE = Color.web("#524a70");
    private static final Color STAIRS_PIT = Color.web("#08080c");
    private static final Color STAIRS_STEP = Color.web("#6b6480");
    private static final Color STAIRS_EDGE = Color.web("#9a8fc0");
    private static final Color HINT_BACKGROUND = Color.web("#15151d", 0.9);
    private static final Color HUD_BACKGROUND = Color.web("#15151d");
    private static final Color HUD_TEXT = Color.web("#7c7c92");
    private static final Color HUD_ACCENT = Color.web("#9a8fc0");
    private static final Color HP_TEXT = Color.web("#c9564f");
    private static final Color GOLD_TEXT = Color.web("#e8c46a");
    private static final Color MESSAGE_TEXT = Color.web("#b6b6c8");
    private static final Color SLOT_BACKGROUND = Color.web("#1e1e2a");
    private static final Color SLOT_BORDER = Color.web("#2f2f40");
    private static final Color SLOT_EQUIPPED = Color.web("#e8c46a");
    private static final Color SLOT_NUMBER = Color.web("#5a5a6e");
    private static final Color SWING_COLOR = Color.web("#e8c46a", 0.28);
    private static final Color HP_BAR_BACKGROUND = Color.web("#000000", 0.55);
    private static final Color HP_BAR_FILL = Color.web("#b64b45");
    private static final Color OVERLAY = Color.web("#0d0d12", 0.78);
    private static final Color OVERLAY_TITLE = Color.web("#c9564f");

    private final SpriteRegistry sprites = new SpriteRegistry();
    private final ColorAdjust hitEffect = new ColorAdjust(0, -0.6, 0.7, 0);
    private final Font hudFont = Font.font("Consolas", 13);
    private final Font slotFont = Font.font("Consolas", 10);
    private final Font titleFont = Font.font("Consolas", 46);

    /** Haritayı, varlıkları, bilgi şeridini ve gerekiyorsa ölüm ekranını çizer. */
    public void render(GraphicsContext gc, Game game) {
        Dungeon dungeon = game.getDungeon();
        double mapWidth = dungeon.getWidth() * (double) TILE_SIZE;
        double mapHeight = dungeon.getHeight() * (double) TILE_SIZE;

        gc.setFill(BACKGROUND);
        gc.fillRect(0, 0, mapWidth, mapHeight + HUD_HEIGHT);

        drawDungeon(gc, dungeon, game.isStairsLocked());

        for (Item item : game.getGroundItems()) {
            drawEntity(gc, item, GROUND_ITEM_SCALE);
        }

        Player player = game.getPlayer();
        if (player.isSwinging()) {
            drawSwing(gc, player);
        }

        for (Enemy enemy : game.getEnemies()) {
            drawEntity(gc, enemy, 1.0);
            drawHealthBar(gc, enemy);
        }
        drawEntity(gc, player, 1.0);

        if (game.getBoss() != null && !game.isOver()) {
            drawBossBar(gc, game, mapWidth);
        }

        if (game.isPlayerOnStairs() && !game.isOver()) {
            drawStairsHint(gc, game, mapWidth, mapHeight);
        }

        drawHud(gc, game, mapWidth, mapHeight);

        if (game.isOver()) {
            drawGameOver(gc, game, mapWidth, mapHeight);
        }
    }

    private void drawDungeon(GraphicsContext gc, Dungeon dungeon, boolean stairsLocked) {
        for (int x = 0; x < dungeon.getWidth(); x++) {
            for (int y = 0; y < dungeon.getHeight(); y++) {
                double px = x * (double) TILE_SIZE;
                double py = y * (double) TILE_SIZE;

                Tile tile = dungeon.getTile(x, y);
                if (tile == Tile.WALL) {
                    gc.setFill(WALL_COLOR);
                    gc.fillRect(px, py, TILE_SIZE, TILE_SIZE);
                    // Üstte ince bir aydınlık şerit duvara hacim hissi veriyor.
                    gc.setFill(WALL_EDGE);
                    gc.fillRect(px, py, TILE_SIZE, 2);
                } else {
                    gc.setFill(FLOOR_COLOR);
                    gc.fillRect(px, py, TILE_SIZE, TILE_SIZE);
                    gc.setFill(FLOOR_SPECK);
                    gc.fillRect(px + TILE_SIZE / 2.0, py + TILE_SIZE / 2.0, 2, 2);

                    if (tile == Tile.STAIRS_DOWN) {
                        drawStairs(gc, px, py, stairsLocked);
                    }
                }
            }
        }
    }

    /** İniş merdiveni: karanlık bir boşluk ve içine inen basamaklar. Kilitliyse kızıl çerçeve. */
    private void drawStairs(GraphicsContext gc, double px, double py, boolean locked) {
        gc.setFill(STAIRS_PIT);
        gc.fillRect(px + 2, py + 2, TILE_SIZE - 4, TILE_SIZE - 4);

        gc.setFill(STAIRS_STEP);
        for (int step = 0; step < 3; step++) {
            double inset = 3 + step * 2.5;
            double y = py + 4 + step * 4.5;
            gc.fillRect(px + inset, y, TILE_SIZE - inset * 2, 2.5);
        }

        gc.setStroke(locked ? OVERLAY_TITLE : STAIRS_EDGE);
        gc.setLineWidth(locked ? 2 : 1);
        gc.strokeRect(px + 2, py + 2, TILE_SIZE - 4, TILE_SIZE - 4);
    }

    /** Varlığı sprite'ıyla çizer; hasar almışsa beyaza yakın parlatır. */
    private void drawEntity(GraphicsContext gc, Entity entity, double scale) {
        if (entity.isFlashing()) {
            gc.setEffect(hitEffect);
        }

        sprites.get(entity.getSpriteName()).draw(
                gc,
                entity.getRenderX() * TILE_SIZE,
                entity.getRenderY() * TILE_SIZE,
                TILE_SIZE * scale * entity.getDrawScale());

        gc.setEffect(null);
    }

    /** Yaralı düşmanların üstünde ince bir can çubuğu. */
    private void drawHealthBar(GraphicsContext gc, Enemy enemy) {
        if (enemy.getHp() >= enemy.getMaxHp()) {
            return;
        }

        double width = TILE_SIZE * 0.8;
        double height = 3;
        double x = enemy.getRenderX() * TILE_SIZE - width / 2;
        double y = enemy.getRenderY() * TILE_SIZE - TILE_SIZE * 0.62;
        double ratio = enemy.getHp() / (double) enemy.getMaxHp();

        gc.setFill(HP_BAR_BACKGROUND);
        gc.fillRect(x, y, width, height);
        gc.setFill(HP_BAR_FILL);
        gc.fillRect(x, y, width * ratio, height);
    }

    /** Vuruş anında oyuncunun etrafında beliren menzil halkası. */
    private void drawSwing(GraphicsContext gc, Player player) {
        double radius = SWING_RADIUS * TILE_SIZE;
        gc.setFill(SWING_COLOR);
        gc.fillOval(
                player.getRenderX() * TILE_SIZE - radius,
                player.getRenderY() * TILE_SIZE - radius,
                radius * 2,
                radius * 2);
    }

    private void drawHud(GraphicsContext gc, Game game, double mapWidth, double mapHeight) {
        gc.setFill(HUD_BACKGROUND);
        gc.fillRect(0, mapHeight, mapWidth, HUD_HEIGHT);

        gc.setFont(hudFont);
        gc.setTextBaseline(VPos.CENTER);
        double firstLine = mapHeight + 15;
        double secondLine = mapHeight + 35;

        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFill(HP_TEXT);
        gc.fillText("Can " + game.getPlayer().getHp() + "/" + game.getPlayer().getMaxHp(), 10, firstLine);

        gc.setFill(GOLD_TEXT);
        gc.fillText("Altin " + game.getGold(), 90, firstLine);

        gc.setFill(HUD_TEXT);
        gc.fillText("Vurus " + game.getPlayer().getAttackPower(), 175, firstLine);
        gc.fillText("Zirh " + game.getPlayer().getDefense(), 250, firstLine);

        gc.setFill(HUD_ACCENT);
        gc.fillText("Kat " + game.getDepth(), 315, firstLine);

        gc.setFill(HUD_TEXT);
        gc.fillText(String.format("Sure %.0fs", game.getElapsedSeconds()), 380, firstLine);
        gc.fillText("Dusman " + game.getEnemies().size(), 465, firstLine);

        DungeonGenerator generator = game.getCurrentGenerator();
        if (generator != null) {
            gc.setFill(HUD_ACCENT);
            gc.fillText(generator.getName(), 555, firstLine);
        }

        gc.setFill(MESSAGE_TEXT);
        gc.fillText(game.getMessageLog().last(), 10, secondLine);

        gc.setTextAlign(TextAlignment.RIGHT);
        gc.setFill(HUD_TEXT);
        gc.fillText("WASD hareket    Bosluk vur    1-8 esya    E in", mapWidth - 10, firstLine);
        gc.fillText("R yeni kat    G uretici    Enter yeniden basla", mapWidth - 10, secondLine);

        drawInventory(gc, game, mapHeight);
    }

    /** Çanta slotları: numarası, içindeki eşyanın sprite'ı, kuşanılan silahın çerçevesi. */
    private void drawInventory(GraphicsContext gc, Game game, double mapHeight) {
        Inventory inventory = game.getInventory();
        double top = mapHeight + 48;

        for (int slot = 0; slot < Inventory.CAPACITY; slot++) {
            double x = SLOT_ORIGIN_X + slot * (SLOT_SIZE + SLOT_GAP);
            Item item = inventory.get(slot);
            boolean equipped = item != null
                    && (item == game.getPlayer().getEquippedWeapon()
                        || item == game.getPlayer().getEquippedArmor());

            gc.setFill(SLOT_BACKGROUND);
            gc.fillRoundRect(x, top, SLOT_SIZE, SLOT_SIZE, 5, 5);

            gc.setStroke(equipped ? SLOT_EQUIPPED : SLOT_BORDER);
            gc.setLineWidth(equipped ? 2 : 1);
            gc.strokeRoundRect(x, top, SLOT_SIZE, SLOT_SIZE, 5, 5);

            if (item != null) {
                sprites.get(item.getSpriteName())
                        .draw(gc, x + SLOT_SIZE / 2.0, top + SLOT_SIZE / 2.0, SLOT_SIZE * 0.82);
            }

            gc.setFont(slotFont);
            gc.setTextAlign(TextAlignment.LEFT);
            gc.setFill(SLOT_NUMBER);
            gc.fillText(String.valueOf(slot + 1), x + 3, top + 7);
        }

        // Kuşanılan takımın adları, slotların sağında.
        double x = SLOT_ORIGIN_X + Inventory.CAPACITY * (SLOT_SIZE + SLOT_GAP) + 10;
        gc.setFont(hudFont);
        gc.setFill(SLOT_EQUIPPED);
        gc.setTextAlign(TextAlignment.LEFT);

        if (game.getPlayer().getEquippedWeapon() != null) {
            gc.fillText(game.getPlayer().getEquippedWeapon().getName(), x, top + 8);
        }
        if (game.getPlayer().getEquippedArmor() != null) {
            gc.fillText(game.getPlayer().getEquippedArmor().getName(), x, top + 24);
        }
    }

    /** Boss yaşarken haritanın üstünde duran can çubuğu. */
    private void drawBossBar(GraphicsContext gc, Game game, double mapWidth) {
        Enemy boss = game.getBoss();
        double barWidth = 320;
        double barHeight = 12;
        double x = (mapWidth - barWidth) / 2;
        double y = 12;

        gc.setFill(HINT_BACKGROUND);
        gc.fillRoundRect(x - 6, y - 16, barWidth + 12, barHeight + 26, 6, 6);

        gc.setFont(hudFont);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.setFill(OVERLAY_TITLE);
        gc.fillText(boss.getName(), mapWidth / 2, y - 6);

        gc.setFill(HP_BAR_BACKGROUND);
        gc.fillRect(x, y + 4, barWidth, barHeight);
        gc.setFill(HP_BAR_FILL);
        gc.fillRect(x, y + 4, barWidth * (boss.getHp() / (double) boss.getMaxHp()), barHeight);
        gc.setStroke(OVERLAY_TITLE);
        gc.setLineWidth(1);
        gc.strokeRect(x, y + 4, barWidth, barHeight);
    }

    /** Merdivenin üstündeyken haritanın altında beliren ipucu. */
    private void drawStairsHint(GraphicsContext gc, Game game, double mapWidth, double mapHeight) {
        boolean locked = game.isStairsLocked();
        String hint = locked ? "Merdiveni tutan seyi once yen" : "E ile bir alt kata in";
        double boxWidth = locked ? 230 : 190;
        double boxHeight = 26;
        double x = (mapWidth - boxWidth) / 2;
        double y = mapHeight - boxHeight - 12;

        gc.setFill(HINT_BACKGROUND);
        gc.fillRoundRect(x, y, boxWidth, boxHeight, 6, 6);
        gc.setStroke(STAIRS_EDGE);
        gc.setLineWidth(1);
        gc.strokeRoundRect(x, y, boxWidth, boxHeight, 6, 6);

        gc.setFont(hudFont);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.setFill(locked ? OVERLAY_TITLE : GOLD_TEXT);
        gc.fillText(hint, mapWidth / 2, y + boxHeight / 2);
    }

    private void drawGameOver(GraphicsContext gc, Game game, double mapWidth, double mapHeight) {
        gc.setFill(OVERLAY);
        gc.fillRect(0, 0, mapWidth, mapHeight);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);

        gc.setFont(titleFont);
        gc.setFill(OVERLAY_TITLE);
        gc.fillText("OLDUN", mapWidth / 2.0, mapHeight / 2.0 - 30);

        gc.setFont(hudFont);
        gc.setFill(GOLD_TEXT);
        gc.fillText(game.getDepth() + ". katta dustun    " + game.getGold() + " altin topladin",
                mapWidth / 2.0, mapHeight / 2.0 + 8);

        gc.setFill(MESSAGE_TEXT);
        gc.fillText("Enter ile yeniden basla", mapWidth / 2.0, mapHeight / 2.0 + 32);
    }
}
