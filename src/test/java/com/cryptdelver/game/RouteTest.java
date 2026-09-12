package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Player;
import com.cryptdelver.world.BspGenerator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Merdivende iki yol: kosunun seklini kim ciziyor.
 *
 * <p>Yirmi kat tek bir koridordu. Kat olaylari katlara kisilik verdi ama
 * <b>kimin karari oldugunu</b> degistirmedi -- yine zardi. Iki yol o karari
 * oyuncuya veriyor. Buradaki sinavlar seciminin gercekten islediğini
 * kovaliyor: vaat edilen sey asagida gerceklesiyor mu, ve secim ekrani
 * kacinilabilir bir soru mu.</p>
 */
class RouteTest {

    private static final int WIDTH = 40;
    private static final int HEIGHT = 22;

    /** Sunulan iki yol: hep zit, hep ayni tohumdan. */
    @Nested
    class TheOffer {

        @Test
        @DisplayName("Her merdivende iki yol var")
        void thereAreAlwaysTwo() {
            for (long seed = 0; seed < 200; seed++) {
                assertEquals(2, Route.from(seed).size());
            }
        }

        /**
         * Iki iyi ya da iki kotu secenek arasinda secim yapmak karar degil,
         * formalite. Bir taraf hep sakin, oteki hep kazancli.
         */
        @Test
        @DisplayName("Iki yol birbirinden farkli")
        void thetwoAlwaysDiffer() {
            for (long seed = 0; seed < 200; seed++) {
                List<Route> routes = Route.from(seed);

                assertNotEquals(routes.get(0).getEvent(), routes.get(1).getEvent(),
                        "Ayni iki yol sunulmamali");
            }
        }

        @Test
        @DisplayName("Kazancli taraf hep ganimet vaat ediyor")
        void thegreedySideAlwaysPays() {
            for (long seed = 0; seed < 200; seed++) {
                FloorEvent greedy = Route.from(seed).get(1).getEvent();

                assertTrue(greedy == FloorEvent.ZENGIN || greedy == FloorEvent.SURU,
                        "Beklenmeyen kazancli yol: " + greedy);
                assertTrue(greedy.getExtraItems() > 0, "Kazancli yol ganimet vermeli");
            }
        }

        /** Sakin taraf bazen bedava, bazen bedeli olan bir guvenlik. */
        @Test
        @DisplayName("Sakin taraf hem duz yol hem karanlik olabiliyor")
        void thecalmSideVaries() {
            Set<FloorEvent> seen = new HashSet<>();
            for (long seed = 0; seed < 300; seed++) {
                seen.add(Route.from(seed).get(0).getEvent());
            }

            assertTrue(seen.contains(null), "Duz yol da cikmali");
            assertTrue(seen.size() > 1, "Sakin taraf tek bir seye sabitlenmemeli");
        }

        /**
         * Zar her seferinde yeniden atilsaydi oyuncu istedigi yol cikana
         * kadar merdivene basip kalkardi ve secim diye bir sey kalmazdi.
         */
        @Test
        @DisplayName("Ayni tohum ayni iki yolu veriyor")
        void theSameSeedGivesTheSameRoutes() {
            for (long seed = 0; seed < 100; seed++) {
                List<Route> first = Route.from(seed);
                List<Route> second = Route.from(seed);

                assertEquals(first.get(0).getEvent(), second.get(0).getEvent());
                assertEquals(first.get(1).getEvent(), second.get(1).getEvent());
            }
        }

        @Test
        @DisplayName("Her yol adini ve vaadini soyluyor")
        void everyRouteIntroducesItself() {
            for (long seed = 0; seed < 50; seed++) {
                for (Route route : Route.from(seed)) {
                    assertFalse(route.getLabel().isBlank());
                    assertFalse(route.getDescription().isBlank());
                }
            }
        }
    }

    /** Oyunun icinde: secim ekrani ve inisin kendisi. */
    @Nested
    class InPlay {

        private final Player player = new Player(0, 0);
        private final Game game = new Game(List.of(new BspGenerator()), WIDTH, HEIGHT, player);

        /** Merdivene basar; secim ekrani aciliyorsa inis olmuyor. */
        private boolean pressStairs() {
            player.setTile(game.getStairs());
            return game.beginDescent();
        }

        /**
         * Ilk iki katin hali sabit: oralar oyunu ogretiyor, secim sunmak
         * ogretilmemis bir seyi sormak olurdu.
         */
        @Test
        @DisplayName("Ilk inislerde secim sorulmuyor")
        void theFirstDescentsAreDirect() {
            assertTrue(pressStairs(), "1. kattan inis dogrudan olmali");
            assertFalse(game.isChoosingRoute());
            assertEquals(2, game.getDepth());
        }

        @Test
        @DisplayName("Ucuncu kata inerken iki yol soruluyor")
        void thechoiceAppears() {
            pressStairs();

            assertFalse(pressStairs(), "Secim ekrani acilinca inis beklemeli");
            assertTrue(game.isChoosingRoute());
            assertEquals(2, game.getDepth(), "Secim yapilmadan kat degismemeli");
            assertEquals(2, game.getRoutes().size());
        }

