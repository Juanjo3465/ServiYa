package com.parosurvivors.serviya.profiles.application.ports.input;

import com.parosurvivors.serviya.profiles.application.dto.SlotRequest;
import com.parosurvivors.serviya.profiles.domain.OffererAvailability;

import java.util.List;

/**
 * Puerto de entrada de OffererAvailabilityService — plantilla de disponibilidad general del oferente.
 * Ver documents/project-structure/estructura-servicios.docx (módulo 2).
 */
public interface OffererAvailabilityServicePort {

    List<OffererAvailability> getSchedule(int offererId);

    void setSchedule(int offererId, List<SlotRequest> slots);

    void deleteSlot(int slotId);

    void activateSlot(int slotId);

    void deactivateSlot(int slotId);
}
