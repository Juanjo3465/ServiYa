import { useState } from "react";
import { useNavigate } from 'react-router-dom';
import { DashboardLayout, Stars, CLIENT_NAV } from '../../../../shared';

const TABS = ['Todos (18)', 'Completados (15)', 'Cancelados (2)', 'Otros (1)'];

const ROWS = [
    { service: 'Reparación de tuberías', initials: 'CM', offerer: 'Carlos M.', date: '5 mayo 2025', status: 'Completado', badge: 'badge-success', price: '$75.000', rating: 5 },
    { service: 'Limpieza de hogar', initials: 'ML', offerer: 'María L.', date: '28 abril 2025', status: 'Completado', badge: 'badge-success', price: '$60.000', rating: 5 },
    { service: 'Instalación eléctrica', initials: 'AR', offerer: 'Ana R.', date: '20 abril 2025', status: 'Completado', badge: 'badge-success', price: '$120.000', rating: 4 },
    { service: 'Poda de jardín', initials: 'PG', offerer: 'Pedro G.', date: '12 abril 2025', status: 'Cancelado', badge: 'badge-danger', price: null, rating: null },
    { service: 'Pintura de sala', initials: 'JV', offerer: 'Jorge V.', date: '3 abril 2025', status: 'Completado', badge: 'badge-success', price: '$200.000', rating: 5 },
    { service: 'Destape de cañerías', initials: 'LR', offerer: 'Luis R.', date: '25 marzo 2025', status: 'No prestado', badge: 'badge-warn', price: null, rating: null },
];

export function ClientHistoryPage() {
    const navigate = useNavigate();
    const [tab, setTab] = useState(0);

    return (
        <DashboardLayout sections={CLIENT_NAV} avatar="JP">
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px', flexWrap: 'wrap', gap: '10px' }}>
                <div className="ph" style={{ margin: 0 }}><h1>Historial de servicios</h1><p>Todos los servicios contratados anteriormente</p></div>
                <select className="input" style={{ width: 'auto', padding: '8px 12px', fontSize: '13px' }}>
                    <option>Todos los estados</option><option>Completados</option><option>Cancelados</option><option>Reprogramados</option><option>No prestado</option>
                </select>
            </div>

            <div className="tabs">
                {TABS.map((t, i) => (
                    <div key={t} className={`tab ${tab === i ? 'active' : ''}`} onClick={() => setTab(i)}>{t}</div>
                ))}
            </div>

            <div className="tbl-wrap">
                <table>
                    <thead><tr><th>Servicio</th><th>Oferente</th><th>Fecha</th><th>Estado</th><th>Precio</th><th>Calificación</th><th>Acciones</th></tr></thead>
                    <tbody>
                        {ROWS.map((r, i) => (
                            <tr key={i}>
                                <td><strong>{r.service}</strong></td>
                                <td><div style={{ display: 'flex', alignItems: 'center', gap: '7px' }}><div className="av av-xs">{r.initials}</div>{r.offerer}</div></td>
                                <td style={{ fontSize: '12px', color: 'var(--c-soft)' }}>{r.date}</td>
                                <td><span className={`badge ${r.badge}`}>{r.status}</span></td>
                                <td style={r.price ? { fontWeight: 600, color: 'var(--c-primary-d)' } : { color: 'var(--c-soft)' }}>{r.price || '—'}</td>
                                <td>{r.rating ? <Stars rating={r.rating} size={12} /> : <span style={{ color: 'var(--c-soft)' }}>—</span>}</td>
                                <td><button className="btn btn-ghost btn-sm" style={{ border: '1px solid var(--c-border)' }} onClick={() => navigate('/services/1')}>Ver</button></td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </DashboardLayout>
    );
}

export default ClientHistoryPage;
