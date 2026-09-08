package com.cryptdelver.ui;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;

/**
 * Varlıkların şekillerle çizilmiş sprite'ları.
 *
 * <p>Hazır çizim dosyası kullanmadan, daire/dikdörtgen/çizgi ile "tanınabilir"
 * figürler üretiyoruz. Bütün koordinatlar birim kutuda tanımlı: kutunun merkezi
 * {@code (0, 0)}, kenarları {@code -0.5} ile {@code 0.5} arası. Çizim anında
 * {@code size} ile ölçekleniyor, böylece aynı sprite hem 20 piksellik tile'da
 * hem 18 piksellik envanter kutusunda düzgün duruyor.</p>
 *
 * <p>Bu sınıf geçici bir çözüm değil, geçerli bir yedek: PNG paketi eklendiğinde
 * {@link SpriteRegistry} onları tercih edecek, dosyası olmayan varlıklar burada
 * çizilmeye devam edecek.</p>
 */
public final class ShapeSprites {

    private ShapeSprites() {
    }

    /** Zemin karesi. */
    public static Sprite floorTile() {
        return (gc, cx, cy, size) -> {
            fillTile(gc, cx, cy, size, Color.web("#1b1b26"));
            fillRect(gc, cx, cy, size, 0.0, 0.0, 0.1, 0.1, Color.web("#262634"));
        };
    }

    /** Duvar karesi; üstteki ince aydınlık şerit hacim hissi veriyor. */
    public static Sprite wallTile() {
        return (gc, cx, cy, size) -> {
            fillTile(gc, cx, cy, size, Color.web("#3f3a56"));
            fillRect(gc, cx, cy, size, -0.5, -0.5, 1.0, 0.1, Color.web("#524a70"));
        };
    }

    /** İniş merdiveni: karanlık bir boşluk ve içine inen basamaklar. */
    public static Sprite stairsTile() {
        return (gc, cx, cy, size) -> {
            fillRect(gc, cx, cy, size, -0.4, -0.4, 0.8, 0.8, Color.web("#08080c"));
            for (int step = 0; step < 3; step++) {
                double inset = 0.35 - step * 0.1;
                double y = -0.3 + step * 0.22;
                fillRect(gc, cx, cy, size, -inset, y, inset * 2, 0.11, Color.web("#6b6480"));
            }
        };
    }

    /** Oyuncu: miğferli kafa, zırhlı gövde ve elinde kılıç. */
    public static Sprite player() {
        return (gc, cx, cy, size) -> {
            // Pelerin / gövde
            fillRoundRect(gc, cx, cy, size, -0.24, -0.02, 0.48, 0.44, 0.2, Color.web("#4a5b7a"));
            // Omuz zırhı
            fillRoundRect(gc, cx, cy, size, -0.30, 0.00, 0.60, 0.16, 0.14, Color.web("#63739b"));
            // Kafa
            fillOval(gc, cx, cy, size, -0.17, -0.38, 0.34, 0.34, Color.web("#d9c2a0"));
            // Miğfer
            fillRoundRect(gc, cx, cy, size, -0.19, -0.42, 0.38, 0.22, 0.12, Color.web("#c9ccd6"));
            // Kılıç
            strokeLine(gc, cx, cy, size, 0.26, 0.22, 0.42, -0.26, Color.web("#e8c46a"), 0.09);
            strokeLine(gc, cx, cy, size, 0.20, 0.16, 0.34, 0.10, Color.web("#8a7a4a"), 0.07);
        };
    }

    /** Fare: yayvan gövde, sivri burun, yuvarlak kulaklar ve kuyruk. */
    public static Sprite rat() {
        return (gc, cx, cy, size) -> {
            // Kuyruk
            strokeLine(gc, cx, cy, size, -0.22, 0.14, -0.46, -0.06, Color.web("#7d6a4c"), 0.06);
            // Gövde
            fillOval(gc, cx, cy, size, -0.34, -0.06, 0.48, 0.36, Color.web("#a08a63"));
            // Kulak
            fillOval(gc, cx, cy, size, 0.02, -0.26, 0.16, 0.16, Color.web("#7d6a4c"));
            // Kafa
            fillOval(gc, cx, cy, size, 0.06, -0.14, 0.30, 0.28, Color.web("#b39a70"));
            // Burun
            fillOval(gc, cx, cy, size, 0.30, 0.00, 0.10, 0.08, Color.web("#d8a0a0"));
            // Göz
            fillOval(gc, cx, cy, size, 0.20, -0.06, 0.07, 0.07, Color.web("#1a1a22"));
        };
    }

