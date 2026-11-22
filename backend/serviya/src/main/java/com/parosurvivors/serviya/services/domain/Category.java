package com.parosurvivors.serviya.services.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {
    
    private Long id;

    // Bloqueo optimista (B16): round-trip de @Version entidad<->dominio.
    private Integer version;
    private String name;

}

