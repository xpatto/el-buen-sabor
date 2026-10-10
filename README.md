# El Buen Sabor

Menú digital del restaurante El Buen Sabor. Etapa actual: inicialización del
proyecto. Incluye arranque Spring Boot, una prueba de contexto y una página
temporal con «El Buen Sabor» y «Proyecto inicializado».

## Tecnologías

- Java 21 (JDK requerido).
- Spring Boot 3.5.16.
- Maven 3.9.11 mediante Maven Wrapper 3.3.4; no requiere Maven instalado.
- Spring Web y Spring Boot Starter Test.
- Coordenadas: `com.elbuensabor:menu`; paquete: `com.elbuensabor.menu`.
- Monolito por capas a implementar progresivamente.

## Ejecutar en Linux / Fish

Desde la raíz del proyecto, verifica el JDK:

```fish
java -version
javac -version
```

Ambos deben indicar Java 21. Si utilizas `JAVA_HOME`, debe apuntar al JDK 21.
La primera ejecución requiere Internet para descargar Maven y dependencias.
El Wrapper utiliza `curl` o `wget` y, según el descargador, `unzip`.

```fish
./mvnw spring-boot:run
```

Abre <http://localhost:8080>. Detén el servidor con `Ctrl+C`.
Si el checkout perdió el permiso de ejecución, ejecuta `chmod +x mvnw`.

## Pruebas y empaquetado

```fish
./mvnw test
./mvnw package
java -jar target/menu-0.0.1-SNAPSHOT.jar
```

`MenuApplicationTests.contextLoads` usa `@SpringBootTest` para comprobar que
el contexto de Spring arranca. Los reportes se generan en `target/surefire-reports/`.

## Alcance y colaboración

Consulta [docs/ALCANCE.md](docs/ALCANCE.md) y [AGENTS.md](AGENTS.md) antes de
cambiar el proyecto. SQLite, API REST, seguridad, administración e interfaz
definitiva están pendientes de etapas posteriores. No hay credenciales ni
bases de datos en el repositorio.

## GitHub (pendiente de autorización)

Con GitHub CLI instalado y autenticado, el comando preparado para crear el
repositorio privado y subir `main` es:

```fish
gh repo create el-buen-sabor --private --source=. --remote=origin --push
```

No ejecutar sin autorización explícita. La entrega final será pública;
la publicación y el cambio de visibilidad se autorizarán posteriormente.
