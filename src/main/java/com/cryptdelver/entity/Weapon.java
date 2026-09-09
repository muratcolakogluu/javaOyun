package com.cryptdelver.entity;

import com.cryptdelver.game.Game;
import com.cryptdelver.game.LootTable;
import java.util.List;

/**
 * Kuşanılabilir silah: vuruş gücüne bonus ekler.
 *
 * <p>Kullanıldığında tükenmez, kuşanılır — {@code use} metodu {@code false}
 * döndürdüğü için çantada kalır. İkinci bir silah kuşanmak öncekini çantada
 * bırakır, istediğinde geri dönebilirsin.</p>
 *
 * <p>Dayanıklılık ve yükseltme işleri {@link Equipment} sınıfında; burada
 * yalnızca "bu bonus vuruşa yazılır" bilgisi var.</p>
 */
public class Weapon extends Equipment {

    public Weapon(int tileX, int tileY, String name, int attackBonus, String spriteName) {
        this(tileX, tileY, name, attackBonus, spriteName, LootTable.weaponDurabilityFor(attackBonus));
    }

    public Weapon(int tileX, int tileY, String name, int attackBonus, String spriteName,
                  int maxDurability) {
        super(tileX, tileY, name, attackBonus, spriteName, maxDurability);
    }

    /** Dövüşte işleyen bonus: kırık kılıç hiçbir şey vermiyor. */
    public int getAttackBonus() {
        return getEffectiveBonus();
    }

    @Override
    public int upgradeCeiling(int depth) {
        return LootTable.weaponBonusForTier(LootTable.bossTierForDepth(depth));
    }

    /**
     * Kılıca basılabilen büyüler.
     *
     * <p>Vampirlik silaha özel: can emmek vurmakla oluyor. Sağlamlık her iki
     * parçada da işe yarıyor, o yüzden iki listede de var.</p>
     */
    @Override
    public List<Enchantment> availableEnchantments() {
        return List.of(Enchantment.VAMPIRLIK, Enchantment.SAGLAMLIK);
    }

    @Override
    public boolean use(Game game) {
        Player player = game.getPlayer();

        if (player.getEquippedWeapon() == this) {
            game.getMessageLog().add(getDisplayName() + " zaten elinde.");
            return false;
        }

        player.equip(this);
        game.getMessageLog().add(getDisplayName() + " kuşandın (+" + getBonus() + " vuruş).");
        return false;
    }

    /**
     * Yerden alındığında, elindekinden iyiyse kendiliğinden kuşanılır.
     *
     * <p>Karşılaştırma yıpranmış değerle değil <em>kağıt üstündeki</em> değerle
     * yapılıyor: kırık ama iyi bir kılıcı, tamir edilebilecekken sağlam ama
     * kötü bir kılıçla değiştirmek istemezsin.</p>
     *
     * <p>Daha kötüsü otomatik takılmaz: zoraki bir "geri alma" hamlesi
     * yaptırmamak için. Onu elle takmak istersen çantada duruyor.</p>
     */
    @Override
    public void onPickup(Game game) {
        Player player = game.getPlayer();
        Weapon current = player.getEquippedWeapon();

        if (current == null || getBonus() > current.getBonus()) {
            player.equip(this);
            game.getMessageLog().add(getDisplayName() + " kuşandın (+" + getBonus() + " vuruş).");
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
}
