package com.cryptdelver.ui;

import com.cryptdelver.entity.Enemy;
import com.cryptdelver.entity.Entity;
import com.cryptdelver.entity.Item;
import com.cryptdelver.entity.Player;
import com.cryptdelver.entity.Weapon;
import com.cryptdelver.game.Game;
import com.cryptdelver.game.Inventory;
import com.cryptdelver.world.Dungeon;
import com.cryptdelver.world.DungeonGenerator;
import com.cryptdelver.world.Tile;
import java.util.List;
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
 * ({@code "imp"}), {@link SpriteRegistry} çizimi veriyor. Tür kontrolü
 * ({@code instanceof}) hiçbir yerde yok.</p>
 */
public class GameRenderer {

    /** Bir tile'ın piksel cinsinden kenar uzunluğu. */
    public static final int TILE_SIZE = 32;

    /** Haritanın altındaki bilgi ve çanta şeridinin yüksekliği. */
    public static final int HUD_HEIGHT = 96;

    /** Yerdeki eşyalar biraz küçük çiziliyor ki karakterlerden ayırt edilsin. */
    private static final double GROUND_ITEM_SCALE = 1.0;

    private static final double SWING_RADIUS = 1.1;

    /** Bilgi seridinde gosterilen olay satiri sayisi. */
    private static final int MESSAGE_LINES = 3;

    /** Elde tutulan silah, yerdekinden biraz küçük çiziliyor. */
    private static final double HELD_WEAPON_SCALE = 0.85;

    /** Silahın boştayken taban açıdan sapması, derece. */
    private static final double WEAPON_REST_TILT = 20;

    /** Savuruş yayının genişliği; yay baktığın yönün etrafında dönüyor. */
    private static final double SWING_ARC = 150;

    /** Elin gövde merkezinden baktığın yöne kayması ve aşağı düşmesi (kare). */
    private static final double HAND_REACH = 0.30;
    private static final double HAND_DROP = 0.18;

    private static final int SLOT_SIZE = 30;
    private static final int SLOT_GAP = 4;
    private static final int SLOT_ORIGIN_X = 10;

    private static final Color BACKGROUND = Color.web("#0d0d12");
    private static final Color STAIRS_EDGE = Color.web("#9a8fc0");
    private static final Color HINT_BACKGROUND = Color.web("#15151d", 0.9);
    private static final Color HUD_BACKGROUND = Color.web("#15151d");
    private static final Color HUD_TEXT = Color.web("#7c7c92");
    private static final Color HUD_ACCENT = Color.web("#9a8fc0");
    private static final Color HP_TEXT = Color.web("#c9564f");
    private static final Color GOLD_TEXT = Color.web("#e8c46a");
    private static final Color MESSAGE_TEXT = Color.web("#b6b6c8");
    private static final Color MESSAGE_FADED = Color.web("#6b6b80");
    private static final Color SLOT_BACKGROUND = Color.web("#1e1e2a");
    private static final Color SLOT_BORDER = Color.web("#2f2f40");
    private static final Color SLOT_EQUIPPED = Color.web("#e8c46a");
    private static final Color SLOT_NUMBER = Color.web("#5a5a6e");
    private static final Color STACK_COUNT = Color.web("#e0e0ee");
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

        // Piksel sanatı bulanıklaşmasın: yumuşatma kapalı, kaynak piksel
        // kenarları keskin kalsın (16 piksellik resim 32 piksellik kareye
        // tam iki katına ölçekleniyor).
        gc.setImageSmoothing(false);

        gc.setFill(BACKGROUND);
        gc.fillRect(0, 0, mapWidth, mapHeight + HUD_HEIGHT);

        drawDungeon(gc, dungeon, game.isStairsLocked());

        for (Item item : game.getGroundItems()) {
            drawEntity(gc, item, GROUND_ITEM_SCALE);
        }

        Player player = game.getPlayer();

        // Silah varsa savuruşu kılıcın kendisi gösteriyor; çıplak elle
        // vururken de bir şey görünsün diye halka o durumda çiziliyor.
        if (player.isSwinging() && player.getEquippedWeapon() == null) {
            drawSwing(gc, player);
        }

        for (Enemy enemy : game.getEnemies()) {
            drawEntity(gc, enemy, 1.0);
            drawHealthBar(gc, enemy);
        }
        drawEntity(gc, player, 1.0);
        drawEquipment(gc, player);

        if (game.getBoss() != null && !game.isOver()) {
            drawBossBar(gc, game, mapWidth);
        }

        if (game.isPlayerOnStairs() && !game.isOver()) {
            drawStairsHint(gc, game, mapWidth, mapHeight);
        }

        drawHud(gc, game, mapWidth, mapHeight);

        if (game.isPaused()) {
            drawPauseScreen(gc, mapWidth, mapHeight);
        }

