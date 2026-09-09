package com.cryptdelver.persistence;

import com.cryptdelver.game.Records;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Rekorları diske yazar ve okur.
 *
 * <p>Ayar dosyasıyla aynı biçim ({@code anahtar|değer}) ama ayrı dosya: ayarlar
 * oyuncunun tercihi, rekorlar geçmişi. Aynı dosyada tutmak "ayarları sıfırla"
 * gibi bir işlemin rekorları da silmesine yol açardı.</p>
 *
 * <p>Bozuk ya da eksik dosya oyunu durdurmuyor: rekor kaybı can sıkıcı ama
 * oyunun açılmaması çok daha kötü.</p>
 */
public class RecordsFile {

    private final Path path;

    /** Varsayılan konum: {@code ~/.cryptdelver/records.txt}. */
    public RecordsFile() {
        this(Path.of(System.getProperty("user.home"), ".cryptdelver", "records.txt"));
    }

    public RecordsFile(Path path) {
        this.path = path;
    }

    public Path getPath() {
        return path;
    }

    /** Dosyadaki değerleri verilen nesneye yazar; dosya yoksa dokunmaz. */
    public void load(Records records) {
        if (!Files.isRegularFile(path)) {
            return;
        }

        int deepest = 0;
        int gold = 0;
        int runs = 0;
        int wins = 0;

        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String[] parts = line.split("\\|", -1);
                if (parts.length < 2) {
                    continue;
                }

                switch (parts[0]) {
                    case "deepest" -> deepest = Integer.parseInt(parts[1]);
                    case "gold" -> gold = Integer.parseInt(parts[1]);
                    case "runs" -> runs = Integer.parseInt(parts[1]);
                    case "wins" -> wins = Integer.parseInt(parts[1]);
                    default -> {
                        // Tanımadığımız satır yok sayılıyor; ileride eklenen bir
                        // alan eski sürümü çalıştırmayı engellemesin.
                    }
                }
            }
            records.restore(deepest, gold, runs, wins);
        } catch (IOException | NumberFormatException e) {
            System.err.println("Rekorlar okunamadı, sıfırdan başlanıyor: " + e.getMessage());
        }
    }

    /** Rekorları diske yazar; klasör yoksa oluşturur. */
    public void save(Records records) {
        List<String> lines = new ArrayList<>();
        lines.add("deepest|" + records.getDeepestFloor());
        lines.add("gold|" + records.getMostGold());
        lines.add("runs|" + records.getRuns());
        lines.add("wins|" + records.getWins());

        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Rekorlar yazılamadı: " + e.getMessage());
        }
    }
}
