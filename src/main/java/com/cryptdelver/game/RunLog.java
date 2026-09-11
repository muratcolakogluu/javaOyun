package com.cryptdelver.game;

/**
 * Bir koşunun defteri: ölüm ekranında anlatılacak hikâye.
 *
 * <p>Ölüm perdesi uzun süre iki sayı gösteriyordu — kaçıncı kat, kaç altın.
 * Oysa bir koşunun anlatacağı şey bu değil: <b>seni ne öldürdü</b>, kaç kat
 * indin, kaç kez geri döndün, kaç düşman devirdin. Bunlar olmadan ölüm bir
 * sonuç bildiriyor ama bir <em>hikâye</em> anlatmıyor, dolayısıyla "bir daha"
 * demenin de sebebi zayıf kalıyor.</p>
 *
 * <p>Neden ayrı bir sınıf: {@link Records} ile karıştırılmasın diye. Records
 * <em>koşular arası</em> hafıza — en derin kat, en çok altın — ve diske
 * yazılıyor. Bu defter ise tek bir koşunun içinde yaşıyor, ölümle birlikte
 * okunuyor ve yeniden başlarken siliniyor. İkisi aynı sınıfta olsaydı "hangi
 * sayı hangi ömre ait" sorusu her okumada yeniden sorulurdu.</p>
 *
 * <p>Sayaçları {@link Game} besliyor, defter kendi başına hiçbir şeye
 * bakmıyor: olayın ne zaman olduğunu bilen taraf oyunun kendisi ve defterin
 * oyunun durumunu okumaya çalışması iki yönlü bir bağ kurardı.</p>
 */
public class RunLog {

    private int kills;
    private int goldFound;
    private int deepestFloor = 1;
    private int returns;
    private int purchases;

    /**
     * Seni öldüren şeyin adı; henüz ölmediyse {@code null}.
     *
     * <p>Ad olarak tutuluyor, düşmanın kendisi olarak değil: öldüren yaratık
     * o an listeden çıkmış, hasar almış, belki de artık var olmayan bir nesne.
     * Ölüm ekranının ihtiyacı olan tek şey ise adı.</p>
     */
    private String killedBy;

    /** Kaç düşman devrildi. */
    public int getKills() {
        return kills;
    }

    /**
     * Koşu boyunca toplanan altın.
     *
     * <p>Kesedeki altından farklı: harcadığın altın da senin topladığın altın.
     * Yalnızca keseye bakan bir özet, tezgâhta doğru kararlar veren oyuncuyu
     * cimri oyuncudan daha kötü gösterirdi.</p>
     */
    public int getGoldFound() {
        return goldFound;
    }

    /** Ulaşılan en derin kat; geri dönmek bunu azaltmıyor. */
    public int getDeepestFloor() {
        return deepestFloor;
    }

    /** Kaç kez yukarı çıkıldı. */
    public int getReturns() {
        return returns;
    }

    /** Satıcıdan kaç parça alındı. */
    public int getPurchases() {
        return purchases;
    }

    public String getKilledBy() {
        return killedBy;
    }

    public boolean hasKiller() {
        return killedBy != null;
    }

    void recordKill() {
        kills++;
    }

    void recordGold(int amount) {
        if (amount > 0) {
            goldFound += amount;
        }
    }

    void recordPurchase() {
        purchases++;
    }

    void reachedFloor(int depth) {
        deepestFloor = Math.max(deepestFloor, depth);
    }

    void recordReturn() {
        returns++;
    }

    /**
     * Öldüren şeyi yazar.
     *
     * <p>İlk yazan kazanıyor: aynı karede hem diken hasarı hem düşman vuruşu
     * işleyebiliyor ve ölümü <em>getiren</em> vuruş ilk olan. Sonrakiler
     * defteri değiştirmemeli.</p>
     */
    void killedBy(String name) {
        if (killedBy == null) {
            killedBy = name;
        }
    }

    /** Yeni koşu: defter beyaz sayfa. */
    void reset() {
        kills = 0;
        goldFound = 0;
        deepestFloor = 1;
        returns = 0;
        purchases = 0;
        killedBy = null;
    }
}
