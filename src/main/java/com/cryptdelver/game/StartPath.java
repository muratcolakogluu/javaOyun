package com.cryptdelver.game;

import com.cryptdelver.entity.Armor;
import com.cryptdelver.entity.Enchantment;
import com.cryptdelver.entity.Item;
import com.cryptdelver.entity.Potion;
import com.cryptdelver.entity.Weapon;

/**
 * Koşuya nasıl başladığın: üç yoldan biri.
 *
 * <p>Şimdiye kadar her koşu birebir aynı başlıyordu — çıplak elle, 1. katta —
 * ve <b>ilk gerçek karar üçüncü kata kadar gelmiyordu</b>. Zindan her seferinde
 * farklı doğuyordu ama oyuncunun kendisi değil, dolayısıyla ilk katların
 * oynanışı hep aynıydı.</p>
 *
 * <p>Üç yol bunu ilk kareye taşıyor: neyle indiğin farklı olunca ilk kattaki
 * ork da farklı bir soru soruyor. Muhafız ona dayanabilir, Haydut onu
 * gelmeden devirmeye çalışır, Tüccar ise hiç karşılaşmamayı seçip altınını
 * saklar.</p>
 *
 * <h2>Neden yeni sistem yok</h2>
 * <p>Üçü de zaten var olan şeyleri farklı dağıtıyor: ekipman kademesi, büyü ve
 * kese. Yeni bir mekanik eklemek daha görkemli olurdu ama yeni bir mekanik
 * dengelenmesi gereken yeni bir şey demek; buradaki üç yol ise oyunun bildiği
 * sayıları kullandığı için ilk günden dengeli.</p>
 */
public enum StartPath {

    /**
     * Muhafız: ayakta kalmak.
     *
     * <p>Deri zırh ve fazladan can. Vuruşu herkesle aynı, yani düşmanı daha
     * yavaş deviriyor — ama hata yapma payı en yüksek olan yol bu. Oyuna yeni
     * başlayan için doğru varsayılan, o yüzden listenin başında.</p>
     */
    MUHAFIZ(Text.PATH_GUARD, Text.PATH_GUARD_WHAT, PathSkill.SARSINTI) {
        @Override
        void outfit(Game game) {
            game.getPlayer().gainMaxHp(GUARD_BONUS_HP);
            wear(game, LootTable.armorForTier(START_TIER, 0, 0));
        }
    },

    /**
     * Haydut: önce vurmak.
     *
     * <p>Aceleyle büyülenmiş bir kılıç. Zırhı yok, yani aldığı her vuruş tam
     * iniyor; buna karşılık dövüşü kısa tutabiliyor. Kaçış adımıyla en iyi
     * anlaşan yol: vur, çekil, geri gir.</p>
     */
    HAYDUT(Text.PATH_ROGUE, Text.PATH_ROGUE_WHAT, PathSkill.SICRAMA) {
        @Override
        void outfit(Game game) {
            Weapon blade = LootTable.weaponForTier(START_TIER, 0, 0);
            blade.enchant(Enchantment.ACELE);
            wield(game, blade);
        }
    },

    /**
     * Tüccar: yolda satın almak.
     *
     * <p>Takımı yok, kesesi dolu. En zor başlangıç — ilk iki katı çıplak elle
     * geçiyorsun — ama ilk büyücüye ya da satıcıya vardığında diğer ikisinin
     * kat kat ilerisinde oluyorsun. Yani bu yol "şimdi mi sonra mı" sorusunu
     * koşunun tamamına yayıyor.</p>
     *
     * <p>Yanında bir iksir var: çıplak elle geçilen ilk katın tamamen zara
     * kalmaması için.</p>
     */
    TUCCAR(Text.PATH_TRADER, Text.PATH_TRADER_WHAT, PathSkill.RUSVET) {
        @Override
        void outfit(Game game) {
            game.addGold(TRADER_PURSE);
            carry(game, new Potion(0, 0));
        }
    };

    /** Başlangıç ekipmanının kademesi: en alt kademe, yani yerde bulunanın aynısı. */
    private static final int START_TIER = 1;

    private static final int GUARD_BONUS_HP = 6;
    private static final int TRADER_PURSE = 150;

    private final Text label;
    private final Text description;
    private final PathSkill skill;

    StartPath(Text label, Text description, PathSkill skill) {
        this.label = label;
        this.description = description;
        this.skill = skill;
    }

    /**
     * Bu yolun Q tuşuna verdiği yetenek.
     *
     * <p>Yol seçimini yirmi katın tamamına yayan şey bu. Yalnızca başlangıç
     * takımı farklı olsaydı seçim ilk beş dakikaya ait olurdu: ilk kattan
     * sonra elindeki kılıç neyse öyle oynardın.</p>
     */
    public PathSkill getSkill() {
        return skill;
    }

    public String getLabel() {
        return label.get();
    }

    public String getDescription() {
        return description.get();
    }

    /**
     * Oyuncuyu bu yola göre donatır.
     *
     * <p>Paket görünür: {@link Game#restart()} çağırıyor ve başka kimsenin
     * koşunun ortasında yol değiştirmesine gerek yok.</p>
     */
    abstract void outfit(Game game);

    /**
     * Oyuncuyu bu yola göre donatır ve yeteneğini öğretir.
     *
     * <p>Yeteneği burada veriyorum, {@link #outfit} içinde değil: üç yolun
     * hiçbiri onu unutmasın. Her birinin kendi {@code outfit}'ine bir satır
     * daha koymak, dördüncü bir yol eklendiğinde sessizce atlanabilecek bir
     * adım olurdu.</p>
     */
    void begin(Game game) {
        game.getPlayer().learn(skill);
        outfit(game);
    }

    /** Çantaya koyar; kuşanılacak bir şey değilse yolu bu. */
    private static void carry(Game game, Item item) {
        game.getInventory().add(item);
    }

    /**
     * Çantaya koyup kuşandırır.
     *
     * <p>Parça çantada <em>kalıyor</em>: oyundaki her ekipman öyle duruyor ve
     * başlangıç parçasının kuralın dışında kalması için bir sebep yok —
     * bıraktığında yerde görünmesi gerekiyor.</p>
     */
    private static void wear(Game game, Armor armor) {
        carry(game, armor);
        game.getPlayer().equip(armor);
    }

    private static void wield(Game game, Weapon weapon) {
        carry(game, weapon);
        game.getPlayer().equip(weapon);
    }
}
