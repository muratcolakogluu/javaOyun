package com.cryptdelver.entity;

import com.cryptdelver.game.Game;

/**
 * Kuşanılabilir kask: başa takılır, savunmaya eklenir.
 *
 * <p>Zırh ve kalkanla aynı mantık, dördüncü slotta. Üçü de savunmaya
 * eklendiği için kaskın katkısı en küçük tutuldu; asıl korumayı gövde zırhı
 * sağlıyor.</p>
 */
public class Helmet extends Item {

    private final int defenseBonus;
    private final String spriteName;

    public Helmet(int tileX, int tileY, String name, int defenseBonus, String spriteName) {
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

        if (player.getEquippedHelmet() == this) {
            game.getMessageLog().add(getName() + " zaten başında.");
            return false;
        }

        player.equip(this);
        game.getMessageLog().add(getName() + " taktın (+" + defenseBonus + " savunma).");
        return false;
    }

    /** Yerden alındığında, baştakinden iyiyse kendiliğinden takılır. */
    @Override
    public void onPickup(Game game) {
        Player player = game.getPlayer();
        Helmet current = player.getEquippedHelmet();

        if (current == null || defenseBonus > current.getDefenseBonus()) {
            player.equip(this);
            game.getMessageLog().add(getName() + " taktın (+" + defenseBonus + " savunma).");
        }
    }

    /** Takılı kask yere bırakılırsa baştan da çıkar. */
    @Override
    public void onDrop(Game game) {
        if (game.getPlayer().getEquippedHelmet() == this) {
            game.getPlayer().unequipHelmet();
        }
    }

    @Override
    public String getSaveKind() {
        return "HELMET";
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
