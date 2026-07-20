package com.parosurvivors.serviya.users.infrastructure.security;

import com.parosurvivors.serviya.shared.exceptions.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Contador de cuotas en memoria (algoritmo token bucket, Bucket4j). Guarda un balde por clave
 * politica+sujeto y lo consume en cada intento.
 *
 * <p><b>Alcance</b>: el almacen es un {@link ConcurrentHashMap} local, valido mientras haya UNA instancia
 * del backend. Con varias reeplicas cada una llevaria su propia cuenta y el limite efectivo se
 * multiplicaria por el numero de instancias. Bucket4j soporta Redis de forma nativa: llegado ese punto se
 * cambia el almacen aqui dentro y ni el filtro ni los controladores se enteran.</p>
 *
 * <p><b>Fuga de memoria acotada</b>: los baldes no se desalojan nunca, asi que el mapa crece con el numero
 * de IPs y correos DISTINTOS vistos desde el arranque. Cada entrada son unas pocas decenas de bytes; a
 * escala de este proyecto es despreciable, pero es la razon por la que un despliegue serio querria Redis
 * con expiracion, y no este mapa.</p>
 */
@Component
public class RateLimiterService {

    /**
     * Interruptor general. Se apaga para poder ejercitar los endpoints en pruebas manuales o de carga
     * sin chocar con las cuotas; en produccion se deja encendido.
     */
    @Value("${serviya.rate-limit.enabled:true}")
    private boolean enabled;

    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Consume una ficha de la cuota del sujeto. Si no queda ninguna, corta la peticion.
     *
     * @throws RateLimitExceededException con los segundos que faltan para recuperar cuota
     */
    public void consumeOrThrow(RateLimitPolicy policy, String subject) {
        if (!enabled || subject == null || subject.isBlank()) {
            return;
        }

        Bucket bucket = buckets.computeIfAbsent(policy.bucketKey(subject), key -> newBucket(policy));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            // Se redondea hacia arriba: devolver 0 invitaria a reintentar de inmediato y fallar otra vez.
            long secondsToWait = Math.max(1, Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds());
            throw new RateLimitExceededException(secondsToWait);
        }
    }

    private Bucket newBucket(RateLimitPolicy policy) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(policy.capacity())
                .refillIntervally(policy.capacity(), policy.window())
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
