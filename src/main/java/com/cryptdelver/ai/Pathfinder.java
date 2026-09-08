package com.cryptdelver.ai;

import com.cryptdelver.world.Dungeon;
import com.cryptdelver.world.Position;

/**
 * Bir noktadan diğerine giden yolun ilk adımını bulan algoritmaların sözleşmesi.
 *
 * <p>Düşmanlar hangi algoritmayı kullandıklarını bilmez, yalnızca bu arayüzü
 * çağırır. Şu an tek uygulama {@link GreedyPathfinder} (hedefe doğru tek adım);
 * 5. adımda eklenecek A* uygulaması, düşman sınıflarının tek satırını bile
 * değiştirmeden yerine geçebilecek.</p>
 *
 * <p>Yalnızca duvarları dikkate alır — hedefteki karede başka bir varlık olup
 * olmadığı oyunun kuralı, yol bulmanın değil.</p>
 */
public interface Pathfinder {

    /**
     * {@code from} karesinden {@code to} karesine giden yolun ilk adımı.
     *
     * @return atılacak komşu kare; makul bir adım yoksa {@code null}
     */
    Position nextStep(Dungeon dungeon, Position from, Position to);

    /** Algoritmanın adı; hata ayıklama ve ekranda gösterim için. */
    String getName();
}
