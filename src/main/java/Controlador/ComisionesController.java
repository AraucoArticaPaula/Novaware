package Controlador;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class ComisionesController implements Initializable {

    // ── Barra usuario ──
    @FXML private MenuButton menuUsuario;

    // ── Filtros ──
    @FXML private MenuButton menuPeriodo;
    @FXML private DatePicker dpFecha;

    // ── Tarjetas ──
    @FXML private Label lblTotalComisiones;
    @FXML private Label lblVta;
    @FXML private Label lblGex;
    @FXML private Label lblLlb;
    @FXML private Label lblSeleccionados;
    @FXML private Label lblCombos;

    // ── Tabla 1: detalle por vendedor ──
    @FXML private TableView<FilaDetalle>          tablaDetalle;
    @FXML private TableColumn<FilaDetalle, String>  colVendedor;
    @FXML private TableColumn<FilaDetalle, String>  colTotalComis;
    @FXML private TableColumn<FilaDetalle, String>  colVta;
    @FXML private TableColumn<FilaDetalle, String>  colGex;
    @FXML private TableColumn<FilaDetalle, String>  colLlbCol;
    @FXML private TableColumn<FilaDetalle, String>  colSelec;
    @FXML private TableColumn<FilaDetalle, String>  colCombo;
    @FXML private TableColumn<FilaDetalle, Integer> colVentas;

    // ── Tabla 2: registro individual ──
    @FXML private TableView<FilaComision>             tablaComisiones;
    @FXML private TableColumn<FilaComision, String>   colComisVendedor;
    @FXML private TableColumn<FilaComision, String>   colComisVenta;
    @FXML private TableColumn<FilaComision, String>   colComisMontoV;
    @FXML private TableColumn<FilaComision, String>   colComisTipo;
    @FXML private TableColumn<FilaComision, String>   colComisPct;
    @FXML private TableColumn<FilaComision, String>   colComisTotal;
    @FXML private TableColumn<FilaComision, String>   colComisFecha;

    private static final String FXML_BASE = "Views/FXML/";

    // ================================================================
    // MODELOS INTERNOS
    // ================================================================

    public static class FilaDetalle {
        private final String  vendedor;
        private final String  totalComis;
        private final String  vta;
        private final String  gex;
        private final String  llb;
        private final String  selec;
        private final String  combo;
        private final int     ventas;

        public FilaDetalle(String vendedor, BigDecimal totalComis, BigDecimal vta,
                           BigDecimal gex, BigDecimal llb, BigDecimal selec,
                           BigDecimal combo, int ventas) {
            this.vendedor   = vendedor;
            this.totalComis = fmt(totalComis);
            this.vta        = fmt(vta);
            this.gex        = fmt(gex);
            this.llb        = fmt(llb);
            this.selec      = fmt(selec);
            this.combo      = fmt(combo);
            this.ventas     = ventas;
        }

        private String fmt(BigDecimal v) {
            return "S/ " + (v == null ? "0.00" : v.setScale(2, RoundingMode.HALF_UP));
        }

        public String  getVendedor()   { return vendedor; }
        public String  getTotalComis() { return totalComis; }
        public String  getVta()        { return vta; }
        public String  getGex()        { return gex; }
        public String  getLlb()        { return llb; }
        public String  getSelec()      { return selec; }
        public String  getCombo()      { return combo; }
        public int     getVentas()     { return ventas; }
    }

    public static class FilaComision {
        private final String vendedor;
        private final String numeroVenta;
        private final String montoVenta;
        private final String tipo;
        private final String porcentaje;
        private final String total;
        private final String fecha;

        public FilaComision(String vendedor, String numeroVenta, BigDecimal montoVenta,
                            String tipo, BigDecimal porcentaje, BigDecimal total, String fecha) {
            this.vendedor    = vendedor;
            this.numeroVenta = numeroVenta;
            this.montoVenta  = "S/ " + montoVenta.setScale(2, RoundingMode.HALF_UP);
            this.tipo        = tipo;
            this.porcentaje  = porcentaje.setScale(2, RoundingMode.HALF_UP) + "%";
            this.total       = "S/ " + total.setScale(2, RoundingMode.HALF_UP);
            this.fecha       = fecha;
        }

        public String getVendedor()    { return vendedor; }
        public String getNumeroVenta() { return numeroVenta; }
        public String getMontoVenta()  { return montoVenta; }
        public String getTipo()        { return tipo; }
        public String getPorcentaje()  { return porcentaje; }
        public String getTotal()       { return total; }
        public String getFecha()       { return fecha; }
    }

    // ================================================================
    // INICIALIZACIÓN
    // ================================================================

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarColumnas();
        configurarFiltros();
        // Cargar este mes por defecto
        LocalDate hoy    = LocalDate.now();
        LocalDate inicio = hoy.withDayOfMonth(1);
        cargarDatos(inicio, hoy);
    }

    private void configurarColumnas() {
        // Tabla 1
        colVendedor.setCellValueFactory(c   -> new SimpleStringProperty(c.getValue().getVendedor()));
        colTotalComis.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTotalComis()));
        colVta.setCellValueFactory(c        -> new SimpleStringProperty(c.getValue().getVta()));
        colGex.setCellValueFactory(c        -> new SimpleStringProperty(c.getValue().getGex()));
        colLlbCol.setCellValueFactory(c     -> new SimpleStringProperty(c.getValue().getLlb()));
        colSelec.setCellValueFactory(c      -> new SimpleStringProperty(c.getValue().getSelec()));
        colCombo.setCellValueFactory(c      -> new SimpleStringProperty(c.getValue().getCombo()));
        colVentas.setCellValueFactory(c     -> new SimpleIntegerProperty(c.getValue().getVentas()).asObject());

        tablaDetalle.setPlaceholder(new Label("Sin datos para el periodo seleccionado"));

        // Tabla 2
        colComisVendedor.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getVendedor()));
        colComisVenta.setCellValueFactory(c    -> new SimpleStringProperty(c.getValue().getNumeroVenta()));
        colComisMontoV.setCellValueFactory(c   -> new SimpleStringProperty(c.getValue().getMontoVenta()));
        colComisTipo.setCellValueFactory(c     -> new SimpleStringProperty(c.getValue().getTipo()));
        colComisPct.setCellValueFactory(c      -> new SimpleStringProperty(c.getValue().getPorcentaje()));
        colComisTotal.setCellValueFactory(c    -> new SimpleStringProperty(c.getValue().getTotal()));
        colComisFecha.setCellValueFactory(c    -> new SimpleStringProperty(c.getValue().getFecha()));

        tablaComisiones.setPlaceholder(new Label("Sin comisiones registradas"));
    }

    private void configurarFiltros() {
        LocalDate hoy = LocalDate.now();

        // Este mes
        menuPeriodo.getItems().get(0).setOnAction(e -> {
            cargarDatos(hoy.withDayOfMonth(1), hoy);
            menuPeriodo.setText("Este mes");
        });
        // Mes anterior
        menuPeriodo.getItems().get(1).setOnAction(e -> {
            LocalDate primerDiaMesAnterior = hoy.minusMonths(1).withDayOfMonth(1);
            LocalDate ultimoDiaMesAnterior = hoy.withDayOfMonth(1).minusDays(1);
            cargarDatos(primerDiaMesAnterior, ultimoDiaMesAnterior);
            menuPeriodo.setText("Mes anterior");
        });
        // Personalizado: usa el DatePicker
        menuPeriodo.getItems().get(2).setOnAction(e -> menuPeriodo.setText("Personalizado"));

        dpFecha.setOnAction(e -> {
            LocalDate fecha = dpFecha.getValue();
            if (fecha != null) {
                cargarDatos(fecha, fecha);
                menuPeriodo.setText("Fecha");
            }
        });
    }

    // ================================================================
    // CARGA DE DATOS
    // ================================================================

    private void cargarDatos(LocalDate desde, LocalDate hasta) {
        cargarTarjetas(desde, hasta);
        cargarTablaDetalle(desde, hasta);
        cargarTablaComisiones(desde, hasta);
    }

    // ── Tarjetas resumen ──
    private void cargarTarjetas(LocalDate desde, LocalDate hasta) {
        String sql = """
            SELECT
                COALESCE(SUM(c.monto_comision), 0) AS total,
                COALESCE(SUM(CASE WHEN tc.codigo = 'VTA'   THEN c.monto_comision ELSE 0 END), 0) AS vta,
                COALESCE(SUM(CASE WHEN tc.codigo = 'GEX'   THEN c.monto_comision ELSE 0 END), 0) AS gex,
                COALESCE(SUM(CASE WHEN tc.codigo = 'LLB'   THEN c.monto_comision ELSE 0 END), 0) AS llb,
                COALESCE(SUM(CASE WHEN tc.codigo = 'SELEC' THEN c.monto_comision ELSE 0 END), 0) AS selec,
                COALESCE(SUM(CASE WHEN tc.codigo = 'COMBO' THEN c.monto_comision ELSE 0 END), 0) AS combo
            FROM comisiones c
            JOIN tipos_producto_comision tc ON tc.id_tipo = c.id_tipo
            JOIN ventas v ON v.id_venta = c.id_venta
            WHERE v.fecha_venta::date BETWEEN ? AND ?
            """;

        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                lblTotalComisiones.setText("S/ " + rs.getBigDecimal("total").setScale(2, RoundingMode.HALF_UP));
                lblVta.setText("S/ "  + rs.getBigDecimal("vta").setScale(2, RoundingMode.HALF_UP));
                lblGex.setText("S/ "  + rs.getBigDecimal("gex").setScale(2, RoundingMode.HALF_UP));
                lblLlb.setText("S/ "  + rs.getBigDecimal("llb").setScale(2, RoundingMode.HALF_UP));
                lblSeleccionados.setText("S/ " + rs.getBigDecimal("selec").setScale(2, RoundingMode.HALF_UP));
                lblCombos.setText("S/ " + rs.getBigDecimal("combo").setScale(2, RoundingMode.HALF_UP));
            }

        } catch (SQLException e) {
            System.err.println("[ComisionesController] Error tarjetas: " + e.getMessage());
        }
    }

    // ── Tabla 1: resumen por vendedor ──
    private void cargarTablaDetalle(LocalDate desde, LocalDate hasta) {
        String sql = """
            SELECT
                col.nombre || ' ' || col.apellidos AS vendedor,
                COALESCE(SUM(c.monto_comision), 0) AS total,
                COALESCE(SUM(CASE WHEN tc.codigo = 'VTA'   THEN c.monto_comision ELSE 0 END), 0) AS vta,
                COALESCE(SUM(CASE WHEN tc.codigo = 'GEX'   THEN c.monto_comision ELSE 0 END), 0) AS gex,
                COALESCE(SUM(CASE WHEN tc.codigo = 'LLB'   THEN c.monto_comision ELSE 0 END), 0) AS llb,
                COALESCE(SUM(CASE WHEN tc.codigo = 'SELEC' THEN c.monto_comision ELSE 0 END), 0) AS selec,
                COALESCE(SUM(CASE WHEN tc.codigo = 'COMBO' THEN c.monto_comision ELSE 0 END), 0) AS combo,
                COUNT(DISTINCT c.id_venta) AS total_ventas
            FROM colaboradores col
            LEFT JOIN comisiones c ON c.id_colaborador = col.id_colaborador
            LEFT JOIN tipos_producto_comision tc ON tc.id_tipo = c.id_tipo
            LEFT JOIN ventas v ON v.id_venta = c.id_venta
                               AND v.fecha_venta::date BETWEEN ? AND ?
            WHERE col.activo = TRUE
            GROUP BY col.id_colaborador, col.nombre, col.apellidos
            ORDER BY total DESC
            """;

        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));
            ResultSet rs = ps.executeQuery();

            ObservableList<FilaDetalle> lista = FXCollections.observableArrayList();
            while (rs.next()) {
                lista.add(new FilaDetalle(
                        rs.getString("vendedor"),
                        rs.getBigDecimal("total"),
                        rs.getBigDecimal("vta"),
                        rs.getBigDecimal("gex"),
                        rs.getBigDecimal("llb"),
                        rs.getBigDecimal("selec"),
                        rs.getBigDecimal("combo"),
                        rs.getInt("total_ventas")
                ));
            }
            tablaDetalle.setItems(lista);

        } catch (SQLException e) {
            System.err.println("[ComisionesController] Error tablaDetalle: " + e.getMessage());
        }
    }

    // ── Tabla 2: registro individual de comisiones ──
    private void cargarTablaComisiones(LocalDate desde, LocalDate hasta) {
        String sql = """
            SELECT
                col.nombre || ' ' || col.apellidos AS vendedor,
                v.numero_comprobante,
                c.monto_venta,
                tc.nombre AS tipo,
                c.porcentaje_aplic,
                c.monto_comision,
                TO_CHAR(v.fecha_venta, 'DD/MM/YYYY HH24:MI') AS fecha
            FROM comisiones c
            JOIN colaboradores col ON col.id_colaborador = c.id_colaborador
            JOIN ventas v          ON v.id_venta         = c.id_venta
            JOIN tipos_producto_comision tc ON tc.id_tipo = c.id_tipo
            WHERE v.fecha_venta::date BETWEEN ? AND ?
            ORDER BY v.fecha_venta DESC
            """;

        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));
            ResultSet rs = ps.executeQuery();

            ObservableList<FilaComision> lista = FXCollections.observableArrayList();
            while (rs.next()) {
                lista.add(new FilaComision(
                        rs.getString("vendedor"),
                        rs.getString("numero_comprobante"),
                        rs.getBigDecimal("monto_venta"),
                        rs.getString("tipo"),
                        rs.getBigDecimal("porcentaje_aplic"),
                        rs.getBigDecimal("monto_comision"),
                        rs.getString("fecha")
                ));
            }
            tablaComisiones.setItems(lista);

        } catch (SQLException e) {
            System.err.println("[ComisionesController] Error tablaComisiones: " + e.getMessage());
        }
    }

    // ================================================================
    // NAVEGACIÓN SIDEBAR
    // ================================================================

    @FXML private void irIndicadores(MouseEvent e) { irA(e, "Indicadores.fxml"); }
    @FXML private void irVendedores(MouseEvent e)  { irA(e, "agregarcolab.fxml"); }
    @FXML private void irInventario(MouseEvent e)  { irA(e, "Inventario.fxml"); }

    @FXML
    private void cerrarSesion() {
        irAConStage((Stage) menuUsuario.getScene().getWindow(), "Login.fxml", "SIGESTI - Login");
    }

    private void irA(MouseEvent e, String fxml) {
        Stage stage = (Stage) ((javafx.scene.Node) e.getSource()).getScene().getWindow();
        irAConStage(stage, fxml, "SIGESTI");
    }

    private void irAConStage(Stage stage, String fxml, String titulo) {
        try {
            URL url = getClass().getClassLoader().getResource(FXML_BASE + fxml);
            if (url == null) {
                System.err.println("[ComisionesController] No encontrado: " + fxml);
                return;
            }
            stage.setScene(new Scene(new FXMLLoader(url).load()));
            stage.setTitle(titulo);
            stage.show();
        } catch (IOException ex) {
            System.err.println("[ComisionesController] Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}