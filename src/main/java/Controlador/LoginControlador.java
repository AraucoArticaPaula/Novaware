package Controlador;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginControlador {

    @FXML private TextField     txtUsuario;
    @FXML private PasswordField txtContrasena;
    @FXML private Label         lblError;

    @FXML
    private void iniciarSesion() {
        String usuario   = txtUsuario.getText().trim();
        String contrasena = txtContrasena.getText();

        if (usuario.isEmpty() || contrasena.isEmpty()) {
            mostrarError("Por favor ingresa usuario y contraseña.");
            return;
        }

        // ── La query usa los nombres reales de tu BD: username y password_hash ──
        String sql = """
            SELECT u.id_usuario, u.username, u.password_hash,
                   r.nombre AS rol
            FROM usuarios u
            JOIN roles r ON r.id_rol = u.id_rol
            WHERE u.username = ?
              AND u.activo = TRUE
            """;

        try (Connection conn = Conexion.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, usuario);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String hashGuardado = rs.getString("password_hash");

                // Comparación directa (texto plano por ahora)
                // Si luego usas BCrypt: BCrypt.checkpw(contrasena, hashGuardado)
                if (!contrasena.equals(hashGuardado)) {
                    mostrarError("Usuario o contraseña incorrectos.");
                    txtContrasena.clear();
                    return;
                }

                // ✅ Login correcto
                lblError.setVisible(false);
                cargarHome();

            } else {
                mostrarError("Usuario o contraseña incorrectos.");
            }

        } catch (SQLException e) {
            mostrarError("Error de conexión: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void mostrarError(String mensaje) {
        lblError.setText(mensaje);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void cargarHome() {
        try {
            java.net.URL url = getClass().getClassLoader().getResource("Views/FXML/Home.fxml");

            if (url == null) {
                mostrarError("No se encontró Home.fxml");
                return;
            }

            FXMLLoader loader = new FXMLLoader(url);
            Scene scene = new Scene(loader.load());

            Stage stage = (Stage) txtUsuario.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("SIGESTI - Inicio");
            stage.show();

        } catch (IOException e) {
            mostrarError("Error al cargar Home: " + e.getMessage());
            e.printStackTrace();
        }
    }
}