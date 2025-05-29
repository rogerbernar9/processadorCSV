package org.processadorcsv.ferramentaBD.view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.processadorcsv.ferramentaBD.model.service.DBService;
import org.processadorcsv.ferramentaBD.model.service.IDBService;
import org.processadorcsv.viewmodel.ExportarCSVDialog;
import org.processadorcsv.viewmodel.util.CSVImporterThread;

public class MainWindow extends JFrame {
    private IDBService dbService;

    private JComboBox<String> tabelaCombo = new JComboBox<>();
    private JTable tabelaDados = new JTable() {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private JButton btnInserir = new JButton("Inserir");
    private JButton btnEditar = new JButton("Editar");
    private JButton btnExcluir = new JButton("Excluir");
    private JButton btnProximo = new JButton("Próximo");
    private JButton btnAnterior = new JButton("Anterior");
    JButton btnExecutarSQL = new JButton("Executar SQL");

    private int paginaAtual = 0;
    private int limitePorPagina = 50;

    private JMenuItem menuItemExportar;
    private JMenuItem menuItemImportar;


    public MainWindow(IDBService service) {

        this.dbService = service;

        setTitle("Visualizador de Tabelas");
        setSize(900, 600);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Tabela:"));
        topPanel.add(tabelaCombo);
        topPanel.add(btnInserir);
        topPanel.add(btnEditar);
        topPanel.add(btnExcluir);
        topPanel.add(btnExecutarSQL);

        JMenuBar menuBar = new JMenuBar();
        JMenu menuExportar = new JMenu("Exportação");
        menuItemExportar = new JMenuItem("Exportar para CSV");
        menuItemImportar = new JMenuItem("Importar CSV");
        JMenu menuImportar = new JMenu("Importação");

        setJMenuBar(menuBar);
        menuBar.add(menuExportar);
        menuBar.add(menuImportar);
        menuExportar.add(menuItemExportar);
        menuImportar.add(menuItemImportar);

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(btnAnterior);
        bottomPanel.add(btnProximo);
        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(tabelaDados), BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        tabelaCombo.addActionListener(e -> {
            paginaAtual = 0;
            carregarDadosTabela();
        });
        btnInserir.addActionListener(e -> abrirFormulario("inserir"));
        btnEditar.addActionListener(e -> abrirFormulario("editar"));
        btnExcluir.addActionListener(e -> excluirRegistro());
        btnProximo.addActionListener(e -> {
            paginaAtual++;
            carregarDadosTabela();
        });
        btnAnterior.addActionListener(e -> {
            if (paginaAtual > 0) {
                paginaAtual--;
                carregarDadosTabela();
            }
        });

        btnExecutarSQL.addActionListener(
                e -> new QueryExecutor(this, dbService)
        );

        menuItemExportar.addActionListener( e -> {
            exportarParaCSV();
        });

        menuItemImportar.addActionListener( e -> {
            importarCSV();
        });

        carregarTabelas();
        setVisible(true);
    }

    private void carregarTabelas() {
        try {
            List<String> tabelas = dbService.listarTabelas(null);
            tabelaCombo.removeAllItems();
            for (String tabela : tabelas) {
                tabelaCombo.addItem(tabela);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao listar tabelas: " + e.getMessage());
        }
    }

    public void carregarDadosTabela() {
        String tabela = (String) tabelaCombo.getSelectedItem();
        if (tabela == null) return;
        try {
            String[] colunas = dbService.listarColunas(tabela);
            List<String[]> dados = dbService.listarDados(tabela, limitePorPagina, paginaAtual * limitePorPagina);
            DefaultTableModel model = new DefaultTableModel(colunas, 0);
            for (String[] linha : dados) {
                model.addRow(linha);
            }
            tabelaDados.setModel(model);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar dados: " + e.getMessage());
        }
    }

    private void abrirFormulario(String acao) {
        String tabela = (String) tabelaCombo.getSelectedItem();
        if (tabela == null) return;
        String[] colunas;
        String[] dados = null;
        try {
            colunas = dbService.listarColunas(tabela);
            if ("editar".equals(acao)) {
                int row = tabelaDados.getSelectedRow();
                if (row == -1) {
                    JOptionPane.showMessageDialog(this, "Selecione uma linha para editar.");
                    return;
                }
                dados = new String[colunas.length];
                for (int i = 0; i < colunas.length; i++) {
                    Object value = tabelaDados.getValueAt(row, i);
                    dados[i] = value != null ? value.toString() : "";
                }
            }
            new FormularioRegistro(this, dbService, tabela, colunas, dados, acao);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro: " + e.getMessage());
        }
    }

    private void excluirRegistro() {
        String tabela = (String) tabelaCombo.getSelectedItem();
        if (tabela == null) return;
        try {
            String[] colunas = dbService.listarColunas(tabela);
            int row = tabelaDados.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Selecione uma linha para excluir.");
                return;
            }
            List<String> condicoes = new ArrayList<>();

            for (int i = 0; i < colunas.length; i++) {
                Object cellValue = tabelaDados.getValueAt(row, i);
                if (cellValue != null) {
                    String valor = cellValue.toString().trim();
                    if (!valor.isEmpty()) {
                        condicoes.add(colunas[i] + " = '" + valor + "'");
                    }
                }
            }

            StringBuilder condicao = new StringBuilder(String.join(" AND ", condicoes));

            int confirm = JOptionPane.showConfirmDialog(this, "Deseja realmente excluir este registro?", "Confirmar Exclusão", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                System.out.println(tabela);
                System.out.println(condicao);
                dbService.excluirRegistro(tabela, condicao.toString());
                carregarDadosTabela();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            JOptionPane.showMessageDialog(this, "Erro ao excluir: " + e.getMessage());
        }
    }

    private void exportarParaCSV() {
        DefaultTableModel model = (DefaultTableModel) tabelaDados.getModel();
        if (model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Nenhum dado para exportar.");
            return;
        }

        List<String> colunas = new ArrayList<>();
        for (int i = 0; i < model.getColumnCount(); i++) {
            colunas.add(model.getColumnName(i));
        }

        ExportarCSVDialog dialog = new ExportarCSVDialog(this, colunas);
        dialog.setVisible(true);
        if (!dialog.isConfirmado()) return;
        List<String> colunasSelecionadas = dialog.getColunasSelecionadas();
        Set<String> colunasParaInteiro = dialog.getColunasParaInteiro();
        boolean incluirCabecalho = dialog.isIncluirCabecalho();
        File arquivo = dialog.getArquivoSelecionado();
        try (FileWriter writer = new FileWriter(arquivo)) {
            if (incluirCabecalho) {
                writer.write(String.join(",", colunasSelecionadas));
                writer.write("\n");
            }
            for (int row = 0; row < model.getRowCount(); row++) {
                List<String> linha = new ArrayList<>();
                for (String coluna : colunasSelecionadas) {
                    int colIndex = colunas.indexOf(coluna);
                    Object valor = model.getValueAt(row, colIndex);
                    if (valor == null) {
                        linha.add("");
                    } else {
                        if (colunasParaInteiro.contains(coluna)) {
                            try {
                                linha.add(String.valueOf(Integer.parseInt(valor.toString())));
                            } catch (NumberFormatException ex) {
                                linha.add("0");
                            }
                        } else {
                            String texto = valor.toString().replace("\"", "\"\"");
                            linha.add("\"" + texto + "\"");
                        }
                    }
                }
                writer.write(String.join(",", linha));
                writer.write("\n");
            }
            JOptionPane.showMessageDialog(this, "Exportação concluída com sucesso!");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao exportar: " + ex.getMessage());
        }
    }

    private void importarCSV() {
        String tabela = (String) tabelaCombo.getSelectedItem();
        if (tabela == null) {
            JOptionPane.showMessageDialog(this, "Nenhuma tabela selecionada.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        int resultado = chooser.showOpenDialog(this);
        if (resultado != JFileChooser.APPROVE_OPTION) return;
        File arquivo = chooser.getSelectedFile();
        CSVImporterThread importer = new CSVImporterThread(
                arquivo,
                tabela,
                dbService,
                this::carregarDadosTabela
        );
        importer.iniciar();
    }

}

