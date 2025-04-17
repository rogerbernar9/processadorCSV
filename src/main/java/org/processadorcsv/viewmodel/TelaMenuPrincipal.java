package org.processadorcsv.viewmodel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TelaMenuPrincipal extends JFrame {
    private JPanel panel1;
    private JTextField textField1;
    private JTextField textField2;
    private JButton exibirDadosButton;

    public TelaMenuPrincipal() {

        setTitle("bem vindo");
        setSize(500, 500);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setVisible(true);
        setContentPane(panel1);


        exibirDadosButton.addActionListener(e -> {
            VisualizadorDados visualizadorDados = new VisualizadorDados();
            visualizadorDados.setVisible(true);
            this.setVisible(false);
        });


    }
}
