package com.cryptdelver;

import com.cryptdelver.entity.LegendWeapon;
import com.cryptdelver.entity.Player;
import com.cryptdelver.game.Game;
import com.cryptdelver.game.Text;
import com.cryptdelver.ui.GameScreen;
import com.cryptdelver.world.BspGenerator;
import com.cryptdelver.world.DungeonGenerator;
import com.cryptdelver.world.RandomWalkGenerator;
import java.util.List;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Uygulamanın giriş noktası.
 *
 * <p>Tek işi parçaları birbirine bağlamak: üreticileri seç, oyun durumunu kur,
 * ekrana ver. Oyun kuralları {@code game}, harita {@code world}, çizim ise
 * {@code ui} paketinde; bu sınıf hepsini tanır ama onlar birbirini tanımaz.</p>
 */
public class Main extends Application {

    private static final int DUNGEON_WIDTH = 40;
    private static final int DUNGEON_HEIGHT = 22;

    @Override
    public void start(Stage stage) {
        // Strateji deseni: oyun bu listeden birini kullanır, hangisi olduğunu bilmez.
        List<DungeonGenerator> generators = List.of(new BspGenerator(), new RandomWalkGenerator());

        // Başlangıç konumu önemsiz; ilk kat üretilirken oyuncu yerleştirilecek.
        Player player = new Player(0, 0);
        Game game = new Game(generators, DUNGEON_WIDTH, DUNGEON_HEIGHT, player);

        grantLegendaryIfRequested(game);

        GameScreen screen = new GameScreen(game);
        Scene scene = new Scene(screen.getRoot(), screen.getWidth(), screen.getHeight());
        screen.attachInput(scene);

        stage.setScene(scene);
        stage.setTitle("CryptDelver");
        stage.setResizable(false);
        stage.setOnCloseRequest(event -> screen.stop());
        stage.show();

        screen.enableSound();
        screen.start();
    }

    /**
     * İstenirse oyuncuyu Yıldızkıran'la başlatır.
     *
     * <p>Efsanevi kılıcın çıkma şansı binde bir, yani onu <em>görmek</em> bile
     * başlı başına bir olay. Denemek ya da göstermek için her seferinde şansa
     * bakmak yerine açık bir kapı bırakıldı.</p>
     *
     * <p>Kapı ortam değişkeni, oyun içi bir seçenek değil: menüye "efsanevi
     * kılıçla başla" satırı koymak ödülü ödül olmaktan çıkarırdı. Kullanımı:
     * {@code CRYPTDELVER_LEGENDARY=1} ile çalıştır.</p>
     */
    private static void grantLegendaryIfRequested(Game game) {
        if (!"1".equals(System.getenv("CRYPTDELVER_LEGENDARY"))
                && !Boolean.getBoolean("cryptdelver.legendary")) {
            return;
        }

        LegendWeapon legend = new LegendWeapon(0, 0);
        game.getInventory().add(legend);
        game.getPlayer().equip(legend);
        game.getMessageLog().add(Text.MSG_LEGEND_START.get());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
