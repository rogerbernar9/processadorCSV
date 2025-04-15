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
## Como usar
1. **Clone o repositório:**
```bash
git clone https://github.com/rogerbernar9/processadorCSV.git
cd processadorCSV
2. Compile e execute:
Com Java:
javac -d out -cp ".;sqlite-jdbc-<versão>.jar" src/org/example/swing/*.java
java -cp ".;out;sqlite-jdbc-<versão>.jar" org.example.swing.CsvReader
Ou utilizando uma IDE como IntelliJ ou Eclipse, basta importar o projeto e executar a classe CsvReader.
3. Passos na interface:
Clique em "Carregar CSV" e selecione o arquivo desejado.
Aguarde o processamento (ocorre em segundo plano).
Clique em "Visualizar dados" para abrir a janela de navegação dos dados.
---
Desenvolvido por Roger Bernar
---