        /** Secim ekrani acikken dunya durmali: karar verirken vurulmak olmaz. */
        @Test
        @DisplayName("Secim ekrani acikken zaman akmiyor")
        void theworldStopsWhileChoosing() {
            pressStairs();
            pressStairs();

            assertTrue(game.isFrozen());
        }

        /** Soruyu cevaplamadan inmenin yolu olmamali. */
        @Test
        @DisplayName("Secim acikken E yeniden inmiyor")
        void thestairsDoNotSkipTheQuestion() {
            pressStairs();
            pressStairs();

            assertFalse(game.beginDescent(), "Secim acikken inis olmamali");
            assertEquals(2, game.getDepth());
        }

        @Test
        @DisplayName("Secilen yol asagida gerceklesiyor")
        void thechosenRouteIsWhatYouGet() {
            pressStairs();
            pressStairs();

            FloorEvent promised = game.getRoutes().get(1).getEvent();
            assertTrue(game.takeRoute(1));

            assertEquals(3, game.getDepth());
            assertEquals(promised, game.getEvent(), "Vaat edilen hâl inilen katta olmali");
            assertFalse(game.isChoosingRoute(), "Ekran kapanmali");
        }

        /**
         * Duz yolun vaadi "olaysiz kat" ve vaat tutulmali.
         *
         * <p>Ilk halde tutulmuyordu: Game secimi "yolun verdigi hal" olarak
         * sakliyordu ve duz yolun hali null oldugu icin "oyuncu duz yolu
         * secti" ile "hic secim olmadi" ayni degerle anlatiliyordu -- yani
         * duz yolu secen oyuncuya rastgele bir olay veriliyordu. Artik yolun
         * kendisi saklaniyor.</p>
         */
        @Test
        @DisplayName("Duz yol seciline siradan bir kat veriyor")
        void theplainWayGivesAnOrdinaryFloor() {
            pressStairs();

            for (int attempt = 0; attempt < 200; attempt++) {
                pressStairs();
                if (game.isChoosingRoute() && game.getRoutes().get(0).getEvent() == null) {
                    assertTrue(game.takeRoute(0));
                    assertNull(game.getEvent(), "Duz yol olaysiz kat vermeli");
                    return;
                }

                game.cancelRoute();
                game.regenerateFloor();
            }
            throw new AssertionError("Duz yol sunulan bir merdiven cikmadi");
        }

        /** Karar geri alinabilir olmali: yanlis tusa basmak koşuyu bozmasin. */
        @Test
        @DisplayName("ESC ile secimden vazgecilebiliyor")
        void thechoiceCanBeSteppedBack() {
            pressStairs();
            pressStairs();
            assertTrue(game.isChoosingRoute());

            game.togglePause();

            assertFalse(game.isChoosingRoute(), "Ekran kapanmali");
            assertFalse(game.isPaused(), "Vazgecmek oyunu duraklatmamali");
            assertEquals(2, game.getDepth(), "Kat degismemeli");
        }

        @Test
        @DisplayName("Olmayan yolu secmek bir sey yapmiyor")
        void amissingRouteDoesNothing() {
            pressStairs();
            pressStairs();

            assertFalse(game.takeRoute(5));
            assertTrue(game.isChoosingRoute(), "Ekran acik kalmali");
            assertEquals(2, game.getDepth());
        }

        /** Boss katinin olayi bossun kendisi; orada secilecek bir sey yok. */
        @Test
        @DisplayName("Boss katina inerken secim sorulmuyor")
        void bossFloorsAreNotAChoice() {
            while (game.getDepth() < 4) {
                if (game.isChoosingRoute()) {
                    game.takeRoute(0);
                    continue;
                }
                pressStairs();
            }

            assertEquals(4, game.getDepth());
            assertFalse(pressStairs() && game.isChoosingRoute(),
                    "5. kata inis dogrudan olmali");
            assertEquals(5, game.getDepth());
            assertFalse(game.isChoosingRoute());
        }

        /**
         * Secim tek bir kat icin gecerli.
         *
         * <p>Yapistigini anlamanin yolu kati defalarca yeniden kurmak: secim
         * hala uygulaniyorsa hepsi ayni hali verir, temizlenmisse kat kendi
         * zarini atar ve baska haller de cikar.</p>
         */
        @Test
        @DisplayName("Secim bir sonraki kata tasinmiyor")
        void thechoiceLastsOneFloor() {
            pressStairs();
            pressStairs();
            assertTrue(game.takeRoute(1));

            Set<FloorEvent> seen = new HashSet<>();
            for (int attempt = 0; attempt < 200; attempt++) {
                game.regenerateFloor();
                seen.add(game.getEvent());
            }

            assertTrue(seen.size() > 1,
                    "Secim yapismis gorunuyor, cikan haller: " + seen);
        }
    }
}
