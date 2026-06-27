import { useState, useEffect } from "react";
import { DashboardLayout, Icon, ToastContainer, useToast, OFFERER_NAV } from '../../../../shared';

import './AvailabilityPage.css';

const WEEKDAYS = [
    { weekDay: 0, label: 'Lunes' },
    { weekDay: 1, label: 'Martes' },
    { weekDay: 2, label: 'Miércoles' },
    { weekDay: 3, label: 'Jueves' },
    { weekDay: 4, label: 'Viernes' },
    { weekDay: 5, label: 'Sábado' },
    { weekDay: 6, label: 'Domingo' },
];

const SERVICE_SLOTS = ['9–10am', '10–11am', '11–12pm', '2–3pm', '3–4pm'];
const SERVICE_DAYS = [
    { day: 'Lunes', on: [true, true, true, false, true] },
    { day: 'Martes', on: [true, false, true, true, false] },
    { day: 'Miércoles', on: [false, false, false, false, false] },
];

function Slot({ label, on }) {
    const [active, setActive] = useState(on);
    return <span className={`slot-btn ${active ? 'on' : ''}`} onClick={() => setActive((v) => !v)}>{label}</span>;
}

function ServiceDayRow({ day, slots, on }) {
    return (
        <div className="day-row">
            <div className="day-inner">
                <span className="day-name">{day}</span>
                <div className="slot-list">
                    {slots.map((label, i) => (
                        <Slot key={label} label={label} on={on[i]} />
                    ))}
                </div>
            </div>
        </div>
    );
}

// "HH:MM:SS" or "HH:MM" -> "HH:MM" (what <input type="time"> needs)
function toInputTime(localTime) {
    return localTime ? localTime.slice(0, 5) : '';
}

// "HH:MM" -> "HH:MM:00" (what the backend's LocalTime expects)
function toBackendTime(inputTime) {
    return inputTime.length === 5 ? `${inputTime}:00` : inputTime;
}

let nextTempId = -1;

function GeneralDayRow({ weekDay, label, slots, onAddSlot, onRemoveSlot, onChangeSlot }) {
    return (
        <div className="day-row">
            <div className="day-inner">
                <span className="day-name">{label}</span>
                <div className="slot-rows">
                    {slots.length === 0 && (
                        <span className="slot-empty-day">Sin disponibilidad</span>
                    )}
                    {slots.map((slot) => (
                        <div className="slot-range-row" key={slot.id}>
                            <input
                                type="time"
                                className="slot-time-input"
                                value={slot.startTime}
                                onChange={(e) => onChangeSlot(weekDay, slot.id, 'startTime', e.target.value)}
                            />
                            <span className="slot-range-sep">a</span>
                            <input
                                type="time"
                                className="slot-time-input"
                                value={slot.endTime}
                                onChange={(e) => onChangeSlot(weekDay, slot.id, 'endTime', e.target.value)}
                            />
                            <button
                                type="button"
                                className="slot-remove-btn"
                                onClick={() => onRemoveSlot(weekDay, slot.id)}
                                aria-label="Eliminar franja"
                            >
                                <Icon name="x" size={13} />
                            </button>
                        </div>
                    ))}
                    <button type="button" className="slot-add-btn" onClick={() => onAddSlot(weekDay)}>
                        <Icon name="plus" size={13} />
                        Agregar franja
                    </button>
                </div>
            </div>
        </div>
    );
}

