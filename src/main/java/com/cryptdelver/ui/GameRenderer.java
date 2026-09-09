package com.cryptdelver.ui;

import com.cryptdelver.entity.Armor;
import com.cryptdelver.entity.Blacksmith;
import com.cryptdelver.entity.Enchantment;
import com.cryptdelver.entity.Enemy;
import com.cryptdelver.entity.Entity;
import com.cryptdelver.entity.Equipment;
import com.cryptdelver.entity.Item;
import com.cryptdelver.entity.Player;
import com.cryptdelver.entity.Weapon;
import com.cryptdelver.game.Forge;
import com.cryptdelver.game.Game;
import com.cryptdelver.game.Inventory;
import com.cryptdelver.game.Settings;
import com.cryptdelver.world.Dungeon;
import com.cryptdelver.world.DungeonGenerator;
import com.cryptdelver.world.Tile;
import java.util.List;
import java.util.Map;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
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

    /** Panel ayraçlarının solunda bıraktığımız boşluk. */
    private static final double PANEL_GAP = 14;

    /**
     * Panellerin sol kenarları.
     *
     * <p>Harita 40 kare, yani 1280 piksel geniş. Şerit üç parçaya bölündü:
     * durum solda, çanta ortada, olaylar sağda. Sayılar sabit çünkü pencere
     * boyutu da sabit; oranla hesaplamak burada gereksiz karmaşa olurdu.</p>
     */
    private static final double STATUS_PANEL_X = 10;
    private static final double INVENTORY_PANEL_X = 260;
    private static final double EVENT_PANEL_X = 600;

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
    private static final Color DURABILITY_FULL = Color.web("#6f9a5a");

    /** Dayanıklılık bunun altına düşünce çubuk sarıya döner. */
    private static final double DURABILITY_WARNING = 0.35;

    /** Demircinin ayağının dibindeki ocak ışığı. */
    private static final Color FORGE_GLOW = Color.web("#ff8a3d");
    private static final int FORGE_GLOW_RINGS = 3;
    private static final long FORGE_PULSE_MILLIS = 1600;

    private final SpriteRegistry sprites = new SpriteRegistry();
    private final ColorAdjust hitEffect = new ColorAdjust(0, -0.6, 0.7, 0);
    private final Font hudFont = Font.font("Consolas", 13);
    private final Font slotFont = Font.font("Consolas", 10);
    private final Font titleFont = Font.font("Consolas", 46);

    /** Yazı genişliği ölçmek için tutulan görünmez düğüm; {@link #measure} kullanıyor. */
    private final Text textMeasure = new Text();

    /**
     * Zırh kademesine göre gövde rengi; anahtar zırhın sprite adı.
     *
     * <p>Değerler {@code ColorAdjust}: ton kayması, doygunluk, parlaklık.
     * Taban gövde yeşil tunikli; her kademe onu başka bir yöne çekiyor.</p>
     */
    private static final Map<String, ColorAdjust> ARMOR_TINTS = Map.of(
            "armor_leather", tint(0.06, -0.30, -0.08),
            "armor_chain", tint(-0.42, -0.55, 0.06),
            "armor_plate", tint(-0.34, -0.80, 0.20),
            "armor_crypt", tint(0.72, -0.05, -0.10));

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

        if (game.getBlacksmith() != null) {
            drawForgeGlow(gc, game.getBlacksmith());
            drawEntity(gc, game.getBlacksmith(), 1.0);
            drawBlacksmithSign(gc, game);
        }

        for (Enemy enemy : game.getEnemies()) {
            drawEntity(gc, enemy, 1.0);
            drawHealthBar(gc, enemy);
        }
        drawEntity(gc, player, 1.0, armorTint(player));
        drawEquipment(gc, player);

        if (game.getBoss() != null && !game.isOver()) {
            drawBossBar(gc, game, mapWidth);
        }

        if (game.isPlayerOnStairs() && !game.isOver()) {
            drawStairsHint(gc, game, mapWidth, mapHeight);
        }

        drawHud(gc, game, mapWidth, mapHeight);

        if (game.isForgeOpen()) {
            drawForgeScreen(gc, game, mapWidth, mapHeight);
        }

        if (game.isPaused()) {
            drawPauseScreen(gc, game, mapWidth, mapHeight);
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
    private void drawPauseScreen(GraphicsContext gc, Game game, double mapWidth, double mapHeight) {
        gc.setFill(OVERLAY);
        gc.fillRect(0, 0, mapWidth, mapHeight);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.setFont(titleFont);
        gc.setFill(GOLD_TEXT);
        gc.fillText("DURAKLATILDI", mapWidth / 2, mapHeight / 2 - 175);

        double y = drawSettingsSection(gc, game, mapWidth, mapHeight / 2 - 115);
        drawKeyList(gc, mapWidth, y + 24);
    }

    /**
     * Ayarlar bölümü: ses seviyesi çubuğu ve sessize alma durumu.
     *
     * @return listelenen son satırın altındaki y konumu
     */
    private double drawSettingsSection(GraphicsContext gc, Game game, double mapWidth, double top) {
        Settings settings = game.getSettings();

        gc.setFont(hudFont);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(HUD_ACCENT);
        gc.fillText("— AYARLAR —", mapWidth / 2, top);

        double rowY = top + 28;
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.setFill(MESSAGE_TEXT);
        gc.fillText("Ses", mapWidth / 2 - 90, rowY);

        drawVolumeBar(gc, settings, mapWidth / 2 - 75, rowY);

        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFill(settings.isMuted() ? HUD_TEXT : GOLD_TEXT);
        gc.fillText(settings.isMuted() ? "kapali" : "%" + settings.getVolumePercent(),
                mapWidth / 2 + 90, rowY);

        rowY += 20;
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(HUD_TEXT);
        gc.fillText("- / +  ile ayarla,  M  ile sustur", mapWidth / 2, rowY);

        return rowY;
    }

    /** On kademeli ses çubuğu; sessizdeyken sönük çiziliyor. */
    private void drawVolumeBar(GraphicsContext gc, Settings settings, double left, double centerY) {
        int steps = (int) Math.round(1 / Settings.VOLUME_STEP);
        int filled = (int) Math.round(settings.getVolume() * steps);

        double cellWidth = 14;
        double height = 12;
        double y = centerY - height / 2;

        for (int i = 0; i < steps; i++) {
            double x = left + i * cellWidth;

            if (i < filled) {
                gc.setFill(settings.isMuted() ? HUD_TEXT : GOLD_TEXT);
                gc.fillRect(x + 1, y, cellWidth - 3, height);
            } else {
                gc.setFill(SLOT_BACKGROUND);
                gc.fillRect(x + 1, y, cellWidth - 3, height);
            }
        }
    }

    private void drawKeyList(GraphicsContext gc, double mapWidth, double top) {
        String[][] keys = {
                {"WASD / oklar", "hareket"},
                {"Bosluk", "vur"},
                {"1-8", "cantadaki esyayi kullan / kusan"},
                {"Shift + 1-8", "esyayi yere birak"},
                {"E", "merdivende bir alt kata in"},
                {"F", "demircinin yaninda tezgahi ac"},
                {"F5 / F9", "kaydet / yukle"},
                {"R / G", "yeni kat / zindan ureticisini degistir"},
                {"Enter", "olunce yeniden basla"},
                {"ESC", "devam et"},
        };

        gc.setFont(hudFont);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(HUD_ACCENT);
        gc.fillText("— TUSLAR —", mapWidth / 2, top);

        double y = top + 26;
        for (String[] row : keys) {
            gc.setTextAlign(TextAlignment.RIGHT);
            gc.setFill(HUD_ACCENT);
            gc.fillText(row[0], mapWidth / 2 - 15, y);

            gc.setTextAlign(TextAlignment.LEFT);
            gc.setFill(MESSAGE_TEXT);
            gc.fillText(row[1], mapWidth / 2 + 15, y);
            y += 21;
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
        drawEntity(gc, entity, scale, null);
    }

    /**
     * Varlığı çizer; {@code tint} verilmişse gövde o renge boyanır.
     *
     * <p>Vuruş parlaması boyamayı eziyor: o an önemli olan "darbe yedim"
     * bilgisi, hangi zırhı giydiğin değil. Parlama zaten çeyrek saniye
     * sürüyor, sonrasında renk geri geliyor.</p>
     */
    private void drawEntity(GraphicsContext gc, Entity entity, double scale, ColorAdjust tint) {
        if (entity.isFlashing()) {
            gc.setEffect(hitEffect);
        } else if (tint != null) {
            gc.setEffect(tint);
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
     * Kuşanılan zırhın gövdeye verdiği renk.
     *
     * <p>Zırhı gövdenin üstüne <em>çizmenin</em> yolu yok: pakette giyilmiş
     * zırh çizimi yok, envanter ikonunu gövdeye bindirmeyi de denedik ve
     * berbat duruyordu. Bunun yerine gövdenin kendisi boyanıyor — deri sıcak
     * kahve, zincir soğuk çelik, plaka parlak beyaz, kript plakası mor. Aynı
     * karakter, farklı renk: "başka birine dönüşmek" hissi olmuyor ama üstünde
     * ne olduğu haritadan bakınca anlaşılıyor.</p>
     *
     * <p>Eşleme zırhın sprite adı üzerinden: model sınıfları renk bilmiyor,
     * bilmesi de gerekmiyor — hangi varlığın nasıl görüneceği bu sınıfın
     * işi.</p>
     */
    private ColorAdjust armorTint(Player player) {
        Armor armor = player.getEquippedArmor();
        return armor == null ? null : ARMOR_TINTS.get(armor.getSpriteName());
    }

    private static ColorAdjust tint(double hue, double saturation, double brightness) {
        return new ColorAdjust(hue, saturation, brightness, 0);
    }

    /**
     * Kuşanılan silahı oyuncunun eline çizer.
     *
     * <p>Yalnızca silah çiziliyor, çünkü paketin silah çizimleri zaten "elde
     * tutulan silah" olarak hazırlanmış ve gövdeyle aynı üslupta. Zırh için
     * gövdeye bindirilecek çizim yok (paketlerde yalnızca envanter ikonu var);
     * o yüzden zırh ayrı bir parça olarak değil, gövdenin <em>rengi</em> olarak
     * görünüyor — bkz. {@link #armorTint(Player)}.</p>
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

    /**
     * Bilgi şeridi: üç ayrı panel.
     *
     * <p>Önceden her şey aynı sütunda üst üsteydi — olay yazıları çantanın
     * hemen üstünde durduğu için "3 hasar aldın" ile çanta slotları
     * karışıyordu. Şimdi <b>Durum</b> solda, <b>Çanta</b> ortada,
     * <b>Olaylar</b> sağda; aralarında ayraç çizgisi var. Gözün nereye
     * bakacağını bilmesi için her panelin başlığı da yazılı.</p>
     */
    private void drawHud(GraphicsContext gc, Game game, double mapWidth, double mapHeight) {
        gc.setFill(HUD_BACKGROUND);
        gc.fillRect(0, mapHeight, mapWidth, HUD_HEIGHT);

        gc.setFont(hudFont);
        gc.setTextBaseline(VPos.CENTER);

        drawStatusPanel(gc, game, mapHeight);
        drawInventoryPanel(gc, game, mapHeight);
        drawEventPanel(gc, game, mapWidth, mapHeight);

        drawPanelDivider(gc, INVENTORY_PANEL_X - PANEL_GAP, mapHeight);
        drawPanelDivider(gc, EVENT_PANEL_X - PANEL_GAP, mapHeight);
    }

    /** Panelleri birbirinden ayıran dikey çizgi. */
    private void drawPanelDivider(GraphicsContext gc, double x, double mapHeight) {
        gc.setStroke(SLOT_BORDER);
        gc.setLineWidth(1);
        gc.strokeLine(x, mapHeight + 8, x, mapHeight + HUD_HEIGHT - 8);
    }

    private void drawPanelTitle(GraphicsContext gc, String title, double x, double mapHeight) {
        gc.setFont(slotFont);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFill(HUD_ACCENT);
        gc.fillText(title, x, mapHeight + 14);
        gc.setFont(hudFont);
    }

    /** Sol panel: can çubuğu, savaş değerleri ve ilerleme. */
    private void drawStatusPanel(GraphicsContext gc, Game game, double mapHeight) {
        drawPanelTitle(gc, "DURUM", STATUS_PANEL_X, mapHeight);
        drawHealthBar(gc, game, mapHeight + 36);

        gc.setTextAlign(TextAlignment.LEFT);
        double line = mapHeight + 60;

        gc.setFill(HUD_TEXT);
        gc.fillText("Vurus " + game.getPlayer().getAttackPower(), STATUS_PANEL_X, line);
        gc.fillText("Zirh " + game.getPlayer().getDefense(), STATUS_PANEL_X + 82, line);

        gc.setFill(GOLD_TEXT);
        gc.fillText("Altin " + game.getGold(), STATUS_PANEL_X + 148, line);

        line += 18;
        gc.setFill(HUD_ACCENT);
        gc.fillText("Kat " + game.getDepth(), STATUS_PANEL_X, line);

        gc.setFill(HUD_TEXT);
        gc.fillText(String.format("Sure %.0fs", game.getElapsedSeconds()), STATUS_PANEL_X + 62, line);
        gc.fillText("Dusman " + game.getEnemies().size(), STATUS_PANEL_X + 150, line);
    }

    /** Orta panel: çanta slotları ve kuşanılan parçaların durumu. */
    private void drawInventoryPanel(GraphicsContext gc, Game game, double mapHeight) {
        drawPanelTitle(gc, "CANTA", INVENTORY_PANEL_X, mapHeight);
        drawInventory(gc, game, INVENTORY_PANEL_X, mapHeight + 20);

        Player player = game.getPlayer();
        drawGearRow(gc, player.getEquippedWeapon(), mapHeight + 64);
        drawGearRow(gc, player.getEquippedArmor(), mapHeight + 82);
    }

    /**
     * Kuşanılan bir parçanın adı ve dayanıklılık çubuğu.
     *
     * <p>Çubuk sayıdan önemli: yıpranmayı fark etmek için "82/120" okumak
     * gerekmesin, çubuğun kısaldığını görmek yetsin. Renk de üç kademede
     * uyarıyor — dolu, azalmış, kırık.</p>
     */
    private void drawGearRow(GraphicsContext gc, Equipment item, double centerY) {
        if (item == null) {
            return;
        }

        gc.setFont(hudFont);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFill(item.isBroken() ? HP_TEXT : SLOT_EQUIPPED);
        gc.fillText(item.getFullName(), INVENTORY_PANEL_X, centerY);

        double barX = INVENTORY_PANEL_X + 170;
        double barWidth = 140;
        double barHeight = 6;
        double y = centerY - barHeight / 2;
        double ratio = item.getDurability() / (double) item.getMaxDurability();

        gc.setFill(HP_BAR_BACKGROUND);
        gc.fillRect(barX, y, barWidth, barHeight);
        gc.setFill(durabilityColor(ratio));
        gc.fillRect(barX, y, barWidth * Math.max(0, ratio), barHeight);
    }

    /** Dolu yeşilimsi, azalmış sarı, kırık kırmızı. */
    private Color durabilityColor(double ratio) {
        if (ratio <= 0) {
            return HP_TEXT;
        }
        return ratio < DURABILITY_WARNING ? GOLD_TEXT : DURABILITY_FULL;
    }

    /** Sağ panel: son olaylar ve tek satırlık yardım ipucu. */
    private void drawEventPanel(GraphicsContext gc, Game game, double mapWidth, double mapHeight) {
        drawPanelTitle(gc, "OLAYLAR", EVENT_PANEL_X, mapHeight);

        gc.setTextAlign(TextAlignment.LEFT);
        List<String> recent = game.getMessageLog().latest(MESSAGE_LINES);
        double line = mapHeight + 36;

        for (int i = 0; i < recent.size(); i++) {
            // En yeni olay parlak, eskiler soluk: sıralama renkten okunuyor.
            gc.setFill(i == 0 ? MESSAGE_TEXT : MESSAGE_FADED);
            gc.fillText(recent.get(i), EVENT_PANEL_X, line + i * 17);
        }

        gc.setTextAlign(TextAlignment.RIGHT);
        gc.setFill(HUD_TEXT);
        gc.fillText("ESC: durdur, ayarlar ve tuslar", mapWidth - 10, mapHeight + 14);

        DungeonGenerator generator = game.getCurrentGenerator();
        if (generator != null) {
            gc.setFill(HUD_ACCENT);
            gc.fillText(generator.getName(), mapWidth - 10, mapHeight + HUD_HEIGHT - 14);
        }
    }

    /** Can çubuğu: sayıyı okumadan da kalan canı görebilesin diye. */
    private void drawHealthBar(GraphicsContext gc, Game game, double centerY) {
        double width = 170;
        double height = 14;
        double x = STATUS_PANEL_X;
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

    /**
     * Çanta slotları: numarası, içindeki eşyanın görseli, kuşanılanın çerçevesi.
     *
     * <p>Nereye çizileceği artık dışarıdan geliyor. Şerit tek sütunken sabit bir
     * köşeden başlamak yetiyordu; panellere bölününce çantanın yeri panelin
     * kararı oldu. Kuşanılan parçaların adlarını da bu yüzden panel yazıyor:
     * burası yalnızca kutuları çiziyor.</p>
     */
    private void drawInventory(GraphicsContext gc, Game game, double originX, double top) {
        Inventory inventory = game.getInventory();

        for (int slot = 0; slot < Inventory.CAPACITY; slot++) {
            double x = originX + slot * (SLOT_SIZE + SLOT_GAP);
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

        gc.setFont(hudFont);
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

    /**
     * Demircinin ayağının dibindeki ocak ışığı.
     *
     * <p>Cüce gövdesi tek başına düşmanlardan ayırt edilmiyordu — haritada
     * hareket etmeyen bir yaratık gibi duruyordu. Sıcak turuncu bir halka onu
     * anında "burada bir şeyler oluyor" karesine çeviriyor. Hafifçe nefes
     * alıyor: sabit bir daire dekor gibi kalırdı, kıpırdayınca ateş oluyor.</p>
     *
     * <p>Işık gövdenin <em>altına</em> çiziliyor, üstüne değil. Daha önce
     * karakterin üstüne bindirilen çizimlerin nasıl durduğunu gördük.</p>
     */
    private void drawForgeGlow(GraphicsContext gc, Entity smith) {
        double centerX = smith.getRenderX() * TILE_SIZE + TILE_SIZE / 2.0;
        double centerY = smith.getRenderY() * TILE_SIZE + TILE_SIZE * 0.72;

        // Saniyede bir tam nefes; sinüs 0..1 arasına çekiliyor.
        double phase = (System.currentTimeMillis() % FORGE_PULSE_MILLIS) / (double) FORGE_PULSE_MILLIS;
        double breath = 0.5 + 0.5 * Math.sin(phase * 2 * Math.PI);

        for (int ring = FORGE_GLOW_RINGS; ring >= 1; ring--) {
            double radius = TILE_SIZE * 0.34 * ring * (0.94 + 0.06 * breath);
            double alpha = 0.10 / ring * (0.75 + 0.25 * breath);

            gc.setFill(Color.color(FORGE_GLOW.getRed(), FORGE_GLOW.getGreen(),
                    FORGE_GLOW.getBlue(), alpha));
            gc.fillOval(centerX - radius, centerY - radius * 0.55, radius * 2, radius * 1.1);
        }
    }

    /**
     * Demircinin üstündeki isim etiketi ve konuşma balonu.
     *
     * <p>Etiket her zaman duruyor: haritada kim olduğunu uzaktan da anlamalısın.
     * Balon ise yalnızca yanına gidince açılıyor ve içindeki cümleyi demircinin
     * kendisi seçiyor — takımın kırıksa onu söylüyor, sağlamsa başka bir şey.
     * Böylece balon hem "bu bir demirci" diyor hem de işe yarıyor.</p>
     */
    private void drawBlacksmithSign(GraphicsContext gc, Game game) {
        Blacksmith smith = game.getBlacksmith();
        double x = smith.getRenderX() * TILE_SIZE + TILE_SIZE / 2.0;
        double top = smith.getRenderY() * TILE_SIZE;

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);

        drawNamePlate(gc, smith.getName(), x, top - 6);

        if (game.isNearBlacksmith() && !game.isForgeOpen()) {
            drawSpeechBubble(gc, smith.greetingFor(game), x, top - 26);
        }
    }

    /** Küçük, soluk ad etiketi; balon yokken de kim olduğu belli olsun diye. */
    private void drawNamePlate(GraphicsContext gc, String name, double centerX, double centerY) {
        gc.setFont(slotFont);
        double width = measure(name, slotFont) + 12;

        gc.setFill(HINT_BACKGROUND);
        gc.fillRoundRect(centerX - width / 2, centerY - 8, width, 16, 5, 5);
        gc.setFill(HUD_ACCENT);
        gc.fillText(name, centerX, centerY);
    }

    /**
     * Kuyruklu konuşma balonu.
     *
     * <p>Genişliği yazıya göre ölçülüyor. Sabit genişlik verseydim kısa
     * cümlelerde kocaman, uzunlarda dar kalırdı; ölçmek {@link #measure} ile
     * tek satır.</p>
     */
    private void drawSpeechBubble(GraphicsContext gc, String line, double centerX, double bottomY) {
        gc.setFont(hudFont);
        String text = line + "   [F]";
        double width = measure(text, hudFont) + 22;
        double height = 26;
        double top = bottomY - height;

        gc.setFill(HINT_BACKGROUND);
        gc.fillRoundRect(centerX - width / 2, top, width, height, 8, 8);
        gc.setStroke(GOLD_TEXT);
        gc.setLineWidth(1);
        gc.strokeRoundRect(centerX - width / 2, top, width, height, 8, 8);

        // Balonun demirciye bakan sivri ucu.
        gc.setFill(HINT_BACKGROUND);
        gc.fillPolygon(
                new double[] {centerX - 6, centerX + 6, centerX},
                new double[] {bottomY - 1, bottomY - 1, bottomY + 7}, 3);

        gc.setFill(MESSAGE_TEXT);
        gc.fillText(text, centerX, top + height / 2);
    }

    /**
     * Yazının piksel genişliği.
     *
     * <p>{@code GraphicsContext} metin ölçmüyor, o yüzden görünmez bir
     * {@code Text} düğümü tutuyoruz. Tek örnek yeniden kullanılıyor: her karede
     * yeni düğüm yaratmak boşuna çöp üretirdi.</p>
     */
    private double measure(String value, Font font) {
        textMeasure.setFont(font);
        textMeasure.setText(value);
        return textMeasure.getLayoutBounds().getWidth();
    }

    /**
     * Demirci tezgâhı.
     *
     * <p>Dört satır, dört rakam: her satırda ne olduğu, ne kadar tuttuğu ve
     * yapılabilir olup olmadığı yazılı. Yapılamayan satırlar soluk ve
     * gerekçesi yanında — "neden olmuyor" sorusunu ekranın kendisi
     * yanıtlıyor, oyuncu deneyip mesaj kaydından öğrenmek zorunda değil.</p>
     */
    private void drawForgeScreen(GraphicsContext gc, Game game, double mapWidth, double mapHeight) {
        gc.setFill(OVERLAY);
        gc.fillRect(0, 0, mapWidth, mapHeight);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.setFont(titleFont);
        gc.setFill(GOLD_TEXT);
        gc.fillText("DEMIRCI", mapWidth / 2, mapHeight / 2 - 190);

        gc.setFont(hudFont);
        gc.setFill(HUD_TEXT);
        gc.fillText("Kesende " + game.getGold() + " altin var.", mapWidth / 2, mapHeight / 2 - 148);

        Player player = game.getPlayer();
        Weapon weapon = player.getEquippedWeapon();
        Armor armor = player.getEquippedArmor();
        double y = mapHeight / 2 - 112;

        y = drawForgeRow(gc, game, mapWidth, y, "1", "Silahi tamir et", weapon, false);
        y = drawForgeRow(gc, game, mapWidth, y, "2", "Zirhi tamir et", armor, false);
        y = drawForgeRow(gc, game, mapWidth, y, "3", "Silahi yukselt", weapon, true);
        y = drawForgeRow(gc, game, mapWidth, y, "4", "Zirhi yukselt", armor, true);

        y += 10;
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(HUD_ACCENT);
        gc.fillText("— BUYULER (her parcada bir tane durur) —", mapWidth / 2, y);
        y += 26;

        y = drawEnchantRows(gc, game, mapWidth, y, "Kilica", weapon, 5);
        y = drawEnchantRows(gc, game, mapWidth, y, "Zirha", armor, 7);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(HUD_TEXT);
        gc.fillText("Yukseltme tavani, bu katta bossun birakacagi parca kadar.",
                mapWidth / 2, y + 18);
        gc.setFill(HUD_ACCENT);
        gc.fillText("F ya da ESC: tezgahtan ayril", mapWidth / 2, y + 40);
    }

    /**
     * Bir parçanın büyü satırları.
     *
     * <p>Her büyünün kendi rakamı var, alt menü yok: dört büyü zaten ekrana
     * sığıyor ve oyuncunun "hangi menüdeydim" diye düşünmesi gerekmiyor. Şu an
     * takılı olan büyü işaretli, çünkü yeni büyü onun yerine geçiyor.</p>
     *
     * @param firstKey bu parçanın ilk büyüsüne düşen rakam
     * @return bir sonraki satırın y'si
     */
    private double drawEnchantRows(GraphicsContext gc, Game game, double mapWidth, double y,
                                   String owner, Equipment item, int firstKey) {
        List<Enchantment> options = item == null
                ? List.of(Enchantment.VAMPIRLIK, Enchantment.SAGLAMLIK)
                : item.availableEnchantments();

        for (int i = 0; i < options.size(); i++) {
            Enchantment option = options.get(i);
            boolean active = item != null && item.getEnchantment() == option;
            boolean available = item != null && !active;
            boolean affordable = available && game.getGold() >= option.getCost();

            gc.setTextAlign(TextAlignment.RIGHT);
            gc.setFill(available ? GOLD_TEXT : SLOT_NUMBER);
            gc.fillText(String.valueOf(firstKey + i), mapWidth / 2 - 300, y);

            gc.setTextAlign(TextAlignment.LEFT);
            gc.setFill(available ? MESSAGE_TEXT : MESSAGE_FADED);
            gc.fillText(owner + ": " + option.getLabel(), mapWidth / 2 - 285, y);

            gc.setFill(active ? GOLD_TEXT : MESSAGE_FADED);
            gc.fillText(active ? "takili — " + option.getDescription() : option.getDescription(),
                    mapWidth / 2 - 60, y);

            gc.setTextAlign(TextAlignment.RIGHT);
            gc.setFill(affordable ? GOLD_TEXT : MESSAGE_FADED);
            gc.fillText(item == null ? "—" : option.getCost() + " altin", mapWidth / 2 + 300, y);

            y += 26;
        }
        return y;
    }

    /**
     * Tezgâhta tek bir satır.
     *
     * @param upgrade tamir mi yükseltme mi; fiyat ve engel gerekçesi buna göre
     * @return bir sonraki satırın y'si
     */
    private double drawForgeRow(GraphicsContext gc, Game game, double mapWidth, double y,
                                String key, String label, Equipment item, boolean upgrade) {
        String detail;
        String cost;
        boolean available;

        if (item == null) {
            detail = "kusanilmis parca yok";
            cost = "—";
            available = false;
        } else if (upgrade) {
            available = item.canUpgrade(game.getDepth());
            detail = item.getFullName() + "  +" + item.getBonus()
                    + (available ? "" : "  (tavan)");
            cost = available ? Forge.upgradeCost(item) + " altin" : "—";
        } else {
            available = item.needsRepair();
            detail = item.getFullName() + "  " + item.getDurability() + "/"
                    + item.getMaxDurability() + (item.isBroken() ? "  KIRIK" : "");
            cost = available ? Forge.repairCost(item) + " altin" : "saglam";
        }

        boolean affordable = available && item != null
                && game.getGold() >= (upgrade ? Forge.upgradeCost(item) : Forge.repairCost(item));

        gc.setTextAlign(TextAlignment.RIGHT);
        gc.setFill(available ? GOLD_TEXT : SLOT_NUMBER);
        gc.fillText(key, mapWidth / 2 - 300, y);

        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFill(available ? MESSAGE_TEXT : MESSAGE_FADED);
        gc.fillText(label, mapWidth / 2 - 285, y);
        gc.setFill(MESSAGE_FADED);
        gc.fillText(detail, mapWidth / 2 - 60, y);

        gc.setTextAlign(TextAlignment.RIGHT);
        gc.setFill(affordable ? GOLD_TEXT : MESSAGE_FADED);
        gc.fillText(cost, mapWidth / 2 + 300, y);

        return y + 30;
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
