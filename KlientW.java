import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class KlientW extends JFrame implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private JTextArea area;
    private JTextField input;
    private JButton send;
    private DefaultListModel<String> usersModel;
    private JList<String> users;

    private String login;

    public KlientW(String host, int port) {  
        setTitle("Czat Java");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(5, 5));

        area = new JTextArea();
        area.setEditable(false);
        add(new JScrollPane(area), BorderLayout.CENTER);

        usersModel = new DefaultListModel<>();
        users = new JList<>(usersModel);
        add(new JScrollPane(users), BorderLayout.EAST);

        JPanel bottom = new JPanel(new BorderLayout(5, 5));
        input = new JTextField();
        send = new JButton("Wyślij");
        bottom.add(input, BorderLayout.CENTER);
        bottom.add(send, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        send.addActionListener(e -> wyslij());
        input.addActionListener(e -> wyslij());

        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            login = JOptionPane.showInputDialog(this, "Podaj login:");
            out.println(login);

            new Thread(this).start();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Błąd połączenia: " + ex.getMessage());
            System.exit(0);
        }
    }

    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("USERS:")) {
    String[] arr = line.substring(6).split(",");
    usersModel.clear();
    usersModel.addElement("ALL"); // dodajemy zawsze opcję „wszyscy”
    for (String u : arr) {
        if (!u.isEmpty()) usersModel.addElement(u);
    }


                } else if (line.startsWith("MSG:")) {
                    String[] parts = line.split(":", 3);
                    area.append(parts[1] + ": " + parts[2] + "\n");
                } else if (line.startsWith("PRIV:")) {
                    String msg = line.substring(5);
                    area.append("(priv) " + msg + "\n");
                } else if (line.startsWith("ERROR:")) {
                    area.append("[Błąd] " + line.substring(6) + "\n");
                }
            }
        } catch (IOException e) {
            area.append("Połączenie zakończone.\n");
        }
    }

   private void wyslij() {
    String txt = input.getText().trim();
    if (txt.isEmpty()) return;

    String target = users.getSelectedValue();
    if (target == null || target.equals("ALL")) {
        out.println("MSG:" + txt);
        area.append(login + ": " + txt + "\n"); // widzimy własną wiadomość
    } else {
        out.println("PRIV:" + target + ":" + txt);
        area.append("(priv) " + login + "->" + target + ": " + txt + "\n"); // widzimy własną prywatną
    }
    input.setText("");
}



    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new KlientW("localhost", 2217).setVisible(true));
    }
}
