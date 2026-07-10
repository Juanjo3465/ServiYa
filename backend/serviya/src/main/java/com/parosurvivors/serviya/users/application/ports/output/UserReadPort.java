package com.parosurvivors.serviya.users.application.ports.output;

import com.parosurvivors.serviya.users.application.dto.item.UserSummaryItem;
import com.parosurvivors.serviya.users.application.dto.query.SearchUsersQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Puerto de salida de LECTURA (CQRS-light) de usuarios: la busqueda enriquecida y paginada del panel admin.
 * La implementa un adaptador con query nativa (join a user_profiles para nombre/foto) y
 * {@code @SqlResultSetMapping}. El filtro por rol llega ya resuelto a {@code roleId} (el nombre se traduce
 * a id en el servicio, con una consulta unica sobre roles), para no unir la tabla roles en la busqueda.
 * El puerto de escritura ({@link UserPersistencePort}) queda con save/find.
 */
public interface UserReadPort {

    Page<UserSummaryItem> searchUsers(SearchUsersQuery query, Integer roleId, Pageable pageable);
}
