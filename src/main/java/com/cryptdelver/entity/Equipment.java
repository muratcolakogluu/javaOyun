package com.cryptdelver.entity;

/**
 * Kuşanılan parçaların ortak atası: silah ve zırh.
 *
 * <p>İkisinin de aynı üç şeyi var — bir bonus, bir dayanıklılık havuzu ve bir
 * yükseltme kademesi. Farkları yalnızca bonusun <em>nereye</em> yazıldığı:
 * silahınki vuruşa, zırhınki savunmaya. O yüzden sayıların yönetimi burada,
 * anlamı alt sınıflarda duruyor.</p>
 *
 * <h2>Dayanıklılık</h2>
 * <p>Kullandıkça aşınıyor: silah isabet ettikçe, zırh darbe yedikçe. Sıfıra
 * inince parça <em>kırılıyor</em> ve bonusunun yarısını veriyor. Tamamen işe
 * yaramaz hâle getirmedim: kırık kılıçla kalan oyuncunun bir sonraki boss
 * katına kadar hiç şansı kalmazdı, oysa demirci yalnızca orada.</p>
 *
 * <h2>Yükseltme</h2>
 * <p>Her kademe bonusa +1 ekliyor ve parçayı yeniliyor. Tavanı
 * {@link #upgradeCeiling(int)} belirliyor: bulunduğun katta bossun bırakacağı
 * parçanın <em>bir altı</em>. Yani altın biriktirip bossu atlamak yok —
 * yükseltme seni ayakta tutuyor, sıçramayı hâlâ boss yaptırıyor.</p>
 */
public abstract class Equipment extends Item {

    /** Kırık parçanın bonusu bu katsayıyla küçülür. */
    private static final double BROKEN_RATIO = 0.5;

    private final int baseBonus;
    private final int maxDurability;
    private final String spriteName;

    private int durability;
    private int upgradeLevel;

    protected Equipment(int tileX, int tileY, String name, int baseBonus,
                        String spriteName, int maxDurability) {
        super(tileX, tileY, name);
        this.baseBonus = baseBonus;
        this.spriteName = spriteName;
        this.maxDurability = Math.max(1, maxDurability);
        this.durability = this.maxDurability;
    }

    /** Parçanın kağıt üstündeki değeri: taban bonus artı yükseltmeler. */
    public int getBonus() {
        return baseBonus + upgradeLevel;
    }

    /** Dövüşte gerçekten işleyen bonus; kırıksa yarısı. */
    public int getEffectiveBonus() {
        return isBroken() ? (int) (getBonus() * BROKEN_RATIO) : getBonus();
    }

    public int getBaseBonus() {
        return baseBonus;
    }

    public int getDurability() {
        return durability;
    }

    public int getMaxDurability() {
        return maxDurability;
    }

    public int getUpgradeLevel() {
        return upgradeLevel;
    }

    public boolean isBroken() {
        return durability <= 0;
    }

    public boolean needsRepair() {
        return durability < maxDurability;
    }

    /**
     * Bir puan aşındırır.
     *
     * @return bu aşınmayla kırıldıysa {@code true} — çağıran mesaj yazsın diye
     */
    public boolean wear() {
        if (durability <= 0) {
            return false;
        }

        durability--;
        return durability == 0;
    }

    /** Dayanıklılığı doldurur. */
    public void repair() {
        durability = maxDurability;
    }

    /** Bir kademe yükseltir ve parçayı yeniler. */
    public void upgrade() {
        upgradeLevel++;
        repair();
    }

    /** Kayıttan dönerken kullanılıyor; oyun içi akışta çağrılmaz. */
    public void restoreState(int durability, int upgradeLevel) {
        this.upgradeLevel = Math.max(0, upgradeLevel);
        this.durability = Math.clamp(durability, 0, maxDurability);
    }

    /**
     * Bu parçanın verilen katta çıkabileceği azami bonus.
     *
     * <p>Silah ve zırh farklı tablolara baktığı için alt sınıflar yanıtlıyor.</p>
     */
    public abstract int upgradeCeiling(int depth);

    /** Yükseltilebilir mi: tavana dayanmadıysa evet. */
    public boolean canUpgrade(int depth) {
        return getBonus() < upgradeCeiling(depth);
    }

    /** Çantada ve demircide görünen ad; yükseltilmişse kademesiyle. */
    public String getDisplayName() {
        return upgradeLevel > 0 ? getName() + " +" + upgradeLevel : getName();
    }

    /** Kayıtta taban bonus saklanıyor; yükseltmeler ayrı alanda. */
    @Override
    public int getSaveValue() {
        return baseBonus;
    }

    @Override
    public int getSaveDurability() {
        return durability;
    }

    @Override
    public int getSaveUpgradeLevel() {
        return upgradeLevel;
    }

    @Override
    public String getSpriteName() {
        return spriteName;
    }
}
