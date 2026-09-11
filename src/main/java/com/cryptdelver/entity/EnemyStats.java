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
 * @param defense        gelen hasardan düşülen savunma (kemik, kabuk, zırh)
 * @param speed          saniyede kaç kare ilerlediği
 * @param attackCooldown iki vuruş arasındaki bekleme (saniye)
 * @param aggroRange     oyuncuyu kaç kare uzaktan fark ettiği (ızgara uzaklığı)
 * @param windup         vuruştan önce beklediği hazırlık süresi (saniye); 0 ise
 *                       habersiz vuruyor
 */
public record EnemyStats(
        int maxHp,
        int attackPower,
        int defense,
        double speed,
        double attackCooldown,
        int aggroRange,
        double windup) {

    public EnemyStats {
        if (maxHp <= 0 || speed <= 0 || aggroRange <= 0) {
            throw new IllegalArgumentException("Düşman değerleri pozitif olmalı");
        }
        if (defense < 0) {
            throw new IllegalArgumentException("Savunma negatif olamaz: " + defense);
        }
        if (windup < 0) {
            throw new IllegalArgumentException("Hazırlık süresi negatif olamaz: " + windup);
        }
    }

    /**
     * İşareti olmayan düşman: vuruşu habersiz iner.
     *
     * <p>Çoğu tür böyle ve alan sonradan eklendi. İşareti zorunlu bir
     * parametre yapsaydım her tür "0" yazmak zorunda kalırdı ve o sıfırlar
     * hiçbir şey anlatmazdı — oysa <em>işareti olan</em> düşman özel olan.
     * Bu kurucu varsayılanı adlandırıyor, sıfırı değil.</p>
     */
    public EnemyStats(int maxHp, int attackPower, int defense, double speed,
                      double attackCooldown, int aggroRange) {
        this(maxHp, attackPower, defense, speed, attackCooldown, aggroRange, 0);
    }
}
