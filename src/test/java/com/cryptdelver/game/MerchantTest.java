package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Item;
import com.cryptdelver.entity.Merchant;
import com.cryptdelver.entity.Player;
import com.cryptdelver.entity.Potion;
import com.cryptdelver.world.BspGenerator;
import com.cryptdelver.world.Position;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Gezgin satici: altinin ikinci musterisi.
 *
 * <p>Buyucu bes katta bir cikiyor, yani arada dort kat boyunca kese oluyordu:
 * topluyordun ama harcayacak yer yoktu. Satici o araligi dolduruyor. Buradaki
 * sinavlar iki sey kovaliyor: <b>takvim</b> (satici buyucunun cikmadigi
 * katlarda cikiyor mu, tezgahi buyucunun dibine kurulmuyor mu) ve
 * <b>alisveris</b> (altin dogru dusuyor mu, alinan parca tezgahtan gidiyor
 * mu, oyuncu parasini bosa vermis olabiliyor mu).</p>
 */
class MerchantTest {

    private static final int WIDTH = 40;
    private static final int HEIGHT = 22;

    /** Katin uzerinde saticinin yeri. */
    @Nested
    class Placement {

        private final Player player = new Player(0, 0);
        private final Game game = new Game(List.of(new BspGenerator()), WIDTH, HEIGHT, player);

        private void goDownOneFloor() {
            player.setTile(game.getStairs());
            assertTrue(game.descend(), "Inis calismali");
        }

        /**
         * Kati yeniden kurarak satici arar.
         *
         * <p>Zar kat basina atiliyor ve ucte bir tutuyor; kirk denemede hic
         * cikmama ihtimali pratikte sifir.</p>
         */
        private Merchant findMerchant() {
            for (int attempt = 0; attempt < 40; attempt++) {
                if (game.getMerchant() != null) {
                    return game.getMerchant();
                }
                game.regenerateFloor();
            }
            throw new AssertionError("Kirk katta bir satici cikmadi");
        }

        /**
         * Boss kati buyucunun adresi. Ikisini ayni kata koymak ikisini de
         * siradanlastirirdi: boss kati "buyuk hazirlik", arasi "yolda ne
         * bulursan".
         */
        @Test
        @DisplayName("Boss katinda satici yok")
        void bossFloorsHaveNoMerchant() {
            for (int i = 0; i < 4; i++) {
                goDownOneFloor();
            }
            assertTrue(game.isBossFloor(), "5. kat boss kati");

            for (int attempt = 0; attempt < 10; attempt++) {
                assertNull(game.getMerchant(), "Boss katinda satici olmamali");
                game.regenerateFloor();
            }
        }

        @Test
        @DisplayName("Siradan katlarda satici cikiyor")
        void ordinaryFloorsCanHaveAMerchant() {
            assertFalse(game.isBossFloor());
            assertNotNull(findMerchant());
        }

        @Test
        @DisplayName("Satici merdivenin ve dogulan yerin uzerinde degil")
        void merchantKeepsItsOwnTile() {
            for (int attempt = 0; attempt < 20; attempt++) {
                Merchant pedlar = findMerchant();

                assertFalse(pedlar.getTile().equals(game.getStairs()), "Merdiveni kapatmamali");
                assertTrue(game.getDungeon().isWalkable(pedlar.getTileX(), pedlar.getTileY()),
                        "Duvarin icinde olmamali");

                game.regenerateFloor();
            }
        }

        /**
         * Saticinin karesinden gecilemiyor; dar bir koridora denk gelirse
         * merdiveni kapatabilirdi. Buyucude ogrendigimiz ders burada da
         * gecerli, cunku ikisi ayni yerlestirme kuralini kullaniyor.
         */
        @Test
        @DisplayName("Satici merdivenin yolunu kesmiyor")
        void merchantNeverSealsTheFloor() {
            for (int attempt = 0; attempt < 20; attempt++) {
                Merchant pedlar = findMerchant();

                assertTrue(game.getDungeon()
                                .walkableDistancesFrom(player.getTile(), Set.of(pedlar.getTile()))
                                .containsKey(game.getStairs()),
                        "Satici kapaliyken de merdivene ulasilabilmeli");

                game.regenerateFloor();
            }
        }

