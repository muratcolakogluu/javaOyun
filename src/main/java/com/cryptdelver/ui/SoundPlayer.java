package com.cryptdelver.ui;

import com.cryptdelver.game.Ambience;
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

    /**
     * Ortam sesinin efektlere göre payı.
     *
     * <p>Efekt bir kez duyulup geçiyor, zemin ise dakikalarca dönüyor ve kulak
     * sürekli bir sese çok daha çabuk yoruluyor; o yüzden zemin efektlerin
     * biraz altında duruyor.</p>
     *
     * <p>Önce 0.55'ti. Zemin oyunda hiç duyulmuyordu ama asıl sebep bu değil,
     * ses dosyalarının frekans içeriğiydi (bkz. {@code AmbienceMaker}). O
     * düzeltildikten sonra ölçüm bu çarpanın da fazla ihtiyatlı olduğunu
     * gösterdi: zeminin orta bantta efektlerin altı yedi desibel altında
     * durması doğru yer.</p>
     */
    private static final double AMBIENCE_MIX = 0.85;

    private final Map<SoundEffect, AudioClip> clips = new EnumMap<>(SoundEffect.class);
    private final Settings settings;

    private Ambience current;
    private AudioClip playing;

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
                warmUp(clip);
            }
        }
    }

    /**
     * Klibi bir kez sessizce çalar.
     *
     * <p>Ses aygıtı ilk çalışta açılıyor ve bu açılış duyulur bir gecikme
     * yaratıyor — oyunda ilk vuruşun sesi geç geliyordu. Sıfır seviyeli bir
     * çalış aygıtı ve kanalı önceden hazırlıyor: oyuncu bunu duymuyor ama
     * ilk gerçek efekt anında çıkıyor.</p>
     */
    private void warmUp(AudioClip clip) {
        try {
            clip.play(0);
        } catch (RuntimeException e) {
            // Isınma olmazsa efekt yine çalar, sadece ilki gecikir.
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

    /**
     * Kat sesini başlatır; zaten o çalıyorsa dokunmuyor.
     *
     * <p>Aynı bölgede kat değiştirmek çok sık oluyor. Her seferinde baştan
     * başlatsaydık ses her inişte kesilip yeniden açılırdı — bölgenin sürekli
     * olması gereken zemini, kat sınırlarını duyuran bir efekte dönüşürdü.</p>
     */
    @Override
    public void playAmbience(Ambience ambience) {
        if (ambience == current) {
            return;
        }

        stopAmbience();
        current = ambience;
        startCurrent();
    }

    @Override
    public void stopAmbience() {
        if (playing != null) {
            playing.stop();
            playing = null;
        }
        current = null;
    }

    /**
     * Ses seviyesi değişince zemini yeni seviyeyle yeniden kurar.
     *
     * <p>{@code AudioClip} çalarken seviyesi değiştirilemiyor; sonraki
     * çalışta geçerli oluyor. Sürekli dönen bir ses için "sonraki çalış"
     * asla gelmediğinden, sesi kısan oyuncu zeminin kısılmadığını duyuyordu.
     * Yeniden başlatmak tek çözüm ve ayar değişikliği zaten seyrek.</p>
     */
    public void refreshAmbienceVolume() {
        if (current == null) {
            return;
        }

        Ambience wanted = current;
        stopAmbience();
        current = wanted;
        startCurrent();
    }

    private void startCurrent() {
        double volume = settings.getEffectiveMusicVolume();
        if (current == null || volume <= 0) {
            return;
        }

        AudioClip clip = loadAmbience(current);
        if (clip == null) {
            return;
        }

        clip.setCycleCount(AudioClip.INDEFINITE);
        clip.setVolume(volume * AMBIENCE_MIX);
        clip.play();
        playing = clip;
    }

    private AudioClip load(SoundEffect effect) {
        return loadFile(effect.getFileName(), effect.name());
    }

    /**
     * Ortam sesleri önden yüklenmiyor.
     *
     * <p>Efektler bellekte duruyor çünkü sık ve ani çalınıyorlar. Ortam sesi
     * ise on iki saniyelik, yarım megabaytlık bir dosya ve aynı anda yalnızca
     * biri gerekiyor; beşini birden bellekte tutmak boşuna.</p>
     */
    private AudioClip loadAmbience(Ambience ambience) {
        return loadFile(ambience.getFileName(), ambience.name());
    }

    private AudioClip loadFile(String fileName, String label) {
        try {
            URL url = getClass().getResource(SOUND_PATH + fileName + ".wav");
            if (url == null) {
                return null;
            }

            return new AudioClip(url.toExternalForm());
        } catch (RuntimeException e) {
            System.err.println("Ses yüklenemedi, sessiz geçiliyor: " + label);
            return null;
        }
    }
}
