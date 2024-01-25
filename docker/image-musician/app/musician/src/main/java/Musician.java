import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import static java.util.concurrent.TimeUnit.*;

public class Musician{
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);
    final static HashMap<String, String> instruments = new HashMap<>(5);
    String uuid;
    final String instrument;
    final static String IPADDR = "239.255.22.5";
    final static int PORT = 9904;

    public Musician(String instrument, String uuid) {
        if(instruments.isEmpty()){
            instruments.put("piano", "ti-ta-ti");
            instruments.put("trumpet", "pouet");
            instruments.put("flute", "trulu");
            instruments.put("violin", "gzi-gzi");
            instruments.put("drum", "boum-boum");
        }
        this.uuid = uuid;
        this.instrument = instrument;
    }
    public Musician(String instrument) {
        this(instrument, null);
        uuid = UUID.randomUUID().toString();
    }

    public void playEverySecond() {
        final Runnable player = () -> sendPayload();
        final ScheduledFuture<?> beeperHandle =
                scheduler.scheduleAtFixedRate(player, 0, 1, SECONDS);
    }

    private void sendPayload() {
        try (DatagramSocket socket = new DatagramSocket()) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            MusicianPayload musicianPayload = new MusicianPayload(this.uuid, instruments.get(this.instrument), Instant.now().toEpochMilli());
            String jsonPayload = gson.toJson(musicianPayload);
            System.out.println(jsonPayload);
            byte[] payload = jsonPayload.getBytes();
            var dest_address = new InetSocketAddress(IPADDR, PORT);
            var packet = new DatagramPacket(payload,
                    payload.length,
                    dest_address);
            socket.send(packet);
        } catch (IOException e) {
            System.out.println("Error sending payload:" + e.getMessage());
        }
    }

    public static void main(String[] args){
        if(args[0] == null)
            return;
        Musician musician = new Musician(args[0]);
        musician.playEverySecond();
    }
}