import { useState, useEffect, useCallback } from "react";
import { DashboardLayout, Icon, ToastContainer, useToast, CLIENT_NAV } from '../../../../shared';
import { proposalApi } from '../../../../shared/api';
import { formatDate } from '../../utils';

import './ReschedulesPage.css';

const PROPOSAL_STATUS_MAP = {
    PENDING:    { label: 'Pendiente',  badge: 'badge-warn' },
    ACCEPTED:   { label: 'Aceptada',   badge: 'badge-success' },
    REJECTED:   { label: 'Rechazada',  badge: 'badge-danger' },
    CANCELLED:  { label: 'Cancelada',  badge: 'badge-gray' },
    SUPERSEDED: { label: 'Reemplazada', badge: 'badge-gray' },
};

export function ReschedulesPage() {
    const { toasts, showToast } = useToast();
    const [pending, setPending] = useState([]);
    const [history, setHistory] = useState([]);
    const [loading, setLoading] = useState(true);

    const fetchProposals = useCallback(async () => {
        setLoading(true);
        try {
            const [pendingRes, historyRes] = await Promise.all([
                proposalApi.getReceived({ page: 0, size: 20, statuses: 'PENDING' }),
                proposalApi.getReceived({ page: 0, size: 50 }),
            ]);
            setPending(pendingRes.content ?? []);
            setHistory((historyRes.content ?? []).filter(p => p.status !== 'PENDING'));
        } catch {
            showToast('No se pudieron cargar las propuestas', 'danger');
        } finally {
            setLoading(false);
        }
    }, [showToast]);

    useEffect(() => { fetchProposals(); }, [fetchProposals]);

    const handleAccept = async (proposal) => {
        try {
            await proposalApi.acceptProposal(proposal.proposalId);
            showToast('Propuesta aceptada. Servicio reprogramado', 'success');
            fetchProposals();
        } catch (err) {
            showToast(err.message || 'No se pudo aceptar la propuesta', 'danger');
        }
    };

    const handleReject = async (proposal) => {
        try {
            await proposalApi.rejectProposal(proposal.proposalId);
            showToast('Propuesta rechazada', 'success');
            fetchProposals();
        } catch (err) {
            showToast(err.message || 'No se pudo rechazar la propuesta', 'danger');
        }
    };

    return (
        <DashboardLayout sections={CLIENT_NAV} avatar="JP">
            <div className="ph"><h1>Propuestas de reprogramación</h1><p>Responde a las propuestas enviadas por los oferentes</p></div>

            {loading ? (
                <div style={{ textAlign: 'center', padding: '40px 0', color: 'var(--c-soft)' }}>Cargando propuestas...</div>
            ) : (
                <>
                    {pending.length > 0 && (
                        <>
                            <div style={{ fontSize: '14px', fontWeight: 700, marginBottom: '12px' }}>Pendientes <span className="badge badge-warn">{pending.length}</span></div>
                            {pending.map((p) => (
                                <div className="resched-pending" key={p.proposalId}>
                                    <div className="rp-head">
                                        <div className="av av-md">{(p.counterpartyName || '??').split(' ').map(w => w[0]).join('').slice(0, 2).toUpperCase()}</div>
                                        <div><div style={{ fontWeight: 700, fontSize: '14px' }}>{p.counterpartyName}</div><div style={{ fontSize: '12px', color: 'var(--c-mid)' }}>{p.serviceTitle}</div></div>
                                        <span className="badge badge-warn" style={{ marginLeft: 'auto' }}>Requiere acción</span>
                                    </div>
                                    <div className="rp-change">
                                        <div style={{ fontSize: '12px', color: 'var(--c-mid)', marginBottom: '6px' }}>Propone cambiar de:</div>
                                        <div className="rp-dates">
                                            <div className="rp-date"><strong>{formatDate(p.originalScheduledDate)}</strong></div>
                                            <Icon name="arrowRight" size={16} style={{ color: 'var(--c-warn)' }} />
                                            <div className="rp-date rp-date-new"><strong>{formatDate(p.proposedDate)}</strong></div>
                                        </div>
                                        {p.reason && <div style={{ fontSize: '12px', color: 'var(--c-mid)', marginTop: '10px', fontStyle: 'italic' }}>"{p.reason}"</div>}
                                    </div>
                                    <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                                        <button className="btn btn-success" onClick={() => handleAccept(p)}><Icon name="check" size={15} />Aceptar propuesta</button>
                                        <button className="btn btn-danger" onClick={() => handleReject(p)}><Icon name="close" size={15} />Rechazar</button>
                                    </div>
                                </div>
                            ))}
                        </>
                    )}

                    {pending.length === 0 && !loading && (
                        <div style={{ textAlign: 'center', padding: '40px 0', color: 'var(--c-soft)' }}>No tienes propuestas pendientes</div>
                    )}

                    {history.length > 0 && (
                        <>
                            <div style={{ fontSize: '14px', fontWeight: 700, marginBottom: '12px' }}>Historial de propuestas</div>
                            <div className="tbl-wrap">
                                <table>
                                    <thead><tr><th>Servicio</th><th>Oferente</th><th>Fecha original</th><th>Fecha propuesta</th><th>Estado</th></tr></thead>
                                    <tbody>
                                        {history.map((h) => {
                                            const st = PROPOSAL_STATUS_MAP[h.status] || { label: h.status, badge: '' };
                                            return (
                                                <tr key={h.proposalId}>
                                                    <td>{h.serviceTitle}</td>
                                                    <td>{h.counterpartyName}</td>
                                                    <td style={{ fontSize: '12px', color: 'var(--c-soft)' }}>{formatDate(h.originalScheduledDate)}</td>
                                                    <td style={{ fontSize: '12px' }}>{formatDate(h.proposedDate)}</td>
                                                    <td><span className={`badge ${st.badge}`}>{st.label}</span></td>
                                                </tr>
                                            );
                                        })}
                                    </tbody>
                                </table>
                            </div>
                        </>
                    )}
                </>
            )}
            <ToastContainer toasts={toasts} />
        </DashboardLayout>
    );
}

export default ReschedulesPage;
