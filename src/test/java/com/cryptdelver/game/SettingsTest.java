package com.cryptdelver.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SettingsTest {

    private Settings settings;

    @BeforeEach
    void setUp() {
        settings = new Settings();
    }

    @Test
    @DisplayName("Varsayilan ses acik ve orta seviyede")
    void startsAtMidVolume() {
        assertFalse(settings.isMuted());
        assertTrue(settings.getVolume() > 0 && settings.getVolume() < 1);
        assertEquals(settings.getVolume(), settings.getEffectiveVolume());
    }

    @Test
    @DisplayName("Ses kademeli artip azalir")
    void volumeMovesInSteps() {
        settings.setVolume(0.5);

        settings.adjustVolume(Settings.VOLUME_STEP);
        assertEquals(0.6, settings.getVolume(), 1e-9);

        settings.adjustVolume(-Settings.VOLUME_STEP);
        assertEquals(0.5, settings.getVolume(), 1e-9);
    }

    /**
     * Sinirlarin disina tasmamasi onemli: eksi ses ya da bire cikan carpan
     * ses aygitinda beklenmedik davranisa yol acardi.
     */
    @Test
    @DisplayName("Ses 0 ile 1 arasinda kalir")
    void volumeStaysInRange() {
        settings.setVolume(1);
        settings.adjustVolume(Settings.VOLUME_STEP * 5);
        assertEquals(1, settings.getVolume(), 1e-9, "Tavani asmamali");

        settings.setVolume(0);
        settings.adjustVolume(-Settings.VOLUME_STEP * 5);
        assertEquals(0, settings.getVolume(), 1e-9, "Eksiye dusmemeli");
    }

    @Test
    @DisplayName("Sessize alinca calma seviyesi sifir, ayar korunur")
    void mutingKeepsTheStoredVolume() {
        settings.setVolume(0.8);

        settings.toggleMuted();

        assertTrue(settings.isMuted());
        assertEquals(0, settings.getEffectiveVolume(), "Sessizken hicbir sey calmamali");
        assertEquals(0.8, settings.getVolume(), 1e-9, "Ayarlanan seviye unutulmamali");

        settings.toggleMuted();
        assertEquals(0.8, settings.getEffectiveVolume(), 1e-9, "Acilinca eski seviye donmeli");
    }

    @Test
    @DisplayName("Yuzde gosterimi yuvarlanir")
    void percentIsRounded() {
        settings.setVolume(0.65);

        assertEquals(65, settings.getVolumePercent());
    }

    @Test
    @DisplayName("Ayarlar oyun yeniden baslayinca sifirlanmaz")
    void settingsSurviveRestart() {
        Game game = new Game(new com.cryptdelver.world.Dungeon(9, 9),
                new com.cryptdelver.entity.Player(4, 4));
        game.getSettings().setVolume(0.2);
        game.getSettings().setMuted(true);

        game.restart();

        assertEquals(0.2, game.getSettings().getVolume(), 1e-9);
        assertTrue(game.getSettings().isMuted());
    }
}
