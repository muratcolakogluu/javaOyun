package com.cryptdelver.entity;

import java.util.List;

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
 * inince parça <em>kırılıyor</em> ve bonusunu tamamen kaybediyor — kırık
 * kılıç elinde sopa, kırık zırh üstünde paçavra.</p>
 *
 * <p>Bu, dayanıklılığı gerçek bir kaynak yapıyor: büyücü beş katta bir
 * olduğu için "bu kılıçla iki kat daha idare eder miyim" sorusunun bedeli
 * var. Yumuşak cezalar (yarım, çeyrek bonus) denendi ve fark
 * edilmiyordu.</p>
 *
 * <h2>Yükseltme</h2>
 * <p>Her kademe bonusa +1 ekliyor ve parçayı yeniliyor. Tavanı
 * {@link #upgradeCeiling(int)} belirliyor: bulunduğun katta bossun bırakacağı
 * parçanın <em>bir altı</em>. Yani altın biriktirip bossu atlamak yok —
 * yükseltme seni ayakta tutuyor, sıçramayı hâlâ boss yaptırıyor.</p>
 */
public abstract class Equipment extends Item {


    private final int baseBonus;
    private final int maxDurability;
    private final String spriteName;

    private int durability;
    private int upgradeLevel;
    private Enchantment enchantment;

    /**
     * Kaç kez aşınma denendiği.
     *
     * <p>Yalnızca {@link Enchantment#SAGLAMLIK} için gerekli: o büyü aşınmayı
     * yarıya indiriyor, bunun için de tek sayılı denemeleri atlamak yetiyor.
     * Kesirli dayanıklılık tutmaktansa sayacı tutmak hem daha basit hem de
     * ekranda gösterilen sayıyı tam sayı olarak koruyor.</p>
     */
    private int wearAttempts;

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

    /**
     * Dövüşte gerçekten işleyen bonus; <b>kırık parça hiçbir şey vermiyor</b>.
     *
     * <p>Önce yarısı, sonra çeyreği veriliyordu. İkisi de yetmedi: taban vuruş
     * zaten 4 olduğu için kırık kılıçla katlarca dolaşıp farkı anlamamak
     * mümkündü. Sıfır, kuralı tartışmasız yapıyor — kırık kılıç elinde
     * sopadan farksız, kırık zırh üstünde paçavra.</p>
     *
     * <p>Parça yine de kaybolmuyor: dayanıklılığı sıfır, ama tamir edilince
     * bütün bonusu geri geliyor. Ayakta kalmanı sağlayan şey oyuncunun taban
     * vuruşu — yani kırık takımla dövüşebiliyorsun, sadece kötü
     * dövüşüyorsun.</p>
     */
    public int getEffectiveBonus() {
        return isBroken() ? 0 : getBonus();
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

        wearAttempts++;
        if (enchantment == Enchantment.SAGLAMLIK && wearAttempts % 2 != 0) {
            return false;
        }

        durability--;
        return durability == 0;
    }

    // ------------------------------------------------------------------ büyü

    /** Parçadaki büyü; yoksa {@code null}. */
    public Enchantment getEnchantment() {
        return enchantment;
    }

    /**
     * Büyüyü basar; parçada zaten bir büyü varsa onun yerine geçer.
     *
     * <p>Bir parçada bir büyü duruyor. Üst üste yığılabilseydi tek bir kılıcı
     * sonsuza kadar besleyip her şeyi çözerdin; tek yuva olunca "bu kılıçta
     * hangisi dursun" diye karar vermen gerekiyor.</p>
     */
    public void enchant(Enchantment enchantment) {
        this.enchantment = enchantment;
    }

    /** Bu parçaya basılabilen büyüler; silah ve zırh farklı listeler veriyor. */
    public abstract List<Enchantment> availableEnchantments();

    public boolean accepts(Enchantment candidate) {
        return candidate != null && availableEnchantments().contains(candidate);
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

    /** Çantada ve tezgâhta görünen tam ad: yükseltme kademesi ve büyüsüyle. */
    public String getFullName() {
        return enchantment == null
                ? getDisplayName()
                : getDisplayName() + " [" + enchantment.getLabel() + "]";
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

    /** Çantada ve büyücüde görünen ad; yükseltilmişse kademesiyle. */
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
    public String getSaveEnchantment() {
        return enchantment == null ? "" : enchantment.name();
    }

    @Override
    public boolean isEnchanted() {
        return enchantment != null;
    }

    @Override
    public String getSpriteName() {
        return spriteName;
    }
}