        @Test
        @DisplayName("Saticinin karesinden gecilemez")
        void merchantBlocksItsTile() {
            Merchant pedlar = findMerchant();

            assertFalse(game.isTileFree(pedlar.getTileX(), pedlar.getTileY(), player));
        }

        /**
         * Ikisi yan yana durursa F hangisini acacagi belirsiz kalir. Gezgin
         * buyucu siradan katlarda da cikabildigi icin bu gercek bir ihtimal,
         * teorik bir endise degil.
         */
        @Test
        @DisplayName("Satici buyucunun dibine kurulmuyor")
        void merchantKeepsAwayFromTheWizard() {
            for (int attempt = 0; attempt < 60; attempt++) {
                game.regenerateFloor();

                if (game.getMerchant() == null || game.getWizard() == null) {
                    continue;
                }

                Position shop = game.getMerchant().getTile();
                Position forge = game.getWizard().getTile();
                assertTrue(Math.abs(shop.x() - forge.x()) > 1 || Math.abs(shop.y() - forge.y()) > 1,
                        "Iki tezgah komsu karelerde durmamali");
            }
        }

        /** Kat kurulumu tohumun saf bir fonksiyonu; satici da buna dahil. */
        @Test
        @DisplayName("Ayni tohum ayni saticiyi veriyor")
        void theSameSeedGivesTheSameMerchant() {
            FloorBuilder builder = new FloorBuilder(List.of(new BspGenerator()), WIDTH, HEIGHT);

            Floor first = builder.layout(0, 3, 4242L);
            Floor second = builder.layout(0, 3, 4242L);

            assertEquals(first.merchant() == null, second.merchant() == null);
            if (first.merchant() != null) {
                assertEquals(first.merchant().getTile(), second.merchant().getTile());
                assertEquals(stockNames(first.merchant()), stockNames(second.merchant()),
                        "Tezgah da tohumdan cikiyor");
            }
        }

        private List<String> stockNames(Merchant pedlar) {
            return pedlar.getStock().stream().map(offer -> offer.item().getName()).toList();
        }
    }

    /** Tezgahin kendisi: ne duruyor, ne kadar tutuyor. */
    @Nested
    class Stall {

        private final Merchant pedlar = Merchant.stocked(new Position(3, 3), 99L);

        @Test
        @DisplayName("Tezgahta uc parca duruyor")
        void theStallHoldsThreeOffers() {
            assertEquals(Merchant.STOCK_SIZE, pedlar.getStock().size());
        }

        /**
         * Saticiyi gormek en azindan bir kez can satin alabilmek demek olmali;
         * tamamen zara birakilsaydi bazi satici tezgahlari oyuncunun o anki
         * derdine hic dokunmazdi.
         */
        @Test
        @DisplayName("Ilk sira her zaman iksir")
        void thereIsAlwaysAPotion() {
            assertTrue(pedlar.offerAt(0).item() instanceof Potion,
                    "Ilk sira iksir olmali, bulundu: " + pedlar.offerAt(0).item().getName());
        }

        @Test
        @DisplayName("Her parcanin bir fiyati var")
        void everyOfferCostsSomething() {
            for (Merchant.Offer offer : pedlar.getStock()) {
                assertTrue(offer.price() > 0, offer.item().getName() + " bedava duruyor");
            }
        }

        @Test
        @DisplayName("Olmayan siraya bakmak cokmuyor")
        void askingForAMissingRowIsSafe() {
            assertNull(pedlar.offerAt(-1));
            assertNull(pedlar.offerAt(Merchant.STOCK_SIZE));
        }

