package com.cryptdelver.entity;

import com.cryptdelver.game.Game;

/**
 * Oyuncunun yönettiği karakter.
 *
 * <p>Hareket ızgaraya kilitli ve dört yönlü: tuş basılı tutuldukça kare kare
 * ilerler, adım ortasında yön değişmez. Girdiyi doğrudan okumaz — {@code ui}
 * katmanı bir yön bırakır ({@link #setMoveInput}), bu sınıf onu adımlara
 * çevirir. Böylece oyuncu mantığı JavaFX tanımadan test edilebiliyor.</p>
 */
public class Player extends Combatant implements Actor {

    private static final int STARTING_HP = 20;
    private static final int STARTING_ATTACK = 4;

    /** Saniyede kaç kare ilerlediği. */
    private static final double SPEED = 6.0;

    /** İki saldırı arasında beklenen süre (saniye). */
    private static final double ATTACK_COOLDOWN = 0.35;

    /** Vuruşun normalde eriştiği kare sayısı: yan yana. */
    private static final int BASE_ATTACK_RANGE = 1;

    /** Çeviklik ve Acele büyülerinin çarpanları. */
    private static final double SWIFT_SPEED_SCALE = 1.35;
    private static final double HASTE_COOLDOWN_SCALE = 0.65;

    /**
     * Vuruş animasyonunun ekranda kalma süresi.
     *
     * <p>Bekleme süresinin (0.35 sn) yarısı kadar: yay tamamlanacak kadar uzun,
     * ama arka arkaya vururken önceki savuruş bitmeden yenisi başlamayacak
     * kadar kısa.</p>
     */
    private static final double SWING_DURATION = 0.18;

    /** Tek karede işlenecek azami adım; takılma durumunda sonsuz döngüyü keser. */
    private static final int MAX_STEPS_PER_FRAME = 8;

    private Weapon equippedWeapon;
    private Armor equippedArmor;

    private int inputX;
    private int inputY;
    private int facingX = 1;
    private int facingY;
    private boolean attackRequested;
    private double attackCooldown;
    private double swingTimer;

    public Player(int tileX, int tileY) {
        super(tileX, tileY, "Kaşif", STARTING_HP);
    }

    /** Çıplak elle vuruş gücü, üstüne kuşanılan silahın bonusu. */
    @Override
    public int getAttackPower() {
        return STARTING_ATTACK + (equippedWeapon == null ? 0 : equippedWeapon.getAttackBonus());
    }

    /** Elindeki silah; hiçbiri kuşanılmadıysa {@code null}. */
    public Weapon getEquippedWeapon() {
        return equippedWeapon;
    }

    public void equip(Weapon weapon) {
        this.equippedWeapon = weapon;
    }

    /** Silahı elinden bırakır; vuruş gücü çıplak elle değerine döner. */
    public void unequipWeapon() {
        this.equippedWeapon = null;
    }

    /** Üstündeki zırh; hiçbiri kuşanılmadıysa {@code null}. */
    public Armor getEquippedArmor() {
        return equippedArmor;
    }

    public void equip(Armor armor) {
        this.equippedArmor = armor;
    }

    /** Zırhı çıkarır; savunma 0'a döner. */
    public void unequipArmor() {
        this.equippedArmor = null;
    }

    /** Savunma tamamen kuşanılan zırhtan gelir; çıplakken 0. */
    @Override
    public int getDefense() {
        return equippedArmor == null ? 0 : equippedArmor.getDefenseBonus();
    }

    /**
     * Azami canı kalıcı olarak büyütür; mevcut can değişmez.
     *
     * <p>Oyuncunun tek kalıcı büyümesi bu ve <b>yalnızca boss öldürünce</b>
     * geliyor. Eskiden her inişte artıyordu; o zaman dövüşmeden merdiven
     * merdiven kaçan oyuncu da güçleniyordu. Şimdi can tavanı doğrudan
     * "kaç bossu geçtin" sorusunun karşılığı.</p>
     *
     * <p>Mevcut canı doldurmuyor: tavan yükseliyor, dolması sana kalıyor.</p>
     */
    public void gainMaxHp(int extra) {
        raiseMaxHp(extra);
    }

    /** Vuruş animasyonu şu an çizilmeli mi. */
    public boolean isSwinging() {
        return swingTimer > 0;
    }

    /**
     * Vuruş animasyonunun neresinde olduğumuz: 0 başlangıç, 1 bitiş.
     *
     * <p>Ekran bunu kılıcın açısına çeviriyor. Kalan süre yerine ilerleme
     * vermek, animasyonun süresini burada değiştirdiğimizde çizim kodunun
     * etkilenmemesini sağlıyor.</p>
     */
    public double getSwingProgress() {
        return swingTimer <= 0 ? 0 : 1 - swingTimer / SWING_DURATION;
    }

    public boolean isAttackReady() {
        return attackCooldown <= 0;
    }

    /**
     * Bu karede hangi yöne gitmek istediği; yalnızca dört yön.
     *
     * <p>İki eksen birden verilirse yatayı seçiyoruz: ızgarada çapraz adım yok,
     * yol bulma da dört yönlü çalışıyor.</p>
     */
    public void setMoveInput(int dx, int dy) {
        if (dx != 0 && dy != 0) {
            dy = 0;
        }
        this.inputX = Integer.signum(dx);
        this.inputY = Integer.signum(dy);

        // Yön yalnızca gerçek bir girdide güncellenir; tuşu bıraktığında en son
        // baktığın yöne bakmaya devam ediyorsun.
        if (inputX != 0 || inputY != 0) {
            this.facingX = inputX;
            this.facingY = inputY;
        }
    }

