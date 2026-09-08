package com.cryptdelver;

/**
 * IDE'den doğrudan çalıştırmak için ince bir sarmalayıcı.
 *
 * <p>{@link Main} sınıfı {@code Application}'dan türediği için JavaFX onu
 * doğrudan çalıştırırken modül yolu (module-path) VM argümanları ister.
 * {@code Application}'dan türemeyen bu sınıfı çalıştırmak ise sınıf yolundan
 * (classpath) sorunsuz başlar — IntelliJ'de ekstra ayar gerekmez.</p>
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Main.main(args);
    }
}
