package com.parosurvivors.serviya.services.application.ports.input;

import com.parosurvivors.serviya.services.application.dto.ServiceSearchCriteria;
import com.parosurvivors.serviya.services.application.dto.command.CreateServiceCommand;
import com.parosurvivors.serviya.services.application.dto.command.UpdateServiceCommand;
import com.parosurvivors.serviya.services.domain.Service;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de entrada del marketplace de servicios. Recibe Commands y devuelve la entidad de dominio
 * (Service); nunca tipos web (Form/Response). El mapeo web<->aplicacion vive en ServiceWebMapper.
 */
public interface MarketplaceServicePort {
    

    Service create(CreateServiceCommand command);
    Optional<Service> getById(Long id);
    List<Service> getAll();
    List<Service> getByOffererId(Long offererId);
    Service update(UpdateServiceCommand command);

    List<Service> search(ServiceSearchCriteria criteria);

    void delete(Long id);
    void softDelete(Long id);
    void activate(Long id);
    void deactivate(Long id);
}
