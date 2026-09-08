package com.cryptdelver.world;

/**
 * Bir zindan katını üretmenin sözleşmesi.
 *
 * <p>Strateji deseni: oyun hangi algoritmanın çalıştığını bilmez, yalnızca bu
 * arayüzü çağırır. Böylece BSP ve rastgele yürüyüş birbirinin yerine
 * geçebiliyor; ileride "mağara katı", "kripta katı" gibi yeni üreticiler
 * eklemek de mevcut kodun tek satırını değiştirmeyi gerektirmiyor.</p>
 */
public interface DungeonGenerator {

    /**
     * Verilen ölçülerde bir kat üretir.
     *
     * <p>Aynı tohum (seed) her zaman aynı haritayı vermelidir — hem testleri
     * tekrarlanabilir kılar hem de kaydetme sisteminde bütün haritayı diske
     * yazmak yerine tek bir sayıyı saklamayı mümkün kılar.</p>
     *
     * @param seed rastgeleliğin tohumu
     * @return tamamı birbirine bağlı, kenarları duvarla çevrili bir kat
     */
    Dungeon generate(int width, int height, long seed);

    /** Üreticinin ekranda gösterilecek adı. */
    String getName();
}
