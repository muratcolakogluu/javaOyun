package com.cryptdelver.entity;

import com.cryptdelver.game.Game;

/**
 * Kuşanılabilir silah: vuruş gücüne kalıcı bonus ekler.
 *
 * <p>Kullanıldığında tükenmez, kuşanılır — {@code use} metodu {@code false}
 * döndürdüğü için çantada kalır. İkinci bir silah kuşanmak öncekini çantada
 * bırakır, istediğinde geri dönebilirsin.</p>
 */
public class Weapon extends Item {

    private final int attackBonus;
    private final String spriteName;

    public Weapon(int tileX, int tileY, String name, int attackBonus, String spriteName) {
        super(tileX, tileY, name);
        this.attackBonus = attackBonus;
        this.spriteName = spriteName;
    }

    /** Erken katlarda bulunan mütevazı silah. */
    public static Weapon rustySword(int tileX, int tileY) {
        return new Weapon(tileX, tileY, "Paslı Kılıç", 2, "sword");
    }

    /** Daha nadir ve daha sert vuran silah. */
    public static Weapon battleAxe(int tileX, int tileY) {
        return new Weapon(tileX, tileY, "Savaş Baltası", 4, "axe");
    }

    public int getAttackBonus() {
        return attackBonus;
    }

    @Override
    public boolean use(Game game) {
        Player player = game.getPlayer();

        if (player.getEquippedWeapon() == this) {
            game.getMessageLog().add(getName() + " zaten elinde.");
            return false;
        }

        player.equip(this);
        game.getMessageLog().add(getName() + " kuşandın (+" + attackBonus + " vuruş).");
        return false;
    }

    @Override
    public String getSpriteName() {
        return spriteName;
    }
}
