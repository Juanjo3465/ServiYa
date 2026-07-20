package com.parosurvivors.serviya.notifications.infrastructure.adapters.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.parosurvivors.serviya.notifications.infrastructure.config.EmailProperties;
import com.parosurvivors.serviya.shared.exceptions.ResourceNotFoundException;
import com.parosurvivors.serviya.users.application.ports.input.UserQueryServicePort;
import com.parosurvivors.serviya.users.domain.User;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

/**
 * Contrato clave de {@link EmailApiAdapter}: {@code send} NUNCA lanza (corre dentro de un flujo
 * transaccional). Ante fallos devuelve false sin tocar la API del proveedor.
 */
@ExtendWith(MockitoExtension.class)
class EmailApiAdapterTest {

    @Mock private UserQueryServicePort userQueryServicePort;

    private EmailApiAdapter adapter() {
        EmailProperties props = new EmailProperties(
                true, "http://localhost:1", "test-key", "no-reply@serviya.com", "ServiYa", 500, 500);
        // RestClient real pero nunca invocado en estos casos (fallan antes de la llamada HTTP).
        return new EmailApiAdapter(RestClient.create(), props, userQueryServicePort);
    }

    @Test
    void send_returnsFalse_andDoesNotThrow_whenUserLookupFails() {
        when(userQueryServicePort.getUserById(1L)).thenThrow(new ResourceNotFoundException("no existe"));

        boolean result = adapter().send(1L, "welcome", "Título", "Mensaje", Map.of());

        assertThat(result).isFalse();
    }

    @Test
    void send_returnsFalse_whenUserHasNoEmail() {
        when(userQueryServicePort.getUserById(2L)).thenReturn(User.builder().id(2L).email(null).build());

        boolean result = adapter().send(2L, "welcome", "Título", "Mensaje", Map.of());

        assertThat(result).isFalse();
    }
}
