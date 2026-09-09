package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Olay kaydi.
 *
 * <p>Ekranda uc satir var ve dovuste "Imp 3 hasar aldi" akiyor. Kirilan
 * kilic ya da uyanan zindan gibi onemli olaylar bir saniyede kayboluyordu --
 * bunu bir testte bizzat gorduk, uyari mesaji son on satirdan tasmisti.</p>
 */
class MessageLogTest {

    private MessageLog log;

    @BeforeEach
    void setUp() {
        log = new MessageLog();
    }

    @Test
    @DisplayName("Arka arkaya gelen ayni mesaj katlaniyor")
    void repeatsAreFolded() {
        log.add("Imp 3 hasar aldi");
        log.add("Imp 3 hasar aldi");
        log.add("Imp 3 hasar aldi");

        assertEquals(1, log.latest(10).size(), "Uc satir degil bir satir");
        assertEquals("Imp 3 hasar aldi x3", log.last());
    }

    /** Araya baska bir olay girdiyse ikisi ayri olay; birlestirmek yaniltir. */
    @Test
    @DisplayName("Araya baska mesaj girince katlama bozuluyor")
    void anInterruptionStartsANewEntry() {
        log.add("Imp 3 hasar aldi");
        log.add("Altin aldin");
        log.add("Imp 3 hasar aldi");

        assertEquals(3, log.latest(10).size());
    }

    @Test
    @DisplayName("Tek mesajda carpan yazmiyor")
    void asingleMessageHasNoCounter() {
        log.add("Altin aldin");

        assertEquals("Altin aldin", log.last());
    }

    @Test
    @DisplayName("Onemli mesajlar isaretli geliyor")
    void importantMessagesAreMarked() {
        log.add("Imp 3 hasar aldi");
        log.addImportant("Kilicin kirildi");

        var entries = log.latestEntries(2);

        assertTrue(entries.get(0).isImportant(), "En yenisi onemli");
        assertFalse(entries.get(1).isImportant(), "Dovus satiri siradan");
    }

    /** Ayni metin onemli ve siradan olarak gelirse ayri kayitlar. */
    @Test
    @DisplayName("Onem farki katlamayi bozuyor")
    void importanceSeparatesOtherwiseIdenticalLines() {
        log.add("Ayni metin");
        log.addImportant("Ayni metin");

        assertEquals(2, log.latest(10).size());
    }

    @Test
    @DisplayName("En yeni mesaj basta geliyor")
    void theNewestComesFirst() {
        log.add("Birinci");
        log.add("Ikinci");

        assertEquals("Ikinci", log.latest(2).get(0));
        assertEquals("Birinci", log.latest(2).get(1));
    }

    @Test
    @DisplayName("Temizlenince kayit bosaliyor")
    void clearingEmptiesTheLog() {
        log.add("Bir sey");

        log.clear();

        assertTrue(log.latest(5).isEmpty());
        assertEquals("", log.last());
    }
}
