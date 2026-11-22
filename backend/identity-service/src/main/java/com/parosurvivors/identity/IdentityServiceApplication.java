package com.parosurvivors.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microservicio de identidad de ServiYa (Fase 8A: separación del módulo {@code users} SIN separar la BD).
 *
 * <p>Comparte el MySQL del monolito (es el único ESCRITOR de la identidad: cuentas, credenciales, roles,
 * baneos, emisión de tokens, refresh, JWKS, {@code min_iat} y reset de contraseña). El monolito sigue
 * LEYENDO las tablas de usuario por la BD compartida (Estrategia B), así que esta fase no separa datos.</p>
 */
@SpringBootApplication
public class IdentityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityServiceApplication.class, args);
    }
}
