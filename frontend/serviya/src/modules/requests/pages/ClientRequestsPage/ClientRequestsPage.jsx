import { useState } from "react";
import { Link, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { DashboardLayout, Icon, Modal, ToastContainer, useToast, CLIENT_NAV, reportApi, requestApi, feedbackApi } from '../../../../shared';
import { ReviewModal } from '../../components/ReviewModal/ReviewModal';
import { STATUS_MAP, formatDate, timeAgo, getInitials, formatPrice, isTerminal } from '../../utils';

import './ClientRequestsPage.css';

const FILTERS = [
    { key: '',        label: 'Todas' },
    { key: 'PENDING', label: 'Pendientes' },
    { key: 'ACCEPTED', label: 'Aceptadas' },
];

export function ClientRequestsPage() {
    const navigate = useNavigate();
    const { toasts, showToast } = useToast();
    const [filter, setFilter] = useState(0);
    const [page, setPage] = useState(0);
    const [confirmOpen, setConfirmOpen] = useState(false);
    const [reschedOpen, setReschedOpen] = useState(false);
    const [reportOpen, setReportOpen] = useState(false);
    // RF-073: solicitud concreta que se esta reportando (antes iba fija a la #1).
    const [reportTarget, setReportTarget] = useState(null);
    const [reportCategory, setReportCategory] = useState('No se presentó');
    const [customCategory, setCustomCategory] = useState('');
    const [reportReason, setReportReason] = useState('');
    const [cancelOpen, setCancelOpen] = useState(false);
    const [cancelTarget, setCancelTarget] = useState(null);
    const [confirmTarget, setConfirmTarget] = useState(null);
    const [reschedTarget, setReschedTarget] = useState(null);
    const [reschedDate, setReschedDate] = useState('');
    const [reschedTime, setReschedTime] = useState('09:00');

    // Solicitudes del cliente (M46: React Query; refetch() refresca tras acciones).
    const requestsQuery = useQuery({
        queryKey: ['client-requests-list', filter, page],
        queryFn: () => {
            const params = { page, size: 10 };
            if (FILTERS[filter].key) params.statuses = FILTERS[filter].key;
            return requestApi.getMyClientRequests(params);
        },
    });
    const requests = requestsQuery.data?.content ?? [];
    const loading = requestsQuery.isPending;
    const totalPages = requestsQuery.data?.totalPages ?? 1;
    const currentPage = requestsQuery.data?.number ?? page;

    // M49: las acciones usan useMutation con ACTUALIZACIÓN OPTIMISTA. La caché de la lista cambia al
    // instante (onMutate) para que el cambio se vea sin esperar el round-trip; se revierte si falla
    // (onError) y se reconcilia con el servidor al terminar (onSettled → invalidateQueries, que invalida
    // TODAS las vistas de la lista: filtros + páginas).
    const queryClient = useQueryClient();

    // Cambia el estado de una solicitud en TODAS las entradas cacheadas de la lista (optimista).
    const patchCachedStatus = (requestId, status) => {
        queryClient.setQueriesData({ queryKey: ['client-requests-list'] }, (old) => {
            if (!old?.content) return old;
            return { ...old, content: old.content.map((r) => (r.requestId === requestId ? { ...r, status } : r)) };
        });
    };
    // Cancela refetches en curso + guarda snapshot (para revertir) y aplica el cambio optimista.
    const beginOptimistic = async (requestId, status) => {
        await queryClient.cancelQueries({ queryKey: ['client-requests-list'] });
        const snapshot = queryClient.getQueriesData({ queryKey: ['client-requests-list'] });
        patchCachedStatus(requestId, status);
        return { snapshot };
    };
    const rollback = (ctx) => ctx?.snapshot?.forEach(([key, data]) => queryClient.setQueryData(key, data));
    const reconcile = () => queryClient.invalidateQueries({ queryKey: ['client-requests-list'] });

    const cancelMutation = useMutation({
        mutationFn: (id) => requestApi.cancelRequest(id),
        onMutate: (id) => beginOptimistic(id, 'CANCELLED'),
        onError: (err, _id, ctx) => { rollback(ctx); showToast('Error al cancelar: ' + err.message, 'danger'); },
        onSuccess: () => {
            setCancelOpen(false);
            setCancelTarget(null);
            showToast('Solicitud cancelada. Oferente notificado', 'success');
        },
        onSettled: reconcile,
    });

    const confirmMutation = useMutation({
        mutationFn: async ({ requestId, rating, comment }) => {
            await requestApi.confirmCompletion(requestId);
            if (rating > 0 || comment) {
                await feedbackApi.submitServiceFeedback(requestId, { rating: rating || null, comment: comment || null });
            }
        },
        onMutate: ({ requestId }) => beginOptimistic(requestId, 'COMPLETED'),
        onError: (err, _vars, ctx) => { rollback(ctx); showToast(err.message || 'No se pudo confirmar el servicio', 'danger'); },
        onSuccess: () => {
            setConfirmOpen(false);
            setConfirmTarget(null);
            showToast('Servicio confirmado y reseña enviada', 'success');
        },
        onSettled: reconcile,
    });

    const rescheduleMutation = useMutation({
        mutationFn: ({ requestId, newDate }) => requestApi.rescheduleRequest(requestId, { newDate }),
        onMutate: ({ requestId }) => beginOptimistic(requestId, 'RESCHEDULED'),
        onError: (err, _vars, ctx) => { rollback(ctx); showToast(err.message || 'No se pudo reprogramar', 'danger'); },
        onSuccess: () => {
            setReschedOpen(false);
            setReschedTarget(null);
            setReschedDate('');
            setReschedTime('09:00');
            showToast('Solicitud reprogramada. Oferente notificado', 'success');
        },
        onSettled: reconcile,
    });

    const handleCancel = () => {
        if (!cancelTarget) return;
        cancelMutation.mutate(cancelTarget.requestId);
    };

    return (
        <DashboardLayout sections={CLIENT_NAV} avatar="JP">
            <div className="page-head">
                <div className="ph" style={{ margin: 0 }}><h1>Solicitudes activas</h1><p>Gestiona tus solicitudes en curso</p></div>
                <Link to="/services" className="btn btn-primary btn-sm"><Icon name="plus" size={13} />Nueva solicitud</Link>
            </div>

            <div className="filter-chips">
                {FILTERS.map((f, i) => (
                    <div key={f.label} className={`chip ${filter === i ? 'active' : ''}`} onClick={() => { setFilter(i); setPage(0); }}>{f.label}</div>
                ))}
            </div>

            {loading ? (
                <div style={{ textAlign: 'center', padding: '40px 0', color: 'var(--c-soft)' }}>Cargando solicitudes...</div>
            ) : requestsQuery.isError ? (
                <div style={{ textAlign: 'center', padding: '40px 0', color: 'var(--c-soft)' }}>No se pudieron cargar las solicitudes</div>
            ) : requests.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '40px 0', color: 'var(--c-soft)' }}>
                    <p>No tienes solicitudes activas</p>
                    <Link to="/services" className="btn btn-primary btn-sm" style={{ marginTop: '12px' }}><Icon name="plus" size={13} />Solicitar un servicio</Link>
                </div>
            ) : (
                <>
                    {requests.map((r) => {
                        const st = STATUS_MAP[r.status] || { label: r.status, badge: '' };
                        const isNew = r.status === 'PENDING' && timeAgo(r.createdAt) === 'Hace 1 min';
                        const canConfirm = r.status === 'PRESUMABLY_COMPLETED';
                        const canReschedule = r.status === 'PENDING' || r.status === 'ACCEPTED';

                        return (
                            <div className={`req-card ${isNew ? 'new' : ''}`} key={r.requestId}>
                                <div className="rq-top">
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '7px' }}>
                                        <span className={`badge ${st.badge}`}>{st.label}</span>
                                        <span style={{ fontSize: '11px', color: 'var(--c-soft)' }}>#{r.requestId}{timeAgo(r.createdAt) ? ` · ${timeAgo(r.createdAt)}` : ''}</span>
                                    </div>
                                    {!isTerminal(r.status) && <button className="btn btn-ghost btn-sm" onClick={() => { setReportTarget(r); setReportOpen(true); }} style={{ color: 'var(--c-danger)' }}><Icon name="alertTriangle" size={13} />Reportar</button>}
                                </div>
                                <div className="rq-main">
                                    <div className="av av-md">{getInitials(r.counterpartyName)}</div>
                                    <div style={{ flex: 1 }}>
                                        <div style={{ fontSize: '14px', fontWeight: 700 }}>{r.serviceTitle}</div>
                                        <div style={{ fontSize: '12px', color: 'var(--c-mid)', marginTop: '2px' }}>{r.counterpartyName}{r.categoryName ? ` · ${r.categoryName}` : ''}</div>
                                        <div className="rq-meta">
                                            {r.scheduledDate && <span><Icon name="calendar" size={12} />{formatDate(r.scheduledDate)}</span>}
                                            {r.city && <span><Icon name="mapPin" size={12} />{r.city}</span>}
                                            {r.requestedPrice != null && <span><Icon name="dollar" size={12} />{formatPrice(r.requestedPrice)}</span>}
                                        </div>
                                    </div>
                                </div>
                                <div style={{ display: 'flex', gap: '7px', flexWrap: 'wrap' }}>
                                    <button className="btn btn-ghost btn-sm" style={{ border: '1px solid var(--c-border)' }} onClick={() => navigate(`/requests/${r.requestId}`, { state: { as: 'client' } })}>Ver detalle</button>
                                    {canReschedule && <button className="btn btn-ghost btn-sm" style={{ border: '1px solid var(--c-border)' }} onClick={() => { setReschedTarget(r); setReschedOpen(true); }}><Icon name="reschedule" size={13} />Reprogramar</button>}
                                    {canConfirm && <button className="btn btn-primary btn-sm" onClick={() => { setConfirmTarget(r); setConfirmOpen(true); }}><Icon name="check" size={13} />Confirmar servicio</button>}
                                    {(r.status === 'PENDING' || r.status === 'ACCEPTED') && <button className="btn btn-danger btn-sm" onClick={() => { setCancelTarget(r); setCancelOpen(true); }}><Icon name="close" size={13} />Cancelar</button>}
                                </div>
                            </div>
                        );
                    })}
                    {totalPages > 1 && (
                        <div className="pager">
                            {Array.from({ length: totalPages }, (_, i) => (
                                <button
                                    key={i}
                                    className={`btn ${currentPage === i ? 'btn-primary' : 'btn-ghost'} btn-sm`}
                                    onClick={() => setPage(i)}
                                >
                                    {i + 1}
                                </button>
                            ))}
                        </div>
                    )}
                </>
            )}

            <ReviewModal
                open={confirmOpen}
                onClose={() => { setConfirmOpen(false); setConfirmTarget(null); }}
                title="Confirmar servicio"
                sub={confirmTarget ? `¿El servicio "${confirmTarget.serviceTitle}" fue realizado correctamente por ${confirmTarget.counterpartyName}?` : '¿El servicio fue realizado correctamente?'}
                ratingLabel="Calificación"
                reviewLabel="Reseña"
                confirmLabel="Confirmar"
                onConfirm={({ rating, comment }) => {
                    if (!confirmTarget) return;
                    confirmMutation.mutate({ requestId: confirmTarget.requestId, rating, comment });
                }}
            />

            <Modal open={reschedOpen} onClose={() => { setReschedOpen(false); setReschedTarget(null); }}>
                <div className="modal-title">Reprogramar solicitud</div>
                <div className="modal-sub">Elige nueva fecha y hora disponible</div>
                <div className="input-group"><label className="label">Nueva fecha</label><input className="input" type="date" value={reschedDate} onChange={(e) => setReschedDate(e.target.value)} min={new Date().toISOString().split('T')[0]} /></div>
                <div className="input-group"><label className="label">Nueva hora</label><input type="time" className="input" value={reschedTime} onChange={(e) => setReschedTime(e.target.value)} /></div>
                <div style={{ display: 'flex', gap: '8px' }}>
                    <button className="btn btn-ghost btn-full" onClick={() => { setReschedOpen(false); setReschedTarget(null); }}>Cancelar</button>
                    <button className="btn btn-primary btn-full" disabled={!reschedDate || rescheduleMutation.isPending} onClick={() => {
                        if (!reschedTarget || !reschedDate) return;
                        rescheduleMutation.mutate({ requestId: reschedTarget.requestId, newDate: `${reschedDate}T${reschedTime}:00` });
                    }}>Confirmar</button>
                </div>
            </Modal>

            <Modal open={cancelOpen} onClose={() => { setCancelOpen(false); setCancelTarget(null); }}>
                <div className="modal-title">Cancelar solicitud</div>
                <div className="modal-sub">¿Estás seguro de cancelar esta solicitud? Esta acción no se puede deshacer.</div>
                <div style={{ display: 'flex', gap: '8px' }}>
                    <button className="btn btn-ghost btn-full" onClick={() => { setCancelOpen(false); setCancelTarget(null); }}>Volver</button>
                    <button className="btn btn-danger btn-full" onClick={handleCancel}><Icon name="close" size={13} />Sí, cancelar solicitud</button>
                </div>
            </Modal>

            <Modal open={reportOpen} onClose={() => { setReportOpen(false); setReportTarget(null); }}>
                <div className="modal-title">Reportar incumplimiento</div>
                <div className="modal-sub">
                    {reportTarget
                        ? <>Solicitud <strong>#{reportTarget.id}</strong>{reportTarget.serviceTitle ? ` · ${reportTarget.serviceTitle}` : ''}. El administrador revisará el caso.</>
                        : 'El administrador revisará el caso.'}
                </div>
                <div className="input-group"><label className="label">Categoría</label><select className="input" value={reportCategory} onChange={(e) => setReportCategory(e.target.value)}><option>No se presentó</option><option>Comportamiento inapropiado</option><option>Fraude</option><option>Otra</option></select></div>
                {reportCategory === 'Otra' && <div className="input-group"><label className="label">Categoría personalizada</label><input className="input" value={customCategory} onChange={(e) => setCustomCategory(e.target.value)} placeholder="Escribe la categoría" /></div>}
                <div className="input-group"><label className="label">Descripción</label><textarea className="input" rows="3" value={reportReason} onChange={(e) => setReportReason(e.target.value)} placeholder="Describe lo que ocurrió..." /></div>
                <div style={{ display: 'flex', gap: '8px' }}>
                    <button className="btn btn-ghost btn-full" onClick={() => setReportOpen(false)}>Cancelar</button>
                    <button className="btn btn-danger btn-full" disabled={!reportReason.trim()} onClick={async () => {
                        try {
                            // RF-073: se reporta LA solicitud seleccionada. El backend deriva a quien se
                            // reporta desde la contraparte de esa solicitud, por eso no se manda reportedUserId
                            // (mandarlo permitiria incriminar a un tercero).
                            await reportApi.createRequestReport({
                                category: reportCategory,
                                customCategory,
                                reason: reportReason,
                                requestId: reportTarget?.requestId,
                            });
                            setReportOpen(false);
                            setReportCategory('No se presentó');
                            setCustomCategory('');
                            setReportReason('');
                            showToast('Reporte enviado', 'success');
                        } catch (error) {
                            showToast(error.message || 'No se pudo enviar el reporte', 'danger');
                        }
                    }}>Enviar reporte</button>
                </div>
            </Modal>
            <ToastContainer toasts={toasts} />
        </DashboardLayout>
    );
}

export default ClientRequestsPage;
