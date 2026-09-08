package com.cryptdelver.ai;

import com.cryptdelver.world.Dungeon;
import com.cryptdelver.world.Position;

/**
 * Hedefe doğru tek adım atan en basit yol bulma: aradaki farkın büyük olduğu
 * eksende bir kare ilerler, orası duvarsa diğer ekseni dener.
 *
 * <p>Bu yaklaşım hafif ve anlaşılır, ama kısa görüşlüdür: bir duvarın önünde
 * sıkışıp kalabilir, köşeyi dönmeyi bilmez. 5. adımda A* eklendiğinde fark
 * gözle görülür olacak — bu sınıfı da silmeyeceğiz, çünkü hızlı ve aptal
 * düşmanlar (fare gibi) için hâlâ doğru davranış.</p>
 */
public class GreedyPathfinder implements Pathfinder {

    @Override
    public Position nextStep(Dungeon dungeon, Position from, Position to) {
        int dx = Integer.signum(to.x() - from.x());
        int dy = Integer.signum(to.y() - from.y());

        boolean horizontalFirst = Math.abs(to.x() - from.x()) >= Math.abs(to.y() - from.y());

        Position primary = horizontalFirst ? from.offset(dx, 0) : from.offset(0, dy);
        Position secondary = horizontalFirst ? from.offset(0, dy) : from.offset(dx, 0);

        if (!primary.equals(from) && dungeon.isWalkable(primary.x(), primary.y())) {
            return primary;
        }
        if (!secondary.equals(from) && dungeon.isWalkable(secondary.x(), secondary.y())) {
            return secondary;
        }
        return null;
    }

    @Override
    public String getName() {
        return "Açgözlü (tek adım)";
    }
}
