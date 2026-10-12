# El Buen Sabor 🍽️

**Menú digital interactivo para un restaurante**, desarrollado con Java y Spring Boot. Permite consultar productos por categoría y administrar el catálogo desde una segunda página.

## ¿De qué trata el proyecto?

El reto consistía en crear un menú digital sencillo y fácil de consultar. Para resolverlo, desarrollé una aplicación web de **dos páginas** que conecta una interfaz responsiva con un backend y una base de datos local.

Este enfoque permite que los productos no estén escritos directamente en el HTML: el menú obtiene su información de una API y refleja los cambios realizados por el administrador.

## ¿Qué se puede hacer?

**Menú público (`/`)**
- Consultar productos de **Comida / Lonchería**, **Cafetería y Bebidas** y **Tienda y Snacks**.
- Filtrar las tarjetas mediante un selector, sin recargar la página.
- Ver nombre, categoría, precio en pesos mexicanos y una breve descripción; algunos productos incluyen opciones sencillas descritas en el texto.

**Administración (`/admin`)**
- Iniciar sesión y gestionar productos desde una interfaz independiente.
- Agregar, editar, ocultar, reactivar o eliminar productos.
- Conservar los cambios en SQLite, incluso después de cerrar y volver a ejecutar la aplicación.

## Desarrollo por etapas

Organicé el proyecto en seis etapas para construir cada componente de manera progresiva:

| Etapa | ¿En qué consistió? |
| --- | --- |
| **1. Inicio del proyecto** | Crear la estructura con Java 21, Spring Boot y Maven Wrapper. |
| **2. Base de datos** | Definir categorías y productos e integrar SQLite para guardar la información. |
| **3. API REST** | Desarrollar los servicios que entregan productos y categorías, con filtrado por categoría. |
| **4. Autenticación** | Proteger el acceso administrativo mediante Spring Security. |
| **5. Administración** | Implementar las operaciones para agregar, editar y cambiar la visibilidad de productos. |
| **6. Interfaz web** | Construir el menú y el panel administrativo responsivos y conectarlos con la API. |

**Ajustes finales:** incorporé la eliminación de productos y un modo de demostración con inicio automático para Windows y Linux, facilitando la revisión del proyecto.

## Tecnologías utilizadas

| Tecnología | Función en el proyecto |
| --- | --- |
| **Java 21 y Spring Boot** | Ejecutan el servidor y la lógica del menú. |
| **Spring Web (API REST)** | Proporciona los productos y las categorías al navegador. |
| **HTML5, CSS3 y JavaScript** | Construyen las interfaces responsivas, las tarjetas y el filtro dinámico. |
| **Thymeleaf y Spring Security** | Gestionan la vista administrativa, el inicio de sesión y el acceso protegido. |
| **SQLite y Spring JDBC** | Almacenan y consultan los productos, precios y categorías. |
| **Maven Wrapper** | Prepara y ejecuta el proyecto sin instalar Maven manualmente. |

## ¿Cómo ejecutar el proyecto?

**Requisito:** tener instalado **JDK 21**. La primera ejecución necesita Internet para descargar las dependencias; no hace falta instalar Maven ni SQLite.

1. Descarga este repositorio desde **Code → Download ZIP** y descomprímelo, o clónalo con Git:

   ```bash
   git clone https://github.com/xpatto/el-buen-sabor.git
   ```

2. Abre la carpeta del proyecto y utiliza el lanzador de tu sistema:

   - **Windows 10/11:** doble clic en `iniciar-windows.bat`.
   - **Linux:** abre una terminal en la carpeta y ejecuta `bash iniciar-linux.sh` (también funciona `./iniciar-linux.sh` si tiene permiso de ejecución).

3. Espera el mensaje `Started MenuApplication` y visita las siguientes direcciones:

   | Página | Dirección |
   | --- | --- |
   | Menú público | http://localhost:8080/ |
   | Administración | http://localhost:8080/admin |

   **Acceso de demostración:** usuario `admin` · contraseña `admin`.

La aplicación crea automáticamente una base de datos local con **3 categorías y 7 productos de ejemplo**. Para detenerla, presiona **Ctrl+C** en la ventana donde está ejecutándose.

> **Nota:** el acceso `admin/admin` es únicamente para la demostración local. Esta versión se ejecuta en `127.0.0.1` y no debe exponerse a Internet.

---

**Repositorio público:** https://github.com/xpatto/el-buen-sabor
