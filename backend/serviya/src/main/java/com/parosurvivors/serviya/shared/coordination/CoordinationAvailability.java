package com.parosurvivors.serviya.shared.coordination;

/**
 * Señal de si el sustrato de coordinación ({@code redis-coord}) está disponible. La usan las rutas que
 * quieren degradar de forma controlada cuando Redis cae: p. ej. el envío de notificaciones externas se
 * difiere mientras el rate limiting no puede proteger (ver protocolo D-07/1a), para no amplificar abuso
 * durante la caída. Interfaz framework-free (kernel compartido); la implementación vive en infraestructura.
 */
public interface CoordinationAvailability {

    /** {@code true} si {@code redis-coord} respondió al último sondeo (cacheado unos segundos). */
    boolean isAvailable();
}
