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

    /** Vuruş animasyonunun ekranda kalma süresi. */
    private static final double SWING_DURATION = 0.14;

    /** Tek karede işlenecek azami adım; takılma durumunda sonsuz döngüyü keser. */
    private static final int MAX_STEPS_PER_FRAME = 8;

    private Weapon equippedWeapon;
    private Armor equippedArmor;

    private int inputX;
    private int inputY;
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

    /** Vuruş animasyonu şu an çizilmeli mi. */
    public boolean isSwinging() {
        return swingTimer > 0;
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
    }

    /** Saldırı tuşuna basıldığını bildirir; bekleme süresi dolmuşsa işler. */
    public void requestAttack() {
        attackRequested = true;
    }

    /** Yeni bir oyuna başlarken canı ve zamanlayıcıları tazeler. */
    public void restore() {
        restoreFullHealth();
        equippedWeapon = null;
        equippedArmor = null;
        attackCooldown = 0;
        swingTimer = 0;
        attackRequested = false;
        setMoveInput(0, 0);
    }

    @Override
    public void update(Game game, double delta) {
        tickTimers(delta);
        attackCooldown = Math.max(0, attackCooldown - delta);
        swingTimer = Math.max(0, swingTimer - delta);

        if (!isAlive()) {
            return;
        }

        advanceSteps(game, SPEED * delta);

        if (attackRequested && attackCooldown <= 0) {
            game.playerAttacks();
            attackCooldown = ATTACK_COOLDOWN;
            swingTimer = SWING_DURATION;
        }
        attackRequested = false;
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

    @Override
    public String getSpriteName() {
        return "player";
    }
}
