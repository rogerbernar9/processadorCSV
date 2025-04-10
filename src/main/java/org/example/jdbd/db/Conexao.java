package org.example.jdbd.db;

import org.mariadb.jdbc.client.result.Result;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class Conexao {

    private static final String URL = "jdbc:mariadb://localhost:3308/app";
    private static final String USUARIOS = "root";
    private static final String SENHA = "123";

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(URL, USUARIOS, SENHA)) {
            System.out.println("Conectado AO MARIA DB ");
            listarUsuarios(conn);

        } catch (SQLException sqlException) {
            System.err.println(sqlException.getMessage());
        }
    }

    public static void listarUsuarios(Connection conexao) {
        String sql = "SELECT * FROM user";
        try {
            PreparedStatement statement = conexao.prepareStatement(sql);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                System.out.println("id" + rs.getInt("id"));
                System.out.println("nome "+rs.getString("nome"));
            }
        } catch (Exception exception) {
            System.err.println(exception.getMessage());
        }
    }


}
