import { useQuery } from '@tanstack/react-query';
import { DashboardLayout, Icon, navForActiveRole, reportApi, userIdFromToken } from '../../../../shared';

const TYPE_LABELS = {
    REQUEST: 'Solicitud',
    SERVICE_FEEDBACK: 'Reseña de servicio',
    CLIENT_FEEDBACK: 'Reseña de cliente',
};

const STATUS_MAP = {
    PENDING: { label: 'Pendiente', badge: 'badge-warn' },
    RESOLVED: { label: 'Resuelto', badge: 'badge-success' },
    CLOSED: { label: 'Cerrado', badge: 'badge-gray' },
};

const fmtDate = (d) => (d ? new Date(d).toLocaleDateString('es-CO', { day: 'numeric', month: 'short', year: 'numeric' }) : '—');

export function MyReportsPage() {
    const sections = navForActiveRole();
    const userId = userIdFromToken();

    // Mis reportes (M46: React Query). `enabled` gatea cuando no hay userId (sin spinner infinito).
    const reportsQuery = useQuery({
        queryKey: ['my-reports', userId],
        queryFn: () => reportApi.getSentByUser(userId),
        enabled: !!userId,
        select: (data) => (Array.isArray(data) ? data : (data.content ?? [])),
    });
    const reports = reportsQuery.data ?? [];
    const loading = reportsQuery.isLoading;

    return (
        <DashboardLayout sections={sections}>
            <div className="ph">
                <h1>Mis reportes</h1>
                <p>Reportes que has enviado y su estado</p>
            </div>

            {loading ? (
                <div style={{ textAlign: 'center', padding: '40px 0', color: 'var(--c-soft)' }}>Cargando reportes...</div>
            ) : reports.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '40px 0', color: 'var(--c-soft)' }}>
                    <Icon name="alertTriangle" size={32} />
                    <p style={{ marginTop: '8px' }}>No has enviado ningún reporte</p>
                </div>
            ) : (
                <div className="tbl-wrap">
                    <table>
                        <thead><tr><th>Tipo</th><th>Motivo</th><th>Descripción</th><th>Estado</th><th>Fecha</th></tr></thead>
                        <tbody>
                            {reports.map((r) => {
                                const st = STATUS_MAP[r.status] || { label: r.status, badge: 'badge-gray' };
                                return (
                                    <tr key={r.id}>
                                        <td><span className="badge badge-primary">{TYPE_LABELS[r.reportType] ?? r.reportType}</span></td>
                                        <td style={{ fontSize: '13px' }}>{r.category || '—'}</td>
                                        <td style={{ fontSize: '12px', color: 'var(--c-mid)', maxWidth: '280px' }}>{r.reason || '—'}</td>
                                        <td><span className={`badge ${st.badge}`}>{st.label}</span></td>
                                        <td style={{ fontSize: '12px', color: 'var(--c-soft)' }}>{fmtDate(r.createdAt)}</td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                </div>
            )}
        </DashboardLayout>
    );
}

export default MyReportsPage;