        if (game.isOver()) {
            drawGameOver(gc, game, mapWidth, mapHeight);
        }
    }

    /**
     * Duraklatma perdesi: aynı zamanda oyunun yardım ekranı.
     *
     * <p>Tuş listesini buraya taşımak HUD'ı boşalttı; oyun sırasında sürekli
     * göz önünde duran iki satır yazı yerine, ihtiyaç duyulduğunda açılan
     * düzgün bir liste var.</p>
     */
    private void drawPauseScreen(GraphicsContext gc, double mapWidth, double mapHeight) {
        gc.setFill(OVERLAY);
        gc.fillRect(0, 0, mapWidth, mapHeight);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.setFont(titleFont);
        gc.setFill(GOLD_TEXT);
        gc.fillText("DURAKLATILDI", mapWidth / 2, mapHeight / 2 - 130);

        String[][] keys = {
                {"WASD / oklar", "hareket"},
                {"Bosluk", "vur"},
                {"1-8", "cantadaki esyayi kullan / kusan"},
                {"Shift + 1-8", "esyayi yere birak"},
                {"E", "merdivende bir alt kata in"},
                {"F5 / F9", "kaydet / yukle"},
                {"R / G", "yeni kat / zindan ureticisini degistir"},
                {"Enter", "olunce yeniden basla"},
                {"ESC", "devam et"},
        };

        gc.setFont(hudFont);
        double y = mapHeight / 2 - 70;
        for (String[] row : keys) {
            gc.setTextAlign(TextAlignment.RIGHT);
            gc.setFill(HUD_ACCENT);
            gc.fillText(row[0], mapWidth / 2 - 15, y);

            gc.setTextAlign(TextAlignment.LEFT);
            gc.setFill(MESSAGE_TEXT);
            gc.fillText(row[1], mapWidth / 2 + 15, y);
            y += 22;
        }
    }

    /**
     * Zindanı çizer.
     *
     * <p>Duvar ve zemin de sprite üzerinden çiziliyor: hazır bir tileset
     * klasöre konduğunda harita da onunla görünsün diye. Merdiven, zeminin
     * üstüne ikinci bir sprite olarak biniyor.</p>
     */
    private void drawDungeon(GraphicsContext gc, Dungeon dungeon, boolean stairsLocked) {
        Sprite floor = sprites.get("floor");
        Sprite wall = sprites.get("wall");
        Sprite stairs = sprites.get("stairs");

        for (int x = 0; x < dungeon.getWidth(); x++) {
            for (int y = 0; y < dungeon.getHeight(); y++) {
                double cx = x * TILE_SIZE + TILE_SIZE / 2.0;
                double cy = y * TILE_SIZE + TILE_SIZE / 2.0;

                Tile tile = dungeon.getTile(x, y);
                if (tile == Tile.WALL) {
                    wall.draw(gc, cx, cy, TILE_SIZE);
                    continue;
                }

                floor.draw(gc, cx, cy, TILE_SIZE);
                if (tile == Tile.STAIRS_DOWN) {
                    stairs.draw(gc, cx, cy, TILE_SIZE);
                    drawStairsFrame(gc, cx, cy, stairsLocked);
                }
            }
        }
    }

    /** Merdivenin çerçevesi: boss tutuyorsa kızıl ve kalın. */
    private void drawStairsFrame(GraphicsContext gc, double cx, double cy, boolean locked) {
        gc.setStroke(locked ? OVERLAY_TITLE : STAIRS_EDGE);
        gc.setLineWidth(locked ? 2 : 1);
        gc.strokeRect(cx - TILE_SIZE / 2.0 + 2, cy - TILE_SIZE / 2.0 + 2, TILE_SIZE - 4, TILE_SIZE - 4);
    }

    /** Varlığı sprite'ıyla çizer; hasar almışsa beyaza yakın parlatır. */
    private void drawEntity(GraphicsContext gc, Entity entity, double scale) {
        if (entity.isFlashing()) {
            gc.setEffect(hitEffect);
        }

        // Adım halindeki varlık yürüyüş animasyonuyla çiziliyor.
        sprites.get(entity.getSpriteName(), entity.isMoving()).draw(
                gc,
                entity.getRenderX() * TILE_SIZE,
                entity.getRenderY() * TILE_SIZE,
                TILE_SIZE * scale * entity.getDrawScale());

        gc.setEffect(null);
    }

    /**
     * Kuşanılan silahı oyuncunun eline çizer.
     *
     * <p>Yalnızca silah çiziliyor, çünkü paketin silah çizimleri zaten "elde
     * tutulan silah" olarak hazırlanmış ve gövdeyle aynı üslupta. Zırh için gövdeye
     * bindirilecek çizim yok (paketlerde yalnızca envanter ikonu var), o yüzden
     * zırhın durumu HUD ve çanta üzerinden okunuyor.</p>
     */
    private void drawEquipment(GraphicsContext gc, Player player) {
        Weapon weapon = player.getEquippedWeapon();
        if (weapon != null) {
            drawHeldWeapon(gc, player, weapon);
        }
    }

    /**
     * Silahı baktığın yönde tutar ve vuruşta o yöne savurur.
     *
     * <p>Paketin silah çizimleri sapı altta, namlusu yukarı bakacak şekilde
     * hazırlanmış. Dönme merkezini resmin <em>alt ucuna</em> (sapa) koyup açıyı
     * değiştirmek gerçek bir savuruş veriyor.</p>
     *
     * <p>Taban açı baktığın yönden geliyor: yukarı bakarken 0, sağa 90, aşağı
     * 180, sola 270 derece. Savuruş bu açının etrafında
     * {@value #SWING_ARC} derecelik bir yay çiziyor, yani kılıç hep yürüdüğün
     * yöne doğru iniyor. El de aynı yöne kayıyor.</p>
     */
    private void drawHeldWeapon(GraphicsContext gc, Player player, Weapon weapon) {
        int facingX = player.getFacingX();
        int facingY = player.getFacingY();
        double baseAngle = baseAngleFor(facingX, facingY);

        double angle = player.isSwinging()
                ? baseAngle - SWING_ARC / 2 + SWING_ARC * player.getSwingProgress()
                : baseAngle + WEAPON_REST_TILT;

        double size = TILE_SIZE * HELD_WEAPON_SCALE;
        double pivotX = player.getRenderX() * TILE_SIZE + facingX * TILE_SIZE * HAND_REACH;
        double pivotY = player.getRenderY() * TILE_SIZE + TILE_SIZE * HAND_DROP
                + facingY * TILE_SIZE * HAND_REACH;

        gc.save();
        gc.translate(pivotX, pivotY);
        gc.rotate(angle);
        // Sprite tabana hizalı çizildiği için, merkezi yarım boy yukarı almak
        // sapı tam dönme merkezine oturtuyor.
        sprites.get(weapon.getSpriteName()).draw(gc, 0, -size / 2, size);
        gc.restore();
    }

    /** Sprite yukarı baktığı için: yukarı 0, sağ 90, aşağı 180, sol -90 derece. */
    private double baseAngleFor(int facingX, int facingY) {
        if (facingY < 0) {
            return 0;
        }
        if (facingY > 0) {
            return 180;
        }
        return facingX < 0 ? -90 : 90;
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

        drawHealthBar(gc, game, firstLine);

        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFill(GOLD_TEXT);
        gc.fillText("Altin " + game.getGold(), 200, firstLine);

        gc.setFill(HUD_TEXT);
        gc.fillText("Vurus " + game.getPlayer().getAttackPower(), 285, firstLine);
        gc.fillText("Zirh " + game.getPlayer().getDefense(), 360, firstLine);

        gc.setFill(HUD_ACCENT);
        gc.fillText("Kat " + game.getDepth(), 425, firstLine);

        gc.setFill(HUD_TEXT);
        gc.fillText(String.format("Sure %.0fs", game.getElapsedSeconds()), 490, firstLine);
        gc.fillText("Dusman " + game.getEnemies().size(), 575, firstLine);

        DungeonGenerator generator = game.getCurrentGenerator();
        if (generator != null) {
            gc.setFill(HUD_ACCENT);
            gc.fillText(generator.getName(), 665, firstLine);
        }

        // Son üç olay: tek satır, hızlı savaşta neyin olduğunu kaçırtıyordu.
        // Eskiler soluk, en yeni parlak.
        List<String> recent = game.getMessageLog().latest(MESSAGE_LINES);
        for (int i = 0; i < recent.size(); i++) {
            gc.setFill(i == 0 ? MESSAGE_TEXT : MESSAGE_FADED);
            gc.fillText(recent.get(i), 10, secondLine + i * 15);
        }

        gc.setTextAlign(TextAlignment.RIGHT);
        gc.setFill(HUD_TEXT);
        gc.fillText("ESC: durdur ve tuslari gor", mapWidth - 10, firstLine);

        drawInventory(gc, game, mapHeight);
    }

    /** Can çubuğu: sayıyı okumadan da kalan canı görebilesin diye. */
    private void drawHealthBar(GraphicsContext gc, Game game, double centerY) {
        double width = 170;
        double height = 14;
        double x = 10;
        double y = centerY - height / 2;
        double ratio = game.getPlayer().getHp() / (double) game.getPlayer().getMaxHp();

        gc.setFill(HP_BAR_BACKGROUND);
        gc.fillRect(x, y, width, height);
        gc.setFill(HP_BAR_FILL);
        gc.fillRect(x, y, width * Math.max(0, ratio), height);
        gc.setStroke(HUD_TEXT);
        gc.setLineWidth(1);
        gc.strokeRect(x, y, width, height);

        gc.setFont(hudFont);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(MESSAGE_TEXT);
        gc.fillText(game.getPlayer().getHp() + " / " + game.getPlayer().getMaxHp(),
                x + width / 2, centerY);
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

            // Yığılmış eşyalarda adet: "3x" gibi, sağ alt köşede.
            int count = inventory.getCount(slot);
            if (count > 1) {
                gc.setTextAlign(TextAlignment.RIGHT);
                gc.setFill(STACK_COUNT);
                gc.fillText(count + "x", x + SLOT_SIZE - 3, top + SLOT_SIZE - 6);
            }
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
