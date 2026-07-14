import React, { useState, useEffect } from 'react';
import { DashboardLayout, Icon, ToastContainer, useToast, OFFERER_NAV, offererAgendaApi } from '../../../../shared';
import { MonthCalendar } from '../../components/MonthCalendar/MonthCalendar';
import { requestApi } from '../../../../shared/api';

const WEEKDAY_NAMES = ['domingo', 'lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado'];
const MONTH_NAMES = [
    'enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio',
    'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre',
];

const STATUS_BADGE = {
    PENDING: { className: 'badge-warn', label: 'Pendiente aceptar' },
    ACCEPTED: { className: 'badge-success', label: 'Aceptada' },
    PRESUMABLY_COMPLETED: { className: 'badge-warn', label: 'Esperando confirmación' },
};

function dayKey(date) {
    // local date key, not UTC, so it matches what the user sees on the calendar
    return `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`;
}

function formatDayTitle(date) {
    const weekday = WEEKDAY_NAMES[date.getDay()];
    const month = MONTH_NAMES[date.getMonth()];
    return `${weekday.charAt(0).toUpperCase() + weekday.slice(1)} ${date.getDate()} de ${month}`;
}

// Groups a flat list of requests (each with a `scheduledDate`) into an
// array of { date, title, events } buckets, sorted chronologically.
function groupByDay(requests) {
    const buckets = new Map();

    for (const r of requests) {
        if (!r.scheduledDate) continue;
        const date = new Date(r.scheduledDate);
        const key = dayKey(date);

        if (!buckets.has(key)) {
            buckets.set(key, { date, title: formatDayTitle(date), events: [] });
        }

        const badge = STATUS_BADGE[r.status] ?? { className: 'badge-gray', label: r.status };
        buckets.get(key).events.push({
            id: r.id,
            hour: date.toLocaleTimeString('es-CO', { hour: 'numeric', minute: '2-digit', hour12: true }),
            name: `Servicio #${r.serviceId}`,
            sub: `Cliente #${r.clientId}`,
            badgeClassName: badge.className,
            label: badge.label,
        });
    }

    return Array.from(buckets.values()).sort((a, b) => a.date - b.date);
}

export function OffererSchedulePage() {
    const { toasts, showToast } = useToast();

    const [requests, setRequests] = useState([]);
    const [selectedDate, setSelectedDate] = useState(null);

    useEffect(() => {
        offererAgendaApi
            .getOffererAgenda()
            .then((data) => {
                setRequests(data.content ?? []);
            })
            .catch((err) => {
                showToast(err.message || 'No se pudo cargar la agenda', 'error');
            });
    }, []);

    const allDays = groupByDay(requests);
    const visibleDays = selectedDate
        ? allDays.filter((d) => dayKey(d.date) === dayKey(selectedDate))
        : allDays;

    return (
        <DashboardLayout sections={OFFERER_NAV} avatar="CM">
            <div className="ph"><h1>Mi agenda</h1><p>Servicios programados para atender</p></div>
            <div className="g2" style={{ gap: '20px', alignItems: 'start' }}>
                <MonthCalendar
                    events={requests}
                    selectedDate={selectedDate}
                    onDayClick={(date) =>
                        setSelectedDate((prev) =>
                            prev && dayKey(prev) === dayKey(date) ? null : date
                        )
                    }
                />
                <div>
                    {selectedDate && (
                        <button
                            className="btn btn-ghost btn-sm"
                            style={{ marginBottom: '10px' }}
                            onClick={() => setSelectedDate(null)}
                        >
                            Ver todos los próximos servicios
                        </button>
                    )}

                    {visibleDays.length === 0 && (
                        <p style={{ fontSize: '13px', color: 'var(--c-mid)' }}>
                            {selectedDate ? 'No tienes servicios este día.' : 'No tienes servicios próximos.'}
                        </p>
                    )}

                    {visibleDays.map((d) => (
                        <React.Fragment key={dayKey(d.date)}>
                            <div className="ev-daytitle">{d.title}</div>
                            {d.events.map((e) => (
                                <div className="ev-item" key={e.id}>
                                    <div className="ev-time">{e.hour}</div>
                                    <div style={{ flex: 1 }}>
                                        <div style={{ fontSize: '13px', fontWeight: 700 }}>{e.name}</div>
                                        <div style={{ fontSize: '12px', color: 'var(--c-mid)', marginTop: '2px' }}>{e.sub}</div>
                                        <div style={{ marginTop: '7px', display: 'flex', gap: '5px', flexWrap: 'wrap', alignItems: 'center' }}>
                                            <span className={`badge ${e.badgeClassName}`}>{e.label}</span>
                                            {e.label === 'Pendiente aceptar' && (
                                                <button
                                                    className="btn btn-success btn-sm"
                                                    onClick={() => showToast('Aceptada. Cliente notificado', 'success')}
                                                >
                                                    <Icon name="check" size={13} />
                                                    Aceptar
                                                </button>
                                            )}
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </React.Fragment>
                    ))}
                </div>
            </div>
            <ToastContainer toasts={toasts} />
        </DashboardLayout>
    );
}

export default OffererSchedulePage;