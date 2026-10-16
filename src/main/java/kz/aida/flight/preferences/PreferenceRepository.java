package kz.aida.flight.preferences;
import kz.aida.flight.shared.CsvStore;
import java.nio.file.Path;
import java.util.*;
public class PreferenceRepository {
    private final CsvStore store;
    public PreferenceRepository(Path directory) { store=new CsvStore(directory.resolve("preferences.csv")); }
    public List<TravelPreference> findAll() {
        List<TravelPreference> result=new ArrayList<>();
        for (String[] r:store.readAll()) {
            if (r.length != 5) throw new IllegalStateException("Invalid preference record");
            result.add(new TravelPreference(r[0],r[1],r[2],Integer.parseInt(r[3]),PreferenceStatus.valueOf(r[4])));
        }
        return result;
    }
    public Optional<TravelPreference> findById(String id) { return findAll().stream().filter(p->p.getId().equals(id)).findFirst(); }
    public void save(TravelPreference p) {
        var rows=store.readAll(); rows.removeIf(r->r[0].equals(p.getId()));
        rows.add(new String[]{p.getId(),p.getPassenger(),p.getMeal(),Integer.toString(p.storedPoints()),p.getStatus().name()});
        store.writeAll(rows);
    }
}
