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

public class IndicadoresController implements Initializable {

    // ── Barra usuario ──
    @FXML private MenuButton menuUsuario;
    @FXML private MenuButton menuPeriodo;
    @FXML private DatePicker dpFecha;

    @FXML private Label lblMontoVendido;
    @FXML private Label lblMetaMes;
    @FXML private Label lblPorcentajeMeta;
    @FXML private Label lblPorcentajeGarantias;
    @FXML private Label lblLlenaBolsa;
    @FXML private Label lblTotalVentas;

    @FXML private TableView<FilaIndicador> tablaIndicadores;

    @FXML private TableColumn<FilaIndicador, String> colVendedor;
    @FXML private TableColumn<FilaIndicador, String> colMonto;
    @FXML private TableColumn<FilaIndicador, String> colMeta;
    @FXML private TableColumn<FilaIndicador, String> colPctMeta;
    @FXML private TableColumn<FilaIndicador, String> colPctGar;
    @FXML private TableColumn<FilaIndicador, String> colLlb;
    @FXML private TableColumn<FilaIndicador, Integer> colVentas;

    private static final String FXML_BASE = "Views/FXML/";

    // ── Modelo interno de fila ──
    public static class FilaIndicador {
        private final String  vendedor;
        private final String  monto;
        private final String  meta;
        private final String  pctMeta;
        private final String  pctGar;
        private final String  llb;
        private final int     ventas;

        public FilaIndicador(String vendedor, BigDecimal monto, BigDecimal meta,
                             BigDecimal pctMeta, BigDecimal pctGar,
                             BigDecimal llb, int ventas) {
            this.vendedor = vendedor;
            this.monto    = "S/ " + monto.setScale(2, RoundingMode.HALF_UP);
            this.meta     = "S/ " + meta.setScale(2, RoundingMode.HALF_UP);
            this.pctMeta  = pctMeta.setScale(2, RoundingMode.HALF_UP) + "%";
            this.pctGar   = pctGar.setScale(2, RoundingMode.HALF_UP) + "%";
            this.llb      = llb.setScale(2, RoundingMode.HALF_UP) + "%";
            this.ventas   = ventas;
        }

        public String  getVendedor() { return vendedor; }
        public String  getMonto()    { return monto; }
        public String  getMeta()     { return meta; }
        public String  getPctMeta()  { return pctMeta; }
        public String  getPctGar()   { return pctGar; }
        public String  getLlb()      { return llb; }
        public int     getVentas()   { return ventas; }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarColumnas();
        configurarFiltros();
        cargarDatos(LocalDate.now().withDayOfMonth(1), LocalDate.now());
    }

