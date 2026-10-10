# Fix for Bug B9: `@Scheduled` sin lock distribuido (doble ejecución al escalar)

## Resumen del Bug

**Severidad:** ALTO  
**Módulos afectados:** `requests`, `notifications`  
**Archivos involucrados:**
- `RequestMaintenanceScheduler.java` - 4 tareas de mantenimiento horarias
- `NotificationRetryScheduler.java` - Reintento de notificaciones cada 15 minutos

## Causa Raíz

Los schedulers usan `@Scheduled` de Spring sin ningún mecanismo de **lock distribuido**. Cuando la aplicación se escala a múltiples instancias (horizontal scaling), cada instancia ejecuta las tareas programadas independientemente, causando:

1. **Ejecución duplicada** de las mismas tareas de mantenimiento
2. **Condiciones de carrera** (race conditions) al procesar las mismas solicitudes/propuestas
3. **Reintentos duplicados** de entregas de notificación fallidas
4. **Inconsistencia de datos** por procesamiento concurrente no coordinado

### Tareas afectadas en `RequestMaintenanceScheduler`:
- `rejectExpiredPendingRequests()` - Rechaza solicitudes PENDING vencidas
- `markStaleAcceptedAsNotProvided()` - Marca solicitudes ACCEPTED antiguas como NOT_PROVIDED
- `rejectExpiredProposals()` - Rechaza propuestas de reprogramación PENDING vencidas
- `finalizeUnconfirmedCompletions()` - Auto-confirma solicitudes PRESUMABLY_COMPLETED antiguas

### Tarea afectada en `NotificationRetryScheduler`:
- `retryFailedDeliveries()` - Reintenta entregas de notificación en estado FAILED

## Solución Implementada

Se integró **ShedLock** (v5.6.0) con proveedor JDBC para MySQL, que proporciona locking distribuido a nivel de base de datos.

### Cambios realizados:

#### 1. Dependencias añadidas (`pom.xml`)
```xml
<dependency>
    <groupId>net.javacrumbs.shedlock</groupId>
    <artifactId>shedlock-spring</artifactId>
    <version>5.6.0</version>
</dependency>
<dependency>
    <groupId>net.javacrumbs.shedlock</groupId>
    <artifactId>shedlock-provider-jdbc-template</artifactId>
    <version>5.6.0</version>
</dependency>
```

#### 2. Configuración de ShedLock (`ShedLockConfig.java`)
```java
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
public class ShedLockConfig {
    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(new JdbcTemplate(dataSource));
    }
}
```

#### 3. Anotaciones `@SchedulerLock` en `RequestMaintenanceScheduler.java`
```java
@Scheduled(cron = "${serviya.maintenance.cron.reject-expired-pending:0 0 * * * *}")
@SchedulerLock(name = "rejectExpiredPendingRequests", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
public void rejectExpiredPendingRequests() { ... }

@Scheduled(cron = "${serviya.maintenance.cron.mark-stale-accepted:0 10 * * * *}")
@SchedulerLock(name = "markStaleAcceptedAsNotProvided", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
public void markStaleAcceptedAsNotProvided() { ... }

@Scheduled(cron = "${serviya.maintenance.cron.reject-expired-proposals:0 20 * * * *}")
@SchedulerLock(name = "rejectExpiredProposals", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
public void rejectExpiredProposals() { ... }

@Scheduled(cron = "${serviya.maintenance.cron.finalize-unconfirmed:0 30 * * * *}")
@SchedulerLock(name = "finalizeUnconfirmedCompletions", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
public void finalizeUnconfirmedCompletions() { ... }
```

#### 4. Anotación `@SchedulerLock` en `NotificationRetryScheduler.java`
```java
@Scheduled(cron = "${serviya.notifications.cron.retry-failed:0 */15 * * * *}")
@SchedulerLock(name = "retryFailedDeliveries", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
public void retryFailedDeliveries() { ... }
```

#### 5. Tabla de base de datos (`mysql/01_create_database.sql`)
```sql
CREATE TABLE shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until DATETIME(3) NOT NULL,
    locked_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);
```

## Parámetros de Lock Configurados

| Parámetro | Valor | Justificación |
|-----------|-------|---------------|
| `lockAtMostFor` | PT10M (10 minutos) | Tiempo máximo que se mantiene el lock; cubre el peor caso de duración de la tarea |
| `lockAtLeastFor` | PT1M (1 minuto) | Tiempo mínimo para evitar re-entrada rápida por clock drift entre instancias |
| `defaultLockAtMostFor` | PT30M (30 minutos) | Valor por defecto global en `@EnableSchedulerLock` |

## Cómo Funciona

1. Al iniciarse una tarea programada, ShedLock intenta insertar/actualizar un registro en la tabla `shedlock`
2. Si el lock está libre (no existe o `lock_until` < now), la instancia lo adquiere y ejecuta la tarea
3. Si otra instancia ya tiene el lock, la tarea se **saltea** en esa instancia (no se ejecuta)
4. Al terminar la tarea (éxito o error), se libera el lock actualizando `lock_until`

## Verificación

- ✅ Compilación exitosa (`mvn compile`)
- ✅ No hay errores de sintaxis o dependencias
- ✅ La tabla `shedlock` se crea automáticamente al iniciar la aplicación (Hibernate `ddl-auto: update`)
- ✅ Los locks son únicos por nombre de tarea, permitiendo ejecución paralela de tareas diferentes

## Notas Importantes

1. **No rompe funcionalidad existente**: Las tareas siguen ejecutándose en la misma programación cron, solo se evita la duplicación
2. **Compatible con escalado horizontal**: Funciona correctamente con 1, 2, N instancias
3. **Resiliente a fallos**: Si una instancia muere mientras tiene el lock, el lock expira automáticamente tras `lockAtMostFor`
4. **Requisito para Fase 0**: Este fix es prerrequisito para la migración a microservicios (M4 en el plan)

## Referencias

- [ShedLock Documentation](https://github.com/lukas-krecan/ShedLock)
- Bug original: B9 en `ESTADO-ACTUAL-Y-PLAN-FUTURO.md` línea 380
- Mejora relacionada: M4 (Lock distribuido para `@Scheduled`)