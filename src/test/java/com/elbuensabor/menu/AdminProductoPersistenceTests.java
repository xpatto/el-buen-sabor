package com.elbuensabor.menu;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

import com.elbuensabor.menu.dto.ProductoRequest;
import com.elbuensabor.menu.security.TestAdminCredentials;
import com.elbuensabor.menu.service.ProductoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.*;

class AdminProductoPersistenceTests {

    @TempDir Path directory;

    private ApplicationContextRunner application() {
        return new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withUserConfiguration(MenuApplication.class)
                .withPropertyValues("spring.datasource.url=jdbc:sqlite:" + directory.resolve("admin.db"),
                        "ADMIN_USERNAME=" + TestAdminCredentials.USERNAME,
                        "ADMIN_PASSWORD_HASH=" + TestAdminCredentials.HASH);
    }

    @Test
    void adminWritesAndSeedEditsSurviveRestartsWithoutBeingOverwritten() {
        AtomicLong id = new AtomicLong();
        application().run(context -> {
            assertThat(context).hasNotFailed();
            var service = context.getBean(ProductoService.class);
            id.set(service.crear(new ProductoRequest("Nuevo", "Descripción", new BigDecimal("12.34"), 1L)).id());
            service.actualizar(id.get(), new ProductoRequest("Editado", "Nueva descripción", new BigDecimal("45.67"), 2L));
            service.cambiarEstado(id.get(), false);
            service.actualizar(1, new ProductoRequest("Semilla editada", "Conservada", new BigDecimal("99.99"), 3L));
            service.cambiarEstado(1, false);
        });
        application().run(context -> {
            assertThat(context).hasNotFailed();
            var service = context.getBean(ProductoService.class);
            var products = service.listarTodos();
            assertThat(products).hasSize(8);
            var edited = products.stream().filter(product -> product.id() == id.get()).findFirst().orElseThrow();
            assertThat(edited.nombre()).isEqualTo("Editado");
            assertThat(edited.descripcion()).isEqualTo("Nueva descripción");
            assertThat(edited.precio()).isEqualByComparingTo("45.67");
            assertThat(edited.categoriaId()).isEqualTo(2);
            assertThat(edited.activo()).isFalse();
            assertThat(products.getFirst().nombre()).isEqualTo("Semilla editada");
            assertThat(products.getFirst().precio()).isEqualByComparingTo("99.99");
            assertThat(products.getFirst().activo()).isFalse();
            assertThat(service.listarActivos(null)).extracting("id").doesNotContain(1L, id.get());
            service.cambiarEstado(id.get(), true);
            assertThat(context.getBean(JdbcTemplate.class).queryForObject(
                    "SELECT file FROM pragma_database_list WHERE name='main'", String.class))
                    .isEqualTo(directory.resolve("admin.db").toString());
        });
        application().run(context -> {
            assertThat(context).hasNotFailed();
            var service = context.getBean(ProductoService.class);
            assertThat(service.listarActivos("bebidas")).extracting("id").contains(id.get());
            long next = service.crear(new ProductoRequest("Otro", "D", BigDecimal.ONE, 1L)).id();
            assertThat(next).isGreaterThan(id.get());
        });
    }

    @Test
    void administrativeWritesParticipateInTransactionAndRollbackTogether() {
        application().run(context -> {
            assertThat(context).hasNotFailed();
            var service = context.getBean(ProductoService.class);
            var original = service.listarTodos();
            var transaction = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
            assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
                service.crear(new ProductoRequest("Temporal", "D", BigDecimal.ONE, 1L));
                service.actualizar(1, new ProductoRequest("Temporal", "D", BigDecimal.ONE, 2L));
                service.cambiarEstado(2, false);
                throw new IllegalStateException("Fallo simulado para rollback");
            })).isInstanceOf(IllegalStateException.class);
            assertThat(service.listarTodos()).isEqualTo(original);
        });
    }
}
