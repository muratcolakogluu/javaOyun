package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cryptdelver.entity.Enchantment;
import com.cryptdelver.entity.Imp;
import com.cryptdelver.entity.Potion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Dil destegi.
 *
 * <p>Oyun Turkce yazildi; Ingilizce bir ceviri. Buradaki testler ceviri
 * katmaninin <em>her yere</em> ulastigini dogruluyor: yalnizca menu degil,
 * dusman adlari, esya aciklamalari ve mesajlar da dil degisince degismeli.
 * Yarim cevrilmis bir oyun, hic cevrilmemis olandan kotu gorunur.</p>
 *
 * <p>Dil durgun bir alanda tutuldugu icin her test kendinden sonrasini
 * temizliyor: birakilan Ingilizce, sirada gelen baska bir testi sasirtir.</p>
 */
class LanguageTest {

    @AfterEach
    void backToTurkish() {
        Text.use(Language.TURKCE);
    }

    @Test
    @DisplayName("Varsayilan dil Turkce")
    void turkishIsTheDefault() {
        assertEquals(Language.TURKCE, Text.current());
        assertEquals("Yeni Oyun", Text.MENU_NEW_GAME.get());
    }

    @Test
    @DisplayName("Dil degisince metin de degisiyor")
    void switchingLanguageSwitchesText() {
        Text.use(Language.ENGLISH);

        assertEquals("New Game", Text.MENU_NEW_GAME.get());
        assertEquals("Settings", Text.MENU_SETTINGS.get());
    }

    /** Ayarlardan degistirmek yetmeli; metin isteyen taraflarin haberi olmali. */
    @Test
    @DisplayName("Ayarlardan secilen dil metinlere gecmis oluyor")
    void settingsDriveTheTextLayer() {
        Settings settings = new Settings();
        settings.setLanguage(Language.ENGLISH);

        assertEquals(Language.ENGLISH, Text.current());
        assertEquals("Quit", Text.MENU_QUIT.get());
    }

    /** Dusman adi kurulusta degil, soruldugunda cozuluyor. */
    @Test
    @DisplayName("Kurulmus bir dusmanin adi da dille degisiyor")
    void livingEntitiesFollowTheLanguage() {
        Imp imp = new Imp(0, 0);
        assertEquals("İmp", imp.getName());

        Text.use(Language.ENGLISH);

        assertEquals("Imp", imp.getName(), "Ad kurulusta dondurulmus olmamali");
    }

    @Test
    @DisplayName("Esya adi ve aciklamasi da ceviriden geciyor")
    void itemsAreTranslated() {
        Potion potion = new Potion(0, 0);
        String turkish = potion.getDescription();

        Text.use(Language.ENGLISH);

        assertEquals("Potion", potion.getName());
        assertNotEquals(turkish, potion.getDescription(), "Aciklama da cevrilmeli");
    }

    @Test
    @DisplayName("Buyu adlari ve aciklamalari cevriliyor")
    void enchantmentsAreTranslated() {
        assertEquals("Vampirlik", Enchantment.VAMPIRLIK.getLabel());

        Text.use(Language.ENGLISH);

        assertEquals("Vampirism", Enchantment.VAMPIRLIK.getLabel());
    }

    @Test
    @DisplayName("Zorluk ve bolge adlari cevriliyor")
    void difficultyAndRegionsAreTranslated() {
        assertEquals("Kolay", Difficulty.KOLAY.getLabel());
        assertEquals("Mahzen", FloorTheme.MAHZEN.getLabel());

        Text.use(Language.ENGLISH);

        assertEquals("Easy", Difficulty.KOLAY.getLabel());
        assertEquals("The Vault", FloorTheme.MAHZEN.getLabel());
    }

    /**
     * Bicimlendirme yalnizca arguman verilince calisiyor: icinde yuzde isareti
     * olan metinler argumansiz cagrilinca oldugu gibi kalmali.
     */
    @Test
    @DisplayName("Argumanli metinler sayilari yerine koyuyor")
    void argumentsAreFilledIn() {
        assertTrue(Text.MSG_ENEMY_HURT.get("İmp", 3).contains("3"));
        assertTrue(Text.MSG_ENEMY_HURT.get("İmp", 3).contains("İmp"));

        Text.use(Language.ENGLISH);

        assertEquals("Imp took 3 damage.", Text.MSG_ENEMY_HURT.get("Imp", 3));
    }

    /** Dilin kendi adi cevrilmiyor: "Turkce" arayan biri "Turkish" yazsa bulamaz. */
    @Test
    @DisplayName("Dil adlari kendi dillerinde yaziliyor")
    void languageNamesStayInTheirOwnLanguage() {
        assertEquals("Türkçe", Language.TURKCE.getLabel());
        assertEquals("English", Language.ENGLISH.getLabel());

        Text.use(Language.ENGLISH);

        assertEquals("Türkçe", Language.TURKCE.getLabel());
    }

    @Test
    @DisplayName("Diller arasinda ileri geri gezilebiliyor")
    void languagesCycleBothWays() {
        assertEquals(Language.ENGLISH, Language.TURKCE.next());
        assertEquals(Language.TURKCE, Language.ENGLISH.next());
        assertEquals(Language.ENGLISH, Language.TURKCE.previous());
    }

    /**
     * Her metnin iki karsiligi da dolu olmali. Ayni satirda yazildiklari icin
     * unutmak zor ama imkansiz degil; bos birakilan bir ceviri oyunda bos bir
     * satir olarak gorunur.
     */
    @Test
    @DisplayName("Hicbir ceviri bos degil")
    void everyTextHasBothLanguages() {
        for (Text text : Text.values()) {
            Text.use(Language.TURKCE);
            assertTrue(!text.get().isBlank(), text + " Turkcesi bos");

            Text.use(Language.ENGLISH);
            assertTrue(!text.get().isBlank(), text + " Ingilizcesi bos");
        }
    }
}
