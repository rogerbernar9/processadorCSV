---
# Processador CSV
Aplicação desktop em Java (Swing) para leitura, visualização e inserção de arquivos CSV em um banco de dados SQLite com suporte a carregamento assíncrono, multithread e paginação.
## Funcionalidades
- Interface Swing amigável para seleção e leitura de arquivos `.csv`.
- Suporte a arquivos com ou sem cabeçalho.
- Inserção rápida no banco de dados utilizando múltiplas threads e `batch insert`.
- Visualização dos dados inseridos via `JTable` com:
    - Paginação (100 linhas por página).
    - Filtro textual por conteúdo.
- Banco de dados SQLite gerado automaticamente.
- Nome das colunas sanitizado para evitar conflitos com o SQLite.
## Tecnologias utilizadas
- Java 17+
- Swing (UI)
- JDBC + SQLite
- ExecutorService (multithread)
- JTable com paginação
- Maven (recomendado para empacotamento)

## Implementações
-  Processamento em paralelo
-  Usa ExecutorService com múltiplas threads: leitura e escrita simultânea.

-  Leitura em blocos (chunking)
-  Evita processar linha por linha — agrupa em blocos (batchSize = 500), o que reduz overhead de I/O e melhora performance de banco.

-  Inserções com PreparedStatement em lote
-  Usa addBatch() e executeBatch(), o que é MUITO mais rápido do que executar linha por linha.

-  Uso de SQLite
-  SQLite é eficiente para arquivos locais, e você está desativando o autocommit (conn.setAutoCommit(false)), o que é essencial para performance.

-  Fila concorrente (BlockingQueue)
-  Boa separação entre produtor (leitor) e consumidores (inseridores). Evita gargalos.

## Tecnologias utilizadas
- Java 17+
- Swing (UI)
- JDBC + SQLite
- ExecutorService (multithread)
- JTable com paginação
- Maven (recomendado para empacotamento)
## Como usar
1. **Clone o repositório:**
```bash
git clone https://github.com/rogerbernar9/processadorCSV.git
cd processadorCSV
2. Compile e execute:
Com Java:
javac -d out -cp ".;sqlite-jdbc-<versão>.jar" src/org/example/swing/*.java
java -cp ".;out;sqlite-jdbc-<versão>.jar" org.example.swing.CsvReader
Ou
java -cp "ProcessadorCSV-1.0-SNAPSHOT.jar:sqlite-jdbc-3.45.1.0.jar" org.processadorcsv.Main
Ou utilizando uma IDE como IntelliJ ou Eclipse, basta importar o projeto e executar a classe CsvReader.
3. Passos na interface:
Clique em "Carregar CSV" e selecione o arquivo desejado.
Aguarde o processamento (ocorre em segundo plano).
Clique em "Visualizar dados" para abrir a janela de navegação dos dados.
---
Desenvolvido por Roger Bernar
---









