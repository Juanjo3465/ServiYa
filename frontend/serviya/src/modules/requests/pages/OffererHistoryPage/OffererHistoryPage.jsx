import { DashboardLayout, Stars, OFFERER_NAV } from '../../../../shared';

const ROWS = [
    { service: 'Reparación de tuberías', initials: 'JP', client: 'Juan P.', date: '5 mayo 2025', status: 'Completado', badge: 'badge-success', price: '$75.000', rating: 5 },
    { service: 'Instalación de grifos', initials: 'SR', client: 'Sandra R.', date: '2 mayo 2025', status: 'Completado', badge: 'badge-success', price: '$90.000', rating: 4 },
    { service: 'Destape de cañerías', initials: 'MV', client: 'Mario V.', date: '28 abril 2025', status: 'Cancelado', badge: 'badge-danger', price: null, rating: null },
    { service: 'Reparación calentador', initials: 'LC', client: 'Laura C.', date: '20 abril 2025', status: 'Completado', badge: 'badge-success', price: '$150.000', rating: 5 },
];

export function OffererHistoryPage() {
    return (
        <DashboardLayout sections={OFFERER_NAV} avatar="CM">
            <div className="ph"><h1>Historial de servicios prestados</h1><p>Registro completo de tu actividad como oferente</p></div>
            <div className="tbl-wrap">
                <table>
                    <thead><tr><th>Servicio</th><th>Cliente</th><th>Fecha</th><th>Estado</th><th>Precio</th><th>Cal. recibida</th><th>Acciones</th></tr></thead>
                    <tbody>
                        {ROWS.map((r, i) => (
                            <tr key={i}>
                                <td><strong>{r.service}</strong></td>
                                <td><div style={{ display: 'flex', alignItems: 'center', gap: '7px' }}><div className="av av-xs">{r.initials}</div>{r.client}</div></td>
                                <td style={{ fontSize: '12px', color: 'var(--c-soft)' }}>{r.date}</td>
                                <td><span className={`badge ${r.badge}`}>{r.status}</span></td>
                                <td style={r.price ? { fontWeight: 600, color: 'var(--c-primary-d)' } : { color: 'var(--c-soft)' }}>{r.price || '—'}</td>
                                <td>{r.rating ? <Stars rating={r.rating} size={12} /> : <span style={{ color: 'var(--c-soft)' }}>—</span>}</td>
                                <td><button className="btn btn-ghost btn-sm" style={{ border: '1px solid var(--c-border)' }}>Ver</button></td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </DashboardLayout>
    );
}

export default OffererHistoryPage;