    /** Baktığı yönün yatay bileşeni (-1, 0, 1). */
    public int getFacingX() {
        return facingX;
    }

    /** Baktığı yönün dikey bileşeni (-1, 0, 1). */
    public int getFacingY() {
        return facingY;
    }

    /** Saldırı tuşuna basıldığını bildirir; bekleme süresi dolmuşsa işler. */
    public void requestAttack() {
        attackRequested = true;
    }

    /**
     * Yeni bir oyuna başlarken canı ve zamanlayıcıları tazeler.
     *
     * <p>Can tavanı da taban değere dönüyor: bosslardan kazanılan azami can
     * ölümle birlikte gidiyor. Kalsaydı ölmek bedelsiz olurdu — üst üste
     * ölerek can biriktirip aşağı inmek mümkün olurdu.</p>
     */
    public void restore() {
        resetMaxHp(STARTING_HP);
        equippedWeapon = null;
        equippedArmor = null;
        attackCooldown = 0;
        swingTimer = 0;
        attackRequested = false;
        setMoveInput(0, 0);
        facingX = 1;
        facingY = 0;
    }

    @Override
    public void update(Game game, double delta) {
        tickTimers(delta);
        attackCooldown = Math.max(0, attackCooldown - delta);
        swingTimer = Math.max(0, swingTimer - delta);

        if (!isAlive()) {
            return;
        }

        advanceSteps(game, getSpeed() * delta);

        if (attackRequested && attackCooldown <= 0) {
            game.playerAttacks();
            attackCooldown = getAttackCooldown();
            swingTimer = SWING_DURATION;
        }
        attackRequested = false;
    }

    /**
     * Yürüme hızı; Çeviklik büyüsü varsa artıyor.
     *
     * <p>Hız, hasardan bağımsız bir eksen: büyü seni daha güçlü yapmıyor, vur
     * ve kaç oynamayı kolaylaştırıyor. "Büyülü parça bossunkinden iyi olamaz"
     * kuralı bu yüzden bozulmuyor.</p>
     */
    public double getSpeed() {
        return hasArmorEnchantment(Enchantment.CEVIKLIK) ? SPEED * SWIFT_SPEED_SCALE : SPEED;
    }

    /** İki savuruş arası bekleme; Acele büyüsü varsa kısalıyor. */
    public double getAttackCooldown() {
        return hasWeaponEnchantment(Enchantment.ACELE)
                ? ATTACK_COOLDOWN * HASTE_COOLDOWN_SCALE
                : ATTACK_COOLDOWN;
    }

    /**
     * Vuruşun kaç kare uzağa eriştiği; Yıldırım büyüsü bir kare ekliyor.
     *
     * <p>Menzili oyuncunun kendisi söylüyor, {@code Game} değil: hangi büyünün
     * ne yaptığı parçanın bilgisi, oyunun genel kuralı değil.</p>
     */
    public int getAttackRange() {
        return hasWeaponEnchantment(Enchantment.YILDIRIM) ? BASE_ATTACK_RANGE + 1
                : BASE_ATTACK_RANGE;
    }

    public boolean hasWeaponEnchantment(Enchantment enchantment) {
        return equippedWeapon != null && equippedWeapon.getEnchantment() == enchantment;
    }

    public boolean hasArmorEnchantment(Enchantment enchantment) {
        return equippedArmor != null && equippedArmor.getEnchantment() == enchantment;
    }

    /**
     * Verilen ilerleme payını adımlara harcar.
     *
     * <p>Pay bir adımı bitirip artarsa kalanı sonraki adıma taşınıyor; yoksa
     * hız, kare süresine göre dalgalanırdı.</p>
     */
    private void advanceSteps(Game game, double budget) {
        int guard = 0;
        while (budget > 0 && guard++ < MAX_STEPS_PER_FRAME) {
            if (!isMoving()) {
                if (inputX == 0 && inputY == 0) {
                    break;
                }
                if (!game.tryStartStep(this, inputX, inputY)) {
                    break;
                }
            }
            budget = advance(budget);
        }
    }

    /**
     * Kuşanılan zırha göre gövde: <em>aynı kişi, üstünde başka bir zırh</em>.
     *
     * <p>Buraya üç denemede gelindi. Önce zırh kademesine göre bambaşka
     * karakter sprite'ları vardı (elf → cüce → şövalye) ama her zırh
     * değişiminde başka bir insana dönüşüyordun. Sonra envanter ikonu gövdenin
     * üstüne bindirildi; 16 piksellik gövdede bir leke gibi durdu. Sonra
     * gövdeye çalışma zamanında ton kaydırma uygulandı; bu sefer ten ve saç da
     * kaydı, karakter mor bir lekeye döndü.</p>
     *
     * <p>Şimdi gövdenin <b>yalnızca tunik pikselleri</b> zırh renkleriyle
     * yeniden boyanmış ayrı kareler var ({@code player_leather_f0} gibi). Ten,
     * saç ve göz olduğu gibi duruyor. Boyama çalışma zamanında değil, önceden
     * yapıldı: piksel sanatı keskin kalıyor ve çizim katmanı hiçbir efekt
     * uygulamıyor.</p>
     */
    @Override
    public String getSpriteName() {
        if (equippedArmor == null) {
            return "player";
        }

        String tag = equippedArmor.getBodyTag();
        return tag.isEmpty() ? "player" : "player_" + tag;
    }
}
