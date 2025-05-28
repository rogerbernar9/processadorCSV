package org.processadorcsv.ferramentaBD.model.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface IDBService {
    List<String> listarTabelas(String schema) throws SQLException;
    String[] listarColunas(String tabela) throws SQLException;
    List<String[]> listarDados(String tabela, int limite, int offset) throws SQLException;
    void excluirRegistro(String tabela, String condicao) throws SQLException;
    Connection getConnection();
    // Adicione estes:
    Map<String, Boolean> colunasAutoIncrementadas(String tabela) throws SQLException;
    Map<String, Boolean> colunasObrigatorias(String tabela) throws SQLException;
    void inserirRegistro(String tabela, Map<String, String> dados) throws SQLException;
    void editarRegistro(String tabela, Map<String, String> dados, String condicao) throws SQLException;
    void inserirRegistro(String tabela, String[] colunas, String[] valores) throws SQLException;

}