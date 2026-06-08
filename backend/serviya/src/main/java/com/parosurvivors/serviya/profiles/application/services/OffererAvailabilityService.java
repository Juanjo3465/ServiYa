package com.parosurvivors.serviya.profiles.application.services;

import com.parosurvivors.serviya.profiles.application.dto.SlotRequest;
import com.parosurvivors.serviya.profiles.application.ports.input.OffererAvailabilityServicePort;
import com.parosurvivors.serviya.profiles.application.ports.output.OffererAvailabilityPersistencePort;
import com.parosurvivors.serviya.profiles.domain.OffererAvailability;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementacion placeholder de OffererAvailabilityServicePort.
 * Metodos sin logica aun (lanzan UnsupportedOperationException); dependencias inyectadas.
 * Ver documents/project-structure/estructura-servicios.docx.
 */
@Component
@RequiredArgsConstructor
public class OffererAvailabilityService implements OffererAvailabilityServicePort {

    private final OffererAvailabilityPersistencePort offererAvailabilityPersistencePort;

    @Override
    public List<OffererAvailability> getSchedule(Long offererId) {
        throw new UnsupportedOperationException("TODO: getSchedule — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void setSchedule(Long offererId, List<SlotRequest> slots) {
        throw new UnsupportedOperationException("TODO: setSchedule — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void deleteSlot(Long slotId) {
        throw new UnsupportedOperationException("TODO: deleteSlot — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void activateSlot(Long slotId) {
        throw new UnsupportedOperationException("TODO: activateSlot — placeholder, ver estructura-servicios.docx");
    }

    @Override
    public void deactivateSlot(Long slotId) {
        throw new UnsupportedOperationException("TODO: deactivateSlot — placeholder, ver estructura-servicios.docx");
    }
}
