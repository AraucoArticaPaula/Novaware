> Novaware

Sistema de gestión de tienda desarrollado en Java (FXML, MVC) con conexión a SQL Server.
Permite administrar vendedores, inventario, comisiones y detalle de ventas en un entorno académico y empresarial.

------------------------------------------------------------
> Características principales
- Login seguro para usuarios registrados
- Dashboard de indicadores: ventas, metas, cumplimiento, garantías, LLB
- Gestión de comisiones: cálculo automático por tipo de venta
- Administración de vendedores: registro de colaboradores con DNI, fecha de ingreso y metas mensuales
- Inventario de productos: agregar, editar y controlar stock, precios y proveedores
- Detalle de ventas: registro por boleta, cliente, vendedor y estado de pago

------------------------------------------------------------
> Estructura del proyecto
- Login.fxml → Pantalla de inicio de sesión
- Home.fxml → Panel principal con indicadores
- Indicadores.fxml → Reporte de ventas y metas
- Comisiones.fxml → Cálculo de comisiones
- Vendedores.fxml → Gestión de colaboradores
- Inventario.fxml → Control de productos y stock
- detalleVentas.fxml → Registro de ventas

------------------------------------------------------------
> Instalación y uso
Requisitos:
- JDK 17+
- NetBeans o VS Code
- PostgreSQL

Pasos:
1. Clonar el repositorio:
   git clone https://github.com/AraucoArticaPaula/Novaware.git
2. Configurar la base de datos en PostgreSQL
3. Importar el proyecto en NetBeans
4. Ejecutar la aplicación desde Home.fxml
