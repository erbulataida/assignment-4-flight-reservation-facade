package kz.aida.flight.preferences;
public class TravelPreference {
    private final String id, passenger, meal;
    private final int points;
    private PreferenceStatus status;
    public TravelPreference(String id, String passenger, String meal, int points) {
        this(id, passenger, meal, points, PreferenceStatus.NEW);
    }
    TravelPreference(String id, String passenger, String meal, int points, PreferenceStatus status) {
        this.id=id; this.passenger=passenger; this.meal=meal; this.points=points; this.status=status;
    }
    public void confirm() {
        if (status != PreferenceStatus.NEW) throw new IllegalStateException("Only NEW preferences can be confirmed");
        status=PreferenceStatus.CONFIRMED;
    }
    public void cancel() {
        if (status != PreferenceStatus.CONFIRMED) throw new IllegalStateException("Only CONFIRMED preferences can be cancelled");
        status=PreferenceStatus.CANCELLED;
    }
    public String getId() { return id; }
    public String getPassenger() { return passenger; }
    public String getMeal() { return meal; }
    public int getPoints() { return status == PreferenceStatus.CONFIRMED ? points : 0; }
    int storedPoints() { return points; }
    public PreferenceStatus getStatus() { return status; }
}
