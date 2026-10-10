# Fix-B13: Listados cargan tabla completa y paginan en memoria

## Información del Bug

- **ID**: B13
- **Severidad**: MEDIO
- **Módulos afectados**: `reports`, `admin`
- **Archivo origen**: `ESTADO-ACTUAL-Y-PLAN-FUTURO.md` (catálogo de bugs, línea 384)

## Descripción del Problema

El bug B13 describe dos problemas de rendimiento relacionados con la paginación en memoria en lugar de usar paginación a nivel de base de datos:

### 1. ReportService.getReports() (Módulo Reports)
**Ubicación**: `ReportService.java:192-204`

El método `getReports()` llamaba a `reportPersistencePort.findAll()` que carga **TODOS** los reportes de la base de datos en memoria, y luego aplicaba filtros (tipo, categoría, estado), ordenación y paginación manualmente usando Java Streams.

```java
// ANTES (problemático)
List<Report> reports = reportPersistencePort.findAll().stream()
    .filter(report -> type == null || type.isBlank() || report.getReportType().name().equalsIgnoreCase(type))
    .filter(report -> category == null || category.isBlank() || report.getCategory().equalsIgnoreCase(category))
    .filter(report -> status == null || status.isBlank() || report.getStatus().name().equalsIgnoreCase(status))
    .sorted(Comparator.comparing(Report::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
    .toList();

// Paginación manual en memoria
int start = (int) pageable.getOffset();
int end = Math.min(start + pageable.getPageSize(), reports.size());
List<Report> pageContent = start >= reports.size() ? List.of() : reports.subList(start, end);
return new PageImpl<>(pageContent, pageable, reports.size());
```

**Problemas**:
- Carga O(N) registros en memoria (N = total de reportes en BD)
- Consumo de memoria proporcional al tamaño de la tabla
- Latencia creciente con el volumen de datos
- No escala para tablas grandes

### 2. AdminService.searchFeedback() (Módulo Admin)
**Ubicación**: `AdminService.java:252-318`

Los métodos `searchServiceFeedback()` y `searchClientFeedback()` usaban `PageRequest.of(0, Integer.MAX_VALUE)` para cargar **TODO** el feedback de servicio/cliente en memoria, y luego aplicaban filtros adicionales (keyword, ratingMin, ratingMax) y paginación manualmente.

```java
// ANTES (problemático)
feedbacks = serviceFeedbackPersistencePort.findByServiceId(query.serviceId(),
    PageRequest.of(0, Integer.MAX_VALUE)).getContent(); // ¡Carga TODO!
```

**Problemas**:
- Igual que arriba: carga O(N) en memoria
- Los puertos de persistencia **ya tenían métodos de paginación** (`Page<T> findByX(Long id, Pageable pageable)`) pero no se usaban
- Filtros de búsqueda (keyword, rating) se aplicaban en memoria en lugar de en BD

## Causa Raíz

1. **Falta de métodos de paginación con filtros en ReportRepository**: El repositorio solo tenía métodos `findAll()` y búsquedas simples que devolvían `List<T>`, sin soporte para `Page<T>` con filtros combinados.

2. **Subutilización de métodos existentes en Feedback**: Los puertos de persistencia de feedback (`ServiceFeedbackPersistencePort`, `ClientFeedbackPersistencePort`) ya tenían métodos `Page<T> findByX(Long id, Pageable pageable)` pero el `AdminService` no los usaba, prefiriendo cargar todo con `Integer.MAX_VALUE`.

3. **Ausencia de consultas con filtros dinámicos**: No existían métodos en los repositorios que permitieran filtrar por keyword, ratingMin, ratingMax a nivel de BD.

## Solución Implementada

### 1. Fix para ReportService.getReports()

#### Cambios en ReportRepository
Se agregaron métodos de paginación con todas las combinaciones de filtros posibles:
```java
Page<ReportEntity> findByReportTypeAndCategoryAndStatus(ReportType, String, ReportStatus, Pageable);
Page<ReportEntity> findByReportTypeAndCategory(ReportType, String, Pageable);
Page<ReportEntity> findByReportTypeAndStatus(ReportType, ReportStatus, Pageable);
Page<ReportEntity> findByCategoryAndStatus(String, ReportStatus, Pageable);
Page<ReportEntity> findByReportType(ReportType, Pageable);
Page<ReportEntity> findByCategory(String, Pageable);
Page<ReportEntity> findByStatus(ReportStatus, Pageable);
@Query("SELECT r FROM ReportEntity r")
Page<ReportEntity> findAllReports(Pageable);
```

#### Cambios en ReportPersistencePort
```java
Page<Report> findReports(String type, String category, String status, Pageable pageable);
```

#### Cambios en ReportPersistenceAdapter
Implementación que selecciona el método de repositorio apropiado según qué filtros estén presentes.

#### Cambios en ReportService
```java
// DESPUÉS (eficiente)
@Override
public Page<Report> getReports(String type, String category, String status, Pageable pageable) {
    return reportPersistencePort.findReports(type, category, status, pageable);
}
```

### 2. Fix para AdminService.searchFeedback()

