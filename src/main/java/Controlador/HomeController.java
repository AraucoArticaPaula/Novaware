package Controlador;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class HomeController implements Initializable {

    // ── Ruta base de FXML ──
    private static final String FXML_BASE = "Views/FXML/";

    // ── fx:id del FXML ──
    @FXML private MenuButton menuUsuario;
    @FXML private Label lblGerenteTienda;
    @FXML private Label lblSubgerente;
    @FXML private Label lblTotalVendedores;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cargarDatos();
    }

    // ─────────────────────────────
    // CARGAR DATOS DASHBOARD
    // ─────────────────────────────
    private void cargarDatos() {

        String sqlVendedores =
                "SELECT COUNT(*) FROM colaboradores WHERE activo = TRUE";

        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlVendedores);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                lblTotalVendedores.setText(
                        String.valueOf(rs.getInt(1))
                );
            }

        } catch (SQLException e) {

            System.err.println(
                    "[HomeController] Error vendedores: "
                            + e.getMessage()
            );
        }


        String sqlGerentes = """
            SELECT c.nombre || ' ' || c.apellidos AS nombre_completo,
                   r.nombre AS rol
            FROM colaboradores c
            JOIN usuarios u
                ON u.id_usuario = c.id_usuario
            JOIN roles r
                ON r.id_rol = u.id_rol
            WHERE c.activo = TRUE
            AND r.nombre IN ('Administrador','Supervisor')
            ORDER BY r.nombre
            LIMIT 2
            """;

        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlGerentes);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                lblGerenteTienda.setText(
                        rs.getString("nombre_completo")
                );
            }

            if (rs.next()) {
                lblSubgerente.setText(
                        rs.getString("nombre_completo")
                );
            }

        } catch (SQLException e) {

            System.err.println(
                    "[HomeController] Error gerentes: "
                            + e.getMessage()
            );
        }
    }


    // ─────────────────────────────
    // NAVEGACIÓN SIDEBAR
    // ─────────────────────────────

    @FXML
    private void irIndicadores(MouseEvent e) {
        irA(e,"Indicadores.fxml");
    }

    @FXML
    private void irComisiones(MouseEvent e) {
        irA(e,"Comisiones.fxml");
    }

    @FXML
    private void irVendedores(MouseEvent e) {
        irA(e,"agregarcolab.fxml");
    }

    @FXML
    private void irInventario(MouseEvent e) {
        irA(e,"Inventario.fxml");
    }

    @FXML
    private void irDetalleVentas(MouseEvent e) {
        irA(e,"DetalleVentas.fxml");
    }


    @FXML
    private void cerrarSesion() {

        Stage stage =
                (Stage) menuUsuario
                        .getScene()
                        .getWindow();

        irAConStage(
                stage,
                "Login.fxml",
                "SIGESTI - Login"
        );
    }



    // ─────────────────────────────
    // HELPERS NAVEGACIÓN
    // ─────────────────────────────

    private void irA(
            MouseEvent e,
            String fxml
    ){

        Stage stage =
                (Stage)((javafx.scene.Node)
                        e.getSource())
                        .getScene()
                        .getWindow();

        irAConStage(
                stage,
                fxml,
                "SIGESTI"
        );
    }



    private void irAConStage(
            Stage stage,
            String fxml,
            String titulo
    ){

        try {

            URL url =
                    getClass()
                            .getClassLoader()
                            .getResource(
                                    FXML_BASE + fxml
                            );

            if(url == null){

                System.err.println(
                        "No encontrado: "
                                + fxml
                );

                return;
            }

            FXMLLoader loader =
                    new FXMLLoader(url);

            stage.setScene(
                    new Scene(
                            loader.load()
                    )
            );

            stage.setTitle(
                    titulo
            );

            stage.show();

        }
        catch(IOException ex){

            System.err.println(
                    "[HomeController] Error cargando "
                            + fxml
                            + ": "
                            + ex.getMessage()
            );

            ex.printStackTrace();
        }
    }
}