import { useState } from "react";
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { DashboardLayout, Icon, Modal, ToastContainer, useToast, CLIENT_NAV } from '../../../../shared';
import { proposalApi, requestApi } from '../../../../shared/api';
import { formatDate, getInitials } from '../../utils';

import './ReschedulesPage.css';

const PROPOSAL_STATUS_MAP = {
    PENDING:    { label: 'Pendiente',  badge: 'badge-warn' },
    ACCEPTED:   { label: 'Aceptada',   badge: 'badge-success' },
    REJECTED:   { label: 'Rechazada',  badge: 'badge-danger' },
    CANCELLED:  { label: 'Cancelada',  badge: 'badge-gray' },
    SUPERSEDED: { label: 'Reemplazada', badge: 'badge-gray' },
};

export function ReschedulesPage() {
    const navigate = useNavigate();
    const { toasts, showToast } = useToast();
    // Detalle de propuesta (modal) + tercera acción del cliente: elegir nueva fecha (supera la propuesta).
    const [detail, setDetail] = useState(null);
    const [detailOpen, setDetailOpen] = useState(false);
    const [freeMode, setFreeMode] = useState(false);
    const [freeDate, setFreeDate] = useState('');
    const [freeTime, setFreeTime] = useState('09:00');

    // Propuestas recibidas (M46: React Query; una query hace el Promise.all, refetch() refresca).
    const proposalsQuery = useQuery({
        queryKey: ['client-proposals'],
        queryFn: async () => {
            const [pendingRes, historyRes] = await Promise.all([
                proposalApi.getReceived({ page: 0, size: 20, statuses: 'PENDING' }),
                proposalApi.getReceived({ page: 0, size: 50 }),
            ]);
            return {
                pending: pendingRes.content ?? [],
                history: (historyRes.content ?? []).filter(p => p.status !== 'PENDING'),
            };
        },
    });
    const pending = proposalsQuery.data?.pending ?? [];
    const history = proposalsQuery.data?.history ?? [];
    const loading = proposalsQuery.isPending;

    // M49: acciones de propuestas con useMutation + optimista. La caché ['client-proposals'] es
    // {pending, history}; al aceptar/rechazar/superar, la propuesta se mueve de pending a history con su
    // nuevo estado (onMutate), se revierte si falla (onError) y se reconcilia con el servidor (onSettled).
    const queryClient = useQueryClient();
    const moveToHistory = (proposalId, status) => {
        queryClient.setQueryData(['client-proposals'], (old) => {
            if (!old) return old;
            const p = old.pending.find((x) => x.proposalId === proposalId);
            return {
                pending: old.pending.filter((x) => x.proposalId !== proposalId),
                history: p ? [{ ...p, status }, ...old.history] : old.history,
            };
        });
    };
    const beginOptimistic = async (proposalId, status) => {
        await queryClient.cancelQueries({ queryKey: ['client-proposals'] });
        const snapshot = queryClient.getQueryData(['client-proposals']);
        moveToHistory(proposalId, status);
        return { snapshot };
    };
    const rollback = (ctx) => { if (ctx && ctx.snapshot !== undefined) queryClient.setQueryData(['client-proposals'], ctx.snapshot); };
    const reconcile = () => queryClient.invalidateQueries({ queryKey: ['client-proposals'] });

    const acceptMutation = useMutation({
        mutationFn: (proposalId) => proposalApi.acceptProposal(proposalId),
        onMutate: (proposalId) => beginOptimistic(proposalId, 'ACCEPTED'),
        onError: (err, _id, ctx) => { rollback(ctx); showToast(err.message || 'No se pudo aceptar la propuesta', 'danger'); },
        onSuccess: () => { setDetailOpen(false); showToast('Propuesta aceptada. Servicio reprogramado', 'success'); },
        onSettled: reconcile,
    });
    const rejectMutation = useMutation({
        mutationFn: (proposalId) => proposalApi.rejectProposal(proposalId),
        onMutate: (proposalId) => beginOptimistic(proposalId, 'REJECTED'),
        onError: (err, _id, ctx) => { rollback(ctx); showToast(err.message || 'No se pudo rechazar la propuesta', 'danger'); },
        onSuccess: () => { setDetailOpen(false); showToast('Propuesta rechazada', 'success'); },
        onSettled: reconcile,
    });
    const freeRescheduleMutation = useMutation({
        mutationFn: ({ requestId, newDate }) => requestApi.rescheduleRequest(requestId, { newDate }),
        onMutate: () => beginOptimistic(detail?.proposalId, 'SUPERSEDED'),
        onError: (err, _v, ctx) => { rollback(ctx); showToast(err.message || 'No se pudo reprogramar', 'danger'); },
        onSuccess: () => { setDetailOpen(false); showToast('Elegiste una nueva fecha; la propuesta fue superada', 'success'); },
        onSettled: reconcile,
    });
    const acting = acceptMutation.isPending || rejectMutation.isPending || freeRescheduleMutation.isPending;

    const handleAccept = (proposal) => acceptMutation.mutate(proposal.proposalId);
    const handleReject = (proposal) => rejectMutation.mutate(proposal.proposalId);

    const openDetail = (proposalId) => {
        setDetail(null);
        setFreeMode(false);
        setFreeDate('');
        setFreeTime('09:00');
        setDetailOpen(true);
        proposalApi.getProposalById(proposalId)
            .then(setDetail)
            .catch(() => showToast('No se pudo cargar el detalle de la propuesta', 'danger'));
    };

    // Tercera vía: el cliente elige libremente una nueva fecha. rescheduleRequest crea una nueva
    // solicitud y "supera" (SUPERSEDED) la propuesta pendiente del oferente.
    const handleFreeReschedule = () => {
        if (!detail || !freeDate) return;
        freeRescheduleMutation.mutate({ requestId: detail.requestId, newDate: `${freeDate}T${freeTime}:00` });
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
                                        <button className="btn btn-ghost" style={{ border: '1px solid var(--c-border)' }} onClick={() => openDetail(p.proposalId)}><Icon name="reschedule" size={15} />Ver detalle / otra fecha</button>
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
                                    <thead><tr><th>Servicio</th><th>Oferente</th><th>Fecha original</th><th>Fecha propuesta</th><th>Estado</th><th></th></tr></thead>
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
                                                    <td><button className="btn btn-ghost btn-sm" style={{ border: '1px solid var(--c-border)' }} onClick={() => openDetail(h.proposalId)}>Ver</button></td>
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
            {/* Detalle de propuesta + 3 acciones del cliente */}
            <Modal open={detailOpen} onClose={() => setDetailOpen(false)}>
                {!detail ? (
                    <div style={{ textAlign: 'center', padding: '20px', color: 'var(--c-soft)' }}>Cargando detalle...</div>
                ) : (() => {
                    const st = PROPOSAL_STATUS_MAP[detail.status] || { label: detail.status, badge: '' };
                    const isPending = detail.status === 'PENDING';
                    return (
                        <>
                            <div className="modal-title">Propuesta de reprogramación</div>
                            <div className="rp-head" style={{ marginBottom: '12px' }}>
                                <div className="av av-md">{getInitials(detail.counterpartyName)}</div>
                                <div>
                                    <div style={{ fontWeight: 700, fontSize: '14px' }}>{detail.counterpartyName}</div>
                                    <div style={{ fontSize: '12px', color: 'var(--c-mid)' }}>{detail.serviceTitle}{detail.categoryName ? ` · ${detail.categoryName}` : ''}</div>
                                </div>
                                <span className={`badge ${st.badge}`} style={{ marginLeft: 'auto' }}>{st.label}</span>
                            </div>

                            <div className="rp-change" style={{ marginBottom: '12px' }}>
                                <div style={{ fontSize: '12px', color: 'var(--c-mid)', marginBottom: '6px' }}>Propone cambiar de:</div>
                                <div className="rp-dates">
                                    <div className="rp-date"><strong>{formatDate(detail.originalScheduledDate)}</strong></div>
                                    <Icon name="arrowRight" size={16} style={{ color: 'var(--c-warn)' }} />
                                    <div className="rp-date rp-date-new"><strong>{formatDate(detail.proposedDate)}</strong></div>
                                </div>
                                {detail.reason && <div style={{ fontSize: '12px', color: 'var(--c-mid)', marginTop: '10px', fontStyle: 'italic' }}>"{detail.reason}"</div>}
                            </div>

                            <div style={{ fontSize: '12px', color: 'var(--c-mid)', marginBottom: '12px' }}>
                                {detail.addressLabel && <div><strong>Dirección:</strong> {detail.addressLabel}</div>}
                                {detail.requestedPrice != null && <div><strong>Precio:</strong> ${Number(detail.requestedPrice).toLocaleString('es-CO')}</div>}
                                <div><strong>Enviada:</strong> {formatDate(detail.createdAt)}</div>
                            </div>

                            <button
                                className="btn btn-outline btn-sm btn-full"
                                style={{ marginBottom: '14px' }}
                                onClick={() => navigate(`/requests/${detail.requestId}`, { state: { as: 'client' } })}
                            >
                                <Icon name="tasks" size={14} />Ver solicitud
                            </button>

                            {isPending && !freeMode && (
                                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                                    <button className="btn btn-success btn-full" disabled={acting} onClick={() => handleAccept(detail)}><Icon name="check" size={15} />Aceptar la fecha propuesta</button>
                                    <button className="btn btn-outline btn-full" disabled={acting} onClick={() => setFreeMode(true)}><Icon name="reschedule" size={15} />Elegir otra fecha</button>
                                    <button className="btn btn-danger btn-full" disabled={acting} onClick={() => handleReject(detail)}><Icon name="close" size={15} />Rechazar</button>
                                </div>
                            )}

                            {isPending && freeMode && (
                                <div>
                                    <div className="modal-sub">Elige tu propia fecha y hora. Esto reemplaza (supera) la propuesta del oferente.</div>
                                    <div className="input-group">
                                        <label className="label">Nueva fecha</label>
                                        <input className="input" type="date" value={freeDate} min={new Date().toISOString().split('T')[0]} onChange={(e) => setFreeDate(e.target.value)} />
                                    </div>
                                    <div className="input-group">
                                        <label className="label">Nueva hora</label>
                                        <input type="time" className="input" value={freeTime} onChange={(e) => setFreeTime(e.target.value)} />
                                    </div>
                                    <div style={{ display: 'flex', gap: '8px' }}>
                                        <button className="btn btn-ghost btn-full" onClick={() => setFreeMode(false)}>Volver</button>
                                        <button className="btn btn-primary btn-full" disabled={!freeDate || acting} onClick={handleFreeReschedule}>Confirmar nueva fecha</button>
                                    </div>
                                </div>
                            )}

                            {!isPending && (
                                <button className="btn btn-ghost btn-full" onClick={() => setDetailOpen(false)}>Cerrar</button>
                            )}
                        </>
                    );
                })()}
            </Modal>

            <ToastContainer toasts={toasts} />
        </DashboardLayout>
    );
}

export default ReschedulesPage;
