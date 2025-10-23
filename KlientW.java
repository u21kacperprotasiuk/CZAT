import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class KlientW extends JFrame {

    private JTextPane area;
    private JTextField input;
    private JButton sendBtn;
    private JList<String> users;
    private DefaultListModel<String> usersModel;
    private PrintWriter out;
    private BufferedReader in;
    private Socket socket;
    private String login;

    private StyledDocument doc;
    private Style stylePublic, stylePrivateIn, stylePrivateOut, styleSystem;

    public KlientW(String host, int port) {
        setTitle("Czat sieciowy");
        setSize(650, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // === PANEL CZATU ===
        area = new JTextPane();
        area.setEditable(false);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        area.setBackground(Color.WHITE);
        doc = area.getStyledDocument();

        // style tekstu
        stylePublic = doc.addStyle("public", null);
        StyleConstants.setForeground(stylePublic, Color.BLACK);
        StyleConstants.setFontSize(stylePublic, 14);

        stylePrivateIn = doc.addStyle("privateIn", null);
        StyleConstants.setForeground(stylePrivateIn, new Color(34, 139, 34)); // zielony
        StyleConstants.setFontSize(stylePrivateIn, 14);

        stylePrivateOut = doc.addStyle("privateOut", null);
        StyleConstants.setForeground(stylePrivateOut, Color.RED);
        StyleConstants.setFontSize(stylePrivateOut, 14);

        styleSystem = doc.addStyle("system", null);
        StyleConstants.setForeground(styleSystem, Color.GRAY);
        StyleConstants.setItalic(styleSystem, true);

        JScrollPane scrollChat = new JScrollPane(area);
        scrollChat.setBorder(new TitledBorder("Czat"));

        // === PANEL UŻYTKOWNIKÓW ===
        usersModel = new DefaultListModel<>();
        users = new JList<>(usersModel);
        users.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        users.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        users.setBorder(new EmptyBorder(5,5,5,5));
        users.setBackground(new Color(245, 245, 245));

        JScrollPane scrollUsers = new JScrollPane(users);
        scrollUsers.setPreferredSize(new Dimension(150, 0));
        scrollUsers.setBorder(new TitledBorder("Użytkownicy"));

        // === DÓŁ: POLE TEKSTOWE + PRZYCISK ===
        JPanel bottom = new JPanel(new BorderLayout(5, 5));
        input = new JTextField();
        input.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        input.setBorder(new CompoundBorder(new LineBorder(new Color(200, 200, 200)), new EmptyBorder(5, 5, 5, 5)));
        sendBtn = new JButton("Wyślij");
        sendBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        sendBtn.setBackground(new Color(52, 152, 219));
        sendBtn.setForeground(Color.WHITE);
        sendBtn.setFocusPainted(false);
        sendBtn.setBorder(new EmptyBorder(5, 15, 5, 15));

        bottom.add(input, BorderLayout.CENTER);
        bottom.add(sendBtn, BorderLayout.EAST);
        bottom.setBorder(new EmptyBorder(5, 5, 5, 5));

        add(scrollChat, BorderLayout.CENTER);
        add(scrollUsers, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        // === Zdarzenia ===
        sendBtn.addActionListener(e -> wyslij());
        input.addActionListener(e -> wyslij());

        // === Połączenie z serwerem ===
        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // login
            if (in.readLine().equals("PODAJ_LOGIN:")) {
                login = JOptionPane.showInputDialog(this, "Podaj login:");
                out.println(login);
            }

            // nasłuch
            new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        if (line.startsWith("MSG:")) {
                            String[] p = line.split(":", 3);
                            if (p.length == 3) {
                                boolean prywatna = false;
                                pokazWiadomosc(p[1], p[2], prywatna);
                            }
                        } else if (line.startsWith("PRIV:")) {
                            String[] p = line.split(":", 4);
                            if (p.length == 4) {
                                boolean prywatna = true;
                                pokazWiadomosc(p[1], p[3], prywatna);

                                // automatycznie ustawia odbiorcę prywatnego
                                SwingUtilities.invokeLater(() -> {
                                    for (int i = 0; i < usersModel.size(); i++) {
                                        if (usersModel.getElementAt(i).equals(p[1])) {
                                            users.setSelectedIndex(i);
                                            break;
                                        }
                                    }
                                });

                                // powiadomienie dźwiękowe
                                Toolkit.getDefaultToolkit().beep();
                            }
                        } else if (line.startsWith("USERS:")) {
                            String[] arr = line.substring(6).split(",");
                            SwingUtilities.invokeLater(() -> {
                                usersModel.clear();
                                usersModel.addElement("ALL");
                                for (String u : arr)
                                    if (!u.isEmpty()) usersModel.addElement(u);
                            });
                        }
                    }
                } catch (IOException e) {
                    pokazSystem("Rozłączono z serwerem.");
                }
            }).start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Nie można połączyć z serwerem!");
            System.exit(0);
        }
    }

    private void wyslij() {
        String txt = input.getText().trim();
        if (txt.isEmpty()) return;

        String target = users.getSelectedValue();
        if (target == null || target.equals("ALL")) {
            out.println("MSG:" + txt);
            // publiczna – dodana tylko przez serwer, nie lokalnie
        } else {
            out.println("PRIV:" + target + ":" + txt);
            // prywatna – dodana tylko przez serwer, nie lokalnie
        }

        input.setText("");
    }

    private void pokazWiadomosc(String kto, String txt, boolean prywatna) {
        try {
            Style styl = stylePublic;
            String prefix = "[Ogólna] ";
            if (prywatna) {
                styl = kto.equals(login) ? stylePrivateOut : stylePrivateIn;
                prefix = "[Prywatna] ";
            }
            doc.insertString(doc.getLength(), prefix + kto + ": " + txt + "\n", styl);
            area.setCaretPosition(doc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void pokazSystem(String msg) {
        try {
            doc.insertString(doc.getLength(), "[System] " + msg + "\n", styleSystem);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new KlientW("localhost", 2217).setVisible(true));
    }
}
