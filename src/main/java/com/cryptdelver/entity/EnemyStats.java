package com.cryptdelver.entity;

/**
 * Bir düşman türünü tanımlayan sayılar.
 *
 * <p>Ayrı bir record olmasının sebebi: {@link Enemy} kurucusuna yarım düzine
 * parametre sıralamak yerine türün "kimliğini" tek yerde toplamak. Yeni bir
 * düşman eklemek, davranış yazmadan önce burada bir satır doldurmak demek.</p>
 *
 * @param maxHp          azami can
 * @param attackPower    vuruş gücü
 * @param speed          saniyede kaç kare ilerlediği
 * @param attackCooldown iki vuruş arasındaki bekleme (saniye)
 * @param aggroRange     oyuncuyu kaç kare uzaktan fark ettiği (ızgara uzaklığı)
 */
public record EnemyStats(
        int maxHp,
        int attackPower,
        double speed,
        double attackCooldown,
        int aggroRange) {

    public EnemyStats {
        if (maxHp <= 0 || speed <= 0 || aggroRange <= 0) {
            throw new IllegalArgumentException("Düşman değerleri pozitif olmalı");
        }
    }
}
