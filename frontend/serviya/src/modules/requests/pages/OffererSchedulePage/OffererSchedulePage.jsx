import React from 'react';
import { DashboardLayout, Icon, ToastContainer, useToast, OFFERER_NAV } from '../../../../shared';
import { MonthCalendar } from '../../components/MonthCalendar/MonthCalendar';

const DAYS = [
    {
        title: 'Lunes 12 de mayo',
        events: [{ hour: '9:00', ampm: 'AM', name: 'Reparación de tuberías', sub: 'Juan P. · Calle 45 #12-34', phone: '+57 310 555 1234', badge: 'badge-warn', label: 'Pendiente aceptar', accept: true }],
    },
    {
        title: 'Miércoles 14 de mayo',
        events: [{ hour: '10:00', ampm: 'AM', name: 'Reparación de tuberías', sub: 'Sandra R. · Carrera 7 #80-21', badge: 'badge-success', label: 'Aceptada' }],
    },
    {
        title: 'Viernes 16 de mayo',
        events: [{ hour: '2:00', ampm: 'PM', name: 'Destape de cañerías', sub: 'Mario V. · Usaquén', badge: 'badge-success', label: 'Aceptada' }],
    },
];

export function OffererSchedulePage() {
    const { toasts, showToast } = useToast();

    return (
        <DashboardLayout sections={OFFERER_NAV} avatar="CM">
            <div className="ph"><h1>Mi agenda</h1><p>Servicios programados para atender</p></div>
            <div className="g2" style={{ gap: '20px', alignItems: 'start' }}>
                <MonthCalendar />
                <div>
                    {DAYS.map((d, i) => (
                        <React.Fragment key={i}>
                            <div className="ev-daytitle">{d.title}</div>
                            {d.events.map((e, j) => (
                                <div className="ev-item" key={j}>
                                    <div className="ev-time">{e.hour}<br />{e.ampm}</div>
                                    <div style={{ flex: 1 }}>
                                        <div style={{ fontSize: '13px', fontWeight: 700 }}>{e.name}</div>
                                        <div style={{ fontSize: '12px', color: 'var(--c-mid)', marginTop: '2px' }}>{e.sub}</div>
                                        {e.phone && <div style={{ fontSize: '11px', color: 'var(--c-mid)', marginTop: '3px', display: 'flex', alignItems: 'center', gap: '4px' }}><Icon name="phone" size={11} />{e.phone}</div>}
                                        <div style={{ marginTop: '7px', display: 'flex', gap: '5px', flexWrap: 'wrap', alignItems: 'center' }}>
                                            <span className={`badge ${e.badge}`}>{e.label}</span>
                                            {e.accept && <button className="btn btn-success btn-sm" onClick={() => showToast('Aceptada. Juan P. notificado', 'success')}><Icon name="check" size={13} />Aceptar</button>}
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
