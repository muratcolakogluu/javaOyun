package com.cryptdelver.entity;

import com.cryptdelver.game.Game;

/**
 * Bomba: kullanıldığı anda çevredeki herkesi vurur.
 *
 * <p>Oyundaki tek <em>alan</em> hasarı bu. Kılıç yan yana olduğunu vuruyor,
 * bomba ise çevrende ne varsa hepsine aynı anda ulaşıyor — yani kalabalığın
 * ortasında sıkışmak artık ölüm demek değil, bir çıkış yolu var.</p>
 *
 * <p>Hasar zırhtan etkilenmiyor: bomba bir vuruş değil, patlama. Bu sayede
 * derin katlarda kalın zırhlı düşmanlara karşı da işe yarıyor, yani "sakla,
 * lazım olacak" hissi kaybolmuyor.</p>
 *
 * <p>Kendine zarar vermiyor. Vermesi daha gerçekçi olurdu ama can zaten
 * oyundaki en kıt kaynak; bombayı kullanmayı bir de canla ödemek onu çantada
 * çürüten bir eşyaya çevirirdi.</p>
 */
public class Bomb extends Item {

    /** Patlamanın kaç kare uzağa ulaştığı. */
    public static final int BLAST_RADIUS = 3;

    /** Menzildeki her düşmana verilen hasar. */
    public static final int BLAST_DAMAGE = 12;

    public Bomb(int tileX, int tileY) {
        super(tileX, tileY, "Bomba");
    }

    @Override
    public boolean use(Game game) {
        game.detonate(BLAST_RADIUS, BLAST_DAMAGE);
        return true;
    }

    @Override
    public boolean isStackable() {
        return true;
    }

    @Override
    public String getSaveKind() {
        return "BOMB";
    }

    @Override
    public String getSpriteName() {
        return "bomb";
    }
}
