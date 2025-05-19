package org.processadorcsv.viewmodel;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.*;
import java.util.List;

public class ExportarCSVDialog extends JDialog {
    private final List<String> colunas;
    private final Map<String, JCheckBox> checkBoxMap = new HashMap<>();
    private final Map<String, JCheckBox> intCheckBoxMap = new HashMap<>();
    private boolean confirmado = false;
    private File arquivoSelecionado;
    private final JCheckBox incluirCabecalhoCheck = new JCheckBox("Incluir cabeçalho", true);


    public ExportarCSVDialog(Frame owner, List<String> colunas) {
        super(owner, "Exportar para CSV", true);
        this.colunas = colunas;
        setLayout(new BorderLayout());
        JPanel checkPanel = new JPanel(new GridLayout(0, 3));
        for (String coluna : colunas) {
            JCheckBox incluir = new JCheckBox("Incluir", true);
            JCheckBox comoInt = new JCheckBox("Converter p/ Int");
            checkPanel.add(new JLabel(coluna));
            checkPanel.add(incluir);
            checkPanel.add(comoInt);
            checkBoxMap.put(coluna, incluir);
            intCheckBoxMap.put(coluna, comoInt);
        }

        JPanel rodapePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rodapePanel.add(incluirCabecalhoCheck);

        JButton exportarBtn = new JButton("Exportar");
        exportarBtn.addActionListener(this::aoConfirmar);
        JButton cancelarBtn = new JButton("Cancelar");
        cancelarBtn.addActionListener(e -> dispose());
        JPanel botoes = new JPanel();
        botoes.add(exportarBtn);
        botoes.add(cancelarBtn);
        add(new JScrollPane(checkPanel), BorderLayout.CENTER);
        add(rodapePanel, BorderLayout.NORTH);
        add(botoes, BorderLayout.SOUTH);
        setSize(500, 400);
        setLocationRelativeTo(owner);
    }

    private void aoConfirmar(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            arquivoSelecionado = fileChooser.getSelectedFile();
            confirmado = true;
            dispose();
        }
    }
    public boolean isConfirmado() {
        return confirmado;
    }
    public File getArquivoSelecionado() {
        return arquivoSelecionado;
    }
    public List<String> getColunasSelecionadas() {
        List<String> selecionadas = new ArrayList<>();
        for (String coluna : colunas) {
            if (checkBoxMap.get(coluna).isSelected()) {
                selecionadas.add(coluna);
            }
        }
        return selecionadas;
    }
    public Set<String> getColunasParaInteiro() {
        Set<String> paraInt = new HashSet<>();
        for (String coluna : colunas) {
            if (checkBoxMap.get(coluna).isSelected() && intCheckBoxMap.get(coluna).isSelected()) {
                paraInt.add(coluna);
            }
        }
        return paraInt;
    }

    public boolean isIncluirCabecalho() {
        return incluirCabecalhoCheck.isSelected();
    }
}
