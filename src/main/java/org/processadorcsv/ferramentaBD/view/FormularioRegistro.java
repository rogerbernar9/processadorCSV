package org.processadorcsv.ferramentaBD.view;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.processadorcsv.ferramentaBD.model.service.IDBService;

public class FormularioRegistro extends JDialog {
    private final IDBService dbService;
    private final String tabela;
    private final String[] colunas;
    private final String[] dados;
    private final String acao;
    private final Map<String, JTextArea> camposTexto = new HashMap<>();
    private final Map<String, Boolean> autoIncrementados;
    private final Map<String, Boolean> obrigatorios;

    public FormularioRegistro(JFrame parent, IDBService dbService, String tabela, String[] colunas, String[] dados, String acao) throws Exception {
        super(parent, true);
        this.dbService = dbService;
        this.tabela = tabela;
        this.colunas = colunas;
        this.dados = dados;
        this.acao = acao;
        this.autoIncrementados = dbService.colunasAutoIncrementadas(tabela);
        this.obrigatorios = dbService.colunasObrigatorias(tabela);
        setTitle((acao.equals("inserir") ? "Inserir" : "Editar") + " Registro");
        setLayout(new BorderLayout());
        JPanel painelCampos = new JPanel(new GridLayout(0, 2, 13, 13));
        for (int i = 0; i < colunas.length; i++) {
            String coluna = colunas[i];
            boolean autoInc = autoIncrementados.getOrDefault(coluna, false);
            JLabel label = new JLabel(coluna + (obrigatorios.getOrDefault(coluna, false) ? " *" : ""));
            JTextArea campo = new JTextArea(2,20);
            campo.setLineWrap(true);
            campo.setWrapStyleWord(true);
            campo.setEnabled(!autoInc || acao.equals("editar"));
            if (dados != null) {
                campo.setText(dados[i]);
            }
            if (!autoInc || acao.equals("editar")) {
                camposTexto.put(coluna, campo);
            }
            painelCampos.add(label);
            painelCampos.add(campo);
        }
        JButton btnSalvar = new JButton("Salvar");
        btnSalvar.addActionListener(e -> salvar());
        add(painelCampos, BorderLayout.CENTER);
        add(btnSalvar, BorderLayout.SOUTH);
        setSize(500, colunas.length * 70 + 100);
        setLocationRelativeTo(parent);
        setVisible(true);
    }

    private void salvar() {
        try {
            Map<String, String> valores = new LinkedHashMap<>();
            for (String coluna : colunas) {
                if (!camposTexto.containsKey(coluna)) continue;
                String valor = camposTexto.get(coluna).getText().trim();
                boolean obrigatorio = obrigatorios.getOrDefault(coluna, false);
                if (obrigatorio && valor.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "O campo '" + coluna + "' é obrigatório.");
                    return;
                }
                if (!valor.isEmpty()) {
                    valores.put(coluna, valor);
                }
            }
            if (acao.equals("inserir")) {
                dbService.inserirRegistro(tabela, valores);
            } else if (acao.equals("editar")) {
                String id = valores.get("id");
                if (id == null || id.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "ID não encontrado ou inválido.");
                    return;
                }
                String condicao = "id = '" + id + "'";
                dbService.editarRegistro(tabela, valores, condicao);
            }
            ((MainWindow) getParent()).carregarDadosTabela();
            dispose();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar: " + e.getMessage());
        }
    }
}
