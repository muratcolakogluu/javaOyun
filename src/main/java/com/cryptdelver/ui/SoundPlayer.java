package com.cryptdelver.ui;

import com.cryptdelver.game.Settings;
import com.cryptdelver.game.SoundEffect;
import com.cryptdelver.game.SoundListener;
import java.net.URL;
import java.util.EnumMap;
import java.util.Map;
import javafx.scene.media.AudioClip;

/**
 * Ses olaylarını gerçekten çalan dinleyici.
 *
 * <p>Klipler açılışta bir kez yükleniyor ve bellekte kalıyor: efektler kısa,
 * sık ve üst üste çalınıyor; her seferinde dosyadan okumak tıkanmaya yol
 * açardı.</p>
 *
 * <p>Bir dosya eksik ya da ses aygıtı yoksa oyun düşmüyor, o efekt sessiz
 * geçiyor — ses, oynanışın çalışması için gerekli değil.</p>
 */
public class SoundPlayer implements SoundListener {

    private static final String SOUND_PATH = "/assets/sound/";

    private final Map<SoundEffect, AudioClip> clips = new EnumMap<>(SoundEffect.class);
    private final Settings settings;

    /**
     * @param settings ses seviyesi buradan okunuyor; oyuncu ayarlar ekranından
     *                 değiştirdiğinde bir sonraki efekt yeni seviyeyle çalıyor
     */
    public SoundPlayer(Settings settings) {
        this.settings = settings;

        for (SoundEffect effect : SoundEffect.values()) {
            AudioClip clip = load(effect);
            if (clip != null) {
                clips.put(effect, clip);
            }
        }
    }

    @Override
    public void play(SoundEffect effect) {
        AudioClip clip = clips.get(effect);
        if (clip == null) {
            return;
        }

        double volume = settings.getEffectiveVolume();
        if (volume > 0) {
            clip.play(volume);
        }
    }

    private AudioClip load(SoundEffect effect) {
        try {
            URL url = getClass().getResource(SOUND_PATH + effect.getFileName() + ".wav");
            if (url == null) {
                return null;
            }

            return new AudioClip(url.toExternalForm());
        } catch (RuntimeException e) {
            System.err.println("Ses yüklenemedi, sessiz geçiliyor: " + effect);
            return null;
        }
    }
}
