package com.cryptdelver.entity;

import com.cryptdelver.game.Game;
import com.cryptdelver.game.LootTable;
import java.util.List;

/**
 * Kuşanılabilir zırh: gelen hasarı azaltır.
 *
 * <p>Silahla aynı mantık, ters yönde çalışıyor: {@link Weapon} vuruş gücüne
 * eklenir, zırh gelen hasardan düşülür. İkisi de {@link Equipment} soyundan
 * geliyor, dayanıklılık ve yükseltme kuralları ortak.</p>
 */
public class Armor extends Equipment {

    /** Zırh sprite adlarının ortak başı; kademe etiketi bunun devamı. */
    private static final String SPRITE_PREFIX = "armor_";

    public Armor(int tileX, int tileY, String name, int defenseBonus, String spriteName) {
        this(tileX, tileY, name, defenseBonus, spriteName, LootTable.armorDurabilityFor(defenseBonus));
    }

    public Armor(int tileX, int tileY, String name, int defenseBonus, String spriteName,
                 int maxDurability) {
        super(tileX, tileY, name, defenseBonus, spriteName, maxDurability);
    }

    @Override
    public String getDescription() {
        return "+" + getBonus() + " savunma  ·  " + getDurability() + "/" + getMaxDurability()
                + (isBroken() ? "  KIRIK" : "");
    }

    /** Dövüşte işleyen bonus: parçalanmış zırh hiç korumuyor. */
    public int getDefenseBonus() {
        return getEffectiveBonus();
    }

    /**
     * Bu zırhın kademe etiketi: {@code "leather"}, {@code "chain"}...
     *
     * <p>Giyen taraf gövde sprite'ının adını bununla kuruyor. Zırh "beni giyen
     * hangi resmi kullansın" sorusunu yanıtlamıyor — kendi kademesini söylüyor,
     * gerisi {@link Player#getSpriteName()} işi.</p>
     *
     * <p>Tanımadığımız bir sprite adı gelirse (elle düzenlenmiş eski bir kayıt)
     * boş dönüyor ve giyen taban gövdesinde kalıyor; olmayan bir dosyaya
     * gidip şekil çizimine düşmektense zırhsız görünmek daha az yanlış.</p>
     */
    public String getBodyTag() {
        String sprite = getSpriteName();
        return sprite.startsWith(SPRITE_PREFIX) ? sprite.substring(SPRITE_PREFIX.length()) : "";
    }

    @Override
    public int upgradeCeiling(int depth) {
        return LootTable.armorBonusForTier(LootTable.bossTierForDepth(depth));
    }

    /**
     * Zırha basılabilen büyüler.
     *
     * <p>Diken zırha özel: yansıtmak için önce darbe yemen gerekiyor.</p>
     */
    @Override
    public List<Enchantment> availableEnchantments() {
        return List.of(Enchantment.DIKEN, Enchantment.YENILENME, Enchantment.CEVIKLIK,
                Enchantment.SAGLAMLIK);
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
     * Yerden alındığında, üstündekinden iyiyse kendiliğinden kuşanılır ve
     * <em>eskisi yere bırakılır</em>.
     *
     * <p>Karşılaştırma yıpranmış değerle değil kağıt üstündeki değerle
     * yapılıyor; daha kötüsü otomatik takılmıyor, çantada bekliyor.</p>
     *
     * <p>Eski zırhı çantada tutmanın bir anlamı yoktu: geri dönüp kötü zırhı
     * giymek diye bir şey yok, ama slotlar birkaç katta doluyordu. Artık yere
     * düşüyor — fikrini değiştirirsen hâlâ ayağının dibinde.</p>
     */
    @Override
    public void onPickup(Game game) {
        Player player = game.getPlayer();
        Armor current = player.getEquippedArmor();

        if (current != null && getBonus() <= current.getBonus()) {
            return;
        }

        player.equip(this);
        game.getMessageLog().add(getDisplayName() + " kuşandın (+" + getBonus() + " savunma).");
        game.discardToGround(current);
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
