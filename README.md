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
| **1. Inicio del proyecto** | Conceptualize la estructura y alcances del proyecto. |
| **2. Base de datos** | Defini categorías y productos, integre SQLite para guardar la información. |
| **3. API REST** | Desarrolle los servicios que entregan productos y categorías, con filtrado por categoría. |
| **4. Autenticación** | Implemente Spring Security para la autenticacion del administrador. |
| **5. Administración** | Implemente las operaciones para agregar, editar y cambiar la visibilidad de productos. |
| **6. Interfaz web** | Construi el menú y el panel administrativo responsivos y conectarlos con la API. |

Nota: Mi idea de proyecto originalmente era màs ambiciosa que el resultado final, por problemas de ajenda me vi forzado a realizar muchos recortes en mi vision original de un backend con muchas mas funcionalidades, no obstante quise dotar al proyecto de una base solida y extremadamente escalable, considero que la estructura de mi proyecto es buena y que la implementacion de posibles mejoras no deberian de presentar ningun problema a cualquier programador trasnochado (como el que escribe estas lineas) el editar el proyecto.

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

**Requisito:** tener una computadoa (obligatorio) con Java **JDK 21** instalado (tambien obligatorio). La primera ejecución necesita Internet para descargar las dependencias; no hace falta instalar Maven ni SQLite.

1. Recomiendo usar Git para clonar el proyecto con el siguiente comando:

   ```bash
   git clone https://github.com/xpatto/el-buen-sabor.git
   ```

2. Abre la carpeta del proyecto y utiliza el lanzador de tu sistema:

   - **Windows 10/11:** ejcutar `iniciar-windows.bat` (no es un virus tienen que creerme).
   - **Linux:** abre una terminal en la carpeta y ejecuta `bash iniciar-linux.sh` (también funciona `./iniciar-linux.sh` si tiene permiso de ejecución).

3. Espera el mensaje `Started MenuApplication` y visita las siguientes direcciones:

   | Página | Dirección |
   | --- | --- |
   | Menú público | http://localhost:8080/ |
   | Administración | http://localhost:8080/admin |

   **Acceso de demostración:** usuario `admin` · contraseña `admin`.

La aplicación crea automáticamente una base de datos local con **3 categorías y 7 productos de ejemplo**. Para detenerla, presiona **Ctrl+C** en la ventana donde está ejecutándose.

Nota: A pesar del "crunch" auto-impuesto en este proyecto me parecio un gran ejercicio de programacion y el hecho de que pudiera implementarlo en Java solo hizo que mejorara la experiencia.

---

**Repositorio público:** https://github.com/xpatto/el-buen-sabor
