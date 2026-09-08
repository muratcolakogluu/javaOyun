package com.cryptdelver.entity;

import com.cryptdelver.game.Game;

/**
 * Kuşanılabilir kalkan: zırhın üstüne savunma ekler.
 *
 * <p>Zırhla aynı işi yapar ama <em>ayrı bir slotta</em> durur; ikisi birden
 * kuşanılabildiği için savunma toplanır. Kalkanların katkısı zırhtan
 * kasten düşük tutuldu — asıl korumayı zırh sağlıyor, kalkan üstüne
 * ekliyor.</p>
 */
public class Shield extends Item {

    private final int defenseBonus;
    private final String spriteName;

    public Shield(int tileX, int tileY, String name, int defenseBonus, String spriteName) {
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

        if (player.getEquippedShield() == this) {
            game.getMessageLog().add(getName() + " zaten kolunda.");
            return false;
        }

        player.equip(this);
        game.getMessageLog().add(getName() + " kuşandın (+" + defenseBonus + " savunma).");
        return false;
    }

    /** Yerden alındığında, elindekinden iyiyse kendiliğinden kuşanılır. */
    @Override
    public void onPickup(Game game) {
        Player player = game.getPlayer();
        Shield current = player.getEquippedShield();

        if (current == null || defenseBonus > current.getDefenseBonus()) {
            player.equip(this);
            game.getMessageLog().add(getName() + " kuşandın (+" + defenseBonus + " savunma).");
        }
    }

    /** Kuşanılmış kalkan yere bırakılırsa kolundan da çıkar. */
    @Override
    public void onDrop(Game game) {
        if (game.getPlayer().getEquippedShield() == this) {
            game.getPlayer().unequipShield();
        }
    }

    @Override
    public String getSaveKind() {
        return "SHIELD";
    }

    @Override
    public int getSaveValue() {
        return defenseBonus;
    }

    @Override
    public String getSpriteName() {
        return spriteName;
    }
}
