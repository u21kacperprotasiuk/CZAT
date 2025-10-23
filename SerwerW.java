import java.io.*;
import java.net.*;
import java.util.Vector;

public class SerwerW {
    // lista wszystkich klientów
    static Vector<WatekKlienta> klienci = new Vector<>();

    public static void main(String[] args) {
        try {
            ServerSocket s = new ServerSocket(2217);
            System.out.println("Serwer uruchomiony na porcie 2217...");

            while (true) {
                Socket incoming = s.accept();
                System.out.println("Nowe połączenie: " + incoming);
                WatekKlienta w = new WatekKlienta(incoming);
                klienci.add(w);
                w.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // wysyłanie listy użytkowników do wszystkich
    public static synchronized void wyslijListeUzytkownikow() {
        StringBuilder sb = new StringBuilder();
        sb.append("USERS:");
        for (WatekKlienta w : klienci) {
            if (w.login != null)
                sb.append(w.login).append(",");
        }
        for (WatekKlienta w : klienci) {
            w.wyslij(sb.toString());
        }
    }

    // rozsyłanie wiadomości
  public static synchronized void rozeslij(String msg, WatekKlienta nadawca) {
    for (WatekKlienta w : klienci) {
        w.wyslij("MSG:" + nadawca.login + ":" + msg);
    }



    }

    // wysłanie prywatnej wiadomości
    public static synchronized void wyslijPrywatna(String cel, String msg, WatekKlienta nadawca) {
    for (WatekKlienta w : klienci) {
        if (w.login.equals(cel) || w == nadawca) {
            w.wyslij("PRIV:" + nadawca.login + ":" + cel + ":" + msg);
        }
    }
}


    // usuwanie klienta po rozłączeniu
    public static synchronized void usunKlienta(WatekKlienta w) {
        klienci.remove(w);
        System.out.println(w.login + " rozłączony.");
        wyslijListeUzytkownikow();
    }
}
