package org.processadorcsv.viewmodel;

import org.processadorcsv.security.AutenticadorLDAP;

import javax.swing.*;
import java.awt.*;

public class LoginLDAPView extends JFrame {

    public LoginLDAPView() {
        setTitle("Login");
        setSize(300, 150);
        setLayout(new GridLayout(3, 2));
        JTextField campoUsuario = new JTextField();
        JPasswordField campoSenha = new JPasswordField();
        JButton btnEntrar = new JButton("Entrar");
        add(new JLabel("Usuário:"));
        add(campoUsuario);
        add(new JLabel("Senha:"));
        add(campoSenha);
        add(new JLabel());
        add(btnEntrar);
        btnEntrar.addActionListener(e -> {
            String usuario = campoUsuario.getText();
            String senha = new String(campoSenha.getPassword());
            if (AutenticadorLDAP.autenticar(usuario, senha)) {
                dispose();
                javax.swing.SwingUtilities.invokeLater(() -> {
                    new CsvReader().setVisible(true);
                });
            } else {
                JOptionPane.showMessageDialog(this, "Usuário ou senha inválidos!");
            }
        });
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
    }



}