    /** İskelet: kafatası, göz çukurları ve kaburgalar. */
    public static Sprite skeleton() {
        return (gc, cx, cy, size) -> {
            // Omurga
            strokeLine(gc, cx, cy, size, 0.0, -0.02, 0.0, 0.36, Color.web("#cfd3dc"), 0.08);
            // Kaburgalar
            strokeLine(gc, cx, cy, size, -0.20, 0.06, 0.20, 0.06, Color.web("#cfd3dc"), 0.07);
            strokeLine(gc, cx, cy, size, -0.17, 0.18, 0.17, 0.18, Color.web("#cfd3dc"), 0.07);
            strokeLine(gc, cx, cy, size, -0.13, 0.30, 0.13, 0.30, Color.web("#cfd3dc"), 0.07);
            // Kafatası
            fillOval(gc, cx, cy, size, -0.22, -0.44, 0.44, 0.40, Color.web("#e6e8ee"));
            // Çene
            fillRoundRect(gc, cx, cy, size, -0.13, -0.12, 0.26, 0.12, 0.06, Color.web("#e6e8ee"));
            // Göz çukurları
            fillOval(gc, cx, cy, size, -0.15, -0.34, 0.12, 0.13, Color.web("#15151d"));
            fillOval(gc, cx, cy, size, 0.03, -0.34, 0.12, 0.13, Color.web("#15151d"));
        };
    }

    /** Kript Lordu: taçlı kafatası, koyu cüppe, kızıl gözler. */
    public static Sprite boss() {
        return (gc, cx, cy, size) -> {
            // Cüppe
            fillRoundRect(gc, cx, cy, size, -0.34, -0.06, 0.68, 0.52, 0.18, Color.web("#2e2340"));
            fillRoundRect(gc, cx, cy, size, -0.40, 0.02, 0.80, 0.18, 0.14, Color.web("#3d2f55"));
            // Kafatası
            fillOval(gc, cx, cy, size, -0.24, -0.40, 0.48, 0.42, Color.web("#e6e8ee"));
            // Göz çukurları
            fillOval(gc, cx, cy, size, -0.16, -0.30, 0.13, 0.14, Color.web("#c9564f"));
            fillOval(gc, cx, cy, size, 0.03, -0.30, 0.13, 0.14, Color.web("#c9564f"));
            // Taç
            strokeLine(gc, cx, cy, size, -0.22, -0.40, -0.22, -0.50, Color.web("#e8c46a"), 0.07);
            strokeLine(gc, cx, cy, size, 0.0, -0.42, 0.0, -0.54, Color.web("#e8c46a"), 0.07);
            strokeLine(gc, cx, cy, size, 0.22, -0.40, 0.22, -0.50, Color.web("#e8c46a"), 0.07);
            strokeLine(gc, cx, cy, size, -0.22, -0.42, 0.22, -0.42, Color.web("#e8c46a"), 0.07);
        };
    }

    /** İksir: mantarlı şişe. */
    public static Sprite potion() {
        return (gc, cx, cy, size) -> {
            // Mantar
            fillRoundRect(gc, cx, cy, size, -0.09, -0.44, 0.18, 0.14, 0.05, Color.web("#8a6b4a"));
            // Boyun
            fillRect(gc, cx, cy, size, -0.07, -0.32, 0.14, 0.16, Color.web("#9aa3b5"));
            // Şişe gövdesi
            fillOval(gc, cx, cy, size, -0.24, -0.20, 0.48, 0.46, Color.web("#c9564f"));
            // Işık yansıması
            fillOval(gc, cx, cy, size, -0.15, -0.12, 0.10, 0.14, Color.web("#f3b6b0"));
        };
    }

    /** Altın: üst üste binmiş sikkeler. */
    public static Sprite gold() {
        return (gc, cx, cy, size) -> {
            fillOval(gc, cx, cy, size, -0.34, 0.06, 0.30, 0.20, Color.web("#b08a3a"));
            fillOval(gc, cx, cy, size, 0.04, 0.06, 0.30, 0.20, Color.web("#b08a3a"));
            fillOval(gc, cx, cy, size, -0.34, 0.00, 0.30, 0.20, Color.web("#e8c46a"));
            fillOval(gc, cx, cy, size, 0.04, 0.00, 0.30, 0.20, Color.web("#e8c46a"));
            fillOval(gc, cx, cy, size, -0.15, -0.18, 0.30, 0.20, Color.web("#f0d68f"));
        };
    }

