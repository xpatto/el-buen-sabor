import java.net.URI;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Descarga el wrapper oficial con el JDK; no requiere PowerShell ni guarda binarios en Git. */
public class PrepararMaven {
    private static final String URL = "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/"
            + "maven-wrapper/3.3.4/maven-wrapper-3.3.4.jar";
    // SHA-256 del artefacto Maven Central 3.3.4, no de una credencial.
    private static final String SHA256 = "4e2fbf6554bc8a4702cdfdd3bef464f423393d784ddbb037216320ce55d5e4e1";

    public static void main(String[] args) {
        try {
            Path wrapper = Path.of(args[0]).resolve(".mvn/wrapper/maven-wrapper.jar");
            if (Files.exists(wrapper)) {
                verificar(wrapper);
                return;
            }
            System.out.println("Descargando Maven Wrapper oficial 3.3.4 desde Maven Central...");
            Path temporary = Files.createTempFile(wrapper.getParent(), "wrapper-", ".tmp");
            try {
                var connection = URI.create(URL).toURL().openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(60000);
                try (var input = connection.getInputStream()) {
                    Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
                }
                verificar(temporary);
                try {
                    Files.move(temporary, wrapper, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException exception) {
                    Files.move(temporary, wrapper, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (SecurityException exception) {
            System.err.println("Fallo de integridad del Maven Wrapper. No se ejecutara; revise o elimine solo .mvn/wrapper/maven-wrapper.jar para descargarlo de nuevo.");
            System.exit(1);
        } catch (Exception exception) {
            System.err.println("No se pudo preparar Maven Wrapper. Revise JDK 21, la conexion HTTPS a Maven Central y los permisos de la carpeta .mvn/wrapper.");
            System.exit(1);
        }
    }

    private static void verificar(Path file) throws Exception {
        // El JAR oficial tiene menos de 100 KiB; limitar la lectura de una caché alterada.
        if (Files.size(file) > 1024 * 1024) throw new SecurityException("Wrapper demasiado grande");
        String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
        if (!SHA256.equals(actual)) throw new SecurityException("Wrapper alterado");
    }
}
