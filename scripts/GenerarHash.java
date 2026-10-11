import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Utilidad local: no forma parte del JAR ni configura credenciales predeterminadas. */
class GenerarHash {
    public static void main(String[] args) throws Exception {
        var console = System.console();
        if (console == null || args.length != 1) {
            throw new IllegalArgumentException("Usar una terminal interactiva y una ruta nueva para el hash.");
        }
        char[] password = console.readPassword("Contraseña local (12 o más caracteres): ");
        char[] confirmation = console.readPassword("Repite la contraseña: ");
        try {
            if (password == null || confirmation == null || password.length < 12
                    || !Arrays.equals(password, confirmation)) {
                throw new IllegalArgumentException("Contraseña corta o confirmación distinta.");
            }
            String value = new String(password);
            if (value.getBytes(StandardCharsets.UTF_8).length > 72) {
                throw new IllegalArgumentException("BCrypt admite como máximo 72 bytes UTF-8.");
            }
            String hash = new BCryptPasswordEncoder(12).encode(value);
            Path destination = Path.of(args[0]);
            if (Files.getFileStore(destination.toAbsolutePath().getParent()).supportsFileAttributeView("posix")) {
                Files.createFile(destination, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            } else {
                // En Windows hereda los permisos del directorio privado del usuario.
                Files.createFile(destination);
            }
            Files.writeString(destination, hash, StandardCharsets.UTF_8, StandardOpenOption.WRITE);
            console.printf("Hash guardado. No compartas el archivo ni su contenido.%n");
        } finally {
            if (password != null) Arrays.fill(password, '\0');
            if (confirmation != null) Arrays.fill(confirmation, '\0');
        }
    }
}
