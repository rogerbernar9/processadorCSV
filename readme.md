---
# Processador CSV
Aplicação desktop em Java (Swing) apresentando grande performance para leitura, visualização e inserção de arquivos CSV em um banco de dados SQLite com suporte a carregamento assíncrono, multithread e paginação.
## Funcionalidades
- Capacidades de leitura de grandes volumes de dados de planilhas CSV, suportando leitura em threads separadas para evitar travamento e usa banco de dados interno para performance na visualização dos dados
- Interface Swing amigável para seleção e leitura de arquivos `.csv`.
- Suporte a arquivos com ou sem cabeçalho.
- Inserção rápida no banco de dados utilizando múltiplas threads e `batch insert`.
- Visualização dos dados inseridos via `JTable` com:
    - Paginação (100 linhas por página).
    - Filtro textual por conteúdo.
- Banco de dados SQLite gerado automaticamente.
- Nome das colunas sanitizado para evitar conflitos com o SQLite.
- Capacidade de inclusão, alteração, exclusão dos dados inseridos em seu banco de dados interno
- Exportação dos dados de seu banco de dados interno SQLite para CSV
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

-  Uso de SQLite para a performance da aplicação
-  Fila concorrente (BlockingQueue)
-  Separação entre produtor (leitor) e consumidores (inseridores). Evita gargalos.
-  Permite alterar, excluir, inserir dados gravados e exportar novamente para csv 
-  Permite exportar como insert SQL, renomeando campos e sanitizando dados
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
2. Instale o JRE ou JDK 17+
3. Compile e execute:
Com Java:
javac -d out -cp ".;sqlite-jdbc-<versão>.jar" src/org/example/swing/*.java
java -cp ".;out;sqlite-jdbc-<versão>.jar" org.example.swing.CsvReader
Ou
java -cp "ProcessadorCSV-1.0-SNAPSHOT.jar:sqlite-jdbc-3.45.1.0.jar" org.processadorcsv.Main
Ou utilizando uma IDE como IntelliJ ou Eclipse, basta importar o projeto e executar a classe CsvReader.
4. Passos na interface:
Clique em "Carregar CSV" e selecione o arquivo desejado.
Aguarde o processamento (ocorre em segundo plano), será criado em '/home/seuusuario/.processadorcsv' o arquivo BD local do sqlite 'data.db'.
Clique em "Visualizar dados" para abrir a janela de navegação dos dados.
---
Desenvolvido por Roger Bernar
roger.bernar9@gmail.com
---









