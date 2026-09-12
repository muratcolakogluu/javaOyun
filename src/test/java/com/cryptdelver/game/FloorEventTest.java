package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Player;
import com.cryptdelver.world.BspGenerator;
import com.cryptdelver.world.Vision;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Kat olaylari: her katin kendi hali.
 *
 * <p>Yirmi kat ayni ritimle geciyordu. Bolge degisince tas ve renk
 * degisiyordu, derinlik arttikca sayilar buyuyordu -- ama oynanis hep ayniydi:
 * gir, temizle, merdiveni bul. Buradaki sinavlar olayin gercekten <b>bir sey
 * degistirdigini</b> ve dordunun ayni yone cekmedigini kovaliyor.</p>
 */
class FloorEventTest {

    /** Olayin kendisi: hangi kaldiraca dokunuyor. */
    @Nested
    class Levers {

        /**
         * Hepsi zorlastirici olsaydi olay "ceza" demek olurdu ve oyuncu her
         * seferinde ayni seyi yapardi: hizlica gec. Sessiz kat bilerek bir
         * rahatlama, zengin kat bilerek bir cazibe.
         */
        @Test
        @DisplayName("Dort olay dort ayri kaldiraca dokunuyor")
        void eachEventPullsItsOwnLever() {
            Map<FloorEvent, String> fingerprints = new EnumMap<>(FloorEvent.class);

            for (FloorEvent event : FloorEvent.values()) {
                fingerprints.put(event, event.scaleVision(Vision.RADIUS) + "|"
                        + event.scaleCrowd(10) + "|" + event.getExtraItems() + "|"
                        + event.scalePatience(40));
            }

            assertEquals(FloorEvent.values().length,
                    fingerprints.values().stream().distinct().count(),
                    "Iki olay ayni seyi yapmamali");
        }

        @Test
        @DisplayName("Karanlik gorusu kisaltiyor, baska bir seye dokunmuyor")
        void theDarkOnlyTakesYourSight() {
            FloorEvent dark = FloorEvent.KARANLIK;

            assertTrue(dark.scaleVision(Vision.RADIUS) < Vision.RADIUS);
            assertEquals(10, dark.scaleCrowd(10), "Kalabalik ayni kalmali");
            assertEquals(0, dark.getExtraItems());
            assertEquals(40.0, dark.scalePatience(40), 0.001);
        }

        /** Ganimet fazlasi olmasa suru yalnizca bir vergi olurdu. */
        @Test
        @DisplayName("Suru kalabaligi da ganimeti de buyutuyor")
        void theSwarmPaysForItself() {
            assertTrue(FloorEvent.SURU.scaleCrowd(10) > 10);
            assertTrue(FloorEvent.SURU.getExtraItems() > 0);
        }

        @Test
        @DisplayName("Sessiz kat sabri uzatiyor")
        void thequietFloorIsARest() {
            assertTrue(FloorEvent.SESSIZ.scalePatience(40) > 40);
        }

        /** Acgozlulugun bedeli dogrudan odeniyor. */
        @Test
        @DisplayName("Zengin kat ganimeti buyutup sabri kisaltiyor")
        void therichFloorCutsItsOwnFuse() {
            assertTrue(FloorEvent.ZENGIN.getExtraItems() > 0);
            assertTrue(FloorEvent.ZENGIN.scalePatience(40) < 40);
        }

        @Test
        @DisplayName("Her olay adini ve ne degistigini soyluyor")
        void everyEventIntroducesItself() {
            for (FloorEvent event : FloorEvent.values()) {
                assertFalse(event.getLabel().isBlank(), event + " adsiz");
                assertFalse(event.getDescription().isBlank(), event + " aciklamasiz");
            }
        }
    }

    /** Katin uzerinde: olay ne zaman cikiyor, oyuna nasil yansiyor. */
    @Nested
    class OnTheFloor {

        private final FloorBuilder builder =
                new FloorBuilder(List.of(new BspGenerator()), 40, 22);

        /**
         * Ilk iki kat oyunun temel kurallarini ogretiyor; istisnayi kural
         * ogrenilmeden gostermek ogretmiyor.
         */
        @Test
        @DisplayName("Ilk iki katta olay yok")
        void theFirstFloorsAreOrdinary() {
            for (long seed = 0; seed < 200; seed++) {
                assertNull(builder.layout(0, 1, seed).event(), "1. katta olay olmamali");
                assertNull(builder.layout(0, 2, seed).event(), "2. katta olay olmamali");
            }
        }

        /** Boss zaten o katin olayi; ustune karanlik eklemek dovusu okunamaz yapardi. */
        @Test
        @DisplayName("Boss katinda olay yok")
        void bossFloorsAreTheirOwnEvent() {
            for (long seed = 0; seed < 200; seed++) {
                assertNull(builder.layout(0, 5, seed).event());
                assertNull(builder.layout(0, 10, seed).event());
            }
        }

        /**
         * Daha sik olsa olay "normal" olurdu ve siradan kat istisnaya
         * donerdi; daha seyrek olsa indiginde okudugun cumle hatirlanmazdi.
         */
        @Test
        @DisplayName("Olay ucte bir katta, dordu de cikiyor")
        void eventsAreOccasionalAndVaried() {
            Map<FloorEvent, Integer> seen = new EnumMap<>(FloorEvent.class);
            int withEvent = 0;
            int floors = 600;

            for (long seed = 0; seed < floors; seed++) {
                FloorEvent event = builder.layout(0, 7, seed).event();
                if (event != null) {
                    withEvent++;
                    seen.merge(event, 1, Integer::sum);
                }
            }

            assertTrue(withEvent > floors / 6, "Fazla seyrek: " + withEvent);
            assertTrue(withEvent < floors / 2, "Fazla sik: " + withEvent);
            assertEquals(FloorEvent.values().length, seen.size(),
                    "Dort olayin hepsi cikmali, cikan: " + seen.keySet());
        }

