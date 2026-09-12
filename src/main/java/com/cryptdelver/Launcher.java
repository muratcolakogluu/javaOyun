package com.cryptdelver;

/**
 * Sınıf yolundan çalıştırmak için ince bir sarmalayıcı.
 *
 * <p>{@link Main} sınıfı {@code Application}'dan türediği için JavaFX onu
 * doğrudan çalıştırırken modül yolu (module-path) VM argümanları ister.
 * {@code Application}'dan türemeyen bu sınıfı çalıştırmak ise sınıf yolundan
 * (classpath) sorunsuz başlar.</p>
 *
 * <p>Başta yalnızca IntelliJ'den tek tuşla çalıştırmak için vardı; şimdi
 * <b>paketlenen sürümün de giriş noktası</b>. Sebep aynı: tek bir jar'ın
 * içinden açılan oyun da sınıf yolundan başlıyor ve {@code Application}
 * doğrudan giriş noktası olsaydı <em>"JavaFX runtime components are
 * missing"</em> diyerek açılmazdı. {@code paketle.bat} bu yüzden
 * {@code --main-class} olarak burayı veriyor.</p>
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Main.main(args);
    }
}
