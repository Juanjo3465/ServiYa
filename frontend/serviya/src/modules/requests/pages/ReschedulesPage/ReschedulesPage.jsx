import { DashboardLayout, Icon, ToastContainer, useToast, CLIENT_NAV } from '../../../../shared';

import './ReschedulesPage.css';

const HISTORY = [
    { service: 'Limpieza de hogar', offerer: 'María L.', from: '5 mayo 10am', to: '7 mayo 9am', status: 'Aceptada', badge: 'badge-success' },
    { service: 'Instalación eléctrica', offerer: 'Ana R.', from: '20 abril 2pm', to: '22 abril 3pm', status: 'Rechazada', badge: 'badge-danger' },
];

export function ReschedulesPage() {
    const { toasts, showToast } = useToast();

    return (
        <DashboardLayout sections={CLIENT_NAV} avatar="JP">
            <div className="ph"><h1>Propuestas de reprogramación</h1><p>Responde a las propuestas enviadas por los oferentes</p></div>

            <div style={{ fontSize: '14px', fontWeight: 700, marginBottom: '12px' }}>Pendientes <span className="badge badge-warn">1</span></div>
            <div className="resched-pending">
                <div className="rp-head">
                    <div className="av av-md">CM</div>
                    <div><div style={{ fontWeight: 700, fontSize: '14px' }}>Carlos Martínez</div><div style={{ fontSize: '12px', color: 'var(--c-mid)' }}>Reparación de tuberías · Solicitud #SR-4821</div></div>
                    <span className="badge badge-warn" style={{ marginLeft: 'auto' }}>Requiere acción</span>
                </div>
                <div className="rp-change">
                    <div style={{ fontSize: '12px', color: 'var(--c-mid)', marginBottom: '6px' }}>Propone cambiar de:</div>
                    <div className="rp-dates">
                        <div className="rp-date"><strong>Lun 12 mayo</strong><br /><span style={{ color: 'var(--c-soft)', fontSize: '11px' }}>9:00 AM</span></div>
                        <Icon name="arrowRight" size={16} style={{ color: 'var(--c-warn)' }} />
                        <div className="rp-date rp-date-new"><strong>Mar 13 mayo</strong><br /><span style={{ color: 'var(--c-primary-d)', fontSize: '11px' }}>10:00 AM</span></div>
                    </div>
                    <div style={{ fontSize: '12px', color: 'var(--c-mid)', marginTop: '10px', fontStyle: 'italic' }}>"Surgió un compromiso previo para ese día"</div>
                </div>
                <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                    <button className="btn btn-success" onClick={() => showToast('Propuesta aceptada. Servicio reprogramado', 'success')}><Icon name="check" size={15} />Aceptar propuesta</button>
                    <button className="btn btn-danger" onClick={() => showToast('Propuesta rechazada', 'danger')}><Icon name="close" size={15} />Rechazar</button>
                    <button className="btn btn-ghost" style={{ border: '1px solid var(--c-border)' }} onClick={() => showToast('Solicitud cancelada', 'danger')}>Cancelar solicitud</button>
                </div>
            </div>

            <div style={{ fontSize: '14px', fontWeight: 700, marginBottom: '12px' }}>Historial de propuestas</div>
            <div className="tbl-wrap">
                <table>
                    <thead><tr><th>Servicio</th><th>Oferente</th><th>Fecha original</th><th>Fecha propuesta</th><th>Estado</th></tr></thead>
                    <tbody>
                        {HISTORY.map((h, i) => (
                            <tr key={i}>
                                <td>{h.service}</td>
                                <td>{h.offerer}</td>
                                <td style={{ fontSize: '12px', color: 'var(--c-soft)' }}>{h.from}</td>
                                <td style={{ fontSize: '12px' }}>{h.to}</td>
                                <td><span className={`badge ${h.badge}`}>{h.status}</span></td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
            <ToastContainer toasts={toasts} />
        </DashboardLayout>
    );
}

export default ReschedulesPage;
