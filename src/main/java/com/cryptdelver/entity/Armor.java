package com.cryptdelver.entity;

import com.cryptdelver.game.Game;

/**
 * Kuşanılabilir zırh: gelen hasarı azaltır.
 *
 * <p>Silahla aynı mantık, ters yönde çalışıyor: {@link Weapon} vuruş gücüne
 * eklenir, zırh gelen hasardan düşülür. İkisi de kullanıldığında tükenmez
 * ({@code use} {@code false} döner), çantada kalır; istediğinde eskisine geri
 * dönebilirsin.</p>
 */
public class Armor extends Item {

    private final int defenseBonus;
    private final String spriteName;

    public Armor(int tileX, int tileY, String name, int defenseBonus, String spriteName) {
        super(tileX, tileY, name);
        this.defenseBonus = defenseBonus;
        this.spriteName = spriteName;
    }

    public int getDefenseBonus() {
        return defenseBonus;
    }

    @Override
    public boolean use(Game game) {
        Player player = game.getPlayer();

        if (player.getEquippedArmor() == this) {
            game.getMessageLog().add(getName() + " zaten üstünde.");
            return false;
        }

        player.equip(this);
        game.getMessageLog().add(getName() + " kuşandın (+" + defenseBonus + " savunma).");
        return false;
    }

    @Override
    public String getSpriteName() {
        return spriteName;
    }
}
