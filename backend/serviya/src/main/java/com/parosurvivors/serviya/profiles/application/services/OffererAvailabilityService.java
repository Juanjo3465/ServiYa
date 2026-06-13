package com.parosurvivors.serviya.profiles.application.services;

import com.parosurvivors.serviya.profiles.application.dto.command.SetAvailabilitySlotCommand;
import com.parosurvivors.serviya.profiles.application.mappers.OffererAvailabilityCommandMapper;
import com.parosurvivors.serviya.profiles.application.ports.input.OffererAvailabilityServicePort;
import com.parosurvivors.serviya.profiles.application.ports.output.OffererAvailabilityPersistencePort;
import com.parosurvivors.serviya.profiles.domain.OffererAvailability;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OffererAvailabilityService implements OffererAvailabilityServicePort {

    private final OffererAvailabilityPersistencePort offererAvailabilityPersistencePort;
    private final OffererAvailabilityCommandMapper commandMapper;

    @Override
    public List<OffererAvailability> getSchedule(Long offererId) {
        return offererAvailabilityPersistencePort.findByOffererId(offererId);
    }

    @Override
    @Transactional
    public void setSchedule(Long offererId, List<SetAvailabilitySlotCommand> slots) {
        log.info("ENTERING setSchedule");
        List<OffererAvailability> availabilities = commandMapper.toDomain(slots);
        log.info("Mapped");
        availabilities.forEach(a -> a.setOffererId(offererId));
        
        validateAvailabilities(availabilities);

        log.info("Validated");

        List<OffererAvailability> normalized = normalizeSchedule(availabilities);
        log.info("Normalized");

        offererAvailabilityPersistencePort.deleteByOffererId(offererId);
        log.info("Deleted old schedule");
        offererAvailabilityPersistencePort.saveAll(normalized);
        log.info("Saved new schedule");
    }

    @Override
    public void deleteSlot(Long slotId) {
        offererAvailabilityPersistencePort.deleteById(slotId);
    }

    @Override
    public void activateSlot(Long slotId) {

        OffererAvailability slot = offererAvailabilityPersistencePort.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Availability slot not found"));

        slot.activate();

        offererAvailabilityPersistencePort.update(slot);
    }

    @Override
    public void deactivateSlot(Long slotId) {

        OffererAvailability slot = offererAvailabilityPersistencePort.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Availability slot not found"));

        slot.deactivate();

        offererAvailabilityPersistencePort.update(slot);
    }

    private void validateAvailabilities(List<OffererAvailability> availabilities) {

        for (OffererAvailability availability : availabilities) {

            if (availability.getWeekDay() == null
                    || availability.getWeekDay() < 0
                    || availability.getWeekDay() > 6) {
                throw new IllegalArgumentException("Invalid weekday");
            }

            if (!availability.isValidRange()) {
                throw new IllegalArgumentException("Invalid availability range");
            }
        }
    }

    private List<OffererAvailability> normalizeSchedule(List<OffererAvailability> availabilities) {

        List<OffererAvailability> activeSlots = availabilities.stream()
                .filter(a -> Boolean.TRUE.equals(a.getActive()))
                .toList();

        List<OffererAvailability> inactiveSlots = availabilities.stream()
                .filter(a -> !Boolean.TRUE.equals(a.getActive()))
                .toList();
        Map<Integer, List<OffererAvailability>> activeSlotsByDay = activeSlots.stream()
                .collect(Collectors.groupingBy(OffererAvailability::getWeekDay));

        List<OffererAvailability> normalized = new ArrayList<>();

        for (List<OffererAvailability> daySlots : activeSlotsByDay.values()) {
            normalized.addAll(mergeDaySlots(daySlots));
        }

        normalized.addAll(inactiveSlots);

        return normalized;
    }

    private List<OffererAvailability> mergeDaySlots(List<OffererAvailability> slots) {

        if (slots.isEmpty()) {
            return List.of();
        }

        List<OffererAvailability> sorted = slots.stream()
                .sorted(Comparator.comparing(OffererAvailability::getStartTime))
                .toList();

        List<OffererAvailability> merged = new ArrayList<>();

        OffererAvailability current = copy(sorted.get(0));

        for (int i = 1; i < sorted.size(); i++) {

            OffererAvailability next = sorted.get(i);

            boolean overlapsOrTouches =
                    !next.getStartTime().isAfter(current.getEndTime());

            if (overlapsOrTouches) {

                LocalTime maxEnd = current.getEndTime().isAfter(next.getEndTime())
                        ? current.getEndTime()
                        : next.getEndTime();

                current.setEndTime(maxEnd);

            } else {

                merged.add(current);
                current = copy(next);
            }
        }

        merged.add(current);

        return merged;
    }

    private OffererAvailability copy(OffererAvailability source) {
    return OffererAvailability.builder()
            .offererId(source.getOffererId())
            .weekDay(source.getWeekDay())
            .startTime(source.getStartTime())
            .endTime(source.getEndTime())
            .active(source.getActive() != null ? source.getActive() : true) // merged slots are always active
            .build();
}
}