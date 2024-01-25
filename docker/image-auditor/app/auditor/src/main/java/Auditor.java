import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.net.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.*;

public class Auditor {
    final static HashMap<String, String> instruments = new HashMap<>(5);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final static String IPADDR_UDP = "239.255.22.5";
    private final static int PORT_TCP = 2205;
    private final static int PORT_UDP = 9904;
    private List<AuditorPayload> musicians = new ArrayList<>();

    public Auditor() {
        if (instruments.isEmpty()) {
            instruments.put("piano", "ti-ta-ti");
            instruments.put("trumpet", "pouet");
            instruments.put("flute", "trulu");
            instruments.put("violin", "gzi-gzi");
            instruments.put("drum", "boum-boum");
        }
    }

    public void addOrReplaceMusician(AuditorPayload payload) {
        musicians.remove(payload);
        musicians.add(payload);
    }

    public void checkListEveryFiveSecond() {
        final Runnable checker = this::removeMusiciansNotPlaying;
        final ScheduledFuture<?> musiciansHandle =
                scheduler.scheduleAtFixedRate(checker, 0, 1, SECONDS);
    }

    private void removeMusiciansNotPlaying() {
        musicians.removeIf(payload -> payload.lastEmission() < (Instant.now().toEpochMilli() - SECONDS.toMillis(5)));
    }

    private Runnable runUDP() {
        return () -> {
            try (DatagramSocket datagramSocket = new DatagramSocket(PORT_UDP);) {
                System.out.println("Starting virtual thread for UDP server on port: " + PORT_UDP);
                var group_address = new InetSocketAddress(IPADDR_UDP, PORT_UDP);
                NetworkInterface netif = NetworkInterface.getByName("eth0");
                datagramSocket.joinGroup(group_address, netif);
                byte[] buffer = new byte[1024];
                var packet = new DatagramPacket(buffer, buffer.length);
                while (true) {
                    datagramSocket.receive(packet);
                    String message = new String(packet.getData(), 0,
                            packet.getLength(), UTF_8);
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    MusicianPayload receivedMusicianPayload = gson.fromJson(message, MusicianPayload.class);
                    for (Map.Entry<String, String> i : Auditor.instruments.entrySet()) {
                        if (i.getValue().equals(receivedMusicianPayload.sound())) {
                            AuditorPayload a = new AuditorPayload(receivedMusicianPayload.uuid(), i.getKey(), receivedMusicianPayload.lasEmission());
                            Auditor.this.addOrReplaceMusician(a);
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Error datagram socket: " + e.getMessage());
            }
        };
    }

    private Runnable runTCP() {
        return () -> {
            System.out.println("Starting virtual thread for TCP server on port: " + PORT_TCP);
            try (ServerSocket serverSocket = new ServerSocket(PORT_TCP);) {
                while (true) {
                    try (Socket socket = serverSocket.accept();
                         var out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8))) {
                        System.out.println("Client connected");
                        Gson gson = new GsonBuilder().setPrettyPrinting().create();
                        String response = gson.toJson(this.musicians);
                        out.write(response + '\n');
                        out.flush();
                    } catch (IOException e) {
                        System.err.println("Error client socket: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                System.err.println("Error in server socket: " + e.getMessage());
            }
        };
    }

    public void serve() {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(runUDP());
            executor.submit(runTCP());
        } catch (Exception e) {
            System.out.println("Error virtual threads: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Auditor auditor = new Auditor();
        auditor.checkListEveryFiveSecond();
        auditor.serve();
    }
}
