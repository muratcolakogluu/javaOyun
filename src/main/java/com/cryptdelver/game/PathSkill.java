package com.cryptdelver.game;

import com.cryptdelver.entity.Enchantment;
import com.cryptdelver.entity.Player;

/**
 * Başlangıç yolunun getirdiği yetenek: <b>Q</b> tuşunun ne yaptığı.
 *
 * <p>Üç yol vardı ama üçü de yalnızca <em>başlangıç takımıyla</em> ayrışıyordu:
 * ilk kattan sonra elindeki kılıç neyse öyle oynuyordun ve Q hepsinde aynı
 * şeyi yapıyordu. Yani seçim ilk beş dakikaya aitti, koşunun tamamına değil.
 * Yeteneği yola bağlamak seçimi <b>yirmi katın tamamına</b> yayıyor: Muhafız
 * kalabalığı savuruyor, Haydut sıçrıyor, Tüccar parayla kurtuluyor.</p>
 *
 * <h2>Neden üçü de aynı tuş</h2>
 * <p>Üç ayrı tuş vermek, oyuncuya hiç kullanmayacağı iki tuş ezberletmek
 * olurdu — seçtiğin yolun dışındaki yetenekler o koşuda hiç yok. Tek tuşun
 * anlamının yola göre değişmesi, tuş listesinde tek satır ve elinde tek
 * alışkanlık demek.</p>
 *
 * <h2>Neden üçü de aynı soruna cevap vermiyor</h2>
 * <p>Üçü de "kaç" ya da üçü de "vur" olsaydı yol seçimi bir renk seçimi
 * olurdu. Sarsıntı etrafını temizliyor (kalabalık sorunu), sıçrama yer
 * değiştiriyor (konum sorunu), rüşvet düşmanı gönderiyor (zaman sorunu) —
 * üçü farklı bir sıkışmayı çözüyor, yani yol seçmek nasıl sıkışmak
 * istediğini seçmek oluyor.</p>
 */
public enum PathSkill {

    /**
     * Sarsıntı: etrafındaki herkesi sersemletir.
     *
     * <p>Muhafızın yolu ayakta kalmak ve ayakta kalmanın en zor olduğu yer
     * kuşatılmış olmak. Sarsıntı o anı çözüyor: dördü birden sersemliyor ve
     * nefes alacak bir pencere açılıyor. Vuruş gücü yok — bu bir saldırı
     * değil, bir <em>çıkış</em>.</p>
     *
     * <p>Beklemesi uzun. Kalabalığı durdurmak oyundaki en güçlü şey ve sık
     * olsa dövüşün ritmi diye bir şey kalmazdı.</p>
     */
    SARSINTI(Text.SKILL_SHOCKWAVE, Text.SKILL_SHOCKWAVE_WHAT, 6.0) {
        @Override
        public boolean use(Game game, Player player) {
            return game.shockwave();
        }
    },

    /**
     * Kaçış adımı: baktığın yöne üç kare sıçra.
     *
     * <p>Haydutun yolu önce vurmak ve konumu seçmek. Sıçrama hem okçunun
     * hattından çıkarıyor hem de düşmanın arkasına düşürüyor — arkadan vuruş
     * iki katı hasar verdiği için Haydut aynı hamleyle hem kaçıyor hem
     * vuruyor.</p>
     */
    SICRAMA(Text.SKILL_DASH, Text.SKILL_DASH_WHAT, 2.5) {
        @Override
        public boolean use(Game game, Player player) {
            if (!player.dash(game)) {
                return false;
            }
            game.onPlayerDashed();
            return true;
        }

        /**
         * Çeviklik büyüsü yalnızca burayı kısaltıyor.
         *
         * <p>Büyü "yer değiştirmek senin için ucuz" diyor; sarsıntı ve rüşvet
         * yer değiştirmekle ilgili değil, o yüzden onlara dokunmuyor.</p>
         */
        @Override
        public double cooldownFor(Player player) {
            return player.hasArmorEnchantment(Enchantment.CEVIKLIK)
                    ? getCooldown() * SWIFT_SCALE
                    : getCooldown();
        }
    },

    /**
     * Rüşvet: kese açılır, yakındakiler dağılır.
     *
     * <p>Tüccarın yolu parayla çözmek. Altın harcıyor, yani tek sınırlı
     * yeteneği bu: kesesi boşsa Tüccarın Q'su çalışmıyor. Buna karşılık
     * çözdüğü şey hiçbir kılıcın çözmediği şey — dövüşten <em>hiç</em>
     * girmemek.</p>
     *
     * <p>Düşmanlar ölmüyor, korkuyor: birkaç saniye sonra geri geliyorlar.
     * Öldürseydi bu bir yetenek değil bir para-silah olurdu ve Tüccar altını
     * hasara çeviren bir sınıfa dönerdi.</p>
     */
    RUSVET(Text.SKILL_BRIBE, Text.SKILL_BRIBE_WHAT, 4.0) {
        @Override
        public boolean use(Game game, Player player) {
            return game.bribe();
        }
    };

    /** Çeviklik büyüsünün sıçrama beklemesini indirdiği oran. */
    private static final double SWIFT_SCALE = 0.65;

    private final Text label;
    private final Text description;
    private final double cooldown;

    PathSkill(Text label, Text description, double cooldown) {
        this.label = label;
        this.description = description;
        this.cooldown = cooldown;
    }

    public String getLabel() {
        return label.get();
    }

    /** Ne yaptığı: yol sayfasında ve tuş listesinde okunan satır. */
    public String getDescription() {
        return description.get();
    }

    /** Yeteneğin taban beklemesi, saniye. */
    public double getCooldown() {
        return cooldown;
    }

    /**
     * Bu oyuncu için bekleme.
     *
     * <p>Varsayılan taban değer; yalnızca sıçrama büyüyle kısalıyor. Kuralı
     * yeteneğin kendisine bırakmak, "hangi büyü hangi yeteneği etkiliyor"
     * sorusunu tek yerde tutuyor.</p>
     */
    public double cooldownFor(Player player) {
        return cooldown;
    }

    /**
     * Yeteneği uygular.
     *
     * <p>Hiçbir şey olmadıysa {@code false} — o zaman bekleme de
     * harcanmıyor. Duvara bakarken sıçramaya çalışmak ya da kesesi boşken
     * rüşvet vermek, oyuncunun elinden yeteneğini almamalı.</p>
     */
    public abstract boolean use(Game game, Player player);
}
