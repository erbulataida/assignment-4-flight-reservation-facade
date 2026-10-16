package kz.aida.flight.preferences;
import java.util.Set;
public class MealRulesEngine {
    public int loyaltyPoints(String meal) {
        if (!Set.of("STANDARD", "VEGETARIAN", "HALAL").contains(meal))
            throw new IllegalArgumentException("Requested meal is unavailable: " + meal);
        return 100;
    }
}
