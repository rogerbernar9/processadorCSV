package org.example;


import org.example.swing.LoginLDAPView;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginLDAPView().setVisible(true));
    }

}