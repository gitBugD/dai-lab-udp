import java.util.Objects;

public record MusicianPayload(String uuid, String sound, long lasEmission){

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MusicianPayload that = (MusicianPayload) o;
        return Objects.equals(uuid, that.uuid) && Objects.equals(sound, that.sound);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, sound);
    }
}