        @Test
        @DisplayName("Satilan parca tezgahtan dusuyor")
        void soldGoodsLeaveTheStall() {
            Merchant.Offer first = pedlar.offerAt(0);
            pedlar.take(first);

            assertEquals(Merchant.STOCK_SIZE - 1, pedlar.getStock().size());
            assertFalse(pedlar.getStock().contains(first));
        }

        @Test
        @DisplayName("Tezgah bosalinca soyledigi degisiyor")
        void anEmptyStallSaysSo() {
            for (Merchant.Offer offer : pedlar.getStock()) {
                pedlar.take(offer);
            }

            assertTrue(pedlar.isSoldOut());
            assertTrue(pedlar.greetingFor(500).contains("bitti"));
        }

        /** Kesesi bos oyuncuya "al" demek anlamsiz. */
        @Test
        @DisplayName("Parasi olmayana baska sey soyluyor")
        void thePennilessGetTheirOwnLine() {
            assertTrue(pedlar.greetingFor(0).contains("Kese"));
            assertFalse(pedlar.greetingFor(500).contains("Kese"));
        }
    }

    /** Alisverisin kendisi: kese, canta ve tezgah ekrani. */
    @Nested
    class Trade {

        private final Player player = new Player(0, 0);
        private final Game game = new Game(List.of(new BspGenerator()), WIDTH, HEIGHT, player);

        /** Oyuncuyu saticinin yanina tasiyip tezgahi acar. */
        private Merchant openStall() {
            for (int attempt = 0; attempt < 40 && game.getMerchant() == null; attempt++) {
                game.regenerateFloor();
            }

            Merchant pedlar = game.getMerchant();
            assertNotNull(pedlar, "Kirk katta bir satici cikmadi");

            player.setTile(pedlar.getTileX() + 1, pedlar.getTileY());
            game.toggleShop();
            assertTrue(game.isShopOpen());
            return pedlar;
        }

        @Test
        @DisplayName("Tezgah yalnizca saticinin yaninda aciliyor")
        void theStallNeedsTheMerchant() {
            for (int attempt = 0; attempt < 40 && game.getMerchant() == null; attempt++) {
                game.regenerateFloor();
            }
            assertNotNull(game.getMerchant());

            // Oyuncu dogdugu yerde; satici en az dort adim oteye kuruluyor.
            game.toggleShop();

            assertFalse(game.isShopOpen(), "Uzaktan acilmamali");
        }

        @Test
        @DisplayName("Satin alinan parca cantaya giriyor, altin duşuyor")
        void buyingMovesGoldAndGoods() {
            Merchant pedlar = openStall();
            Merchant.Offer offer = pedlar.offerAt(0);
            game.addGold(offer.price());

            assertTrue(game.buy(0));

            assertEquals(0, game.getGold(), "Fiyat kadar altin dusmeli");
            assertTrue(game.getInventory().getItems().contains(offer.item()),
                    "Alinan parca cantada olmali");
            assertNull(pedlar.offerAt(Merchant.STOCK_SIZE - 1),
                    "Tezgahta bir sira eksilmis olmali");
        }

        @Test
        @DisplayName("Altin yetmezse hicbir sey olmuyor")
        void anEmptyPurseBuysNothing() {
            Merchant pedlar = openStall();
            Merchant.Offer offer = pedlar.offerAt(0);

            assertFalse(game.buy(0));

            assertEquals(0, game.getGold());
            assertTrue(pedlar.getStock().contains(offer), "Parca tezgahta kalmali");
            assertTrue(game.getInventory().isEmpty());
        }

