package com.cryptdelver.entity;

import com.cryptdelver.game.Text;

import com.cryptdelver.game.Game;

/**
 * Boss katlarında duran büyücü.
 *
 * <p>Dövüşmez, hareket etmez, ölmez — bu yüzden {@link Combatant} değil düz bir
 * {@link Entity}. Tek işi orada durmak; ne yapabileceğine {@code Game} karar
 * veriyor, yanına gidip {@code F} tuşuna basınca büyücü ekranı açılıyor.</p>
 *
 * <p>Neden yalnızca boss katlarında: altının bir yere harcanması gerekiyordu ama
 * her katta bir büyücü olsaydı yıpranma diye bir şey kalmazdı — her kat sonu
 * uğrar, hiç düşünmeden tamir ettirirdin. Beş katta bir olunca dayanıklılık
 * gerçekten bir kaynak oluyor: "bu kılıçla iki kat daha idare eder miyim?"
 * sorusu ancak böyle anlam kazanıyor.</p>
 *
 * <p><b>Ne söyleyeceğini de kendi biliyor.</b> {@link #greetingFor(Game)}
 * takımının hâline bakıp tek bir cümle seçiyor. Rastgele lafların sırayla
 * dönmesi de olurdu ama o zaman balon süs olurdu; böyle bakınca büyücü sana
 * <em>o an</em> işine yarayacak şeyi söylüyor.</p>
 */
public class Wizard extends Entity {

    /** Dayanıklılık bunun altına düşünce büyücü laf atmaya başlıyor. */
    private static final double WORN_RATIO = 0.5;

    public Wizard(int tileX, int tileY) {
        super(tileX, tileY, Text.WIZARD_NAME);
    }

    /**
     * Oyuncunun durumuna göre tek satırlık laf.
     *
     * <p>Sıra önemli: en acil olan kazanıyor. Kırık parça her şeyin önünde,
     * çünkü oyuncu yarım güçle dövüşüyor ve bunu fark etmemiş olabilir.</p>
     */
    public String greetingFor(Game game) {
        Player player = game.getPlayer();
        Equipment weapon = player.getEquippedWeapon();
        Equipment armor = player.getEquippedArmor();

        if (isBroken(weapon) || isBroken(armor)) {
            return Text.WIZARD_BROKEN.get();
        }
        if (isWorn(weapon) || isWorn(armor)) {
            return Text.WIZARD_WORN.get();
        }
        if (weapon == null && armor == null) {
            return Text.WIZARD_EMPTY.get();
        }
        if (canUpgrade(weapon, game) || canUpgrade(armor, game)) {
            return Text.WIZARD_UPGRADE.get();
        }
        if (game.isStairsLocked()) {
            return Text.WIZARD_BOSS.get();
        }
        return Text.WIZARD_IDLE.get();
    }

    private static boolean isBroken(Equipment item) {
        return item != null && item.isBroken();
    }

    private static boolean isWorn(Equipment item) {
        return item != null && item.getDurability() < item.getMaxDurability() * WORN_RATIO;
    }

    private static boolean canUpgrade(Equipment item, Game game) {
        return item != null && item.canUpgrade(game.getDepth());
    }

    @Override
    public String getSpriteName() {
        return "wizard";
    }
}
