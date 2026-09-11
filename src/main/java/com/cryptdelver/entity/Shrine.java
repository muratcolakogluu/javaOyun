package com.cryptdelver.entity;

import com.cryptdelver.game.Game;
import com.cryptdelver.game.LootTable;
import com.cryptdelver.game.Text;
import com.cryptdelver.world.Position;
import java.util.List;
import java.util.Random;

/**
 * Kader taşı: bedeli altın olmayan tek tezgâh.
 *
 * <p>Oyundaki her karar altınla ödeniyordu — büyücü, satıcı, hepsi aynı keseyi
 * boşaltıyordu. Bu da altını tek bir kaynak, bütün kararları da tek bir
 * soruya indirgiyordu: <em>param yetiyor mu</em>. Kader taşı ikinci bir para
 * birimi açıyor ve o para birimi <b>kendi ömrün</b>.</p>
 *
 * <p>Takas geri alınamaz: azami can yalnızca bossla geri geliyor. Yani "şimdi
 * güçlen, sonra idare et" diyen oyuncuyla "canımı koru, yavaş ilerle" diyen
 * oyuncu aynı zindanda farklı yerlere varıyor — koşuları birbirinden ayıran
 * şey de tam olarak bu.</p>
 *
 * <h2>Neden tek seferlik ve tek teklif</h2>
 * <p>Bir menü açıp "şunlardan hangisini istersin" diye sorsaydı taş ikinci
 * bir dükkân olurdu. Taşın işi seçenek sunmak değil, <b>tek bir teklifi</b>
 * masaya koymak: al ya da alma. Reddetmek de bir cevap ve o cevabın anlamlı
 * olması için ikinci bir teklif olmamalı.</p>
 */
public class Shrine extends Entity {

    /** Taşın verdiği şeyler; tekliften önce hangisinin işe yaradığına bakılıyor. */
    public enum Boon {

        /** Kılıcını bir kademe büyütür. */
        CELIK(Text.SHRINE_STEEL, 4) {
            @Override
            boolean applies(Game game) {
                return canGrow(game.getPlayer().getEquippedWeapon(), game);
            }

            @Override
            void grant(Game game) {
                game.getPlayer().getEquippedWeapon().upgrade();
            }
        },

        /** Zırhını bir kademe büyütür. */
        KABUK(Text.SHRINE_SHELL, 4) {
            @Override
            boolean applies(Game game) {
                return canGrow(game.getPlayer().getEquippedArmor(), game);
            }

            @Override
            void grant(Game game) {
                game.getPlayer().getEquippedArmor().upgrade();
            }
        },

        /**
         * Takımına bir büyü basar.
         *
         * <p>En pahalısı: büyücüde bu iş en az iki yüz altın tutuyor ve
         * büyücü beş katta bir çıkıyor.</p>
         */
        BUYU(Text.SHRINE_SPELL, 6) {
            @Override
            boolean applies(Game game) {
                return openPiece(game) != null;
            }

            @Override
            void grant(Game game) {
                Equipment piece = openPiece(game);
                List<Enchantment> options = piece.availableEnchantments();
                piece.enchant(options.get(new Random().nextInt(options.size())));
            }
        },

        /** Eli boşsa çelik verir. */
        SILAH(Text.SHRINE_BLADE, 4) {
            @Override
            boolean applies(Game game) {
                return game.getPlayer().getEquippedWeapon() == null;
            }

            @Override
            void grant(Game game) {
                Weapon blade = LootTable.weaponForTier(
                        LootTable.tierForDepth(game.getDepth()), 0, 0);
                game.getInventory().add(blade);
                game.getPlayer().equip(blade);
            }
        },

        /**
         * Son çare: kese.
         *
         * <p>Her zaman geçerli. Takımı tavana dayanmış bir oyuncuya taşın
         * "sana verecek bir şeyim yok" demesi, taşı bulmanın hiçbir anlamı
         * olmadığı anlamına gelirdi.</p>
         */
        KESE(Text.SHRINE_PURSE, 4) {
            @Override
            boolean applies(Game game) {
                return true;
            }

            @Override
            void grant(Game game) {
                game.addGold(PURSE);
            }
        };

