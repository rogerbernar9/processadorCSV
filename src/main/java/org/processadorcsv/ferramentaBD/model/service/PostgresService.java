package org.processadorcsv.ferramentaBD.model.service;

import javax.swing.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PostgresService extends BaseDBService {

    public PostgresService(Connection connection) {
        super(connection);
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public Map<String, Boolean> colunasAutoIncrementadas(String tabela) throws SQLException {
        Map<String, Boolean> autoIncMap = new LinkedHashMap<>();
        String query = "SELECT a.attname AS column_name, " +
                "pg_get_serial_sequence(a.attrelid::regclass::text, a.attname) IS NOT NULL AS is_serial " +
                "FROM pg_attribute a " +
                "JOIN pg_class c ON a.attrelid = c.oid " +
                "JOIN pg_namespace n ON c.relnamespace = n.oid " +
                "WHERE c.relname = ? AND a.attnum > 0 AND NOT a.attisdropped";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, tabela);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    autoIncMap.put(rs.getString("column_name"), rs.getBoolean("is_serial"));
                }
            }
        }
        return autoIncMap;
    }
    @Override
    public Map<String, Boolean> colunasObrigatorias(String tabela) throws SQLException {
        Map<String, Boolean> obrigatorias = new LinkedHashMap<>();
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet rs = metaData.getColumns(null, null, tabela, null)) {
            while (rs.next()) {
                String nome = rs.getString("COLUMN_NAME");
                boolean obrigatorio = rs.getInt("NULLABLE") == DatabaseMetaData.columnNoNulls;
                obrigatorias.put(nome, obrigatorio);
            }
        }
        return obrigatorias;
    }
    @Override
    public void inserirRegistro(String tabela, Map<String, String> dados) throws SQLException {
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(tabela).append(" (");
        StringBuilder values = new StringBuilder("VALUES (");
        List<String> campos = new ArrayList<>(dados.keySet());
        for (int i = 0; i < campos.size(); i++) {
            sql.append(campos.get(i));
            values.append("?");
            if (i < campos.size() - 1) {
                sql.append(", ");
                values.append(", ");
            }
        }
        sql.append(") ").append(values).append(")");
        try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < campos.size(); i++) {
                String valor = dados.get(campos.get(i));
                if (valor == null || valor.trim().isEmpty()) {
                    stmt.setNull(i + 1, Types.NULL);
                } else {

                    if (valor.startsWith("[") && valor.endsWith("]")) {

                        valor = valor.replaceAll("\"\"", "\"");
                    }
                    stmt.setString(i + 1, valor);
                }
            }
            stmt.executeUpdate();
        }
    }

    @Override
    public void editarRegistro(String tabela, Map<String, String> novosDados, String condicaoWhere) throws SQLException {
        StringBuilder sql = new StringBuilder("UPDATE ").append(tabela).append(" SET ");
        List<String> campos = new ArrayList<>(novosDados.keySet());
        campos.remove("id"); // evita editar o ID, se houver
        for (int i = 0; i < campos.size(); i++) {
            sql.append(campos.get(i)).append(" = ?");
            if (i < campos.size() - 1) {
                sql.append(", ");
            }
        }
        sql.append(" WHERE ").append(condicaoWhere);
        try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < campos.size(); i++) {
                stmt.setString(i + 1, novosDados.get(campos.get(i)));
            }
            stmt.executeUpdate();
        } catch (SQLException ex) {
            System.out.println("Erro ao editar registro: " + ex.getMessage());
            JOptionPane.showMessageDialog(null, ex.getMessage());
        }
    }

    @Override
    public void inserirRegistro(String tabela, String[] colunas, String[] valores) throws SQLException {
        if (colunas.length != valores.length) {
            throw new IllegalArgumentException("Número de colunas e valores não coincidem.");
        }
        Map<String, String> dados = new LinkedHashMap<>();
        for (int i = 0; i < colunas.length; i++) {
            dados.put(colunas[i], valores[i]);
        }
        inserirRegistro(tabela, dados); // reutiliza o método já existente
    }

    @Override
    public List<String> listarTabelas(String schema) throws SQLException {
        List<String> tabelas = new ArrayList<>();
        if (schema == null || schema.isEmpty()) {
            schema = "public"; // default no PostgreSQL
        }
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet rs = metaData.getTables(null, schema, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tabelas.add(rs.getString("TABLE_NAME"));
            }
        }
        return tabelas;
    }
    @Override
    public List<String[]> listarDados(String tabela, int limite, int offset) throws SQLException {
        List<String[]> dados = new ArrayList<>();
        String sql = "SELECT * FROM " + tabela + " LIMIT " + limite + " OFFSET " + offset;
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            int colCount = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                String[] linha = new String[colCount];
                for (int i = 0; i < colCount; i++) {
                    linha[i] = rs.getString(i + 1);
                }
                dados.add(linha);
            }
        }
        return dados;
    }
    @Override
    public String[] listarColunas(String tabela) throws SQLException {
        String sql = "SELECT * FROM " + tabela + " LIMIT 1";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            String[] colunas = new String[colCount];
            for (int i = 0; i < colCount; i++) {
                colunas[i] = meta.getColumnName(i + 1);
            }
            return colunas;
        }
    }
    @Override
    public void excluirRegistro(String tabela, String condicao) throws SQLException {
        String sql = "DELETE FROM " + tabela + " WHERE " + condicao;
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }
    // Se necessário, sobrescreva métodos específicos do PostgreSQL aqui
}