package com.cryptdelver.ui;

import com.cryptdelver.entity.Armor;
import com.cryptdelver.entity.Enchantment;
import com.cryptdelver.entity.Enemy;
import com.cryptdelver.entity.Entity;
import com.cryptdelver.entity.Equipment;
import com.cryptdelver.entity.Item;
import com.cryptdelver.entity.Player;
import com.cryptdelver.entity.Weapon;
import com.cryptdelver.entity.Wizard;
import com.cryptdelver.game.FloorTheme;
import com.cryptdelver.game.Forge;
import com.cryptdelver.game.Game;
import com.cryptdelver.game.Inventory;
import com.cryptdelver.game.Records;
import com.cryptdelver.game.Settings;
import com.cryptdelver.world.Dungeon;
import com.cryptdelver.world.DungeonGenerator;
import com.cryptdelver.world.Tile;
import com.cryptdelver.world.Vision;
import java.util.List;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
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

    /** Menü perdesi oyun perdesinden daha kapalı: menü ön planda.  */
    private static final Color MENU_BACKDROP = Color.web("#0b0b10", 0.92);

    /**
     * Hatırlanan ama görünmeyen karelerin üstündeki perde.
     *
     * <p>Yüzde kırk: gezdiğin yerin şeklini, merdiveni, koridorun nereye
     * gittiğini rahatça okuyabiliyorsun. Daha koyusunda harita "açılmış" gibi
     * durmuyordu, daha açığında ise ışık altında olmakla olmamak arasındaki
     * fark kayboluyor.</p>
     */
    private static final Color FORGOTTEN_VEIL = Color.web("#05050a", 0.40);

    /**
     * Işığın hiç sönmediği çekirdek: yarıçapın bu oranına kadar tam aydınlık.
     *
     * <p>Sıfırdan başlatınca oyuncunun hemen dibi bile hafif karanlık
     * oluyordu; yürüdüğün karenin net görünmesi gerekiyor. Kalan yüzde otuz
     * beşlik dilimde ışık yumuşakça sönüyor.</p>
     */
    private static final double LIGHT_CORE = 0.65;

    /** Menü çerçevesinin ve satırlarının genişliği. */
    private static final double MENU_FRAME_WIDTH = 620;
    private static final double MENU_ROW_WIDTH = 460;
    private static final double MENU_ROW_HEIGHT = 34;

    /** Tezgâh satırının tıklanabilir alanı; satırın tamamını kaplıyor. */
    private static final double FORGE_ROW_WIDTH = 620;
    private static final double FORGE_ROW_HEIGHT = 26;
    private static final Color OVERLAY_TITLE = Color.web("#c9564f");
    private static final Color DURABILITY_FULL = Color.web("#6f9a5a");

    /** Etkin iksir rozetlerinin renkleri. */
    private static final Color HASTE_BADGE = Color.web("#6fd0a0");
    private static final Color FURY_BADGE = Color.web("#e8a24a");

    /** Dayanıklılık bunun altına düşünce çubuk sarıya döner. */
    private static final double DURABILITY_WARNING = 0.35;

    /** Büyücünün ayağının dibindeki ocak ışığı. */
    private static final Color FORGE_GLOW = Color.web("#ff8a3d");
    private static final int FORGE_GLOW_RINGS = 3;
    private static final long FORGE_PULSE_MILLIS = 1600;

    /** Büyülü parçaların çevresindeki parıltı. */
    private static final Color ENCHANT_GLOW = Color.web("#c07cff");
    private static final int ENCHANT_GLOW_RINGS = 4;
    private static final long ENCHANT_PULSE_MILLIS = 1400;

    /** Silahı saran halenin yayılma yarıçapı, piksel. */
    private static final double ENCHANT_AURA_RADIUS = 22;

    /** Çantadaki büyülü eşyanın ikonunu saran halenin yarıçapı. */
    private static final double ENCHANT_SLOT_RADIUS = 12;

    /** Büyülü zırhın gövdeyi saran halesi; kılıcınkinden geniş. */
    private static final double ENCHANT_BODY_RADIUS = 26;

    /**
     * Tezgâhta büyülere düşen tuşlar.
     *
     * <p>Rakamlar tamir ve yükseltmede tükendi. Harflere geçerken klavyedeki
     * yerleşim işe koşuldu: üst sıra ({@code Q W E R}) kılıcın, ana sıra
     * ({@code A S D F}) zırhın. Ekrandaki iki satır grubu da aynı düzende, yani
     * elin nereye gideceğini görüntü söylüyor.</p>
     */
    private static final String[] WEAPON_ENCHANT_KEYS = {"Q", "W", "E", "R"};
    private static final String[] ARMOR_ENCHANT_KEYS = {"A", "S", "D", "F"};

    private final SpriteRegistry sprites = new SpriteRegistry();
    private final ColorAdjust hitEffect = new ColorAdjust(0, -0.6, 0.7, 0);

    /** Kırık parçanın soluk görünümü: rengi çekilmiş ve kararmış. */
    private final ColorAdjust brokenEffect = new ColorAdjust(0, -0.85, -0.35, 0);
    private final Font hudFont = Font.font("Consolas", 13);
    private final Font slotFont = Font.font("Consolas", 10);
    private final Font titleFont = Font.font("Consolas", 46);
    private final Font menuFont = Font.font("Consolas", 20);

    /** Yazı genişliği ölçmek için tutulan görünmez düğüm; {@link #measure} kullanıyor. */
    private final Text textMeasure = new Text();

    /** Büyülü parçaları saran hale; her karede nefesine göre güncelleniyor. */
    private final DropShadow enchantAura = new DropShadow(ENCHANT_AURA_RADIUS, ENCHANT_GLOW);

    /** O karede ekranda duran tıklanabilir bölgeler. */
    private final ClickMap clicks = new ClickMap();

    /**
     * Her şeyi çizer; menü açıksa onu da oyunun üstüne koyar.
     *
     * @param menu başlangıç menüsü; {@code null} verilebilir (menüsüz çizim)
     */
    public void render(GraphicsContext gc, Game game, StartMenu menu, Records records) {
        // Tıklanabilir bölgeler her karede sıfırdan kuruluyor: ekranda ne
        // varsa tıklanabilir olan da odur.
        clicks.clear();

        render(gc, game);

        if (menu != null && menu.isOpen()) {
            Dungeon dungeon = game.getDungeon();
            drawStartMenu(gc, menu, game, records, dungeon.getWidth() * (double) TILE_SIZE,
                    dungeon.getHeight() * (double) TILE_SIZE);
        }
    }

    /** Fareyle tıklanabilir bölgeler; {@link GameScreen} buradan soruyor. */
    public ClickMap getClicks() {
        return clicks;
    }

    /**
     * Başlangıç menüsü.
     *
     * <p>Fonu oyunun kendisi: arkada birinci kat duruyor, üstüne koyu bir perde
     * ve başlık geliyor. Boş siyah bir ekran yerine oyunu göstermek, menüyü
     * oyunun bir parçası gibi hissettiriyor.</p>
     */
    private void drawStartMenu(GraphicsContext gc, StartMenu menu, Game game, Records records,
                               double mapWidth, double mapHeight) {
        gc.setFill(MENU_BACKDROP);
        gc.fillRect(0, 0, mapWidth, mapHeight + HUD_HEIGHT);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);

        double centerX = mapWidth / 2;
        drawMenuFrame(gc, centerX, mapHeight);

        gc.setFont(titleFont);
        gc.setFill(GOLD_TEXT);
        gc.fillText("CRYPTDELVER", centerX, 120);

        gc.setFont(hudFont);
        gc.setFill(HUD_ACCENT);
        gc.fillText("Kripte in, ganimeti topla, Kript Lordunu gec.", centerX, 158);

        switch (menu.getPane()) {
            case MAIN -> {
                drawMainPane(gc, menu, centerX);
                drawRecords(gc, records, centerX, mapHeight);
            }
            case SETTINGS -> drawSettingsPane(gc, menu, game.getSettings(), centerX);
            case HELP -> drawHelpPane(gc, mapWidth);
        }

        gc.setFont(hudFont);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(HUD_TEXT);
        gc.fillText(menu.getPane() == StartMenu.Pane.MAIN
                        ? "Yon tuslariyla sec, Enter ile onayla"
                        : "Yon tuslariyla degistir, ESC ile geri don",
                centerX, mapHeight - 60);
    }

    /**
     * Menünün altındaki rekor satırı.
     *
     * <p>Ölünce her şey siliniyordu ve geriye hiçbir kayıt kalmıyordu.
     * Kaybedilen bir koşunun da bir anlamı olsun diye: "15'i geçebildim" diye
     * bir hedef ancak önceki denemeyi hatırlarsan doğuyor.</p>
     *
     * <p>Hiç koşu yoksa hiçbir şey yazılmıyor — sıfırlarla dolu bir satır
     * yeni oyuncuya bir şey söylemez, yalnızca ekranı doldururdu.</p>
     */
    private void drawRecords(GraphicsContext gc, Records records, double centerX,
                             double mapHeight) {
        if (records == null || !records.hasAnyRun()) {
            return;
        }

        double y = mapHeight - 110;

        gc.setFont(hudFont);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(HUD_ACCENT);
        gc.fillText("— GECMIS —", centerX, y);

        gc.setFill(MESSAGE_TEXT);
        gc.fillText("En derin kat " + records.getDeepestFloor() + "/" + FloorTheme.MAX_DEPTH
                        + "   ·   En cok altin " + records.getMostGold(),
                centerX, y + 22);

        gc.setFill(records.getWins() > 0 ? GOLD_TEXT : MESSAGE_FADED);
        gc.fillText(records.getWins() > 0
                        ? records.getRuns() + " kosu, " + records.getWins() + " kez kurtuldun"
                        : records.getRuns() + " kosu, henuz kurtulamadin",
                centerX, y + 42);
    }

    /** Menüyü çerçeveleyen ince altın hat; ekranı bir "sayfa" gibi topluyor. */
    private void drawMenuFrame(GraphicsContext gc, double centerX, double mapHeight) {
        double width = MENU_FRAME_WIDTH;
        double top = 60;
        double height = mapHeight - 100;

        gc.setStroke(SLOT_BORDER);
        gc.setLineWidth(1);
        gc.strokeRoundRect(centerX - width / 2, top, width, height, 10, 10);

        // Başlığın altındaki ayraç; başlıkla listeyi ayırıyor.
        gc.setStroke(HUD_ACCENT);
        gc.strokeLine(centerX - width / 2 + 40, 180, centerX + width / 2 - 40, 180);
    }

    private void drawMainPane(GraphicsContext gc, StartMenu menu, double centerX) {
        List<StartMenu.Option> options = menu.getOptions();
        double y = 240;

        for (int i = 0; i < options.size(); i++) {
            StartMenu.Option option = options.get(i);
            boolean hovered = register(new UiAction.Menu(option), centerX, y + i * 44);

            drawMenuRow(gc, option.getLabel(), centerX, y + i * 44,
                    hovered || i == menu.getIndex());
        }
    }

    /**
     * Menü satırı boyutundaki bir bölgeyi tıklanabilir yapar.
     *
     * <p>Çizimle aynı koordinatlar kullanıldığı için kayıt ve görüntü asla
     * ayrı düşmüyor.</p>
     *
     * @return fare o an bu satırın üstündeyse {@code true}
     */
    private boolean register(UiAction action, double centerX, double centerY) {
        return clicks.add(action, centerX - MENU_ROW_WIDTH / 2, centerY - MENU_ROW_HEIGHT / 2,
                MENU_ROW_WIDTH, MENU_ROW_HEIGHT);
    }

    /**
     * Ayarlar sayfası: her satırda ad ve o anki değer.
     *
     * <p>Değerler {@code < ... >} işaretleri arasında: bir listeden seçildikleri
     * ve sağ/sol ile değiştikleri, ayrı bir açıklama yazmadan anlaşılıyor.</p>
     */
    private void drawSettingsPane(GraphicsContext gc, StartMenu menu, Settings settings,
                                  double centerX) {
        List<StartMenu.SettingRow> rows = menu.getSettingRows();
        double y = 240;

        for (int i = 0; i < rows.size(); i++) {
            StartMenu.SettingRow row = rows.get(i);
            double rowY = y + i * 44 + (row == StartMenu.SettingRow.BACK ? 12 : 0);
            boolean hovered = register(new UiAction.Setting(row), centerX, rowY);
            boolean selected = hovered || i == menu.getIndex();

            if (row == StartMenu.SettingRow.BACK) {
                drawMenuRow(gc, row.getLabel(), centerX, rowY, selected);
                continue;
            }

            drawSettingRow(gc, row.getLabel(), settingValue(row, settings), centerX, rowY,
                    selected);
        }

        gc.setFont(hudFont);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(MESSAGE_FADED);
        gc.fillText(difficultyHint(settings), centerX, y + rows.size() * 44 + 24);
    }

    /** Seçili zorluğun ne yaptığını tek satırda anlatır. */
    private String difficultyHint(Settings settings) {
        return switch (settings.getDifficulty()) {
            case KOLAY -> "Kolay: kat daha tenha, dusmanlar derinlikle yavas sertlesir.";
            case NORMAL -> "Normal: oyunun dengelendigi kademe.";
            case ZOR -> "Zor: kat kalabalik, dusmanlar derinlikle hizla sertlesir.";
        };
    }

    private String settingValue(StartMenu.SettingRow row, Settings settings) {
        return switch (row) {
            case VOLUME -> "%" + settings.getVolumePercent();
            case MUTE -> settings.isMuted() ? "Acik" : "Kapali";
            case DIFFICULTY -> settings.getDifficulty().getLabel();
            case AUTO_SAVE -> settings.isAutoSave() ? "Acik" : "Kapali";
            case BACK -> "";
        };
    }

    /** Ayar satırı: solda ad, sağda değer. */
    private void drawSettingRow(GraphicsContext gc, String label, String value,
                                double centerX, double centerY, boolean selected) {
        double width = MENU_ROW_WIDTH;
        double height = MENU_ROW_HEIGHT;

        if (selected) {
            gc.setFill(HINT_BACKGROUND);
            gc.fillRoundRect(centerX - width / 2, centerY - height / 2, width, height, 8, 8);
            gc.setStroke(GOLD_TEXT);
            gc.setLineWidth(1);
            gc.strokeRoundRect(centerX - width / 2, centerY - height / 2, width, height, 8, 8);
        }

        gc.setFont(menuFont);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFill(selected ? MESSAGE_TEXT : MESSAGE_FADED);
        gc.fillText(label, centerX - width / 2 + 22, centerY);

        gc.setTextAlign(TextAlignment.RIGHT);
        gc.setFill(selected ? GOLD_TEXT : HUD_TEXT);
        gc.fillText(selected ? "< " + value + " >" : value, centerX + width / 2 - 22, centerY);
    }

    /** Yardım sayfası: duraklatma perdesindeki tuş listesinin aynısı. */
    private void drawHelpPane(GraphicsContext gc, double mapWidth) {
        drawKeyList(gc, mapWidth, 230);
    }

    /** Menüde tek satır; seçili olan çerçeveli ve parlak. */
    private void drawMenuRow(GraphicsContext gc, String label, double centerX, double centerY,
                             boolean selected) {
        double width = MENU_ROW_WIDTH;
        double height = MENU_ROW_HEIGHT;

        if (selected) {
            gc.setFill(HINT_BACKGROUND);
            gc.fillRoundRect(centerX - width / 2, centerY - height / 2, width, height, 8, 8);
            gc.setStroke(GOLD_TEXT);
            gc.setLineWidth(1);
            gc.strokeRoundRect(centerX - width / 2, centerY - height / 2, width, height, 8, 8);
        }

        gc.setFont(menuFont);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(selected ? GOLD_TEXT : MESSAGE_FADED);
        gc.fillText(label, centerX, centerY);
    }

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

        Player player = game.getPlayer();
        Vision vision = game.getVision();
        drawDungeon(gc, dungeon, vision, game.isStairsLocked());
        drawThemeWash(gc, game.getTheme(), mapWidth, mapHeight);
        // Eşyalar gölgeden önce çiziliyor, çünkü onlar da hatırlanıyor: yerdeki
        // eşya kıpırdamıyor, dolayısıyla gördüğün zırhın nerede kaldığını
        // bilmen doğru. Karanlıkta kalan eşya perdenin altında soluk görünüyor
        // — "orada bir şey vardı" diyecek kadar.
        for (Item item : game.getGroundItems()) {
            if (vision.isRemembered(item.getTileX(), item.getTileY())) {
                drawEntity(gc, item, GROUND_ITEM_SCALE);
            }
        }

        drawShadows(gc, dungeon, vision, player, mapWidth, mapHeight);

        // Silah varsa savuruşu kılıcın kendisi gösteriyor; çıplak elle
        // vururken de bir şey görünsün diye halka o durumda çiziliyor.
        if (player.isSwinging() && player.getEquippedWeapon() == null) {
            drawSwing(gc, player);
        }

        if (game.getWizard() != null && isSeen(vision, game.getWizard())) {
            drawForgeGlow(gc, game.getWizard());
            drawEntity(gc, game.getWizard(), 1.0);
            drawWizardSign(gc, game);
        }

        for (Enemy enemy : game.getEnemies()) {
            if (!isSeen(vision, enemy)) {
                continue;
            }
            drawEntity(gc, enemy, 1.0);
            drawHealthBar(gc, enemy);
        }
        drawPlayer(gc, player);
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

        if (game.isWon()) {
            drawVictory(gc, game, mapWidth, mapHeight);
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

        // Zorluk ve otomatik kaydetme burada yalnızca gösteriliyor. Oyunun
        // ortasında ok tuşlarıyla zorluk değiştirmek kolayca yanlışlıkla
        // yapılırdı; ikisi de menüdeki ayarlar sayfasından değişiyor.
        rowY += 24;
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.setFill(MESSAGE_TEXT);
        gc.fillText("Zorluk", mapWidth / 2 - 20, rowY);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFill(GOLD_TEXT);
        gc.fillText(settings.getDifficulty().getLabel(), mapWidth / 2 + 20, rowY);

        rowY += 20;
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.setFill(MESSAGE_TEXT);
        gc.fillText("Otomatik kaydetme", mapWidth / 2 - 20, rowY);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFill(settings.isAutoSave() ? GOLD_TEXT : HUD_TEXT);
        gc.fillText(settings.isAutoSave() ? "acik" : "kapali", mapWidth / 2 + 20, rowY);

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
                {"1-8 / tik", "cantadaki esyayi kullan / kusan"},
                {"Shift + 1-8 / tik", "esyayi yere birak"},
                {"E", "merdivende bir alt kata in"},
                {"F", "büyücünün yaninda tezgahi ac"},
                {"F5 / F9", "kaydet / yukle"},
                {"- / + / M", "ses azalt / artir / sustur"},
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
    /**
     * Varlık şu anda ışık altında mı.
     *
     * <p>Adım halindeki varlık iki karenin arasında; hangisine bakılacağı
     * belirsiz olmasın diye mantıksal karesine bakılıyor.</p>
     */
    private boolean isSeen(Vision vision, Entity entity) {
        return vision.isVisible(entity.getTileX(), entity.getTileY());
    }

    private void drawDungeon(GraphicsContext gc, Dungeon dungeon, Vision vision,
                             boolean stairsLocked) {
        Sprite floor = sprites.get("floor");
        Sprite wall = sprites.get("wall");
        Sprite stairs = sprites.get("stairs");

        for (int x = 0; x < dungeon.getWidth(); x++) {
            for (int y = 0; y < dungeon.getHeight(); y++) {
                // Hiç görülmemiş kare hiç çizilmiyor: arkasında ne olduğunu
                // bilmiyorsun, harita orada boş kalıyor.
                if (!vision.isRemembered(x, y)) {
                    continue;
                }

                double cx = x * TILE_SIZE + TILE_SIZE / 2.0;
                double cy = y * TILE_SIZE + TILE_SIZE / 2.0;

                Tile tile = dungeon.getTile(x, y);
                if (tile == Tile.WALL) {
                    wall.draw(gc, cx, cy, TILE_SIZE);
                } else {
                    floor.draw(gc, cx, cy, TILE_SIZE);
                    if (tile == Tile.STAIRS_DOWN) {
                        stairs.draw(gc, cx, cy, TILE_SIZE);
                        drawStairsFrame(gc, cx, cy, stairsLocked);
                    }
                }
            }
        }
    }

    /**
     * Gezdiğin ama şu an ışık altında olmayan kareleri hafifçe karartır.
     *
     * <p>Perde <em>bölge perdesinden sonra</em> çiziliyor. Önce karo başına
     * çiziliyordu ve bölge perdesi üstüne biniyordu: hatırlanan kareler iki kat
     * karartma alıp neredeyse siyaha düşüyordu, yani gezdiğin yer açılmış gibi
     * durmuyordu. Şimdi tek kat ve daha açık — haritayı okuyabiliyorsun, ama
     * neyin ışık altında olduğu hâlâ belli.</p>
     */
    private void drawShadows(GraphicsContext gc, Dungeon dungeon, Vision vision, Player player,
                             double mapWidth, double mapHeight) {
        // Işık, oyuncunun çizim konumunun etrafında yumuşak bir daire. Kare
        // başına tek bir karartma kullanınca görüş alanı basamak basamak
        // açılıyor ve fener değil ızgara gibi duruyordu; renk geçişi bunu
        // ortadan kaldırıyor ve oyuncu yürüdükçe daire onunla birlikte
        // kayıyor.
        double centerX = player.getRenderX() * TILE_SIZE + TILE_SIZE / 2.0;
        double centerY = player.getRenderY() * TILE_SIZE + TILE_SIZE / 2.0;
        double radius = Vision.RADIUS * (double) TILE_SIZE;

        // Yarıçapın dışında son durak rengi geçerli, yani uzak her yer eşit
        // koyulukta: karanlığın nerede bittiğini gösteren bir halka olmuyor.
        gc.setFill(new RadialGradient(0, 0, centerX, centerY, radius, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.TRANSPARENT),
                new Stop(LIGHT_CORE, Color.TRANSPARENT),
                new Stop(1.0, FORGOTTEN_VEIL)));
        gc.fillRect(0, 0, mapWidth, mapHeight);

        // Yarıçapın içinde ama duvarın arkasında kalan kareler ayrıca
        // karartılıyor: ışık halkası duvarları bilmiyor, köşenin arkasını
        // aydınlatmaması gerekiyor.
        gc.setFill(FORGOTTEN_VEIL);
        for (int x = 0; x < dungeon.getWidth(); x++) {
            for (int y = 0; y < dungeon.getHeight(); y++) {
                if (vision.isRemembered(x, y) && !vision.isVisible(x, y)) {
                    gc.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
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
    /**
     * Oyuncunun gövdesi; büyülü zırh varsa gövde de parlıyor.
     *
     * <p>Zırh ayrı bir parça olarak çizilmiyor — gövdenin kendisi. Dolayısıyla
     * zırhın büyüsü de gövdeyi sarmalı: kılıç nasıl parlıyorsa üstündeki zırh
     * da öyle. Kırık zırh parlamıyor; parlayan bir paçavra yanlış mesaj
     * verirdi.</p>
     */
    private void drawPlayer(GraphicsContext gc, Player player) {
        Armor armor = player.getEquippedArmor();
        boolean glowing = armor != null && armor.isEnchanted() && !armor.isBroken();

        if (glowing && !player.isFlashing()) {
            gc.setEffect(enchantAura(ENCHANT_BODY_RADIUS));
        }

        sprites.get(player.getSpriteName(), player.isMoving()).draw(
                gc,
                player.getRenderX() * TILE_SIZE,
                player.getRenderY() * TILE_SIZE,
                TILE_SIZE * player.getDrawScale());

        gc.setEffect(null);

        // Vuruş parlaması ayrı çiziliyor: hale efektiyle aynı anda
        // uygulanamıyor, ikisi de tek bir efekt yuvasını paylaşıyor.
        if (player.isFlashing()) {
            gc.setEffect(hitEffect);
            sprites.get(player.getSpriteName(), player.isMoving()).draw(
                    gc,
                    player.getRenderX() * TILE_SIZE,
                    player.getRenderY() * TILE_SIZE,
                    TILE_SIZE * player.getDrawScale());
            gc.setEffect(null);
        }
    }

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
     * tutulan silah" olarak hazırlanmış ve gövdeyle aynı üslupta. Zırh için
     * gövdeye bindirilecek çizim yok; o yüzden zırh ayrı bir katman değil,
     * gövdenin kendisi — {@link Player#getSpriteName()} kuşanılan zırha göre
     * tuniği boyanmış kareyi seçiyor.</p>
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

        // Kırık kılıç haritada da kırık görünüyor: rengi çekiliyor ve
        // kararıyor. Büyü halesi kırıkken görünmüyor — parlayan bir hurda
        // yanlış mesaj verirdi.
        if (weapon.isBroken()) {
            gc.setEffect(brokenEffect);
        } else if (weapon.isEnchanted()) {
            gc.setEffect(enchantAura(ENCHANT_AURA_RADIUS));
        }

        // Sprite tabana hizalı çizildiği için, merkezi yarım boy yukarı almak
        // sapı tam dönme merkezine oturtuyor.
        sprites.get(weapon.getSpriteName()).draw(gc, 0, -size / 2, size);

        gc.setEffect(null);
        gc.restore();
    }

    /**
     * Büyü halesi: çizilen şeklin çevresine yayılan mor ışık.
     *
     * <p>{@code DropShadow} gölge için düşünülmüş ama kaydırmayı sıfır bırakıp
     * rengi açık seçince tam da istediğimiz şeye dönüşüyor: şeklin dış hattını
     * saran bir parıltı. Tek nesne tutulup her karede nefesine göre
     * güncelleniyor.</p>
     */
    private DropShadow enchantAura(double radius) {
        double breath = enchantBreath();

        enchantAura.setRadius(radius * (0.7 + 0.3 * breath));
        enchantAura.setColor(Color.color(ENCHANT_GLOW.getRed(), ENCHANT_GLOW.getGreen(),
                ENCHANT_GLOW.getBlue(), 0.65 + 0.35 * breath));
        return enchantAura;
    }

    /** Büyü parıltısının nefesi: 0 ile 1 arasında gidip geliyor. */
    private double enchantBreath() {
        double phase = (System.currentTimeMillis() % ENCHANT_PULSE_MILLIS)
                / (double) ENCHANT_PULSE_MILLIS;
        return 0.5 + 0.5 * Math.sin(phase * 2 * Math.PI);
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
        gc.fillText("Kat " + game.getDepth() + "/" + FloorTheme.MAX_DEPTH, STATUS_PANEL_X, line);

        gc.setFill(HUD_TEXT);
        gc.fillText(String.format("Sure %.0fs", game.getElapsedSeconds()), STATUS_PANEL_X + 62, line);
        gc.fillText("Dusman " + game.getEnemies().size(), STATUS_PANEL_X + 150, line);

        drawActiveEffects(gc, game.getPlayer(), mapHeight);

        // Zindan uyandıysa kalıcı bir uyarı: takviyeler gelirken oyuncu
        // "neden birden kalabalıklaştı" diye düşünmesin.
        if (game.isDungeonAwake()) {
            gc.setTextAlign(TextAlignment.LEFT);
            gc.setFill(HP_TEXT);
            gc.fillText("ZINDAN UYANDI", STATUS_PANEL_X, mapHeight + 14);
        }
    }

    /**
     * Etkin iksirlerin kalan süresi, can çubuğunun sağında.
     *
     * <p>Süreli etki ekranda görünmezse oyuncu ne zaman bittiğini bilemez ve
     * hızın kesildiği anı ancak bir düşmana yakalanınca fark eder. İki kısa
     * rozet yetiyor: harf ve saniye.</p>
     */
    private void drawActiveEffects(GraphicsContext gc, Player player, double mapHeight) {
        double x = STATUS_PANEL_X + 186;
        double y = mapHeight + 36;

        if (player.isHasted()) {
            drawEffectBadge(gc, "H", player.getHasteRemaining(), HASTE_BADGE, x, y);
            x += 30;
        }
        if (player.isFurious()) {
            drawEffectBadge(gc, "O", player.getFuryRemaining(), FURY_BADGE, x, y);
        }
    }

    private void drawEffectBadge(GraphicsContext gc, String letter, double remaining,
                                 Color color, double x, double centerY) {
        gc.setFill(HP_BAR_BACKGROUND);
        gc.fillRoundRect(x, centerY - 9, 26, 18, 5, 5);
        gc.setStroke(color);
        gc.setLineWidth(1);
        gc.strokeRoundRect(x, centerY - 9, 26, 18, 5, 5);

        gc.setFont(slotFont);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(color);
        gc.fillText(letter + (int) Math.ceil(remaining), x + 13, centerY);
        gc.setFont(hudFont);
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

        // Kırık parça ayrıca yazıyla söyleniyor. Yalnızca kırmızı ad ve boş
        // çubuk yetmiyordu: oyuncu kırık kılıçla katlarca dolaşıp durumu fark
        // etmedi. "KIRIK" kelimesi gözden kaçmıyor.
        String label = item.isBroken()
                ? item.getFullName() + "  KIRIK"
                : item.getFullName();
        gc.fillText(label, INVENTORY_PANEL_X, centerY);

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

        // Burada eskiden zindan üreticisinin adı yazıyordu. Oyuncu üreticiyi
        // artık seçemediği için o bilgi ona bir şey söylemiyordu; yerini
        // bulunduğu bölgenin adı aldı.
        gc.setFill(HUD_ACCENT);
        gc.fillText(game.getTheme().getLabel() + " · " + game.getDepth() + "/"
                        + FloorTheme.MAX_DEPTH,
                mapWidth - 10, mapHeight + HUD_HEIGHT - 14);
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

            if (item != null && item.isEnchanted()) {
                drawEnchantGlow(gc, x, top, SLOT_SIZE, SLOT_SIZE);
            }

            gc.setStroke(equipped ? SLOT_EQUIPPED : SLOT_BORDER);
            gc.setLineWidth(equipped ? 2 : 1);
            gc.strokeRoundRect(x, top, SLOT_SIZE, SLOT_SIZE, 5, 5);

            if (clicks.add(new UiAction.Slot(slot), x, top, SLOT_SIZE, SLOT_SIZE)) {
                // Fare slotun üstündeyken çerçeve parlıyor: tıklanabilir
                // olduğu görüntüden anlaşılsın.
                gc.setStroke(GOLD_TEXT);
                gc.setLineWidth(2);
                gc.strokeRoundRect(x, top, SLOT_SIZE, SLOT_SIZE, 5, 5);
            }

            if (item != null) {
                if (item.isEnchanted()) {
                    gc.setEffect(enchantAura(ENCHANT_SLOT_RADIUS));
                }
                sprites.get(item.getSpriteName())
                        .draw(gc, x + SLOT_SIZE / 2.0, top + SLOT_SIZE / 2.0, SLOT_SIZE * 0.82);
                gc.setEffect(null);
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
        String hint;
        if (locked) {
            hint = "Merdiveni tutan seyi once yen";
        } else if (game.isFinalFloor()) {
            // Yirminci katın merdiveni aşağı değil dışarı çıkıyor.
            hint = "E ile kriptten cik";
        } else {
            hint = "E ile bir alt kata in";
        }

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
     * Büyücünün ayağının dibindeki ocak ışığı.
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
     * Büyücünün üstündeki isim etiketi ve konuşma balonu.
     *
     * <p>Etiket her zaman duruyor: haritada kim olduğunu uzaktan da anlamalısın.
     * Balon ise yalnızca yanına gidince açılıyor ve içindeki cümleyi büyücünün
     * kendisi seçiyor — takımın kırıksa onu söylüyor, sağlamsa başka bir şey.
     * Böylece balon hem "bu bir büyücü" diyor hem de işe yarıyor.</p>
     */
    private void drawWizardSign(GraphicsContext gc, Game game) {
        Wizard smith = game.getWizard();
        double x = smith.getRenderX() * TILE_SIZE + TILE_SIZE / 2.0;
        double top = smith.getRenderY() * TILE_SIZE;

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);

        drawNamePlate(gc, smith.getName(), x, top - 6);

        if (game.isNearWizard() && !game.isForgeOpen()) {
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

        // Balonun büyücüye bakan sivri ucu.
        gc.setFill(HINT_BACKGROUND);
        gc.fillPolygon(
                new double[] {centerX - 6, centerX + 6, centerX},
                new double[] {bottomY - 1, bottomY - 1, bottomY + 7}, 3);

        gc.setFill(MESSAGE_TEXT);
        gc.fillText(text, centerX, top + height / 2);
    }

    /**
     * Büyülü parçanın altındaki nefes alan parıltı.
     *
     * <p>Büyü sayılara dokunmuyor, sadece davranışa; o yüzden hangi parçanın
     * büyülü olduğunu ada bakmadan da görebilmek gerekiyordu. Parıltı içten
     * dışa üç katman, hepsi mor: çantadaki altın çerçeve "kuşanılmış",
     * mor parıltı "büyülü" demek — iki bilgi birbirine karışmıyor.</p>
     */
    private void drawEnchantGlow(GraphicsContext gc, double x, double y,
                                 double width, double height) {
        double breath = enchantBreath();

        for (int ring = ENCHANT_GLOW_RINGS; ring >= 1; ring--) {
            double spread = ring * 2.5 * (0.85 + 0.15 * breath);
            double alpha = 0.22 / ring * (0.6 + 0.4 * breath);

            gc.setFill(Color.color(ENCHANT_GLOW.getRed(), ENCHANT_GLOW.getGreen(),
                    ENCHANT_GLOW.getBlue(), alpha));
            gc.fillRoundRect(x - spread, y - spread,
                    width + spread * 2, height + spread * 2, 8, 8);
        }
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
     * Büyücü tezgâhı.
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
        gc.fillText("BUYUCU", mapWidth / 2, mapHeight / 2 - 190);

        gc.setFont(hudFont);
        gc.setFill(HUD_TEXT);
        gc.fillText("Kesende " + game.getGold() + " altin var.", mapWidth / 2, mapHeight / 2 - 148);

        Player player = game.getPlayer();
        Weapon weapon = player.getEquippedWeapon();
        Armor armor = player.getEquippedArmor();
        double y = mapHeight / 2 - 112;

        y = drawForgeRow(gc, game, mapWidth, y, "1", "Silahi tamir et", weapon, false,
                UiAction.Bench.REPAIR_WEAPON);
        y = drawForgeRow(gc, game, mapWidth, y, "2", "Zirhi tamir et", armor, false,
                UiAction.Bench.REPAIR_ARMOR);
        y = drawForgeRow(gc, game, mapWidth, y, "3", "Silahi yukselt", weapon, true,
                UiAction.Bench.UPGRADE_WEAPON);
        y = drawForgeRow(gc, game, mapWidth, y, "4", "Zirhi yukselt", armor, true,
                UiAction.Bench.UPGRADE_ARMOR);

        y += 10;
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(HUD_ACCENT);
        gc.fillText("— BUYULER (her parcada bir tane durur) —", mapWidth / 2, y);
        y += 26;

        y = drawEnchantRows(gc, game, mapWidth, y, "Kilica", weapon, WEAPON_ENCHANT_KEYS);
        y = drawEnchantRows(gc, game, mapWidth, y, "Zirha", armor, ARMOR_ENCHANT_KEYS);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(HUD_TEXT);
        gc.fillText("Yukseltme tavani, bu katta bossun birakacagi parca kadar.",
                mapWidth / 2, y + 18);
        gc.setFill(HUD_ACCENT);
        gc.fillText("ESC ile tezgahtan ayril", mapWidth / 2, y + 40);
    }

    /**
     * Bir parçanın büyü satırları.
     *
     * <p>Her büyünün kendi rakamı var, alt menü yok: dört büyü zaten ekrana
     * sığıyor ve oyuncunun "hangi menüdeydim" diye düşünmesi gerekmiyor. Şu an
     * takılı olan büyü işaretli, çünkü yeni büyü onun yerine geçiyor.</p>
     *
     * @param keys bu parçanın büyülerine düşen tuşlar, sırasıyla
     * @return bir sonraki satırın y'si
     */
    private double drawEnchantRows(GraphicsContext gc, Game game, double mapWidth, double y,
                                   String owner, Equipment item, String[] keys) {
        List<Enchantment> options = item == null
                ? List.of()
                : item.availableEnchantments();

        for (int i = 0; i < options.size() && i < keys.length; i++) {
            Enchantment option = options.get(i);
            boolean active = item != null && item.hasEnchantment(option);
            boolean available = item != null && !active;
            boolean affordable = available && game.getGold() >= option.getCost();

            boolean onWeapon = item instanceof Weapon;
            if (registerForgeRow(new UiAction.Enchant(onWeapon, option), mapWidth, y)) {
                drawForgeHighlight(gc, mapWidth, y);
            }

            gc.setTextAlign(TextAlignment.RIGHT);
            gc.setFill(available ? GOLD_TEXT : SLOT_NUMBER);
            gc.fillText(keys[i], mapWidth / 2 - 300, y);

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
                                String key, String label, Equipment item, boolean upgrade,
                                UiAction.Bench bench) {
        boolean hovered = registerForgeRow(new UiAction.Forge(bench), mapWidth, y);
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

        if (hovered) {
            drawForgeHighlight(gc, mapWidth, y);
        }

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

    /**
     * Tezgâh satırını tıklanabilir yapar.
     *
     * <p>Bölge satırın tamamını kaplıyor: rakamın da yazının da fiyatın da
     * üstüne tıklamak aynı işi yapıyor, "nereye basmam gerekiyor" diye
     * düşünmek gerekmiyor.</p>
     */
    private boolean registerForgeRow(UiAction action, double mapWidth, double y) {
        return clicks.add(action, mapWidth / 2 - FORGE_ROW_WIDTH / 2, y - FORGE_ROW_HEIGHT / 2,
                FORGE_ROW_WIDTH, FORGE_ROW_HEIGHT);
    }

    /** Üstüne gelinen tezgâh satırının arkasındaki soluk şerit. */
    private void drawForgeHighlight(GraphicsContext gc, double mapWidth, double y) {
        gc.setFill(HINT_BACKGROUND);
        gc.fillRoundRect(mapWidth / 2 - FORGE_ROW_WIDTH / 2, y - FORGE_ROW_HEIGHT / 2,
                FORGE_ROW_WIDTH, FORGE_ROW_HEIGHT, 6, 6);
    }

    /**
     * Bölgenin rengini haritanın üstüne ince bir perde olarak serer.
     *
     * <p>Karo çizimlerini boyamak denenebilirdi ama taş neredeyse gri:
     * doygunluğu artırmak griyi renklendirmiyor, ton kaydırmanın da
     * kaydıracağı renk yok. Perde ise işe yarıyor.</p>
     *
     * <p>Perde <em>varlıklardan önce</em> çiziliyor: oyuncu, düşmanlar ve
     * eşyalar üstünde kalıyor, yani zemin renk değiştirirken okunaklılık
     * bozulmuyor.</p>
     */
    private void drawThemeWash(GraphicsContext gc, FloorTheme theme,
                               double mapWidth, double mapHeight) {
        gc.setFill(Color.web(theme.getTint(), theme.getTintAlpha()));
        gc.fillRect(0, 0, mapWidth, mapHeight);
    }

    /**
     * Zafer perdesi: yirminci katı geçince.
     *
     * <p>Ölüm ekranıyla aynı yapıda ama tersi bir renkte — kırmızı yerine
     * altın. Aynı yapıyı kullanmak bilinçli: oyuncu ekranın nerede ne
     * yazdığını zaten biliyor, öğrenecek yeni bir şey yok.</p>
     */
    private void drawVictory(GraphicsContext gc, Game game, double mapWidth, double mapHeight) {
        gc.setFill(OVERLAY);
        gc.fillRect(0, 0, mapWidth, mapHeight);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);

        gc.setFont(titleFont);
        gc.setFill(GOLD_TEXT);
        gc.fillText("KURTULDUN", mapWidth / 2, mapHeight / 2 - 60);

        gc.setFont(menuFont);
        gc.setFill(MESSAGE_TEXT);
        gc.fillText("Yirmi kat indin ve geri dondun.", mapWidth / 2, mapHeight / 2 - 10);

        gc.setFont(hudFont);
        gc.setFill(HUD_ACCENT);
        gc.fillText(String.format("%d altin  ·  %.0f saniye", game.getGold(),
                game.getElapsedSeconds()), mapWidth / 2, mapHeight / 2 + 26);

        gc.setFill(HUD_TEXT);
        gc.fillText("Enter ile yeniden basla", mapWidth / 2, mapHeight / 2 + 58);
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
