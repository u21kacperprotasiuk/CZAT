import java.io.*;
import java.net.*;

public class WatekKlienta extends Thread {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    public String login;

    public WatekKlienta(Socket s) {
        this.socket = s;
    }

    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("PODAJ_LOGIN:");
            login = in.readLine();
            System.out.println("Zalogował się: " + login);
            SerwerW.wyslijListeUzytkownikow();

            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("MSG:")) {
                    String msg = line.substring(4);
                    SerwerW.rozeslij(msg, this);
                } else if (line.startsWith("PRIV:")) {
                    // PRIV:cel:wiadomosc
                    String[] parts = line.split(":", 3);
                    if (parts.length == 3) {
                        SerwerW.wyslijPrywatna(parts[1], parts[2], this);
                    }
                } else if (line.equals("LOGOUT")) {
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("Błąd klienta: " + e.getMessage());
        } finally { 
            SerwerW.usunKlienta(this);
            try {
                socket.close();
            } catch (IOException ignored) {} 
        }
    }

    public void wyslij(String msg) {
        out.println(msg);
    }
}
