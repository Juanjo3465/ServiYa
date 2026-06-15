import React from 'react';
import { useNavigate } from 'react-router-dom';
import { DashboardLayout, CLIENT_NAV } from '../../../../shared';
import { MonthCalendar } from '../../components/MonthCalendar/MonthCalendar';

const DAYS = [
    {
        title: 'Lunes 12 de mayo',
        events: [{ time: '9:00\nAM', name: 'Reparación de tuberías', sub: 'Carlos M. · Calle 45 #12-34', badge: 'badge-warn', label: 'Pendiente', detail: true }],
    },
    {
        title: 'Miércoles 14 de mayo',
        events: [{ time: '10:00\nAM', name: 'Limpieza de hogar', sub: 'María L. · Carrera 7 #80-21', badge: 'badge-success', label: 'Confirmada' }],
    },
    {
        title: 'Viernes 16 de mayo',
        events: [{ time: '2:00\nPM', name: 'Instalación eléctrica', sub: 'Ana R. · Calle 45 #12-34', badge: 'badge-success', label: 'Confirmada' }],
    },
];

export function ClientSchedulePage() {
    const navigate = useNavigate();

    return (
        <DashboardLayout sections={CLIENT_NAV} avatar="JP">
            <div className="ph"><h1>Mi agenda</h1><p>Visualiza y organiza tus servicios programados</p></div>
            <div className="g2" style={{ gap: '20px', alignItems: 'start' }}>
                <MonthCalendar />
                <div>
                    {DAYS.map((d, i) => (
                        <React.Fragment key={i}>
                            <div className="ev-daytitle">{d.title}</div>
                            {d.events.map((e, j) => (
                                <div className="ev-item" key={j}>
                                    <div className="ev-time">{e.time.split('\n').map((t, k) => <React.Fragment key={k}>{t}{k === 0 && <br />}</React.Fragment>)}</div>
                                    <div style={{ flex: 1 }}>
                                        <div style={{ fontSize: '13px', fontWeight: 700 }}>{e.name}</div>
                                        <div style={{ fontSize: '12px', color: 'var(--c-mid)', marginTop: '2px' }}>{e.sub}</div>
                                        <div style={{ marginTop: '7px', display: 'flex', gap: '8px', alignItems: 'center' }}>
                                            <span className={`badge ${e.badge}`}>{e.label}</span>
                                            {e.detail && <a style={{ fontSize: '11px', color: 'var(--c-primary)', cursor: 'pointer', fontWeight: 600 }} onClick={() => navigate('/services/1')}>Ver detalle →</a>}
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </React.Fragment>
                    ))}
                </div>
            </div>
        </DashboardLayout>
    );
}

export default ClientSchedulePage;