#### Cambios en ServiceFeedbackPersistencePort y ClientFeedbackPersistencePort
Se agregaron métodos con filtros de búsqueda:
```java
Page<ServiceFeedback> findByServiceIdAndFilters(Long serviceId, String keyword, Integer ratingMin, Integer ratingMax, Pageable pageable);
Page<ServiceFeedback> findByClientIdAndFilters(Long clientId, String keyword, Integer ratingMin, Integer ratingMax, Pageable pageable);
Page<ClientFeedback> findByClientIdAndFilters(Long clientId, String keyword, Integer ratingMin, Integer ratingMax, Pageable pageable);
Page<ClientFeedback> findByOffererIdAndFilters(Long offererId, String keyword, Integer ratingMin, Integer ratingMax, Pageable pageable);
```

#### Cambios en Repositorios (ServiceFeedbackRepository, ClientFeedbackRepository)
Consultas JPQL con parámetros opcionales:
```java
@Query("SELECT f FROM ServiceFeedbackEntity f WHERE f.serviceId = :serviceId "
    + "AND (:keyword IS NULL OR :keyword = '' OR LOWER(f.comment) LIKE LOWER(CONCAT('%', :keyword, '%'))) "
    + "AND (:ratingMin IS NULL OR f.rating >= :ratingMin) "
    + "AND (:ratingMax IS NULL OR f.rating <= :ratingMax) "
    + "ORDER BY f.createdAt DESC")
Page<ServiceFeedbackEntity> findByServiceIdAndFilters(Long serviceId, String keyword, Integer ratingMin, Integer ratingMax, Pageable pageable);
```

#### Cambios en AdminService
Reescritura completa de `searchFeedback()` y métodos auxiliares para usar paginación nativa:
```java
// DESPUÉS (eficiente)
private Page<AdminFeedbackSearchResult> searchServiceFeedback(AdminFeedbackSearchQuery query, Pageable pageable) {
    if (query.serviceId() == null && query.clientId() == null) {
        return Page.empty(pageable);
    }
    Page<ServiceFeedback> feedbackPage;
    if (query.serviceId() != null) {
        feedbackPage = serviceFeedbackPersistencePort.findByServiceIdAndFilters(
                query.serviceId(), query.keyword(), query.ratingMin(), query.ratingMax(), pageable);
    } else {
        feedbackPage = serviceFeedbackPersistencePort.findByClientIdAndFilters(
                query.clientId(), query.keyword(), query.ratingMin(), query.ratingMax(), pageable);
    }
    return feedbackPage.map(f -> new AdminFeedbackSearchResult(...));
}
```

## Archivos Modificados

### Módulo Reports
1. `backend/serviya/src/main/java/com/parosurvivors/serviya/reports/infrastructure/repositories/ReportRepository.java`
2. `backend/serviya/src/main/java/com/parosurvivors/serviya/reports/application/ports/output/ReportPersistencePort.java`
3. `backend/serviya/src/main/java/com/parosurvivors/serviya/reports/infrastructure/adapters/output/ReportPersistenceAdapter.java`
4. `backend/serviya/src/main/java/com/parosurvivors/serviya/reports/application/services/ReportService.java`

### Módulo Feedback
5. `backend/serviya/src/main/java/com/parosurvivors/serviya/feedback/application/ports/output/ServiceFeedbackPersistencePort.java`
6. `backend/serviya/src/main/java/com/parosurvivors/serviya/feedback/application/ports/output/ClientFeedbackPersistencePort.java`
7. `backend/serviya/src/main/java/com/parosurvivors/serviya/feedback/infrastructure/repositories/ServiceFeedbackRepository.java`
8. `backend/serviya/src/main/java/com/parosurvivors/serviya/feedback/infrastructure/repositories/ClientFeedbackRepository.java`
9. `backend/serviya/src/main/java/com/parosurvivors/serviya/feedback/infrastructure/adapters/output/ServiceFeedbackPersistenceAdapter.java`
10. `backend/serviya/src/main/java/com/parosurvivors/serviya/feedback/infrastructure/adapters/output/ClientFeedbackPersistenceAdapter.java`

### Módulo Admin
11. `backend/serviya/src/main/java/com/parosurvivors/serviya/admin/application/services/AdminService.java`

## Verificación

- ✅ Compilación exitosa (`./mvnw clean compile`)
- ✅ Tests unitarios pasan: `ReportServiceTest` (9 tests), `AdminServiceTest` (5 tests)
- ✅ No se rompieron tests existentes

## Impacto

- **Memoria**: Reducción drástica del consumo de memoria (de O(N) a O(pageSize))
- **Rendimiento**: Consultas ejecutadas en BD con índices, aprovechando el motor de base de datos
- **Escalabilidad**: El sistema ahora escala correctamente con el crecimiento de datos
- **Latencia**: Tiempos de respuesta predecibles y constantes independiente del tamaño de tabla

## Notas Adicionales

Este fix es parte de la **Fase 0 - Endurecer el monolito modular** (mejora M7: Read-models / proyecciones para lecturas compuestas pesadas) del plan de migración a microservicios, ya que resuelve uno de los cuellos de botella de rendimiento identificados en el diagnóstico transversal (§3.4).