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

    /**
     * Yerden alındığında, elindekinden iyiyse kendiliğinden kuşanılır.
     *
     * <p>Daha kötüsü otomatik takılmaz: zoraki bir "geri alma" hamlesi
     * yaptırmamak için. Onu elle takmak istersen çantada duruyor.</p>
     */
    @Override
    public void onPickup(Game game) {
        Player player = game.getPlayer();
        Weapon current = player.getEquippedWeapon();

        if (current == null || attackBonus > current.getAttackBonus()) {
            player.equip(this);
            game.getMessageLog().add(getName() + " kuşandın (+" + attackBonus + " vuruş).");
        }
    }

    /** Kuşanılmış silah yere bırakılırsa elden de çıkar. */
    @Override
    public void onDrop(Game game) {
        if (game.getPlayer().getEquippedWeapon() == this) {
            game.getPlayer().unequipWeapon();
        }
    }

    @Override
    public String getSaveKind() {
        return "WEAPON";
    }

    @Override
    public int getSaveValue() {
        return attackBonus;
    }

    @Override
    public String getSpriteName() {
        return spriteName;
    }
}
