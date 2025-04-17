package org.processadorcsv;


import org.processadorcsv.viewmodel.CsvReader;
import org.processadorcsv.viewmodel.LoginLDAPView;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginLDAPView().setVisible(true));
    }

}