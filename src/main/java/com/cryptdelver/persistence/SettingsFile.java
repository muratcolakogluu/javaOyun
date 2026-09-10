package com.cryptdelver.persistence;

import com.cryptdelver.game.Difficulty;
import com.cryptdelver.game.Settings;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Ayarları diske yazar ve okur.
 *
 * <p>Kayıt dosyasından ayrı duruyor: ayarlar oyundan bağımsız yaşıyor, ölünce
 * ya da yeni oyuna başlayınca sıfırlanmıyor. Biçim yine {@code |} ayraçlı düz
 * metin, aynı gerekçeyle — elle açılıp okunabilsin.</p>
 *
 * <p>Bozuk ya da eksik bir dosya sorun değil: okuma sessizce varsayılanları
 * bırakıyor. Ayar dosyası yüzünden oyunun açılmaması saçma olurdu.</p>
 */
public class SettingsFile {

    private final Path path;

    /** Varsayılan konum: kaydın yanında, {@code .cryptdelver/settings.txt}. */
    public SettingsFile() {
        this(Path.of(System.getProperty("user.home"), ".cryptdelver", "settings.txt"));
    }

    public SettingsFile(Path path) {
        this.path = path;
    }

    public Path getPath() {
        return path;
    }

    /** Dosyadaki değerleri ayarlara uygular; dosya yoksa hiçbir şey değişmez. */
    public void load(Settings settings) {
        if (!Files.isRegularFile(path)) {
            return;
        }

        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                apply(settings, line);
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("Ayarlar okunamadı, varsayılanlar kullanılıyor: " + e.getMessage());
        }
    }

    /** Ayarları diske yazar; klasör yoksa oluşturur. */
    public void save(Settings settings) {
        List<String> lines = new ArrayList<>();
        lines.add("volume|" + settings.getVolume());
        lines.add("music|" + settings.getMusicVolume());
        lines.add("muted|" + settings.isMuted());
        lines.add("difficulty|" + settings.getDifficulty().name());
        lines.add("autosave|" + settings.isAutoSave());

        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Ayarlar yazılamadı: " + e.getMessage());
        }
    }

    private void apply(Settings settings, String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length < 2) {
            return;
        }

        switch (parts[0]) {
            case "volume" -> settings.setVolume(Double.parseDouble(parts[1]));
            case "music" -> settings.setMusicVolume(Double.parseDouble(parts[1]));
            case "muted" -> settings.setMuted(Boolean.parseBoolean(parts[1]));
            case "difficulty" -> settings.setDifficulty(parseDifficulty(parts[1]));
            case "autosave" -> settings.setAutoSave(Boolean.parseBoolean(parts[1]));
            default -> {
                // Tanımadığımız satırı yok sayıyoruz; ileride eklenen bir ayar
                // eski sürümü çalıştırmayı engellemesin.
            }
        }
    }

    /**
     * Zorluk etiketini çözer; tanımadığımız değer varsayılana düşüyor.
     *
     * <p>Elle düzenlenmiş ya da ileride kaldırılmış bir kademe yüzünden oyunun
     * açılmaması saçma olurdu — dosyadaki diğer ayarlar okunmaya devam
     * ediyor.</p>
     */
    private Difficulty parseDifficulty(String value) {
        for (Difficulty candidate : Difficulty.values()) {
            if (candidate.name().equalsIgnoreCase(value)) {
                return candidate;
            }
        }
        return Difficulty.NORMAL;
    }
}