    // ── Configurar columnas ──
    private void configurarColumnas() {
        colVendedor.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getVendedor()));
        colMonto.setCellValueFactory(c    -> new SimpleStringProperty(c.getValue().getMonto()));
        colMeta.setCellValueFactory(c     -> new SimpleStringProperty(c.getValue().getMeta()));
        colPctMeta.setCellValueFactory(c  -> new SimpleStringProperty(c.getValue().getPctMeta()));
        colPctGar.setCellValueFactory(c   -> new SimpleStringProperty(c.getValue().getPctGar()));
        colLlb.setCellValueFactory(c      -> new SimpleStringProperty(c.getValue().getLlb()));
        colVentas.setCellValueFactory(c   -> new SimpleIntegerProperty(c.getValue().getVentas()).asObject());

        tablaIndicadores.setPlaceholder(new Label("Sin datos para el periodo seleccionado"));
    }

    // ── Configurar menú de periodo y datepicker ──
    private void configurarFiltros() {
        // Hoy
        menuPeriodo.getItems().get(0).setOnAction(e -> {
            LocalDate hoy = LocalDate.now();
            cargarDatos(hoy, hoy);
            menuPeriodo.setText("Hoy");
        });
        // Esta semana
        menuPeriodo.getItems().get(1).setOnAction(e -> {
            LocalDate hoy   = LocalDate.now();
            LocalDate inicio = hoy.minusDays(hoy.getDayOfWeek().getValue() - 1);
            cargarDatos(inicio, hoy);
            menuPeriodo.setText("Esta semana");
        });
        // Este mes
        menuPeriodo.getItems().get(2).setOnAction(e -> {
            LocalDate hoy   = LocalDate.now();
            LocalDate inicio = hoy.withDayOfMonth(1);
            cargarDatos(inicio, hoy);
            menuPeriodo.setText("Este mes");
        });

        // DatePicker: filtrar por día exacto
        dpFecha.setOnAction(e -> {
            LocalDate fecha = dpFecha.getValue();
            if (fecha != null) {
                cargarDatos(fecha, fecha);
                menuPeriodo.setText("Fecha");
            }
        });
    }

    // ── Carga principal de datos ──
    private void cargarDatos(LocalDate desde, LocalDate hasta) {
        cargarTarjetas(desde, hasta);
        cargarTabla(desde, hasta);
    }

    // ── Tarjetas resumen (totales de la tienda) ──
    private void cargarTarjetas(LocalDate desde, LocalDate hasta) {
        String sql = """
            SELECT
                COALESCE(SUM(v.total), 0)                          AS monto_vendido,
                COUNT(v.id_venta)                                   AS total_ventas,
                COALESCE(SUM(mm.meta_monto), 0)                    AS meta_mes,
                CASE WHEN COALESCE(SUM(mm.meta_monto),0) > 0
                     THEN (SUM(v.total) / SUM(mm.meta_monto)) * 100
                     ELSE 0 END                                     AS pct_meta,
                COALESCE(
                    (SELECT COUNT(*) * 100.0 / NULLIF(COUNT(v2.id_venta),0)
                     FROM ventas v2
                     JOIN detalle_venta dv2 ON dv2.id_venta = v2.id_venta
                     JOIN garantias g       ON g.id_detalle_v = dv2.id_detalle
                     WHERE v2.fecha_venta::date BETWEEN ? AND ?
                       AND v2.estado = 'COMPLETADA'), 0)            AS pct_garantias,
                COALESCE(
                    (SELECT SUM(c2.monto_comision)
                     FROM comisiones c2
                     JOIN tipos_producto_comision tc ON tc.id_tipo = c2.id_tipo
                     WHERE tc.codigo = 'LLB'
                       AND c2.anio = EXTRACT(YEAR FROM ?::date)
                       AND c2.mes  = EXTRACT(MONTH FROM ?::date)), 0) AS llena_bolsa
            FROM ventas v
            LEFT JOIN colaboradores col ON col.id_colaborador = v.id_colaborador
            LEFT JOIN metas_mensuales mm
                   ON mm.id_colaborador = col.id_colaborador
                  AND mm.anio = EXTRACT(YEAR FROM ?::date)
                  AND mm.mes  = EXTRACT(MONTH FROM ?::date)
            WHERE v.fecha_venta::date BETWEEN ? AND ?
              AND v.estado = 'COMPLETADA'
            """;

        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Setear todos los parámetros
            Date d = Date.valueOf(desde);
            Date h = Date.valueOf(hasta);
            ps.setDate(1, d);  ps.setDate(2, h);   // pct_garantias subquery
            ps.setDate(3, d);  ps.setDate(4, d);   // llena_bolsa subquery anio/mes
            ps.setDate(5, d);                        // metas_mensuales anio/mes
            ps.setDate(6, d);  ps.setDate(7, h);   // WHERE principal

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                BigDecimal monto    = rs.getBigDecimal("monto_vendido");
                BigDecimal meta     = rs.getBigDecimal("meta_mes");
                BigDecimal pctMeta  = rs.getBigDecimal("pct_meta");
                BigDecimal pctGar   = rs.getBigDecimal("pct_garantias");
                BigDecimal llb      = rs.getBigDecimal("llena_bolsa");
                int        ventas   = rs.getInt("total_ventas");

                lblMontoVendido.setText("S/ " + monto.setScale(2, RoundingMode.HALF_UP));
                lblMetaMes.setText("S/ " + meta.setScale(2, RoundingMode.HALF_UP));
                lblPorcentajeMeta.setText(pctMeta.setScale(2, RoundingMode.HALF_UP) + "%");
                lblPorcentajeGarantias.setText(pctGar.setScale(2, RoundingMode.HALF_UP) + "%");
                lblLlenaBolsa.setText(llb.setScale(2, RoundingMode.HALF_UP) + "%");
                lblTotalVentas.setText(String.valueOf(ventas));
            }

        } catch (SQLException e) {
            System.err.println("[IndicadoresController] Error tarjetas: " + e.getMessage());
        }
    }

    // ── Tabla: detalle por vendedor ──
    private void cargarTabla(LocalDate desde, LocalDate hasta) {
        String sql = """
            SELECT
                c.nombre || ' ' || c.apellidos                       AS vendedor,
                COALESCE(SUM(v.total), 0)                            AS monto,
                COALESCE(MAX(mm.meta_monto), 0)                      AS meta,
                CASE WHEN COALESCE(MAX(mm.meta_monto), 0) > 0
                     THEN (SUM(v.total) / MAX(mm.meta_monto)) * 100
                     ELSE 0 END                                       AS pct_meta,
                COALESCE(
                    (SELECT COUNT(g.id_garantia) * 100.0
                            / NULLIF(COUNT(dv2.id_detalle), 0)
                     FROM detalle_venta dv2
                     JOIN ventas v2 ON v2.id_venta = dv2.id_venta
                     LEFT JOIN garantias g ON g.id_detalle_v = dv2.id_detalle
                     WHERE v2.id_colaborador = c.id_colaborador
                       AND v2.fecha_venta::date BETWEEN ? AND ?
                       AND v2.estado = 'COMPLETADA'), 0)              AS pct_gar,
                COALESCE(
                    (SELECT SUM(com.monto_comision)
                     FROM comisiones com
                     JOIN tipos_producto_comision tc ON tc.id_tipo = com.id_tipo
                     WHERE com.id_colaborador = c.id_colaborador
                       AND tc.codigo = 'LLB'
                       AND com.anio = EXTRACT(YEAR FROM ?::date)
                       AND com.mes  = EXTRACT(MONTH FROM ?::date)), 0) AS llb,
                COUNT(v.id_venta)                                     AS total_ventas
            FROM colaboradores c
            LEFT JOIN ventas v
                   ON v.id_colaborador = c.id_colaborador
                  AND v.fecha_venta::date BETWEEN ? AND ?
                  AND v.estado = 'COMPLETADA'
            LEFT JOIN metas_mensuales mm
                   ON mm.id_colaborador = c.id_colaborador
                  AND mm.anio = EXTRACT(YEAR FROM ?::date)
                  AND mm.mes  = EXTRACT(MONTH FROM ?::date)
            WHERE c.activo = TRUE
            GROUP BY c.id_colaborador, c.nombre, c.apellidos
            ORDER BY monto DESC
            """;

        try (Connection conn = Conexion.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            Date d = Date.valueOf(desde);
            Date h = Date.valueOf(hasta);
            ps.setDate(1, d); ps.setDate(2, h);  // pct_gar subquery
            ps.setDate(3, d); ps.setDate(4, d);  // llb subquery anio/mes
            ps.setDate(5, d); ps.setDate(6, h);  // JOIN ventas
            ps.setDate(7, d); ps.setDate(8, d);  // JOIN metas anio/mes

            ObservableList<FilaIndicador> lista = FXCollections.observableArrayList();
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(new FilaIndicador(
                        rs.getString("vendedor"),
                        rs.getBigDecimal("monto"),
                        rs.getBigDecimal("meta"),
                        rs.getBigDecimal("pct_meta"),
                        rs.getBigDecimal("pct_gar"),
                        rs.getBigDecimal("llb"),
                        rs.getInt("total_ventas")
                ));
            }
            tablaIndicadores.setItems(lista);

        } catch (SQLException e) {
            System.err.println("[IndicadoresController] Error tabla: " + e.getMessage());
        }
    }

    // ── Navegación sidebar ──
    @FXML private void irComisiones(MouseEvent e) { irA(e, "Comisiones.fxml"); }
    @FXML private void irVendedores(MouseEvent e) { irA(e, "agregarcolab.fxml"); }
    @FXML private void irInventario(MouseEvent e) { irA(e, "Inventario.fxml"); }

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
                System.err.println("[IndicadoresController] No encontrado: " + fxml);
                return;
            }
            stage.setScene(new Scene(new FXMLLoader(url).load()));
            stage.setTitle(titulo);
            stage.show();
        } catch (IOException ex) {
            System.err.println("[IndicadoresController] Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}