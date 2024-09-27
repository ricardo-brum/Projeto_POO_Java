import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/padaria_db";
    private static final String USER = "root"; // Alterar conforme necessário
    private static final String PASSWORD = ""; // Alterar conforme necessário

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}




class Produto {
    private int id;
    private String nome;
    private double preco;
    private int quantidade;

    public Produto(int id, String nome, double preco, int quantidade) {
        this.id = id;
        this.nome = nome;
        this.preco = preco;
        this.quantidade = quantidade;
    }

    // Getters e setters
    public int getId() { return id; }
    public String getNome() { return nome; }
    public double getPreco() { return preco; }
    public int getQuantidade() { return quantidade; }

    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }

    @Override
    public String toString() {
        return "Produto{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", preco=" + preco +
                ", quantidade=" + quantidade +
                '}';
    }
}





class ProdutoDAO {

    public List<Produto> listarProdutos() throws SQLException {
        List<Produto> produtos = new ArrayList<>();
        String sql = "SELECT * FROM produtos";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Produto produto = new Produto(
                        rs.getInt("id"),
                        rs.getString("nome"),
                        rs.getDouble("preco"),
                        rs.getInt("quantidade")
                );
                produtos.add(produto);
            }
        }
        return produtos;
    }

    public Produto buscarProdutoPorId(int id) throws SQLException {
        String sql = "SELECT * FROM produtos WHERE id = ?";
        Produto produto = null;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    produto = new Produto(
                            rs.getInt("id"),
                            rs.getString("nome"),
                            rs.getDouble("preco"),
                            rs.getInt("quantidade")
                    );
                }
            }
        }
        return produto;
    }

    public void atualizarQuantidade(int id, int novaQuantidade) throws SQLException {
        String sql = "UPDATE produtos SET quantidade = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, novaQuantidade);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }
}




class Venda implements Runnable {
    private ProdutoDAO produtoDAO;
    private int produtoId;
    private int quantidadeVendida;

    public Venda(ProdutoDAO produtoDAO, int produtoId, int quantidadeVendida) {
        this.produtoDAO = produtoDAO;
        this.produtoId = produtoId;
        this.quantidadeVendida = quantidadeVendida;
    }

    @Override
    public void run() {
        try {
            Produto produto = produtoDAO.buscarProdutoPorId(produtoId);

            if (produto != null) {
                synchronized (produto) {  // Sincronização para evitar conflitos de concorrência
                    if (produto.getQuantidade() >= quantidadeVendida) {
                        int novaQuantidade = produto.getQuantidade() - quantidadeVendida;
                        produtoDAO.atualizarQuantidade(produtoId, novaQuantidade);
                        System.out.println("Venda realizada: " + quantidadeVendida + " unidades de " + produto.getNome());
                    } else {
                        System.out.println("Estoque insuficiente para " + produto.getNome());
                    }
                }
            } else {
                System.out.println("Produto não encontrado.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}




public class PadariaMain {
    public static void main(String[] args) {
        ProdutoDAO produtoDAO = new ProdutoDAO();
        Scanner scanner = new Scanner(System.in);

        System.out.println("Produtos disponíveis:");
        try {
            for (Produto p : produtoDAO.listarProdutos()) {
                System.out.println(p);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        System.out.print("Digite o ID do produto para vender: ");
        int produtoId = scanner.nextInt();

        System.out.print("Digite a quantidade a ser vendida: ");
        int quantidadeVendida = scanner.nextInt();

        Venda venda1 = new Venda(produtoDAO, produtoId, quantidadeVendida);

        // Criação de threads para simular múltiplas vendas concorrentes
        Thread thread1 = new Thread(venda1);
        thread1.start();
        scanner.close();
    }
}