        private static final int PURSE = 120;

        private final Text label;
        private final int cost;

        Boon(Text label, int cost) {
            this.label = label;
            this.cost = cost;
        }

        /** Ne verdiğini anlatan cümle; taşın üstündeki balonda yazıyor. */
        public String getLabel() {
            return label.get();
        }

        /** Kaç azami cana mal olduğu. */
        public int getCost() {
            return cost;
        }

        abstract boolean applies(Game game);

        abstract void grant(Game game);

        private static boolean canGrow(Equipment piece, Game game) {
            return piece != null && piece.canUpgrade(game.getDepth());
        }

        /** Büyü basılabilecek parça: önce kılıç, yoksa zırh. */
        private static Equipment openPiece(Game game) {
            Equipment weapon = game.getPlayer().getEquippedWeapon();
            if (weapon != null && !weapon.availableEnchantments().isEmpty()) {
                return weapon;
            }

            Equipment armor = game.getPlayer().getEquippedArmor();
            return armor != null && !armor.availableEnchantments().isEmpty() ? armor : null;
        }
    }

    /**
     * Teklif sırasının nereden başladığı; katın tohumundan.
     *
     * <p>Sıra tohumdan, geçerlilik oyuncudan: taş listeyi buradan başlatıp
     * dönüyor ve <em>o an işine yarayan</em> ilk teklifte duruyor. Böylece
     * her taş aynı şeyi teklif etmiyor ama hiçbir taş da boş çıkmıyor.</p>
     */
    private final int start;

    private boolean spent;

    public Shrine(int tileX, int tileY, int start) {
        super(tileX, tileY, Text.SHRINE_NAME);
        this.start = Math.floorMod(start, Boon.values().length);
    }

    /** Katın tohumundan bir taş kurar. */
    public static Shrine seeded(Position spot, long seed) {
        return new Shrine(spot.x(), spot.y(), new Random(seed).nextInt(Boon.values().length));
    }

    /** Taş kullanıldı mı; bir kez veriyor. */
    public boolean isSpent() {
        return spent;
    }

    /**
     * Bu oyuncuya ne teklif ediyor; verecek bir şeyi yoksa {@code null}.
     *
     * <p>Teklif her sorulduğunda yeniden hesaplanıyor, saklanmıyor: takımını
     * değiştirdiysen taş da fikrini değiştiriyor. Sabitlenmiş bir teklif,
     * eline yeni bir kılıç geçtikten sonra artık anlamsız kalan bir cümleye
     * dönüşürdü.</p>
     */
    public Boon offerFor(Game game) {
        if (spent) {
            return null;
        }

        Boon[] boons = Boon.values();
        for (int step = 0; step < boons.length; step++) {
            Boon boon = boons[(start + step) % boons.length];
            if (boon.applies(game)) {
                return boon;
            }
        }
        return null;
    }

    /**
     * Teklifi kabul eder: bedeli alır, karşılığını verir, taş susar.
     *
     * <p>Sıra önemli — önce bedel, sonra karşılık. Tersi olsaydı ödenemeyen
     * bir bedelin ardından geri alınması gereken bir armağan kalırdı.</p>
     *
     * @return takas olduysa kabul edilen teklif, olmadıysa {@code null}
     */
    public Boon accept(Game game) {
        Boon boon = offerFor(game);
        if (boon == null) {
            return null;
        }

        spent = true;
        game.getPlayer().spendMaxHp(boon.getCost());
        boon.grant(game);
        return boon;
    }

    /** Taşın üstündeki tek satır: ne verdiği ve neye mal olduğu. */
    public String greetingFor(Game game) {
        Boon boon = offerFor(game);
        return boon == null
                ? Text.SHRINE_SILENT.get()
                : Text.SHRINE_OFFER.get(boon.getLabel(), boon.getCost());
    }

    @Override
    public String getSpriteName() {
        return "shrine";
    }
}
