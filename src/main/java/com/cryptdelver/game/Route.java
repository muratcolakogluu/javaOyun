package com.cryptdelver.game;

import java.util.List;
import java.util.Random;

/**
 * Merdivende sunulan iki yoldan biri.
 *
 * <p>Yirmi kat tek bir koridordu: iniyordun, aşağıda ne olduğunu zar
 * belirliyordu. Kat olayları katlara kişilik verdi ama <b>kimin kararı
 * olduğunu</b> değiştirmedi — yine zardı. Merdivende iki yol, o kararı
 * oyuncuya veriyor: aşağıda ne bulacağını <em>inmeden önce</em> biliyorsun ve
 * hangisine değeceğine sen karar veriyorsun.</p>
 *
 * <p>Yani kat olayı artık bir sürpriz değil bir <em>seçim</em>. Sürpriz olması
 * daha heyecanlı gelebilir ama seçim olması koşuyu senin çizdiğin bir şeye
 * çeviriyor: altını koruyan oyuncuyla can pahasına hazine toplayan oyuncu
 * aynı zindanda farklı yerlere varıyor.</p>
 *
 * <h2>Neden iki, neden hep zıt</h2>
 * <p>Üç seçenek ezberlenecek bir menü olurdu; tek seçenek karar değil. İki yol
 * hep birbirine zıt çıkıyor — biri sakin, biri kazançlı — çünkü iki iyi ya da
 * iki kötü seçenek arasında seçim yapmak karar değil, formalite.</p>
 */
public final class Route {

    /**
     * Sakin taraf: az tehlike, az ganimet.
     *
     * <p>{@code null} de bir seçenek: "sıradan kat" yani olay yok. Hiçbir
     * şey vaat etmeyen bir yol, bazen istenen şeyin kendisi.</p>
     */
    private static final List<FloorEvent> CALM =
            List.of(FloorEvent.SESSIZ, FloorEvent.KARANLIK);

    /** Kazançlı taraf: ikisi de ganimet veriyor, ikisi de bedelini alıyor. */
    private static final List<FloorEvent> GREEDY =
            List.of(FloorEvent.ZENGIN, FloorEvent.SURU);

    private final FloorEvent event;

    private Route(FloorEvent event) {
        this.event = event;
    }

    /** Bu yolu seçersen inilen katın hâli; sıradan bir kat ise {@code null}. */
    public FloorEvent getEvent() {
        return event;
    }

    /** Merdivende görünen ad. */
    public String getLabel() {
        return event == null ? Text.ROUTE_PLAIN.get() : event.getLabel();
    }

    /** Ne vaat ettiği: inmeden önce okunan tek satır. */
    public String getDescription() {
        return event == null ? Text.ROUTE_PLAIN_WHAT.get() : event.getDescription();
    }

    /**
     * Bu kattan aşağı inen iki yolu verir.
     *
     * <p>Çift katın tohumundan çıkıyor: aynı kata geri dönüp merdivene tekrar
     * bastığında aynı iki yol duruyor. Zar her seferinde yeniden atılsaydı
     * oyuncu istediği yolu çıkana kadar merdivene basıp kalkardı ve seçim
     * diye bir şey kalmazdı.</p>
     *
     * <p>Sakin tarafta üçte bir olasılıkla hiç olay yok. Yani "güvenli yol"
     * bazen gerçekten sadece güvenli, bazen de bir bedeli olan bir güvenlik
     * (karanlık kat gibi) — sakin olanı seçmek de her zaman bedava değil.</p>
     */
    public static List<Route> from(long seed) {
        Random dice = mixed(seed);

        FloorEvent calm = dice.nextInt(3) == 0
                ? null
                : CALM.get(dice.nextInt(CALM.size()));
        FloorEvent greedy = GREEDY.get(dice.nextInt(GREEDY.size()));

        return List.of(new Route(calm), new Route(greedy));
    }

    /**
     * Tohumu yollara dağıtan karıştırma.
     *
     * <p>{@link FloorBuilder} ile aynı sebep: {@code java.util.Random} tohumu
     * yalnızca hafifçe karıştırıyor ve birbirine yakın tohumlar birbirine
     * yakın ilk değerler veriyor.</p>
     */
    private static Random mixed(long seed) {
        long value = seed ^ ROUTE_SALT;
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return new Random(value);
    }

    private static final long ROUTE_SALT = 0x27D4EB2FL;
}
