import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import javax.tools.ToolProvider;

/** Comprobación previa compartida por los lanzadores; solo utiliza el JDK. */
public class VerificarDemo {
    public static void main(String[] args) {
        if (Runtime.version().feature() != 21 || ToolProvider.getSystemJavaCompiler() == null) {
            System.err.println("Se necesita JDK 21. Revise Java en PATH y JAVA_HOME.");
            System.exit(1);
        }
        try (var socket = new ServerSocket()) {
            // Permitir reiniciar tras Ctrl+C aunque queden conexiones TCP en TIME_WAIT.
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 8080));
        } catch (BindException exception) {
            System.err.println("El puerto 8080 esta ocupado. Cierre la otra instancia o libere el puerto y vuelva a iniciar.");
            System.err.println("No se ha detenido ningun proceso ajeno.");
            System.exit(1);
        } catch (Exception exception) {
            System.err.println("No se pudo comprobar el puerto local 8080. Revise los permisos de red locales.");
            System.exit(1);
        }
    }
}
