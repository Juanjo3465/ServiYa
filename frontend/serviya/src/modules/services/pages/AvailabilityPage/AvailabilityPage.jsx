import { useState } from "react";
import { DashboardLayout, Icon, ToastContainer, useToast, OFFERER_NAV } from '../../../../shared';

import './AvailabilityPage.css';

const GENERAL_SLOTS = ['8–10am', '10–12pm', '2–4pm', '4–6pm'];
const GENERAL_DAYS = [
    { day: 'Lunes', on: [true, true, true, true] },
    { day: 'Martes', on: [true, false, true, true] },
    { day: 'Miércoles', on: [true, true, true, false] },
    { day: 'Jueves', on: [false, false, true, true] },
    { day: 'Viernes', on: [true, true, true, false] },
    { day: 'Sábado', on: [true, true, false, false] },
    { day: 'Domingo', on: [false, false, false, false] },
];

const SERVICE_SLOTS = ['9–10am', '10–11am', '11–12pm', '2–3pm', '3–4pm'];
const SERVICE_DAYS = [
    { day: 'Lunes', on: [true, true, true, false, true] },
    { day: 'Martes', on: [true, false, true, true, false] },
    { day: 'Miércoles', on: [false, false, false, false, false] },
];

// Lightweight slot button; each toggles its own on/off state.
function Slot({ label, on }) {
    const [active, setActive] = useState(on);
    return <span className={`slot-btn ${active ? 'on' : ''}`} onClick={() => setActive((v) => !v)}>{label}</span>;
}

function DayRow({ day, slots, on }) {
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

export function AvailabilityPage() {
    const { toasts, showToast } = useToast();
    const [tab, setTab] = useState(0);

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
                    {GENERAL_DAYS.map((d) => (
                        <DayRow key={d.day} day={d.day} slots={GENERAL_SLOTS} on={d.on} />
                    ))}
                    <div style={{ marginTop: '16px', display: 'flex', gap: '8px' }}>
                        <button className="btn btn-primary" onClick={() => showToast('Horario general guardado', 'success')}><Icon name="save" size={15} />Guardar horario general</button>
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
                        <DayRow key={d.day} day={d.day} slots={SERVICE_SLOTS} on={d.on} />
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
