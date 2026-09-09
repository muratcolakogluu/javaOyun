package com.cryptdelver.entity;

import com.cryptdelver.game.Game;
import com.cryptdelver.game.LootTable;

/**
 * Kuşanılabilir zırh: gelen hasarı azaltır.
 *
 * <p>Silahla aynı mantık, ters yönde çalışıyor: {@link Weapon} vuruş gücüne
 * eklenir, zırh gelen hasardan düşülür. İkisi de {@link Equipment} soyundan
 * geliyor, dayanıklılık ve yükseltme kuralları ortak.</p>
 */
public class Armor extends Equipment {

    public Armor(int tileX, int tileY, String name, int defenseBonus, String spriteName) {
        this(tileX, tileY, name, defenseBonus, spriteName, LootTable.armorDurabilityFor(defenseBonus));
    }

    public Armor(int tileX, int tileY, String name, int defenseBonus, String spriteName,
                 int maxDurability) {
        super(tileX, tileY, name, defenseBonus, spriteName, maxDurability);
    }

    /** Dövüşte işleyen bonus: parçalanmış zırh yarım korur. */
    public int getDefenseBonus() {
        return getEffectiveBonus();
    }

    @Override
    public int upgradeCeiling(int depth) {
        return LootTable.armorBonusForTier(LootTable.bossTierForDepth(depth));
    }

    @Override
    public boolean use(Game game) {
        Player player = game.getPlayer();

        if (player.getEquippedArmor() == this) {
            game.getMessageLog().add(getDisplayName() + " zaten üstünde.");
            return false;
        }

        player.equip(this);
        game.getMessageLog().add(getDisplayName() + " kuşandın (+" + getBonus() + " savunma).");
        return false;
    }

    /**
     * Yerden alındığında, üstündekinden iyiyse kendiliğinden kuşanılır.
     *
     * <p>Silahla aynı kural: karşılaştırma kağıt üstündeki değerle yapılıyor ve
     * daha kötüsü otomatik takılmıyor, çantada bekliyor.</p>
     */
    @Override
    public void onPickup(Game game) {
        Player player = game.getPlayer();
        Armor current = player.getEquippedArmor();

        if (current == null || getBonus() > current.getBonus()) {
            player.equip(this);
            game.getMessageLog().add(getDisplayName() + " kuşandın (+" + getBonus() + " savunma).");
        }
    }

    /** Kuşanılmış zırh yere bırakılırsa üstünden de çıkar. */
    @Override
    public void onDrop(Game game) {
        if (game.getPlayer().getEquippedArmor() == this) {
            game.getPlayer().unequipArmor();
        }
    }

    @Override
    public String getSaveKind() {
        return "ARMOR";
    }
}
