package org.processadorcsv.ferramentaBD.view;

import org.processadorcsv.ferramentaBD.model.service.IDBService;
import org.processadorcsv.ferramentaBD.model.service.MySQLService;
import org.processadorcsv.ferramentaBD.model.service.PostgresService;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.prefs.Preferences;

public class LoginView extends JFrame {
    private JTextField hostField = new JTextField("localhost");
    private JTextField portField = new JTextField("3306");
    private JTextField dbField = new JTextField("meubanco");
    private JTextField userField = new JTextField("root");
    private JPasswordField passField = new JPasswordField("senha");
    private JComboBox<String> tipoBanco = new JComboBox<>(new String[]{"MySQL","Pgsql"});
    private JButton conectarButton = new JButton("Conectar");
    private Preferences prefs = Preferences.userNodeForPackage(LoginView.class);

    public LoginView() {
        super("Conectar ao Banco de Dados");
        hostField.setText(prefs.get("host", "localhost"));
        portField.setText(prefs.get("port", "3306"));
        dbField.setText(prefs.get("banco", "meubanco"));
        userField.setText(prefs.get("usuario", "root"));
        passField.setText(prefs.get("senha", "senha"));
        tipoBanco.setSelectedItem(prefs.get("tipoBanco", "MySQL"));

        setLayout(new GridLayout(7, 2, 5, 5));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);
        add(new JLabel("Tipo de Banco:"));
        add(tipoBanco);
        add(new JLabel("Host:"));
        add(hostField);
        add(new JLabel("Porta:"));
        add(portField);
        add(new JLabel("Banco:"));
        add(dbField);
        add(new JLabel("Usuário:"));
        add(userField);
        add(new JLabel("Senha:"));
        add(passField);
        add(new JLabel(""));
        add(conectarButton);
        conectarButton.addActionListener(e -> conectar());
        setVisible(true);
    }
    private void conectar() {

        String tipo = (String) tipoBanco.getSelectedItem();
        String host = hostField.getText();
        String porta = portField.getText();
        String banco = dbField.getText();
        String usuario = userField.getText();
        String senha = new String(passField.getPassword());

        prefs.put("host", host);
        prefs.put("port", porta);
        prefs.put("banco", banco);
        prefs.put("usuario", usuario);
        prefs.put("senha", senha);
        prefs.put("tipoBanco", tipo);
        String url;
        Connection conn;
        try {
            IDBService dbService;
            if ("MySQL".equals(tipo)) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                url = "jdbc:mysql://" + host + ":" + porta + "/" + banco + "?useSSL=false";
                conn = DriverManager.getConnection(url, usuario, senha);
                dbService = new MySQLService(conn);
            } else {
                Class.forName("org.postgresql.Driver");
                url = "jdbc:postgresql://" + host + ":" + porta + "/" + banco;
                conn = DriverManager.getConnection(url, usuario, senha);
                dbService = new PostgresService(conn);
            }
            JOptionPane.showMessageDialog(this, "Conectado com sucesso!");
            dispose();
            new MainWindow(dbService);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro na conexão: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
