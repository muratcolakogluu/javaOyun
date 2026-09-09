package com.cryptdelver.entity;

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
 */
public class Blacksmith extends Entity {

    public Blacksmith(int tileX, int tileY) {
        super(tileX, tileY, "Demirci");
    }

    @Override
    public String getSpriteName() {
        return "blacksmith";
    }
}
