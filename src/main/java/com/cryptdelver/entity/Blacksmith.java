package com.cryptdelver.entity;

import com.cryptdelver.game.Game;

/**
 * Boss katlarında duran demirci.
 *
 * <p>Dövüşmez, hareket etmez, ölmez — bu yüzden {@link Combatant} değil düz bir
 * {@link Entity}. Tek işi orada durmak; ne yapabileceğine {@code Game} karar
 * veriyor, yanına gidip {@code F} tuşuna basınca demirci ekranı açılıyor.</p>
 *
 * <p>Neden yalnızca boss katlarında: altının bir yere harcanması gerekiyordu ama
 * her katta bir demirci olsaydı yıpranma diye bir şey kalmazdı — her kat sonu
 * uğrar, hiç düşünmeden tamir ettirirdin. Beş katta bir olunca dayanıklılık
 * gerçekten bir kaynak oluyor: "bu kılıçla iki kat daha idare eder miyim?"
 * sorusu ancak böyle anlam kazanıyor.</p>
 *
 * <p><b>Ne söyleyeceğini de kendi biliyor.</b> {@link #greetingFor(Game)}
 * takımının hâline bakıp tek bir cümle seçiyor. Rastgele lafların sırayla
 * dönmesi de olurdu ama o zaman balon süs olurdu; böyle bakınca demirci sana
 * <em>o an</em> işine yarayacak şeyi söylüyor.</p>
 */
public class Blacksmith extends Entity {

    /** Dayanıklılık bunun altına düşünce demirci laf atmaya başlıyor. */
    private static final double WORN_RATIO = 0.5;

    public Blacksmith(int tileX, int tileY) {
        super(tileX, tileY, "Demirci");
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
            return "Kırılmış o! Bırak da döveyim.";
        }
        if (isWorn(weapon) || isWorn(armor)) {
            return "Şu takımın hâline baksana. Tamir ister.";
        }
        if (weapon == null && armor == null) {
            return "Elin boş gezme. Bir demir bul, işleyeyim.";
        }
        if (canUpgrade(weapon, game) || canUpgrade(armor, game)) {
            return "Altınını sayma, çeliğini büyütelim.";
        }
        if (game.isStairsLocked()) {
            return "Aşağıdakine böyle gitme, bir düşün.";
        }
        return "Demir sıcak, çekiç hazır.";
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
        return "blacksmith";
    }
}