        /**
         * Sira onemli: once yer, sonra altin. Tersi olsaydi cantasi dolu bir
         * oyuncu parasini odeyip eline hicbir sey gecmedigini gorurdu.
         */
        @Test
        @DisplayName("Canta doluysa altin harcanmiyor")
        void afullBagKeepsTheGold() {
            Merchant pedlar = openStall();

            // Yigilmayan parcalarla canta tikaniyor; iksir yigildigi icin is
            // gormezdi. Kilic her seferinde yeni bir slot aciyor.
            for (int i = 0; i < Inventory.CAPACITY; i++) {
                game.getInventory().add(LootTable.weaponForTier(i % LootTable.MAX_TIER + 1, 0, 0));
            }
            assertTrue(game.getInventory().isFull());

            Merchant.Offer offer = pedlar.offerAt(0);
            game.addGold(offer.price());

            assertFalse(game.buy(0));

            assertEquals(offer.price(), game.getGold(), "Altin kesede kalmali");
            assertTrue(pedlar.getStock().contains(offer), "Parca tezgahta kalmali");
        }

        /**
         * Ayni saticidan sinirsiz iksir almak altini bir karar olmaktan
         * cikarirdi: yeterince altinla her kata tam dolu inerdin.
         */
        @Test
        @DisplayName("Ayni parca iki kez satilmiyor")
        void nothingIsSoldTwice() {
            Merchant pedlar = openStall();
            Merchant.Offer offer = pedlar.offerAt(0);
            game.addGold(offer.price() * 2);

            assertTrue(game.buy(0));
            int left = game.getGold();

            // Ilk sira artik baska bir parca; onu degil, bosalan son sirayi
            // deniyoruz.
            assertFalse(game.buy(Merchant.STOCK_SIZE - 1));
            assertEquals(left, game.getGold());
        }

        @Test
        @DisplayName("Tezgah acikken dunya duruyor")
        void theWorldStopsAtTheStall() {
            openStall();

            assertTrue(game.isFrozen(), "Tezgah acikken zaman akmamali");
        }

        @Test
        @DisplayName("ESC once tezgahi kapatiyor")
        void escapeClosesTheStallFirst() {
            openStall();

            game.togglePause();

            assertFalse(game.isShopOpen());
            assertFalse(game.isPaused(), "Tezgahi kapatan ESC oyunu duraklatmamali");
        }

        /** F "buradakiyle bir sey yap" tusu; satici da bir "burada". */
        @Test
        @DisplayName("Saticinin yaninda F tezgahi aciyor")
        void interactingBesideTheMerchantOpensTheStall() {
            for (int attempt = 0; attempt < 40 && game.getMerchant() == null; attempt++) {
                game.regenerateFloor();
            }

            Merchant pedlar = game.getMerchant();
            assertNotNull(pedlar);
            player.setTile(pedlar.getTileX() + 1, pedlar.getTileY());

            // Ayagin altindaki parca F'nin ilk isi; once orayi bosaltiyoruz ki
            // sinav gercekten tezgahi olcsun.
            game.pickUp();

            assertTrue(game.interact());
            assertTrue(game.isShopOpen());
        }

        /** Tezgah kapaliyken satin alma komutu bir sey yapmamali. */
        @Test
        @DisplayName("Tezgah kapaliyken alisveris yok")
        void noBuyingWithTheStallClosed() {
            for (int attempt = 0; attempt < 40 && game.getMerchant() == null; attempt++) {
                game.regenerateFloor();
            }
            assertNotNull(game.getMerchant());
            game.addGold(500);

            assertFalse(game.buy(0));
            assertEquals(500, game.getGold());
        }

        /** Alinan iksir gercekten calisiyor mu: tezgah sahte esya satmiyor. */
        @Test
        @DisplayName("Satin alinan iksir icilebiliyor")
        void theBoughtPotionWorks() {
            Merchant pedlar = openStall();
            Merchant.Offer offer = pedlar.offerAt(0);
            game.addGold(offer.price());
            assertTrue(game.buy(0));

            game.toggleShop();
            player.takeDamage(5);
            int hurt = player.getHp();

            Item bought = game.getInventory().get(game.getInventory().slotOf(offer.item()));
            assertEquals(offer.item(), bought);
            game.useItem(game.getInventory().slotOf(offer.item()));
            assertTrue(player.getHp() > hurt, "Iksir can vermeli");
        }
    }
}
