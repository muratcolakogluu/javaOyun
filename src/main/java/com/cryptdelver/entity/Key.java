package com.cryptdelver.entity;

import com.cryptdelver.game.Game;
import com.cryptdelver.game.Text;

/**
 * Kilitli mahzenin anahtarı.
 *
 * <p>Kattaki bir düşman taşıyor ve ölünce düşürüyor. Yani mahzeni açmak
 * haritada bir yere gitmekten değil, <b>doğru düşmanı bulup devirmekten</b>
 * geçiyor — katın kendisi bir soru soruyor: "şu kalabalığın içindeki
 * hangisi?"</p>
 *
 * <h2>Neden bir eşya, bir sayaç değil</h2>
 * <p>Oyunun durumunda {@code hasKey} diye bir bayrak tutmak daha kısa olurdu.
 * Ama o zaman anahtar görünmez bir şey olurdu: yerde duramaz, çantada yer
 * kaplamaz, bırakılamaz. Oyundaki her şey eşya — anahtarın kuralın dışında
 * kalması için bir sebep yok, ve çantada bir slot tutması onu gerçek bir
 * yük yapıyor.</p>
 *
 * <p>Kendiliğinden toplanıyor: tek bir anahtar için düşmanın öldüğü kareye
 * basıp ayrıca F'ye basmak, hiçbir karar içermeyen fazladan bir adım
 * olurdu.</p>
 */
public class Key extends Item {

    public Key(int tileX, int tileY) {
        super(tileX, tileY, Text.ITEM_KEY);
    }

    /**
     * Anahtar kullanılmıyor, <em>kapıda</em> harcanıyor.
     *
     * <p>Çantadan seçip kullanmak mantıklı görünürdü ama kapının önünde
     * durmadan anahtarı kullanmanın hiçbir anlamı yok. Kapıyı açan şey F ve
     * anahtarı da o iş harcıyor; burada yapılacak bir şey yok.</p>
     */
    @Override
    public boolean use(Game game) {
        game.getMessageLog().item(Text.MSG_KEY_IDLE.get());
        return false;
    }

    @Override
    public String getKind() {
        return "KEY";
    }

    @Override
    public String getDescription() {
        return Text.ITEM_KEY_INFO.get();
    }

    /** Bir anahtar bir kapı açıyor, yani ikincisi ayrı bir yük değil. */
    @Override
    public boolean isStackable() {
        return true;
    }

    @Override
    public boolean isAutoPickedUp() {
        return true;
    }

    @Override
    public String getSpriteName() {
        return "key";
    }
}