        /** Kat kurulumu tohumun saf bir fonksiyonu; olay da buna dahil. */
        @Test
        @DisplayName("Ayni tohum ayni olayi veriyor")
        void theSameSeedGivesTheSameEvent() {
            for (long seed = 0; seed < 50; seed++) {
                assertEquals(builder.layout(0, 8, seed).event(),
                        builder.layout(0, 8, seed).event());
            }
        }

        @Test
        @DisplayName("Suru kati gercekten daha kalabalik")
        void theSwarmFloorIsCrowded() {
            int plain = 0;
            int swarm = 0;
            int plainFloors = 0;
            int swarmFloors = 0;

            for (long seed = 0; seed < 400; seed++) {
                Floor floor = builder.build(0, 7, seed, Difficulty.NORMAL);
                if (floor.event() == FloorEvent.SURU) {
                    swarm += floor.enemies().size();
                    swarmFloors++;
                } else if (floor.event() == null) {
                    plain += floor.enemies().size();
                    plainFloors++;
                }
            }

            assertTrue(swarmFloors > 0 && plainFloors > 0, "Iki kat turu de cikmali");
            assertTrue(swarm / (double) swarmFloors > plain / (double) plainFloors,
                    "Suru kati daha kalabalik olmali");
        }
    }

    /** Oyunun icinde: olay gercekten isliyor mu. */
    @Nested
    class InPlay {

        private final Player player = new Player(0, 0);
        private final Game game = new Game(List.of(new BspGenerator()), 40, 22, player);

        /**
         * Istenen olayin cikana kadar kati yeniden kurar.
         *
         * <p>{@code null} verilince siradan bir kat ariyor. Bu gerekli:
         * karsilastirma icin "siradan kat" olcumu alirken kati rastgele
         * birakirsak o kat da olayli cikabiliyor ve olcut kayiyor -- once
         * sessiz kata denk gelip sonra "sessiz kat daha sabirli mi" diye
         * sormak, her zaman esitlik verirdi.</p>
         */
        private void findFloorWith(FloorEvent wanted) {
            for (int attempt = 0; attempt < 400; attempt++) {
                if (game.getEvent() == wanted) {
                    return;
                }
                game.regenerateFloor();
            }
            throw new AssertionError((wanted == null ? "Siradan" : wanted.toString())
                    + " kat cikmadi");
        }

        /** Ilk iki katta olay yok; sinavlar icin asagi inmek gerekiyor. */
        private void goDown(int floors) {
            for (int i = 0; i < floors; i++) {
                player.setTile(game.getStairs());
                assertTrue(game.descend());
            }
        }

        @Test
        @DisplayName("Siradan katta gorus tam")
        void anordinaryFloorSeesFully() {
            assertNull(game.getEvent(), "1. kat siradan");
            assertEquals(Vision.RADIUS, game.getVision().getRadius());
        }

        @Test
        @DisplayName("Karanlik katta fener daralıyor")
        void theDarkFloorNarrowsTheLantern() {
            goDown(2);
            findFloorWith(FloorEvent.KARANLIK);

            assertTrue(game.getVision().getRadius() < Vision.RADIUS,
                    "Gorus yaricapi kisalmali");
            assertTrue(game.getVision().getRadius() > 0, "Ama tamamen kor olmamali");
        }

        @Test
        @DisplayName("Sessiz katta zindanin sabri uzun")
        void thequietFloorIsPatient() {
            goDown(2);
            findFloorWith(null);
            double ordinary = game.getFloorPatience();

            findFloorWith(FloorEvent.SESSIZ);

            assertTrue(game.getFloorPatience() > ordinary, "Sabir uzamali");
        }

        @Test
        @DisplayName("Zengin katta zindan cabuk uyaniyor")
        void therichFloorIsImpatient() {
            goDown(2);
            findFloorWith(null);
            double ordinary = game.getFloorPatience();

            findFloorWith(FloorEvent.ZENGIN);

            assertTrue(game.getFloorPatience() < ordinary, "Sabir kisalmali");
        }

        /** Indigin anda okunan cumle olayin butun isi. */
        @Test
        @DisplayName("Kata inince olay duyuruluyor")
        void arrivingAnnouncesTheEvent() {
            goDown(2);
            findFloorWith(FloorEvent.SURU);

            // startsWith, equals degil: mesaj kaydi tekrarlanan satirlari
            // katliyor ve ekranda "... x2" olarak gosteriyor. Arama sirasinda
            // ayni olay iki kez cikmissa cumlenin sonuna carpan ekleniyor.
            boolean announced = game.getMessageLog().latest(6).stream()
                    .anyMatch(line -> line.startsWith(FloorEvent.SURU.getDescription()));

            assertTrue(announced, "Olay cumlesi kayitta olmali");
        }

        @Test
        @DisplayName("Geri donulen katta olay duruyor")
        void theEventSurvivesTheClimb() {
            goDown(2);
            findFloorWith(FloorEvent.KARANLIK);
            int narrowed = game.getVision().getRadius();

            assertTrue(game.ascend());
            player.setTile(game.getStairs());
            assertTrue(game.descend());

            assertEquals(FloorEvent.KARANLIK, game.getEvent(), "Kat kendi hâlini korumali");
            assertEquals(narrowed, game.getVision().getRadius());
        }

        @Test
        @DisplayName("Olay kayda da yaziliyor")
        void theEventIsPartOfTheFloor() {
            goDown(2);
            findFloorWith(FloorEvent.ZENGIN);

            assertNotNull(game.getEvent());
            assertFalse(game.getEvent().getLabel().isBlank());
        }
    }
}
