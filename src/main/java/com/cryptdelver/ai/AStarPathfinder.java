package com.cryptdelver.ai;

import com.cryptdelver.world.Dungeon;
import com.cryptdelver.world.Position;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * A* algoritmasıyla en kısa yolu bulan yol bulucu.
 *
 * <p>Her kareye iki sayı yazılır: {@code g} — başlangıçtan buraya gelmenin
 * gerçek maliyeti, {@code h} — buradan hedefe kalan yolun tahmini. Sıradaki
 * kare her zaman {@code f = g + h} değeri en küçük olandır, yani "şu ana kadarki
 * yol + kalan tahmin" toplamı en umutlu olan. Öncelik kuyruğu bu seçimi
 * logaritmik zamanda yapıyor.</p>
 *
 * <p>Tahmin olarak Manhattan uzaklığını kullanıyoruz. Dört yönlü hareket ve
 * kare başına 1 maliyet olduğu için bu tahmin gerçek maliyeti <em>asla</em>
 * aşmaz — algoritmanın en kısa yolu bulma garantisi buna dayanıyor. (Çapraz
 * hareket ekleseydik bu tahmin geçersiz olurdu.)</p>
 *
 * <p>{@link GreedyPathfinder}'dan farkı: o yalnızca hedefin yönüne bakar ve
 * duvara toslayınca kalır; bu, duvarın etrafından dolaşmayı bulur.</p>
 */
public class AStarPathfinder implements Pathfinder {

    /**
     * Aramanın genişleteceği azami kare sayısı.
     *
     * <p>Ulaşılmaz bir hedef için A*, erişilebilir haritanın tamamını tarar.
     * Bu sınır, her düşmanın her adımında en kötü ihtimalle ne kadar iş
     * yapacağını sabitliyor.</p>
     */
    private static final int MAX_EXPANSIONS = 5000;

    private static final int[][] DIRECTIONS = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};

    @Override
    public Position nextStep(Dungeon dungeon, Position from, Position to) {
        if (from.equals(to) || !dungeon.isWalkable(to.x(), to.y())) {
            return null;
        }

        Map<Position, Position> cameFrom = new HashMap<>();
        Map<Position, Integer> costSoFar = new HashMap<>();
        Set<Position> visited = new HashSet<>();
        PriorityQueue<Node> frontier = new PriorityQueue<>(Comparator.comparingInt(Node::priority));

        costSoFar.put(from, 0);
        frontier.add(new Node(from, from.manhattanDistance(to)));

        int expansions = 0;
        while (!frontier.isEmpty() && expansions++ < MAX_EXPANSIONS) {
            Position current = frontier.poll().position();

            if (current.equals(to)) {
                return firstStepOf(cameFrom, from, to);
            }
            // Kuyrukta aynı karenin eski (daha kötü) kopyaları kalmış olabilir.
            if (!visited.add(current)) {
                continue;
            }

            int currentCost = costSoFar.get(current);
            for (int[] direction : DIRECTIONS) {
                Position next = current.offset(direction[0], direction[1]);
                if (!dungeon.isWalkable(next.x(), next.y())) {
                    continue;
                }

                int newCost = currentCost + 1;
                Integer knownCost = costSoFar.get(next);
                if (knownCost != null && knownCost <= newCost) {
                    continue;
                }

                costSoFar.put(next, newCost);
                cameFrom.put(next, current);
                frontier.add(new Node(next, newCost + next.manhattanDistance(to)));
            }
        }

        return null;
    }

    @Override
    public String getName() {
        return "A* (en kısa yol)";
    }

    /**
     * Bulunan yolu hedeften geriye sararak başlangıcın hemen ardındaki kareyi
     * verir. Tüm yolu döndürmüyoruz: düşman her adımda yeniden hesaplıyor,
     * çünkü oyuncu bu arada yer değiştiriyor.
     */
    private Position firstStepOf(Map<Position, Position> cameFrom, Position from, Position to) {
        Deque<Position> path = new ArrayDeque<>();
        Position current = to;

        while (current != null && !current.equals(from)) {
            path.addFirst(current);
            current = cameFrom.get(current);
        }

        return path.isEmpty() ? null : path.getFirst();
    }

    /** Öncelik kuyruğundaki bir kare ve onun {@code f = g + h} değeri. */
    private record Node(Position position, int priority) {
    }
}
