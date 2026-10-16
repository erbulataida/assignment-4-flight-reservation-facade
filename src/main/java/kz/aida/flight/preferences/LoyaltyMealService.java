package kz.aida.flight.preferences;
import java.util.*;
public class LoyaltyMealService {
    private final PreferenceRepository repository;
    private final MealRulesEngine rules=new MealRulesEngine();
    public LoyaltyMealService(PreferenceRepository repository) { this.repository=repository; }
    public TravelPreference confirm(String id, String passenger, String meal) {
        if (repository.findById(id).isPresent()) throw new IllegalArgumentException("Duplicate preference ID");
        var p=new TravelPreference(id, passenger, meal, rules.loyaltyPoints(meal));
        p.confirm(); repository.save(p); return p;
    }
    public void cancel(String id) {
        var p=repository.findById(id).orElseThrow(); p.cancel(); repository.save(p);
    }
    public Optional<TravelPreference> find(String id) { return repository.findById(id); }
    public List<TravelPreference> all() { return repository.findAll(); }
}
