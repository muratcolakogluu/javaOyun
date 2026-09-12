package com.cryptdelver.game;

/**
 * Bir kata o kata özgü bir hâl veren olay.
 *
 * <p>Yirmi kat aynı ritimle geçiyordu. Bölge değişince taş ve renk
 * değişiyordu, derinlik arttıkça sayılar büyüyordu — ama <b>oynanış hep
 * aynıydı</b>: gir, temizle, merdiveni bul. Olaylar o tekdüzeliği kırıyor:
 * üçte bir katta indiğin anda bir cümle karşılıyor ve o kat başka türlü
 * oynanıyor.</p>
 *
 * <h2>Neden dördü de aynı yöne çekmiyor</h2>
 * <p>Hepsi zorlaştırıcı olsaydı olay "ceza" demek olurdu ve oyuncu her
 * seferinde aynı şeyi yapardı: hızlıca geç. Sessiz kat bilerek bir
 * <em>rahatlama</em>; zengin kat bilerek bir <em>cazibe</em>. Dördü dört ayrı
 * cevap istiyor — dikkatli yürü, dövüş ya da kaç, oyalan, kap ve kaç.</p>
 *
 * <p>Her olay tek bir kaldıraca dokunuyor (görüş, kalabalık, ganimet, sabır).
 * İkisini birden çevirmek, oyuncunun katın neden farklı geldiğini
 * okuyamaması demekti.</p>
 */
public enum FloorEvent {

    /**
     * Karanlık: görüşün yarıya iniyor.
     *
     * <p>Cevabı dikkatli yürümek. Okçular ve şamanlar bu katta gerçekten
     * tehlikeli, çünkü onları görmeden önce onlar seni görüyor.</p>
     */
    KARANLIK(Text.EVENT_DARK, Text.EVENT_DARK_WHAT, 0.5, 1.0, 0, 1.0),

    /**
     * Sürü: kalabalık ama ganimeti bol.
     *
     * <p>Cevabı "dövüşecek miyim" sorusunu baştan cevaplamak. Ganimet fazlası
     * olmasa yalnızca bir vergi olurdu — böyle bir seçim oluyor.</p>
     */
    SURU(Text.EVENT_SWARM, Text.EVENT_SWARM_WHAT, 1.0, 1.6, 2, 1.0),

    /**
     * Sessiz: zindan burada geç uyanıyor.
     *
     * <p>Cevabı oyalanmak. Yirmi katın tamamı baskı altında geçiyordu; ara
     * ara nefes almak, baskının kendisini de daha anlamlı yapıyor.</p>
     */
    SESSIZ(Text.EVENT_QUIET, Text.EVENT_QUIET_WHAT, 1.0, 1.0, 0, 4.0),

    /**
     * Zengin: hazine çok ama zindan çabuk uyanıyor.
     *
     * <p>Cevabı kap ve kaç. Açgözlülüğün bedeli doğrudan ödeniyor: ne kadar
     * çok toplarsan o kadar çok takviye karşılıyor.</p>
     */
    ZENGIN(Text.EVENT_RICH, Text.EVENT_RICH_WHAT, 1.0, 1.0, 3, 0.25);

    private final Text label;
    private final Text description;
    private final double visionScale;
    private final double crowdScale;
    private final int extraItems;
    private final double patienceScale;

    FloorEvent(Text label, Text description, double visionScale, double crowdScale,
               int extraItems, double patienceScale) {
        this.label = label;
        this.description = description;
        this.visionScale = visionScale;
        this.crowdScale = crowdScale;
        this.extraItems = extraItems;
        this.patienceScale = patienceScale;
    }

    /** Şeritte görünen kısa ad. */
    public String getLabel() {
        return label.get();
    }

    /** Kata inince okunan cümle: ne değişti ve ne yapmalısın. */
    public String getDescription() {
        return description.get();
    }

    /** Görüş yarıçapının katı. */
    public double scaleVision(int radius) {
        return radius * visionScale;
    }

    public int scaleCrowd(int count) {
        return (int) Math.round(count * crowdScale);
    }

    /** Kata fazladan konan nadir eşya sayısı. */
    public int getExtraItems() {
        return extraItems;
    }

    /** Zindanın sabrının katı: büyükse geç uyanıyor, küçükse çabuk. */
    public double scalePatience(double seconds) {
        return seconds * patienceScale;
    }
}
