package org.processadorcsv.viewmodel;

import org.processadorcsv.viewmodel.util.Preferencias;

import javax.swing.*;
import java.awt.*;

public class PreferenciasView extends JDialog {
    public PreferenciasView(JFrame parent) {
        super(parent, "Preferências", true);
        setSize(300, 150);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        JCheckBox checkOtimizar = new JCheckBox("Criar índices após carregamento");
        checkOtimizar.setSelected(Preferencias.isOtimizarCarregamento());

        JButton salvarBtn = new JButton("Salvar");
        salvarBtn.addActionListener(e -> {
            Preferencias.setOtimizarCarregamento(checkOtimizar.isSelected());
            dispose();
        });

        JPanel panel = new JPanel();
        panel.add(checkOtimizar);

        add(panel, BorderLayout.CENTER);
        add(salvarBtn, BorderLayout.SOUTH);
    }
}
