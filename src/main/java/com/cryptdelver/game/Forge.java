package com.cryptdelver.game;

import com.cryptdelver.entity.Enchantment;
import com.cryptdelver.entity.Equipment;

/**
 * Büyücünün fiyat listesi.
 *
 * <p>Kuralları oyundan ayrı tuttum: burada ne mesaj yazılıyor ne kese
 * karıştırılıyor, yalnızca "bu işin fiyatı kaç" sorusu yanıtlanıyor. Denge
 * ayarı yaparken tek dosyaya bakmak yetiyor ve fiyatlar oyun döngüsü
 * çalıştırılmadan test edilebiliyor.</p>
 *
 * <h2>Fiyatlar neye göre</h2>
 * <p>Tamir eksik dayanıklılıkla doğru orantılı: az yıpranmışsa ucuz, kırılmışsa
 * pahalı. Böylece "bozulana kadar bekle" ile "her fırsatta uğra" arasında
 * gerçek bir seçim oluyor — ikisi de aynı toplam altını götürüyor, ama biri
 * seni kırık kılıçla dövüşe sokuyor.</p>
 *
 * <p>Yükseltme ise mevcut bonusla birlikte pahalanıyor: parça güçlendikçe
 * sonraki kademe daha çok altın istiyor. Kat başına toplanan altın kabaca
 * sabit olduğu için bu, yükseltmenin doğal olarak yavaşlaması demek.</p>
 */
public final class Forge {

    /** Eksik her dayanıklılık puanının tamir bedeli. */
    private static final int REPAIR_COST_PER_POINT = 1;

    /** Uğramaya değsin diye taban tamir ücreti. */
    private static final int MIN_REPAIR_COST = 5;

    private static final int UPGRADE_BASE_COST = 30;
    private static final int UPGRADE_COST_PER_BONUS = 20;

    /**
     * Üstündeki her büyünün bir sonraki büyüye bindirdiği pay.
     *
     * <p>Büyüler sabit fiyatlıyken geri dönüş mekaniği dengeyi bozuyordu:
     * derin katlarda altın biriktir, yukarıdaki büyücüye dön, <em>hepsini</em>
     * al. Fiyatlar sabit olduğu için biriken altın doğrudan güce çevriliyordu
     * ve tam takım kuşanmış oyuncu için sığ katlarda hiçbir tehlike
     * kalmıyordu.</p>
     *
     * <p>Şimdi her büyü bir sonrakini pahalandırıyor: ilk büyü tam fiyat,
     * ikincisi 1.6 katı, üçüncüsü 2.2 katı. Bir büyü hâlâ erişilebilir, tam
     * takım ise gerçekten pahalı — yani "hangisini alayım" bir soru olarak
     * kalıyor. Yükseltmenin mevcut bonusla pahalanmasıyla aynı fikir, bir
     * eksen ötede.</p>
     */
    private static final double ENCHANT_SURCHARGE = 0.6;

    private Forge() {
    }

    /** Parçayı tam dolduran tamirin bedeli. */
    public static int repairCost(Equipment item) {
        int missing = item.getMaxDurability() - item.getDurability();
        if (missing <= 0) {
            return 0;
        }
        return Math.max(MIN_REPAIR_COST, missing * REPAIR_COST_PER_POINT);
    }

    /** Bir kademe yükseltmenin bedeli. */
    public static int upgradeCost(Equipment item) {
        return UPGRADE_BASE_COST + UPGRADE_COST_PER_BONUS * item.getBonus();
    }

    /**
     * Bir büyü basmanın bedeli.
     *
     * <p>Fiyat büyünün kendi değerine <em>ve</em> üstünde zaten kaç büyü
     * taşıdığına bağlı; ikisi de sayılıyor, çünkü pahalı olan tek bir büyü
     * değil tam takım kuşanmak.</p>
     *
     * @param carried silahında ve zırhında hâlihazırda duran büyü sayısı
     */
    public static int enchantCost(Enchantment enchantment, int carried) {
        return (int) Math.round(enchantment.getCost() * (1 + ENCHANT_SURCHARGE * carried));
    }
}