export function AvailabilityPage() {
    const { toasts, showToast } = useToast();
    const [tab, setTab] = useState(0);

    // { [weekDay]: [{ id, startTime, endTime }, ...] }
    const [schedule, setSchedule] = useState(
        Object.fromEntries(WEEKDAYS.map((d) => [d.weekDay, []]))
    );
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetch('http://localhost:8080/api/v1/offerers/me/availability')
            .then((response) => response.json())
            .then((data) => {
                const grouped = Object.fromEntries(WEEKDAYS.map((d) => [d.weekDay, []]));
                for (const slot of data) {
                    if (slot.active === false) continue; // only show active slots in the editor
                    grouped[slot.weekDay].push({
                        id: slot.id,
                        startTime: toInputTime(slot.startTime),
                        endTime: toInputTime(slot.endTime),
                    });
                }
                setSchedule(grouped);
            })
            .finally(() => setLoading(false));
    }, []);

    function handleAddSlot(weekDay) {
        setSchedule((prev) => ({
            ...prev,
            [weekDay]: [...prev[weekDay], { id: nextTempId--, startTime: '08:00', endTime: '10:00' }],
        }));
    }

    function handleRemoveSlot(weekDay, slotId) {
        setSchedule((prev) => ({
            ...prev,
            [weekDay]: prev[weekDay].filter((s) => s.id !== slotId),
        }));
    }

    function handleChangeSlot(weekDay, slotId, field, value) {
        setSchedule((prev) => ({
            ...prev,
            [weekDay]: prev[weekDay].map((s) =>
                s.id === slotId ? { ...s, [field]: value } : s
            ),
        }));
    }

    function handleSaveGeneralSchedule() {
        const allSlots = Object.entries(schedule).flatMap(([weekDay, slots]) =>
            slots.map((s) => ({
                weekDay: Number(weekDay),
                startTime: toBackendTime(s.startTime),
                endTime: toBackendTime(s.endTime),
                active: true,
            }))
        );

        const invalid = allSlots.some((s) => !s.startTime || !s.endTime || s.startTime >= s.endTime);
        if (invalid) {
            showToast('Revisa que cada franja tenga hora de inicio antes que de fin', 'error');
            return;
        }

        fetch('http://localhost:8080/api/v1/offerers/me/availability', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(allSlots),
        })
            .then((response) => {
                if (!response.ok) throw new Error('Save failed');
                showToast('Horario general guardado', 'success');
            })
            .catch(() => showToast('No se pudo guardar el horario', 'error'));
    }

    return (
        <DashboardLayout sections={OFFERER_NAV} avatar="CM">
            <div className="ph"><h1>Gestión de disponibilidad</h1><p>Configura tu horario general y por servicio</p></div>

            <div className="tabs">
                <div className={`tab ${tab === 0 ? 'active' : ''}`} onClick={() => setTab(0)}>Horario general (RF-072)</div>
                <div className={`tab ${tab === 1 ? 'active' : ''}`} onClick={() => setTab(1)}>Por servicio (RF-021)</div>
            </div>

            {tab === 0 && (
                <div>
                    <div className="avail-banner">
                        <strong>Plantilla semanal general</strong> — Define los días y horarios en que normalmente estás disponible. Puedes usarla como punto de partida al configurar la disponibilidad de cada servicio.
                    </div>
                    {loading && <p style={{ fontSize: '13px', color: 'var(--c-mid)' }}>Cargando horario...</p>}
                    {!loading && WEEKDAYS.map((d) => (
                        <GeneralDayRow
                            key={d.weekDay}
                            weekDay={d.weekDay}
                            label={d.label}
                            slots={schedule[d.weekDay]}
                            onAddSlot={handleAddSlot}
                            onRemoveSlot={handleRemoveSlot}
                            onChangeSlot={handleChangeSlot}
                        />
                    ))}
                    <div style={{ marginTop: '16px', display: 'flex', gap: '8px' }}>
                        <button className="btn btn-primary" onClick={handleSaveGeneralSchedule}>
                            <Icon name="save" size={15} />
                            Guardar horario general
                        </button>
                    </div>
                </div>
            )}

            {tab === 1 && (
                <div>
                    <div className="input-group" style={{ marginBottom: '16px' }}>
                        <label className="label">Servicio a configurar</label>
                        <select className="input"><option>Reparación de tuberías</option><option>Destape de cañerías</option><option>Instalación de grifos</option></select>
                    </div>
                    <div style={{ display: 'flex', gap: '8px', marginBottom: '16px' }}>
                        <button className="btn btn-ghost btn-sm" style={{ border: '1px solid var(--c-border)' }} onClick={() => showToast('Horario general aplicado', 'success')}><Icon name="reschedule" size={13} />Aplicar plantilla general</button>
                    </div>
                    {SERVICE_DAYS.map((d) => (
                        <ServiceDayRow key={d.day} day={d.day} slots={SERVICE_SLOTS} on={d.on} />
                    ))}
                    <div style={{ marginTop: '16px' }}>
                        <button className="btn btn-primary" onClick={() => showToast('Disponibilidad del servicio guardada', 'success')}><Icon name="save" size={15} />Guardar disponibilidad</button>
                    </div>
                </div>
            )}
            <ToastContainer toasts={toasts} />
        </DashboardLayout>
    );
}

export default AvailabilityPage;
