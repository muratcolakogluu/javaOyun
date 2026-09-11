package com.cryptdelver.ui;

import com.cryptdelver.entity.Armor;
import com.cryptdelver.entity.Bogucu;
import com.cryptdelver.entity.Enchantment;
import com.cryptdelver.entity.Enemy;
import com.cryptdelver.entity.Entity;
import com.cryptdelver.entity.Equipment;
import com.cryptdelver.entity.Item;
import com.cryptdelver.entity.Player;
import com.cryptdelver.entity.Projectile;
import com.cryptdelver.entity.Seytan;
import com.cryptdelver.entity.Weapon;
import com.cryptdelver.entity.Wizard;
import com.cryptdelver.game.FloorTheme;
import com.cryptdelver.game.Forge;
import com.cryptdelver.game.Game;
import com.cryptdelver.game.Inventory;
import com.cryptdelver.game.MessageLog;
import com.cryptdelver.game.Records;
import com.cryptdelver.game.Settings;
import com.cryptdelver.game.Text;
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

    /** Kuşanılan parçanın yuvası; çanta slotundan bilerek büyük. */
    private static final int GEAR_SLOT_SIZE = 38;

    /**
     * Bir kalp kaç can.
     *
     * <p>Dörtte karar kıldık: taban can 20, boss ödülleriyle 40'a çıkıyor. İkişer
     * canlık kalpler yirmi kalp demek olurdu ve şeride sığmazdı; dörder canla
     * beş ile on kalp arasında kalıyor, yani dizinin uzunluğu bir bakışta
     * okunabiliyor.</p>
     */
    private static final int HP_PER_HEART = 4;
    private static final double HEART_SIZE = 11;
    private static final double HEART_STEP = 13;

    /** Panel ayraçlarının solunda bıraktığımız boşluk. */
    private static final double PANEL_GAP = 14;

    /**
     * Panellerin sol kenarları.
     *
     * <p>Harita 40 kare, yani 1280 piksel geniş. Şerit beş sütuna bölündü:
     * karakter, çanta ve <em>ayrı ayrı</em> savaş, eşya, durum yazıları.
     * Sayılar sabit çünkü pencere boyutu da sabit; oranla hesaplamak burada
     * gereksiz karmaşa olurdu.</p>
     *
     * <p>Mesajlar tek sütundayken üçü birbirini kovalıyordu: iksiri içtiğini
     * görmek için dövüş satırlarının arasından okumak gerekiyordu. Artık her
     * sorunun kendi sütunu var ve göz nereye bakacağını biliyor.</p>
     */
    private static final double CHARACTER_PANEL_X = 10;
    private static final double INVENTORY_PANEL_X = 268;
    private static final double COMBAT_PANEL_X = 556;
    private static final double ITEM_PANEL_X = 796;
    private static final double STATUS_PANEL_X = 1036;

    private static final Color BACKGROUND = Color.web("#0d0d12");
    private static final Color STAIRS_EDGE = Color.web("#9a8fc0");
    private static final Color UP_STAIRS_EDGE = Color.web("#7fb08a");

    /** Küçük haritanın ölçeği ve renkleri. */
    private static final double MINIMAP_SCALE = 3;
    private static final double MINIMAP_MARGIN = 12;
    private static final Color MINIMAP_BACKDROP = Color.web("#0b0b10", 0.82);
    private static final Color MINIMAP_WALL = Color.web("#3a3a4c");
    private static final Color MINIMAP_FLOOR = Color.web("#7c7c92");
    private static final Color MINIMAP_PLAYER = Color.web("#e8c46a");
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
    /** Boss uyarilari: Bogucunun salvo bandi, Seytanin ofke halkasi. */
    private static final Color VOLLEY_TELL = Color.web("#c9564f");
    private static final Color ENRAGE_GLOW = Color.web("#ff5a3d");
    private static final int ENRAGE_RINGS = 3;
    private static final long ENRAGE_PULSE_MILLIS = 900;

    /** Ok gövdesi ve arkasindaki iz. */
    private static final Color ARROW_COLOR = Color.web("#e8d8a8");
    private static final Color ARROW_TRAIL = Color.web("#e8d8a8", 0.28);
    private static final double ARROW_LENGTH = 9;
    private static final double ARROW_HEAD = 4;
    private static final double ARROW_TRAIL_LENGTH = 22;

    private static final Color HEART_FULL = Color.web("#d4544c");
    private static final Color HEART_EMPTY = Color.web("#3a2a30");
    private static final Color OVERLAY = Color.web("#0d0d12", 0.78);

    /** Menü perdesi oyun perdesinden daha kapalı: menü ön planda.  */
    /**
     * Menunun arkasindaki perde.
     *
     * <p>Tam opak degil: altta duran zindan hafif bir doku birakiyor. Ama 0.92
     * fazla seffafti -- altta kalan oyun yazilari ("E ile bir ust kata cik")
     * menunun icinden okunuyordu ve ekran kirli gorunuyordu.</p>
     */
    private static final Color MENU_BACKDROP = Color.web("#0b0b10", 0.965);

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
    private static final double MENU_FRAME_WIDTH = 660;

    /** Cerceve tuvalin dort kenarindan bu kadar iceride; boylece kendisi ortali. */
    private static final double MENU_MARGIN = 44;

    /** Cerceve icindeki dikey ritim. */
    private static final double TITLE_OFFSET = 76;
    private static final double TAGLINE_OFFSET = 40;
    private static final double DIVIDER_OFFSET = 64;
    private static final double LIST_GAP = 28;
    private static final double HINT_INSET = 28;
    private static final double FOOTER_GAP = 34;

    /** Gecmis blogunun kapladigi yukseklik: baslik ve iki satir. */
    private static final double RECORDS_BLOCK = 96;

    /** Gecmis bloğundaki satır aralığı. */
    private static final double RECORDS_LINE = 22;

    /** Menü ve ayar satırlarının dikey aralığı. */
    private static final double MENU_ROW_SPACING = 44;

    /** Ayarlar sayfasındaki zorluk ipucunun kapladığı satır. */
    private static final double HINT_LINE = 26;

    /** Tus listesindeki satir sayisi ve araligi; yerlesim hesabi buna dayaniyor. */
    private static final int KEY_ROWS = 11;
    private static final double KEY_ROW_SPACING = 21;

    /** Baslik isigi: mesale gibi nefes aliyor. */
    private static final Color TITLE_GLOW = Color.web("#ff9a3d");
    private static final int TITLE_GLOW_RINGS = 3;
    private static final long TITLE_PULSE_MILLIS = 2600;

    /** Ayracin ustundeki karakterlerin boyu ve merkezden uzakligi. */
    private static final double CAST_SIZE = 44;
    private static final double CAST_SPREAD = 248;

    /** Cercevenin kose centiklerinin boyu. */
    private static final double CORNER_TICK = 16;
    private static final double MENU_ROW_WIDTH = 460;
    private static final double MENU_ROW_HEIGHT = 34;

    /** Ayar satirinda deger ve yon isaretlerinin sag kenardan uzakligi. */
    private static final double ARROW_RIGHT_INSET = 24;
    private static final double VALUE_INSET = 52;
    private static final double ARROW_LEFT_INSET = 168;

    /** Yon isaretinin tiklama karesi; harften genis, parmak degil fare icin bile. */
    private static final double ARROW_HIT = 30;

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
    private final javafx.scene.text.Text textMeasure = new javafx.scene.text.Text();

    /** Farenin üstünde durduğu çanta eşyası; balon bunun için çiziliyor. */
    private Item tooltipItem;
    private double tooltipX;
    private double tooltipY;

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
        double totalHeight = mapHeight + HUD_HEIGHT;

        gc.setFill(MENU_BACKDROP);
        gc.fillRect(0, 0, mapWidth, totalHeight);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);

        double centerX = mapWidth / 2;
        double frameTop = MENU_MARGIN;
        double frameBottom = totalHeight - MENU_MARGIN;

        drawMenuFrame(gc, centerX, frameTop, frameBottom);

        double titleY = frameTop + TITLE_OFFSET;
        double dividerY = titleY + DIVIDER_OFFSET;

        // Başlık bir meşale gibi yanıyor: menü, oyunun kendi ışığını taşısın.
        drawTitleGlow(gc, centerX, titleY);
        drawMenuCast(gc, centerX, titleY);

        gc.setFont(titleFont);
        gc.setFill(GOLD_TEXT);
        gc.fillText(Text.GAME_TITLE.get(), centerX, titleY);

        gc.setFont(hudFont);
        gc.setFill(HUD_ACCENT);
        gc.fillText(Text.GAME_TAGLINE.get(), centerX, titleY + TAGLINE_OFFSET);

        drawMenuDivider(gc, centerX, dividerY);

        // İpucu ve geçmiş alttan yukarı yerleşiyor, listeyse ikisinin arasında
        // kalan boşluğa ortalanıyor. Sabit koordinatlarla yazılmışken liste
        // üstte toplanıp altta kocaman bir boşluk bırakıyor, geçmiş de ipucuyla
        // üst üste biniyordu.
        double hintY = frameBottom - HINT_INSET;
        boolean showRecords = menu.getPane() == StartMenu.Pane.MAIN
                && records != null && records.hasAnyRun();

        double listBottom = showRecords ? hintY - RECORDS_BLOCK : hintY - FOOTER_GAP;
        double listTop = dividerY + LIST_GAP;

        switch (menu.getPane()) {
            case MAIN -> {
                drawMainPane(gc, menu, centerX, listTop, listBottom);
                if (showRecords) {
                    drawRecords(gc, records, centerX, hintY - FOOTER_GAP);
                }
            }
            case SETTINGS -> drawSettingsPane(gc, menu, game.getSettings(), centerX,
                    listTop, listBottom);
            case HELP -> drawHelpPane(gc, mapWidth, listTop, listBottom);
        }

        gc.setFont(hudFont);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(HUD_TEXT);
        gc.fillText(menu.getPane() == StartMenu.Pane.MAIN
                        ? Text.MENU_HINT.get()
                        : Text.SETTINGS_HINT.get(),
                centerX, hintY);
    }

    /**
     * Satır listesinin ilk satırının y'si.
     *
     * <p>Liste, kendisine ayrılan boşluğa <em>ortalanıyor</em>. Böylece dört
     * satırlık ana sayfa ile altı satırlık ayarlar sayfası aynı dengede
     * duruyor; sabit bir başlangıç noktası ikisinden birini mutlaka yukarı
     * yapıştırırdı.</p>
     */
    private double centeredListStart(double top, double bottom, int rows, double spacing) {
        double height = (rows - 1) * spacing;
        return top + (bottom - top - height) / 2;
    }

    /**
     * Başlığın arkasındaki sıcak ışık.
     *
     * <p>Menü, oyunun geri kalanıyla aynı dili konuşsun diye: aynı nefes alan
     * halka büyücünün ocağında ve öfkelenen bossun çevresinde de var. Sabit bir
     * parlaklık dekor gibi kalırdı; kıpırdayınca meşale oluyor.</p>
     */
    private void drawTitleGlow(GraphicsContext gc, double centerX, double centerY) {
        double phase = (System.currentTimeMillis() % TITLE_PULSE_MILLIS)
                / (double) TITLE_PULSE_MILLIS;
        double breath = 0.5 + 0.5 * Math.sin(phase * 2 * Math.PI);

        for (int ring = TITLE_GLOW_RINGS; ring >= 1; ring--) {
            double radiusX = 150.0 * ring * (0.94 + 0.06 * breath);
            double radiusY = 34.0 * ring * (0.94 + 0.06 * breath);
            double alpha = 0.085 / ring * (0.75 + 0.25 * breath);

            gc.setFill(Color.color(TITLE_GLOW.getRed(), TITLE_GLOW.getGreen(),
                    TITLE_GLOW.getBlue(), alpha));
            gc.fillOval(centerX - radiusX, centerY - radiusY, radiusX * 2, radiusY * 2);
        }
    }

    /**
     * Ayracın üstünde duran kadro: kâşif bir yanda, zindan öbür yanda.
     *
     * <p>Menü uzun süre yalnızca yazıydı ve oyunla hiçbir görsel bağı yoktu —
     * aynı yazı listesi başka bir oyunun menüsü de olabilirdi. Oyunun kendi
     * sprite'ları, ne oynayacağını daha ilk ekranda söylüyor. Hepsi canlı
     * çerçeveler olduğu için menü de kıpırdıyor.</p>
     */
    private void drawMenuCast(GraphicsContext gc, double centerX, double titleY) {
        sprites.get("player").draw(gc, centerX - CAST_SPREAD, titleY, CAST_SIZE);
        sprites.get("skeleton").draw(gc, centerX + CAST_SPREAD, titleY, CAST_SIZE);
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
                             double bottom) {
        // Blok alttan yukarı diziliyor: son satır verilen sınırda bitiyor, yani
        // ipucuyla arasındaki boşluk her zaman aynı.
        double y = bottom - 2 * RECORDS_LINE;

        gc.setFont(hudFont);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(HUD_ACCENT);
        gc.fillText(Text.RECORDS_TITLE.get(), centerX, y);

        gc.setFill(MESSAGE_TEXT);
        gc.fillText(Text.RECORDS_DEEPEST.get(records.getDeepestFloor())
                        + "   ·   " + Text.RECORDS_GOLD.get(records.getMostGold()),
                centerX, y + RECORDS_LINE);

        gc.setFill(records.getWins() > 0 ? GOLD_TEXT : MESSAGE_FADED);
        gc.fillText(records.getWins() > 0
                        ? Text.RECORDS_RUNS.get(records.getRuns(), records.getWins())
                        : Text.RECORDS_NO_WIN.get(records.getRuns()),
                centerX, y + 2 * RECORDS_LINE);
    }

    /** Menüyü çerçeveleyen ince altın hat; ekranı bir "sayfa" gibi topluyor. */
    private void drawMenuFrame(GraphicsContext gc, double centerX, double top, double bottom) {
        double width = MENU_FRAME_WIDTH;
        double left = centerX - width / 2;

        gc.setStroke(SLOT_BORDER);
        gc.setLineWidth(1);
        gc.strokeRoundRect(left, top, width, bottom - top, 10, 10);

        // Köşe çentikleri: düz bir dikdörtgen pencere gibi duruyordu, çentikler
        // onu duvara asılı bir levhaya çeviriyor.
        drawFrameCorners(gc, left, top, width, bottom - top);
    }

    /** Başlıkla listeyi ayıran altın hat. */
    private void drawMenuDivider(GraphicsContext gc, double centerX, double y) {
        gc.setStroke(HUD_ACCENT);
        gc.setLineWidth(1);
        gc.strokeLine(centerX - MENU_FRAME_WIDTH / 2 + 46, y,
                centerX + MENU_FRAME_WIDTH / 2 - 46, y);
    }

    /** Çerçevenin dört köşesindeki kısa altın çentikler. */
    private void drawFrameCorners(GraphicsContext gc, double left, double top,
                                  double width, double height) {
        double right = left + width;
        double bottom = top + height;

        gc.setStroke(HUD_ACCENT);
        gc.setLineWidth(2);

        for (int corner = 0; corner < 4; corner++) {
            double x = (corner % 2 == 0) ? left : right;
            double y = (corner < 2) ? top : bottom;
            double towardsX = (corner % 2 == 0) ? CORNER_TICK : -CORNER_TICK;
            double towardsY = (corner < 2) ? CORNER_TICK : -CORNER_TICK;

            gc.strokeLine(x, y, x + towardsX, y);
            gc.strokeLine(x, y, x, y + towardsY);
        }
    }

    private void drawMainPane(GraphicsContext gc, StartMenu menu, double centerX,
                              double top, double bottom) {
        List<StartMenu.Option> options = menu.getOptions();
        double y = centeredListStart(top, bottom, options.size(), MENU_ROW_SPACING);

        for (int i = 0; i < options.size(); i++) {
            StartMenu.Option option = options.get(i);
            double rowY = y + i * MENU_ROW_SPACING;
            boolean hovered = register(new UiAction.Menu(option), centerX, rowY);

            drawMenuRow(gc, option.getLabel(), centerX, rowY, hovered || i == menu.getIndex());
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
                                  double centerX, double top, double bottom) {
        List<StartMenu.SettingRow> rows = menu.getSettingRows();

        // Zorluk ipucu da listeyle birlikte ortalanıyor: onu listenin dışında
        // bıraksaydık liste yukarı kayar, altında boşluk kalırdı.
        double y = centeredListStart(top, bottom - HINT_LINE, rows.size(), MENU_ROW_SPACING);

        for (int i = 0; i < rows.size(); i++) {
            StartMenu.SettingRow row = rows.get(i);
            double rowY = y + i * MENU_ROW_SPACING + (row == StartMenu.SettingRow.BACK ? 12 : 0);
            boolean hovered = register(new UiAction.Setting(row, 1), centerX, rowY);
            boolean selected = hovered || i == menu.getIndex();

            if (row == StartMenu.SettingRow.BACK) {
                drawMenuRow(gc, row.getLabel(), centerX, rowY, selected);
                continue;
            }

            drawSettingRow(gc, row, settingValue(row, settings), centerX, rowY, selected);
        }

        gc.setFont(hudFont);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(MESSAGE_FADED);
        gc.fillText(difficultyHint(settings), centerX,
                y + rows.size() * MENU_ROW_SPACING + 20);
    }

    /** Seçili zorluğun ne yaptığını tek satırda anlatır. */
    private String difficultyHint(Settings settings) {
        return switch (settings.getDifficulty()) {
            case KOLAY -> Text.DIFFICULTY_EASY_HINT.get();
            case NORMAL -> Text.DIFFICULTY_NORMAL_HINT.get();
            case ZOR -> Text.DIFFICULTY_HARD_HINT.get();
        };
    }

    private String settingValue(StartMenu.SettingRow row, Settings settings) {
        return switch (row) {
            case LANGUAGE -> settings.getLanguage().getLabel();
            case VOLUME -> "%" + settings.getVolumePercent();
            case MUSIC -> "%" + settings.getMusicPercent();
            case MUTE -> settings.isMuted() ? Text.ON.get() : Text.OFF.get();
            case DIFFICULTY -> settings.getDifficulty().getLabel();
            case BACK -> "";
        };
    }

    /** Ayar satırı: solda ad, sağda değer. */
    private void drawSettingRow(GraphicsContext gc, StartMenu.SettingRow row, String value,
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
        gc.fillText(row.getLabel(), centerX - width / 2 + 22, centerY);

        gc.setTextAlign(TextAlignment.RIGHT);
        gc.setFill(selected ? GOLD_TEXT : HUD_TEXT);
        gc.fillText(value, centerX + width / 2 - VALUE_INSET, centerY);

        // Oklar her zaman görünüyor, yalnızca seçiliyken değil: "bu satır iki
        // yöne de gidiyor" bilgisi, satırın üstüne gelmeden de okunmalı.
        drawSettingArrow(gc, row, -1, centerX + width / 2 - ARROW_LEFT_INSET, centerY, selected);
        drawSettingArrow(gc, row, 1, centerX + width / 2 - ARROW_RIGHT_INSET, centerY, selected);
    }

    /**
     * Ayar satırının iki ucundaki yön işareti.
     *
     * <p>Kendi tıklama bölgesini satırdan <em>sonra</em> kaydediyor: üst üste
     * binen bölgelerde sonra kaydedilen kazanıyor, yani oka basmak satıra
     * basmaktan farklı bir şey yapabiliyor.</p>
     */
    private void drawSettingArrow(GraphicsContext gc, StartMenu.SettingRow row, int step,
                                  double x, double centerY, boolean selected) {
        boolean hovered = clicks.add(new UiAction.Setting(row, step),
                x - ARROW_HIT / 2, centerY - ARROW_HIT / 2, ARROW_HIT, ARROW_HIT);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(hovered ? GOLD_TEXT : (selected ? SLOT_EQUIPPED : MESSAGE_FADED));
        gc.fillText(step < 0 ? "<" : ">", x, centerY);
    }

    /** Yardım sayfası: duraklatma perdesindeki tuş listesinin aynısı. */
    private void drawHelpPane(GraphicsContext gc, double mapWidth, double top, double bottom) {
        // Tuş listesi de kendi boşluğuna ortalanıyor; satır sayısı değişirse
        // liste yine ortada kalıyor.
        drawKeyList(gc, mapWidth, centeredListStart(top, bottom, KEY_ROWS + 1, KEY_ROW_SPACING));
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

        // Seçili satırın yanında bir kılıç: hangi satırda olduğun çerçeveden
        // önce buradan anlaşılıyor ve işaret oyunun kendi ikonundan geliyor.
        if (selected) {
            sprites.get("sword_steel").draw(gc, centerX - width / 2 + 26, centerY, 22);
        }
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
        drawThemeWash(gc, game.getTheme(), game.isCaveFloor(), mapWidth, mapHeight);
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
            // Uyarı gövdenin altına: üstüne binen çizimlerin nasıl durduğunu
            // daha önce gördük.
            drawBossTells(gc, enemy);
            drawEntity(gc, enemy, 1.0);
            drawHealthBar(gc, enemy);
        }
        drawPlayer(gc, player);
        drawEquipment(gc, player);

        // Oklar herkesin üstünde: uçan bir okun bir gövdenin arkasında
        // kaybolması, kaçınılabilir olmasının tek şartını yok ederdi.
        drawArrows(gc, game, vision);

        drawMinimap(gc, game, vision, mapWidth);

        if (game.getBoss() != null && !game.isOver()) {
            drawBossBar(gc, game, mapWidth);
        }

        if ((game.isPlayerOnStairs() || game.isPlayerOnUpStairs()) && !game.isOver()) {
            drawStairsHint(gc, game, mapWidth, mapHeight);
        }

        if (!game.isOver()) {
            drawInteractHint(gc, game, mapWidth, mapHeight);
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
        gc.fillText(Text.PAUSED.get(), mapWidth / 2, mapHeight / 2 - 175);

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
        gc.fillText(Text.SETTINGS_TITLE.get(), mapWidth / 2, top);

        double rowY = top + 28;
        drawLevelRow(gc, settings, Text.SETTING_EFFECTS.get(), settings.getVolume(),
                settings.getVolumePercent(), mapWidth, rowY);

        // Müzik kendi satırında: efektten ayrı bir seviye olmasının anlamı
        // ancak ayrı görünürse var.
        rowY += 22;
        drawLevelRow(gc, settings, Text.SETTING_MUSIC.get(), settings.getMusicVolume(),
                settings.getMusicPercent(), mapWidth, rowY);

        rowY += 20;
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(HUD_TEXT);
        gc.fillText(Text.PAUSE_SOUND_HINT.get(), mapWidth / 2, rowY);

        // Zorluk burada yalnızca gösteriliyor. Oyunun ortasında ok tuşlarıyla
        // zorluk değiştirmek kolayca yanlışlıkla yapılırdı; menüdeki ayarlar
        // sayfasından değişiyor.
        rowY += 24;
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.setFill(MESSAGE_TEXT);
        gc.fillText(Text.SETTING_DIFFICULTY.get(), mapWidth / 2 - 20, rowY);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFill(GOLD_TEXT);
        gc.fillText(settings.getDifficulty().getLabel(), mapWidth / 2 + 20, rowY);

        return rowY;
    }

    /** On kademeli ses çubuğu; sessizdeyken sönük çiziliyor. */
    /** Duraklatma perdesinde tek bir seviye satırı: ad, çubuk, yüzde. */
    private void drawLevelRow(GraphicsContext gc, Settings settings, String label,
                              double level, int percent, double mapWidth, double rowY) {
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.setFill(MESSAGE_TEXT);
        gc.fillText(label, mapWidth / 2 - 90, rowY);

        drawVolumeBar(gc, settings, level, mapWidth / 2 - 75, rowY);

        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFill(settings.isMuted() ? HUD_TEXT : GOLD_TEXT);
        gc.fillText(settings.isMuted() ? Text.OFF.get() : "%" + percent, mapWidth / 2 + 90, rowY);
    }

    private void drawVolumeBar(GraphicsContext gc, Settings settings, double level,
                              double left, double centerY) {
        int steps = (int) Math.round(1 / Settings.VOLUME_STEP);
        int filled = (int) Math.round(level * steps);

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
        Text[][] keys = {
                {Text.KEY_MOVE, Text.KEY_MOVE_WHAT},
                {Text.KEY_ATTACK, Text.KEY_ATTACK_WHAT},
                {Text.KEY_USE, Text.KEY_USE_WHAT},
                {Text.KEY_DROP, Text.KEY_DROP_WHAT},
                {Text.KEY_TAKE, Text.KEY_TAKE_WHAT},
                {Text.KEY_STAIRS, Text.KEY_STAIRS_WHAT},
                {Text.KEY_FORGE, Text.KEY_FORGE_WHAT},
                {null, Text.KEY_AUTOPICK_NOTE},
                {Text.KEY_VOLUME, Text.KEY_VOLUME_WHAT},
                {Text.KEY_RESTART, Text.KEY_RESTART_WHAT},
                {Text.KEY_PAUSE, Text.KEY_PAUSE_WHAT},
        };

        gc.setFont(hudFont);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(HUD_ACCENT);
        gc.fillText(Text.KEYS_TITLE.get(), mapWidth / 2, top);

        double y = top + KEY_ROW_SPACING + 5;
        for (Text[] row : keys) {
            gc.setTextAlign(TextAlignment.RIGHT);
            gc.setFill(HUD_ACCENT);
            gc.fillText(row[0] == null ? "" : row[0].get(), mapWidth / 2 - 15, y);

            gc.setTextAlign(TextAlignment.LEFT);
            gc.setFill(MESSAGE_TEXT);
            gc.fillText(row[1].get(), mapWidth / 2 + 15, y);
            y += KEY_ROW_SPACING;
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
                    } else if (tile == Tile.STAIRS_UP) {
                        // Aynı çizim, farklı çerçeve: yukarı çıkan merdiven
                        // soluk yeşil, aşağı inen mor. Renk tek başına
                        // hangisi olduğunu söylüyor.
                        stairs.draw(gc, cx, cy, TILE_SIZE);
                        drawUpStairsFrame(gc, cx, cy);
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

    /** Yukarı çıkan merdivenin çerçevesi; aşağı inenden ayrılsın diye yeşil. */
    private void drawUpStairsFrame(GraphicsContext gc, double cx, double cy) {
        gc.setStroke(UP_STAIRS_EDGE);
        gc.setLineWidth(1);
        gc.strokeRect(cx - TILE_SIZE / 2.0 + 2, cy - TILE_SIZE / 2.0 + 2,
                TILE_SIZE - 4, TILE_SIZE - 4);
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

        // Balon bütün paneller çizildikten sonra geliyor, yoksa sonraki panel
        // onun üstüne binerdi; hangi slotun anlatılacağı çizim sırasında
        // belirleniyor.
        tooltipItem = null;
        drawCharacterPanel(gc, game, mapHeight);
        drawInventoryPanel(gc, game, mapHeight);

        drawMessageColumn(gc, game, MessageLog.Channel.COMBAT, Text.PANEL_COMBAT.get(),
                COMBAT_PANEL_X, mapHeight);
        drawMessageColumn(gc, game, MessageLog.Channel.ITEM, Text.PANEL_ITEM.get(),
                ITEM_PANEL_X, mapHeight);
        drawMessageColumn(gc, game, MessageLog.Channel.STATUS, Text.PANEL_STATUS.get(),
                STATUS_PANEL_X, mapHeight);
        drawFooter(gc, game, mapWidth, mapHeight);

        drawPanelDivider(gc, INVENTORY_PANEL_X - PANEL_GAP, mapHeight);
        drawPanelDivider(gc, COMBAT_PANEL_X - PANEL_GAP, mapHeight);
        drawPanelDivider(gc, ITEM_PANEL_X - PANEL_GAP, mapHeight);
        drawPanelDivider(gc, STATUS_PANEL_X - PANEL_GAP, mapHeight);

        drawSlotTooltip(gc);
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

    /**
     * Sol panel: kuşandıkların, canın ve savaş değerlerin.
     *
     * <p>Zırhın ve silahın artık <b>kendi yuvalarında</b> duruyor, çantanın
     * yanında bir yazı satırı olarak değil. Kuşanılan parça çantadaki sekiz
     * slottan biriydi ve "hangisi üstümde" sorusunun cevabı ince bir sarı
     * çerçeveydi; oyuncu kırık zırhla katlarca dolaşabiliyordu. Ayrı yuva bunu
     * bir bakışta gösteriyor — Minecraft'ın zırh yuvaları da tam bu yüzden
     * çantadan ayrı.</p>
     *
     * <p>Can da aynı dilde: çubuk yerine kalp dizisi. Çubuk "yarısı gitti"
     * diyordu, kalpler "üç vuruş kaldı" diyor — dövüşün ortasında okunması
     * gereken şey bu.</p>
     */
    private void drawCharacterPanel(GraphicsContext gc, Game game, double mapHeight) {
        drawPanelTitle(gc, Text.PANEL_CHARACTER.get(), CHARACTER_PANEL_X, mapHeight);

        Player player = game.getPlayer();
        double slotTop = mapHeight + 22;

        drawGearSlot(gc, player.getEquippedArmor(), "Z", CHARACTER_PANEL_X, slotTop);
        drawGearSlot(gc, player.getEquippedWeapon(), "S",
                CHARACTER_PANEL_X + GEAR_SLOT_SIZE + SLOT_GAP, slotTop);

        double right = CHARACTER_PANEL_X + 2 * (GEAR_SLOT_SIZE + SLOT_GAP) + 8;
        drawHearts(gc, player, right, slotTop + 12);

        // Rozetler can sayisinin sagina siraliyor: kalplerin altinda ayri bir
        // satir acmak seride sigmiyordu.
        drawActiveEffects(gc, player, right + 62, slotTop + 28);

        gc.setTextAlign(TextAlignment.LEFT);
        double line = mapHeight + 72;

        gc.setFill(HUD_TEXT);
        gc.fillText(Text.HUD_ATTACK.get(player.getAttackPower()), CHARACTER_PANEL_X, line);
        gc.fillText(Text.HUD_DEFENSE.get(player.getDefense()), CHARACTER_PANEL_X + 76, line);

        gc.setFill(GOLD_TEXT);
        gc.fillText(Text.HUD_GOLD.get(game.getGold()), CHARACTER_PANEL_X + 136, line);

        gc.setFill(HUD_ACCENT);
        gc.fillText(Text.HUD_DEPTH.get(game.getDepth(), FloorTheme.MAX_DEPTH),
                CHARACTER_PANEL_X, line + 16);
        gc.setFill(HUD_TEXT);
        gc.fillText(Text.HUD_TIME.get(game.getElapsedSeconds()),
                CHARACTER_PANEL_X + 76, line + 16);
        gc.fillText(Text.HUD_ENEMIES.get(game.getEnemies().size()), CHARACTER_PANEL_X + 156, line + 16);

        // Zindan uyandıysa kalıcı bir uyarı: takviyeler gelirken oyuncu
        // "neden birden kalabalıklaştı" diye düşünmesin.
        if (game.isDungeonAwake()) {
            gc.setFont(slotFont);
            gc.setTextAlign(TextAlignment.RIGHT);
            gc.setFill(HP_TEXT);
            gc.fillText(Text.HUD_DUNGEON_AWAKE.get(), INVENTORY_PANEL_X - PANEL_GAP - 6, mapHeight + 14);
            gc.setFont(hudFont);
        }
    }

    /**
     * Kuşanılan tek bir parçanın yuvası: görseli, yıpranması, kırıklığı.
     *
     * <p>Boş yuva da çiziliyor — köşesindeki harf ("Z" zırh, "S" silah) orada
     * bir şey <em>olması gerektiğini</em> söylüyor. Boşluğu hiç göstermeseydik
     * zırhsız dolaşan oyuncu eksiği fark etmezdi.</p>
     */
    private void drawGearSlot(GraphicsContext gc, Equipment item, String letter,
                              double x, double top) {
        gc.setFill(SLOT_BACKGROUND);
        gc.fillRoundRect(x, top, GEAR_SLOT_SIZE, GEAR_SLOT_SIZE, 5, 5);

        if (item != null && item.isEnchanted()) {
            drawEnchantGlow(gc, x, top, GEAR_SLOT_SIZE, GEAR_SLOT_SIZE);
        }

        // Kırık parçanın çerçevesi kırmızı: yuvaya bakan gözün ilk gördüğü şey
        // parçanın işe yaramaz hâle geldiği olsun.
        gc.setStroke(item == null ? SLOT_BORDER : (item.isBroken() ? HP_TEXT : SLOT_EQUIPPED));
        gc.setLineWidth(item == null ? 1 : 2);
        gc.strokeRoundRect(x, top, GEAR_SLOT_SIZE, GEAR_SLOT_SIZE, 5, 5);

        if (item != null && clicks.isOver(x, top, GEAR_SLOT_SIZE, GEAR_SLOT_SIZE)) {
            tooltipItem = item;
            tooltipX = x + GEAR_SLOT_SIZE / 2.0;
            tooltipY = top;
        }

        gc.setFont(slotFont);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFill(SLOT_NUMBER);
        gc.fillText(letter, x + 3, top + 7);
        gc.setFont(hudFont);

        if (item == null) {
            return;
        }

        if (item.isEnchanted()) {
            gc.setEffect(enchantAura(ENCHANT_SLOT_RADIUS));
        }
        sprites.get(item.getSpriteName())
                .draw(gc, x + GEAR_SLOT_SIZE / 2.0, top + GEAR_SLOT_SIZE / 2.0 - 2,
                        GEAR_SLOT_SIZE * 0.72);
        gc.setEffect(null);

        // Dayanıklılık yuvanın dibinde ince bir şerit: sayı okumadan da
        // "tamire gitme vakti" görünüyor.
        double ratio = item.getDurability() / (double) item.getMaxDurability();
        double barY = top + GEAR_SLOT_SIZE - 6;
        gc.setFill(HP_BAR_BACKGROUND);
        gc.fillRect(x + 3, barY, GEAR_SLOT_SIZE - 6, 3);
        gc.setFill(durabilityColor(ratio));
        gc.fillRect(x + 3, barY, (GEAR_SLOT_SIZE - 6) * Math.max(0, ratio), 3);

        // Kırık parça ayrıca yazıyla söyleniyor. Kırmızı çerçeve ve boş çubuk
        // yetmiyordu: oyuncu kırık kılıçla katlarca dolaşıp durumu fark etmedi.
        // "KIRIK" kelimesi gözden kaçmıyor.
        if (item.isBroken()) {
            gc.setFont(slotFont);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setFill(HP_BAR_BACKGROUND);
            gc.fillRect(x, top + GEAR_SLOT_SIZE / 2.0 - 7, GEAR_SLOT_SIZE, 13);
            gc.setFill(HP_TEXT);
            gc.fillText(Text.GEAR_BROKEN.get(), x + GEAR_SLOT_SIZE / 2.0, top + GEAR_SLOT_SIZE / 2.0);
            gc.setFont(hudFont);
        }
    }

    /**
     * Can, kalp dizisi olarak.
     *
     * <p>Bir kalp {@value #HP_PER_HEART} can; azami can boss ödülleriyle
     * büyüdüğü için dizi de uzuyor, yani "canım arttı" ekranda görünüyor.
     * Yarım kalp de çiziliyor, yoksa tek canlık fark yuvarlanıp kaybolurdu.</p>
     */
    private void drawHearts(GraphicsContext gc, Player player, double x, double centerY) {
        int hearts = (int) Math.ceil(player.getMaxHp() / (double) HP_PER_HEART);
        double left = x;

        for (int i = 0; i < hearts; i++) {
            double filled = Math.max(0, Math.min(1,
                    (player.getHp() - i * (double) HP_PER_HEART) / HP_PER_HEART));
            drawHeart(gc, left + i * HEART_STEP, centerY, filled);
        }

        gc.setFont(slotFont);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFill(MESSAGE_TEXT);
        gc.fillText(player.getHp() + " / " + player.getMaxHp(), left, centerY + 16);
        gc.setFont(hudFont);
    }

    /** Tek bir kalp; {@code filled} 0 boş, 1 dolu, arası yarım. */
    private void drawHeart(GraphicsContext gc, double x, double centerY, double filled) {
        double size = HEART_SIZE;
        double top = centerY - size / 2;

        gc.setFill(HEART_EMPTY);
        fillHeart(gc, x, top, size);

        if (filled <= 0) {
            return;
        }

        // Kısmen dolu kalp soldan doluyor: kırpma alanı kalbin sol parçası.
        gc.save();
        gc.beginPath();
        gc.rect(x, top, size * filled, size);
        gc.clip();
        gc.setFill(HEART_FULL);
        fillHeart(gc, x, top, size);
        gc.restore();
    }

    /** Kalbin gövdesi: iki yay ve aşağı inen bir uç. */
    private void fillHeart(GraphicsContext gc, double x, double top, double size) {
        double half = size / 2;

        gc.fillOval(x, top, half + 1, half + 1);
        gc.fillOval(x + half - 1, top, half + 1, half + 1);

        gc.beginPath();
        gc.moveTo(x, top + half * 0.55);
        gc.lineTo(x + half, top + size);
        gc.lineTo(x + size, top + half * 0.55);
        gc.closePath();
        gc.fill();
    }

    /**
     * Etkin iksirlerin kalan süresi, kalplerin altında.
     *
     * <p>Süreli etki ekranda görünmezse oyuncu ne zaman bittiğini bilemez ve
     * hızın kesildiği anı ancak bir düşmana yakalanınca fark eder. İki kısa
     * rozet yetiyor: harf ve saniye.</p>
     */
    private void drawActiveEffects(GraphicsContext gc, Player player, double x, double y) {
        double left = x;

        if (player.isHasted()) {
            drawEffectBadge(gc, "H", player.getHasteRemaining(), HASTE_BADGE, left, y);
            left += 30;
        }
        if (player.isFurious()) {
            drawEffectBadge(gc, "O", player.getFuryRemaining(), FURY_BADGE, left, y);
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

    /**
     * Orta panel: çantadaki sekiz slot.
     *
     * <p>Kuşanılan parçaların satırları buradan çıktı: artık sol paneldeki
     * kendi yuvalarında duruyorlar. Aynı bilgiyi iki yerde göstermek, ikisinin
     * er geç ayrı düşmesi demekti — ve çantanın işi taşıdıkların, kuşandıkların
     * değil.</p>
     */
    private void drawInventoryPanel(GraphicsContext gc, Game game, double mapHeight) {
        drawPanelTitle(gc, Text.PANEL_BAG.get(), INVENTORY_PANEL_X, mapHeight);
        drawInventory(gc, game, INVENTORY_PANEL_X, mapHeight + 28);

        // Slotların altındaki tek satır: toplama tuşa bağlandığından beri
        // "eşyayla ne yapabilirim" sorusunun cevabı sürekli göz önünde dursun.
        gc.setFont(slotFont);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFill(SLOT_NUMBER);
        gc.fillText(Text.HUD_BAG_HINT.get(),
                INVENTORY_PANEL_X, mapHeight + 76);
        gc.setFont(hudFont);
    }

    /**
     * Farenin altındaki slotun ne olduğunu söyleyen balon.
     *
     * <p>Slotta yalnızca ikon vardı ve dört nadir eşyanın üçü aynı şekilde
     * şişe: hangisinin hız hangisinin öfke olduğu ezberdi. Balon adı ve ne
     * yaptığını söylüyor — ekipmanda bonusu ve dayanıklılığı da.</p>
     *
     * <p>Şeridin <em>üstüne</em> açılıyor, haritanın içine doğru: aşağı açsa
     * pencerenin dışına taşardı.</p>
     */
    private void drawSlotTooltip(GraphicsContext gc) {
        if (tooltipItem == null) {
            return;
        }

        String name = tooltipItem.getFullTooltipName();
        String detail = tooltipItem.getDescription();

        gc.setFont(hudFont);
        double width = Math.max(measure(name, hudFont), measure(detail, hudFont)) + 22;
        double height = detail.isEmpty() ? 26 : 42;
        double left = tooltipX - width / 2;
        double top = tooltipY - height - 8;

        gc.setFill(HINT_BACKGROUND);
        gc.fillRoundRect(left, top, width, height, 6, 6);
        gc.setStroke(SLOT_EQUIPPED);
        gc.setLineWidth(1);
        gc.strokeRoundRect(left, top, width, height, 6, 6);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.setFill(SLOT_EQUIPPED);
        gc.fillText(name, tooltipX, top + (detail.isEmpty() ? height / 2 : 15));

        if (!detail.isEmpty()) {
            gc.setFill(MESSAGE_TEXT);
            gc.fillText(detail, tooltipX, top + 31);
        }
    }

    /** Dolu yeşilimsi, azalmış sarı, kırık kırmızı. */
    private Color durabilityColor(double ratio) {
        if (ratio <= 0) {
            return HP_TEXT;
        }
        return ratio < DURABILITY_WARNING ? GOLD_TEXT : DURABILITY_FULL;
    }

    /**
     * Bir mesaj sütunu: tek bir kanalın son satırları.
     *
     * <p>Üç sütun da aynı koddan çiziliyor, farkları yalnızca kanal, başlık ve
     * sol kenar. Her sütunun kendi tarihçesi olduğu için kalabalık bir dövüş
     * artık "Altın topladın"ı ekrandan itemiyor.</p>
     */
    private void drawMessageColumn(GraphicsContext gc, Game game, MessageLog.Channel channel,
                                   String title, double x, double mapHeight) {
        drawPanelTitle(gc, title, x, mapHeight);

        gc.setTextAlign(TextAlignment.LEFT);
        List<MessageLog.Entry> recent = game.getMessageLog().latestEntries(channel, MESSAGE_LINES);
        double line = mapHeight + 36;

        for (int i = 0; i < recent.size(); i++) {
            MessageLog.Entry entry = recent.get(i);

            // Önemli olaylar gürültünün arasında renkle ayrılıyor; sıradan
            // satırlarda en yenisi parlak, eskiler soluk.
            if (entry.isImportant()) {
                gc.setFill(i == 0 ? GOLD_TEXT : SLOT_EQUIPPED.deriveColor(0, 1, 0.7, 1));
            } else {
                gc.setFill(i == 0 ? MESSAGE_TEXT : MESSAGE_FADED);
            }
            gc.fillText(entry.getDisplay(), x, line + i * 17);
        }
    }

    /** Şeridin sağ alt köşesi: bulunduğun bölge ve tek satırlık yardım ipucu. */
    private void drawFooter(GraphicsContext gc, Game game, double mapWidth, double mapHeight) {
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.setFill(HUD_ACCENT);
        gc.fillText(game.getTheme().getLabel(), mapWidth - 10, mapHeight + 14);

        gc.setFill(HUD_TEXT);
        gc.fillText(Text.HUD_ESC_HINT.get(), mapWidth - 10, mapHeight + HUD_HEIGHT - 10);
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

            boolean hovered = clicks.add(new UiAction.Slot(slot), x, top, SLOT_SIZE, SLOT_SIZE);
            if (hovered && item != null) {
                // Balon bütün slotlar çizildikten sonra çiziliyor, yoksa
                // sonraki slot onun üstüne binerdi.
                tooltipItem = item;
                tooltipX = x + SLOT_SIZE / 2.0;
                tooltipY = top;
            }

            if (hovered) {
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

    /**
     * Sağ üst köşede keşfedilmiş katın küçük haritası.
     *
     * <p>Görüş alanı ve geri dönüş geldikten sonra <b>yön bulmak gerçek bir iş
     * hâline geldi</b>: karanlıkta 40x22'lik bir katta "yukarı merdiven
     * neredeydi" diye dolaşmak, ikisinin birlikte yarattığı bir sürtünmeydi.
     * Küçük harita onu tasarlanmış bir şeye çeviriyor.</p>
     *
     * <p>Yalnızca gezdiğin yer çiziliyor — yani harita hile değil, hafıza.
     * Bilmediğin koridoru göstermiyor.</p>
     *
     * <p>Bilgi şeridine değil haritanın köşesine kondu: şerit zaten üç panelle
     * dolu ve küçük harita bakarken gözün oyunda kalmalı.</p>
     */
    private void drawMinimap(GraphicsContext gc, Game game, Vision vision, double mapWidth) {
        Dungeon dungeon = game.getDungeon();
        double width = dungeon.getWidth() * MINIMAP_SCALE;
        double height = dungeon.getHeight() * MINIMAP_SCALE;
        double left = mapWidth - width - MINIMAP_MARGIN;
        double top = MINIMAP_MARGIN;

        gc.setFill(MINIMAP_BACKDROP);
        gc.fillRoundRect(left - 4, top - 4, width + 8, height + 8, 5, 5);
        gc.setStroke(SLOT_BORDER);
        gc.setLineWidth(1);
        gc.strokeRoundRect(left - 4, top - 4, width + 8, height + 8, 5, 5);

        for (int x = 0; x < dungeon.getWidth(); x++) {
            for (int y = 0; y < dungeon.getHeight(); y++) {
                if (!vision.isRemembered(x, y)) {
                    continue;
                }

                gc.setFill(minimapColor(dungeon.getTile(x, y)));
                gc.fillRect(left + x * MINIMAP_SCALE, top + y * MINIMAP_SCALE,
                        MINIMAP_SCALE, MINIMAP_SCALE);
            }
        }

        // Oyuncu en son çiziliyor ki merdivenin üstündeyken de görünsün.
        Player player = game.getPlayer();
        gc.setFill(MINIMAP_PLAYER);
        gc.fillRect(left + player.getTileX() * MINIMAP_SCALE - 1,
                top + player.getTileY() * MINIMAP_SCALE - 1,
                MINIMAP_SCALE + 2, MINIMAP_SCALE + 2);
    }

    /** Küçük haritada karenin rengi; merdivenler zeminden ayrılıyor. */
    private Color minimapColor(Tile tile) {
        return switch (tile) {
            case WALL -> MINIMAP_WALL;
            case STAIRS_DOWN -> STAIRS_EDGE;
            case STAIRS_UP -> UP_STAIRS_EDGE;
            case FLOOR -> MINIMAP_FLOOR;
        };
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

    /**
     * Bossların yeteneklerinin görünen tarafı.
     *
     * <p>Bir mekanik ancak <em>görülebiliyorsa</em> mekanik: Boğucunun salvosu
     * uyarısız gelseydi kaçınılamaz olurdu, Şeytanın öfkesi de "boss birden
     * hızlandı" diye anlaşılmaz bir sıçrama gibi dururdu. Uyarıyı sormak yerine
     * durumu bossun kendisine soruyoruz.</p>
     */
    private void drawBossTells(GraphicsContext gc, Enemy enemy) {
        if (enemy instanceof Bogucu choker && choker.isWindingUp()) {
            drawVolleyTell(gc, choker);
        }
        if (enemy instanceof Seytan devil && devil.isEnraged()) {
            drawEnrageAura(gc, devil);
        }
    }

    /**
     * Boğucunun salvosu: dört kola uzanan kızıl bantlar.
     *
     * <p>Bant, salvo yaklaştıkça koyulaşıyor — kaçmak için ne kadar vaktin
     * kaldığını sayı okumadan görüyorsun. Kaçınma yolu da bantların şeklinden
     * anlaşılıyor: aralarındaki köşeler boş.</p>
     */
    private void drawVolleyTell(GraphicsContext gc, Bogucu choker) {
        double centerX = choker.getRenderX() * TILE_SIZE;
        double centerY = choker.getRenderY() * TILE_SIZE;
        double reach = choker.getVolleyRange() * TILE_SIZE;
        double thickness = TILE_SIZE * 0.55;
        double alpha = 0.15 + 0.35 * choker.getWindupProgress();

        gc.setFill(Color.color(VOLLEY_TELL.getRed(), VOLLEY_TELL.getGreen(),
                VOLLEY_TELL.getBlue(), alpha));
        gc.fillRect(centerX - reach, centerY - thickness / 2, reach * 2, thickness);
        gc.fillRect(centerX - thickness / 2, centerY - reach, thickness, reach * 2);
    }

    /** Öfkelenen Şeytanın çevresindeki kızıl halka: hızlanmanın görünen hâli. */
    private void drawEnrageAura(GraphicsContext gc, Seytan devil) {
        double centerX = devil.getRenderX() * TILE_SIZE;
        double centerY = devil.getRenderY() * TILE_SIZE + TILE_SIZE * 0.2;

        double phase = (System.currentTimeMillis() % ENRAGE_PULSE_MILLIS)
                / (double) ENRAGE_PULSE_MILLIS;
        double breath = 0.5 + 0.5 * Math.sin(phase * 2 * Math.PI);

        for (int ring = ENRAGE_RINGS; ring >= 1; ring--) {
            double radius = TILE_SIZE * 0.38 * ring * (0.9 + 0.1 * breath);
            double alpha = 0.16 / ring * (0.7 + 0.3 * breath);

            gc.setFill(Color.color(ENRAGE_GLOW.getRed(), ENRAGE_GLOW.getGreen(),
                    ENRAGE_GLOW.getBlue(), alpha));
            gc.fillOval(centerX - radius, centerY - radius * 0.6, radius * 2, radius * 1.2);
        }
    }

    /**
     * Havadaki okları çizer.
     *
     * <p>Sprite değil, doğrudan çizim: ok dört yöne gidiyor ve döndürülmüş bir
     * resim yerine gövde-uç çizmek hem her yönde doğru duruyor hem de dosya
     * gerektirmiyor. Arkasında soluk bir iz var — göz, hızlı giden küçük bir
     * şeyi ancak izinden yakalıyor.</p>
     */
    private void drawArrows(GraphicsContext gc, Game game, Vision vision) {
        for (Projectile arrow : game.getProjectiles()) {
            if (!vision.isVisible(arrow.getTileX(), arrow.getTileY())) {
                continue;
            }

            double x = arrow.getRenderX() * TILE_SIZE;
            double y = arrow.getRenderY() * TILE_SIZE;
            double dx = arrow.getStepX();
            double dy = arrow.getStepY();

            gc.setStroke(ARROW_TRAIL);
            gc.setLineWidth(2);
            gc.strokeLine(x - dx * ARROW_TRAIL_LENGTH, y - dy * ARROW_TRAIL_LENGTH, x, y);

            gc.setStroke(ARROW_COLOR);
            gc.setLineWidth(2);
            gc.strokeLine(x - dx * ARROW_LENGTH, y - dy * ARROW_LENGTH,
                    x + dx * ARROW_LENGTH, y + dy * ARROW_LENGTH);

            // Uç: gidiş yönünde bir üçgen, yani dikey giderken de doğru bakıyor.
            gc.setFill(ARROW_COLOR);
            gc.fillPolygon(
                    new double[] {x + dx * ARROW_LENGTH,
                            x + dy * ARROW_HEAD - dx * 2,
                            x - dy * ARROW_HEAD - dx * 2},
                    new double[] {y + dy * ARROW_LENGTH,
                            y - dx * ARROW_HEAD - dy * 2,
                            y + dx * ARROW_HEAD - dy * 2},
                    3);
        }
    }

    /**
     * F tuşunun şu an ne yapacağını söyleyen ipucu.
     *
     * <p>Toplama tuşa bağlanınca yerdeki eşyayı görmek yetmez oldu: üstünde
     * duruyorsun ve bir şey olmuyor, bunun bir tuş beklediği hiçbir yerden
     * anlaşılmıyor. Aynı boşluk büyücüde de vardı — yanına gidiyorsun, balon
     * konuşuyor, ama hangi tuşun tezgâhı açtığı yazmıyor.</p>
     *
     * <p>İpucu tuşun <em>o andaki</em> işini yazıyor, sabit bir tuş listesi
     * değil: F'nin ne yapacağı nerede durduğuna bağlı ve cevabı ekranda
     * görmek, ezberlemekten iyi.</p>
     *
     * <p>Merdiven ipucuyla aynı kutuyu kullanıyor ama onun bir satır üstünde:
     * merdivenin üstünde duran eşya ikisini birden gerektiriyor ve üst üste
     * binmeleri her ikisini de okunmaz yapardı.</p>
     */
    private void drawInteractHint(GraphicsContext gc, Game game, double mapWidth,
                                  double mapHeight) {
        List<Item> here = game.itemsUnderfoot();
        String hint;

        if (here.size() == 1) {
            hint = Text.HINT_TAKE_ONE.get(here.get(0).getName());
        } else if (!here.isEmpty()) {
            hint = Text.HINT_TAKE_MANY.get(here.size());
        } else if (game.isNearWizard()) {
            hint = Text.HINT_OPEN_BENCH.get();
        } else {
            return;
        }

        double boxWidth = Math.max(160, measure(hint, hudFont) + 28);
        double boxHeight = 26;
        double x = (mapWidth - boxWidth) / 2;
        double y = mapHeight - boxHeight - 44;

        gc.setFill(HINT_BACKGROUND);
        gc.fillRoundRect(x, y, boxWidth, boxHeight, 6, 6);
        gc.setStroke(GOLD_TEXT);
        gc.setLineWidth(1);
        gc.strokeRoundRect(x, y, boxWidth, boxHeight, 6, 6);

        gc.setFont(hudFont);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.setFill(GOLD_TEXT);
        gc.fillText(hint, mapWidth / 2, y + boxHeight / 2);
    }

    /** Merdivenin üstündeyken haritanın altında beliren ipucu. */
    private void drawStairsHint(GraphicsContext gc, Game game, double mapWidth, double mapHeight) {
        boolean onDown = game.isPlayerOnStairs();
        boolean locked = onDown && game.isStairsLocked();
        String hint;
        if (locked) {
            hint = Text.HINT_STAIRS_LOCKED.get();
        } else if (!onDown) {
            // Yukarı çıkan merdiven: geldiğin yer.
            hint = Text.HINT_STAIRS_UP.get();
        } else if (game.isFinalFloor()) {
            // Yirminci katın merdiveni aşağı değil dışarı çıkıyor.
            hint = Text.HINT_STAIRS_EXIT.get();
        } else {
            hint = Text.HINT_STAIRS_DOWN.get();
        }

        double boxWidth = locked ? 230 : 200;
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
        gc.fillText(Text.WIZARD_SIGN.get(), mapWidth / 2, mapHeight / 2 - 190);

        gc.setFont(hudFont);
        gc.setFill(HUD_TEXT);
        gc.fillText(Text.FORGE_PURSE.get(game.getGold()), mapWidth / 2, mapHeight / 2 - 148);

        Player player = game.getPlayer();
        Weapon weapon = player.getEquippedWeapon();
        Armor armor = player.getEquippedArmor();
        double y = mapHeight / 2 - 112;

        y = drawForgeRow(gc, game, mapWidth, y, "1", Text.FORGE_REPAIR.get(Text.FORGE_WEAPON.get()), weapon, false,
                UiAction.Bench.REPAIR_WEAPON);
        y = drawForgeRow(gc, game, mapWidth, y, "2", Text.FORGE_REPAIR.get(Text.FORGE_ARMOR.get()), armor, false,
                UiAction.Bench.REPAIR_ARMOR);
        y = drawForgeRow(gc, game, mapWidth, y, "3", Text.FORGE_UPGRADE.get(Text.FORGE_WEAPON.get()), weapon, true,
                UiAction.Bench.UPGRADE_WEAPON);
        y = drawForgeRow(gc, game, mapWidth, y, "4", Text.FORGE_UPGRADE.get(Text.FORGE_ARMOR.get()), armor, true,
                UiAction.Bench.UPGRADE_ARMOR);

        y += 10;
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(HUD_ACCENT);
        gc.fillText(Text.FORGE_ENCHANTS_TITLE.get(), mapWidth / 2, y);
        y += 26;

        y = drawEnchantRows(gc, game, mapWidth, y, Text.FORGE_TO_WEAPON.get(), weapon, WEAPON_ENCHANT_KEYS);
        y = drawEnchantRows(gc, game, mapWidth, y, Text.FORGE_TO_ARMOR.get(), armor, ARMOR_ENCHANT_KEYS);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(HUD_TEXT);
        gc.fillText(Text.FORGE_CAP_NOTE.get(),
                mapWidth / 2, y + 18);
        gc.setFill(HUD_ACCENT);
        gc.fillText(Text.FORGE_LEAVE.get(), mapWidth / 2, y + 40);
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
            int price = game.enchantPrice(option);
            boolean affordable = available && game.getGold() >= price;

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
            gc.fillText(active ? Text.FORGE_ACTIVE.get(option.getDescription()) : option.getDescription(),
                    mapWidth / 2 - 60, y);

            gc.setTextAlign(TextAlignment.RIGHT);
            gc.setFill(affordable ? GOLD_TEXT : MESSAGE_FADED);
            gc.fillText(item == null ? Text.FORGE_NONE.get() : Text.FORGE_COST.get(price), mapWidth / 2 + 300, y);

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
            detail = Text.FORGE_NO_GEAR.get();
            cost = Text.FORGE_NONE.get();
            available = false;
        } else if (upgrade) {
            available = item.canUpgrade(game.getDepth());
            detail = item.getFullName() + "  +" + item.getBonus()
                    + (available ? "" : "  (tavan)");
            cost = available ? Text.FORGE_COST.get(Forge.upgradeCost(item)) : Text.FORGE_NONE.get();
        } else {
            available = item.needsRepair();
            detail = item.getFullName() + "  " + item.getDurability() + "/"
                    + item.getMaxDurability() + (item.isBroken() ? "  KIRIK" : "");
            cost = available ? Text.FORGE_COST.get(Forge.repairCost(item)) : Text.FORGE_SOUND.get();
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
    private void drawThemeWash(GraphicsContext gc, FloorTheme theme, boolean cave,
                               double mapWidth, double mapHeight) {
        gc.setFill(Color.web(theme.getTint(cave), theme.getTintAlpha(cave)));
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
        gc.fillText(Text.VICTORY.get(), mapWidth / 2, mapHeight / 2 - 60);

        gc.setFont(menuFont);
        gc.setFill(MESSAGE_TEXT);
        gc.fillText(Text.VICTORY_LINE.get(), mapWidth / 2, mapHeight / 2 - 10);

        gc.setFont(hudFont);
        gc.setFill(HUD_ACCENT);
        gc.fillText(String.format("%d altin  ·  %.0f saniye", game.getGold(),
                game.getElapsedSeconds()), mapWidth / 2, mapHeight / 2 + 26);

        gc.setFill(HUD_TEXT);
        gc.fillText(Text.GAME_OVER_HINT.get(), mapWidth / 2, mapHeight / 2 + 58);
    }

    private void drawGameOver(GraphicsContext gc, Game game, double mapWidth, double mapHeight) {
        gc.setFill(OVERLAY);
        gc.fillRect(0, 0, mapWidth, mapHeight);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);

        gc.setFont(titleFont);
        gc.setFill(OVERLAY_TITLE);
        gc.fillText(Text.GAME_OVER.get(), mapWidth / 2.0, mapHeight / 2.0 - 30);

        gc.setFont(hudFont);
        gc.setFill(GOLD_TEXT);
        gc.fillText(Text.GAME_OVER_LINE.get(game.getDepth(), game.getGold()),
                mapWidth / 2.0, mapHeight / 2.0 + 8);

        gc.setFill(MESSAGE_TEXT);
        gc.fillText(Text.GAME_OVER_HINT.get(), mapWidth / 2.0, mapHeight / 2.0 + 32);
    }
}
