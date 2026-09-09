package com.cryptdelver.game;

import com.cryptdelver.entity.Equipment;

/**
 * Demircinin fiyat listesi.
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
}
