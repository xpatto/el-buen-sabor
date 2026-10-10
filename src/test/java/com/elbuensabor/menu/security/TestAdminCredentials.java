package com.elbuensabor.menu.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Datos ficticios exclusivos de pruebas; nunca se incluyen en el JAR. */
public final class TestAdminCredentials {

    public static final String USERNAME = "admin-test";
    public static final String PASSWORD = "solo-pruebas-no-usar-en-produccion";
    public static final String HASH = new BCryptPasswordEncoder(4).encode(PASSWORD);

    private TestAdminCredentials() {
    }
}
