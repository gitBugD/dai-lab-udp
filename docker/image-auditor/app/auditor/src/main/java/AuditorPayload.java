import java.util.Objects;

public record AuditorPayload(String uuid, String instrument, long lastEmission){
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditorPayload that = (AuditorPayload) o;
        return Objects.equals(uuid, that.uuid) && Objects.equals(instrument, that.instrument);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, instrument);
    }
}