    /**
     * Kılıç: çapraz namlu, balçak ve topuz.
     *
     * <p>Renkler parametre: aynı çizim paslı kılıçtan Kript Kılıcı'na kadar
     * bütün kademeleri veriyor, her kademe için ayrı çizim yazmaya gerek yok.</p>
     */
    public static Sprite sword(Color blade, Color hilt) {
        return (gc, cx, cy, size) -> {
            strokeLine(gc, cx, cy, size, -0.26, 0.30, 0.30, -0.30, blade, 0.13);
            strokeLine(gc, cx, cy, size, -0.30, 0.06, -0.06, 0.32, hilt, 0.10);
            fillOval(gc, cx, cy, size, -0.38, 0.24, 0.16, 0.16, hilt);
        };
    }

    /** Zırh: göğüslük, omuzlar ve kemer. Renkleri kademeye göre değişiyor. */
    public static Sprite armor(Color plate, Color trim) {
        return (gc, cx, cy, size) -> {
            // Omuzlar
            fillRoundRect(gc, cx, cy, size, -0.38, -0.26, 0.76, 0.20, 0.10, trim);
            // Göğüslük
            fillRoundRect(gc, cx, cy, size, -0.28, -0.24, 0.56, 0.54, 0.14, plate);
            // Yaka
            fillRoundRect(gc, cx, cy, size, -0.14, -0.30, 0.28, 0.12, 0.06, trim);
            // Kemer
            fillRect(gc, cx, cy, size, -0.28, 0.16, 0.56, 0.09, trim);
            // Orta dikiş
            strokeLine(gc, cx, cy, size, 0.0, -0.14, 0.0, 0.14, trim, 0.05);
        };
    }

    /** Balta: sap ve hilal ağız. */
    public static Sprite axe() {
        return (gc, cx, cy, size) -> {
            strokeLine(gc, cx, cy, size, -0.22, 0.34, 0.18, -0.28, Color.web("#8a6b4a"), 0.11);
            fillOval(gc, cx, cy, size, -0.06, -0.42, 0.40, 0.34, Color.web("#b6bcc9"));
            fillOval(gc, cx, cy, size, -0.18, -0.38, 0.34, 0.30, Color.web("#15151d"));
        };
    }

    /** Tanınmayan sprite adı için göze batan yer tutucu. */
    public static Sprite unknown() {
        return (gc, cx, cy, size) -> {
            fillRect(gc, cx, cy, size, -0.4, -0.4, 0.8, 0.8, Color.web("#ff00ff"));
            strokeLine(gc, cx, cy, size, -0.3, -0.3, 0.3, 0.3, Color.BLACK, 0.08);
            strokeLine(gc, cx, cy, size, 0.3, -0.3, -0.3, 0.3, Color.BLACK, 0.08);
        };
    }

    // ------------------------------------------------- birim kutu yardımcıları

    /** Karenin tamamını tek renkle doldurur; tile sprite'ları için. */
    private static void fillTile(GraphicsContext gc, double cx, double cy, double size, Color color) {
        gc.setFill(color);
        gc.fillRect(cx - size / 2, cy - size / 2, size, size);
    }

    private static void fillOval(GraphicsContext gc, double cx, double cy, double size,
                                 double x, double y, double width, double height, Color color) {
        gc.setFill(color);
        gc.fillOval(cx + x * size, cy + y * size, width * size, height * size);
    }

    private static void fillRect(GraphicsContext gc, double cx, double cy, double size,
                                 double x, double y, double width, double height, Color color) {
        gc.setFill(color);
        gc.fillRect(cx + x * size, cy + y * size, width * size, height * size);
    }

    private static void fillRoundRect(GraphicsContext gc, double cx, double cy, double size,
                                      double x, double y, double width, double height,
                                      double corner, Color color) {
        gc.setFill(color);
        gc.fillRoundRect(cx + x * size, cy + y * size,
                width * size, height * size, corner * size, corner * size);
    }

    private static void strokeLine(GraphicsContext gc, double cx, double cy, double size,
                                   double x1, double y1, double x2, double y2,
                                   Color color, double thickness) {
        gc.setStroke(color);
        gc.setLineWidth(Math.max(1, thickness * size));
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.strokeLine(cx + x1 * size, cy + y1 * size, cx + x2 * size, cy + y2 * size);
    }
}
