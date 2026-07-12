import { useState, useEffect } from "react";
import { Link, useNavigate } from 'react-router-dom';
import { DashboardLayout, Icon, Modal, StatCard, ToastContainer, useToast, CLIENT_NAV, requestApi } from '../../../../shared';
import { ReviewModal } from '../../components/ReviewModal/ReviewModal';
import { metricsApi, notificationApi } from '../../../../shared/api';
import { MetricsCards } from '../../../metrics';
import { STATUS_MAP, formatDate, timeAgo, formatPrice, categoryIcon, isTerminal } from '../../utils';

import './ClientDashboardPage.css';

const AGENDA = [
    { day: '12', month: 'Mayo', title: 'Reparación de tuberías', sub: 'Carlos M. · 9:00 AM · Calle 45 #12-34', badge: 'badge-warn', label: 'Pendiente' },
    { day: '14', month: 'Mayo', title: 'Limpieza de hogar', sub: 'María L. · 10:00 AM · Carrera 7', badge: 'badge-success', label: 'Confirmada' },
    { day: '16', month: 'Mayo', title: 'Instalación eléctrica', sub: 'Ana R. · 2:00 PM · Calle 45 #12-34', badge: 'badge-success', label: 'Confirmada' },
];

const TYPE_META = {
    new_request:         { icon: 'tasks',     cls: '' },
    request_accepted:    { icon: 'check',     cls: 'success' },
    request_rejected:    { icon: 'close',     cls: 'danger' },
    request_cancelled:   { icon: 'close',     cls: 'danger' },
    service_completed:   { icon: 'check',     cls: 'success' },
    reschedule_proposed: { icon: 'reschedule', cls: 'warn' },
    request_rescheduled: { icon: 'reschedule', cls: 'warn' },
};

function metaForType(type) {
    return TYPE_META[type] || { icon: 'bell', cls: '' };
}

export function ClientDashboardPage() {
    const navigate = useNavigate();
    const { toasts, showToast } = useToast();
    const [confirmOpen, setConfirmOpen] = useState(false);
    const [reschedOpen, setReschedOpen] = useState(false);
    const [cancelOpen, setCancelOpen] = useState(false);
    const [cancelTarget, setCancelTarget] = useState(null);
    const [clientMetrics, setClientMetrics] = useState(null);
    const [loading, setLoading] = useState(true);
    const [requests, setRequests] = useState([]);
    const [loadingRequests, setLoadingRequests] = useState(true);
    const [notifications, setNotifications] = useState([]);
    const [loadingNotifications, setLoadingNotifications] = useState(true);

    useEffect(() => {
        metricsApi.getMyMetrics()
            .then(data => setClientMetrics(data.clientMetrics))
            .catch(() => showToast('Error al cargar métricas', 'danger'))
            .finally(() => setLoading(false));
    }, []);

    useEffect(() => {
        requestApi.getMyClientRequests({ page: 0, size: 5 })
            .then(data => setRequests(data.content || []))
            .catch(() => showToast('Error al cargar solicitudes', 'danger'))
            .finally(() => setLoadingRequests(false));
    }, []);

    useEffect(() => {
        notificationApi.getNotifications({ page: 0, size: 3 })
            .then(data => setNotifications(data.content || []))
            .catch(() => {})
            .finally(() => setLoadingNotifications(false));
    }, []);

    const handleCancel = () => {
        if (!cancelTarget) return;
        requestApi.cancelRequest(cancelTarget.requestId)
            .then(() => {
                setRequests(prev => prev.map(r =>
                    r.requestId === cancelTarget.requestId
                        ? { ...r, status: 'CANCELLED' }
                        : r
                ));
                setCancelOpen(false);
                setCancelTarget(null);
                showToast('Solicitud cancelada. Oferente notificado', 'success');
            })
            .catch(err => showToast('Error al cancelar: ' + err.message, 'danger'));
    };

    const stats = clientMetrics ? [
        { icon: 'tasks', value: String(clientMetrics.totalAcceptedRequests ?? 0), label: 'Solicitudes activas' },
        { icon: 'checkCircle', value: String(clientMetrics.totalCompletedRequests ?? 0), label: 'Servicios completados', variant: 'success' },
        { icon: 'star', value: (clientMetrics.averageRating ?? 0).toFixed(1) + '★', label: 'Mi calificación', variant: 'warn', fill: 'currentColor' },
        {
            icon: 'xCircle',
            value: (clientMetrics.totalRequestsSent > 0 ? (clientMetrics.totalCancelledRequests / clientMetrics.totalRequestsSent * 100) : 0).toFixed(0) + '%',
            label: 'Tasa cancelaciones',
            variant: 'danger',
        },
    ] : [];

    return (
        <DashboardLayout sections={CLIENT_NAV} avatar="JP">
            <div className="ph"><h1>¡Hola, Juan Pablo!</h1><p>Aquí tienes un resumen de tu actividad en ServiYa</p></div>

            <div className="g4" style={{ marginBottom: '22px' }}>
                {loading ? <div className="loading-pulse" style={{ height: 80 }} /> : stats.map((s) => <StatCard key={s.label} {...s} />)}
            </div>

            <div className="card resched-banner">
                <div className="resched-head">
                    <Icon name="reschedule" size={18} style={{ color: 'var(--c-warn)' }} />
                    <span style={{ fontSize: '14px', fontWeight: 700 }}>Propuesta de reprogramación</span>
                    <span className="badge badge-warn">Requiere acción</span>
                </div>
                <div className="resched-body">
                    <div style={{ fontSize: '13px', fontWeight: 600, marginBottom: '4px' }}>Reparación de tuberías — Carlos M.</div>
                    <div style={{ fontSize: '12px', color: 'var(--c-mid)', marginBottom: '10px' }}>
                        Carlos propone cambiar de <strong>Lun 12 mayo 9am</strong> a <strong>Mar 13 mayo 10am</strong><br />
                        <span style={{ fontStyle: 'italic', marginTop: '3px', display: 'block' }}>Motivo: "Surgió un compromiso previo ese día"</span>
                    </div>
                    <div style={{ display: 'flex', gap: '7px', flexWrap: 'wrap' }}>
                        <button className="btn btn-success btn-sm" onClick={() => showToast('Cita reprogramada correctamente', 'success')}><Icon name="check" size={13} />Aceptar propuesta</button>
                        <button className="btn btn-danger btn-sm" onClick={() => showToast('Propuesta rechazada', 'danger')}><Icon name="close" size={13} />Rechazar</button>
                        <button className="btn btn-ghost btn-sm" style={{ border: '1px solid var(--c-border)' }} onClick={() => showToast('Solicitud cancelada', 'danger')}>Cancelar solicitud</button>
                    </div>
                </div>
            </div>

            <div className="g2" style={{ gap: '20px' }}>
                <div>
                    <div className="col-head">
                        <div style={{ fontSize: '15px', fontWeight: 700 }}>Solicitudes activas</div>
                        <Link to="/requests" className="link-more">Ver todas →</Link>
                    </div>
                    {loadingRequests ? (
                        <div style={{ padding: '20px 0', color: 'var(--c-soft)', fontSize: '13px' }}>Cargando solicitudes...</div>
                    ) : requests.length === 0 ? (
                        <div style={{ padding: '20px 0', color: 'var(--c-soft)', fontSize: '13px' }}>No tienes solicitudes activas</div>
                    ) : (
                        requests.map((r) => {
                            const st = STATUS_MAP[r.status] || { label: r.status, badge: '' };
                            const isNew = r.status === 'PENDING' && timeAgo(r.createdAt) === 'Hace 1 min';
                            const canConfirm = r.status === 'PRESUMABLY_COMPLETED';
                            const canReschedule = r.status === 'PENDING' || r.status === 'ACCEPTED';
                            const meta = [r.counterpartyName, formatDate(r.scheduledDate), r.city].filter(Boolean).join(' · ');

                            return (
                                <div className={`req-card ${isNew ? 'new' : ''}`} key={r.requestId}>
                                    <div className="req-header">
                                        <div className="req-icon"><Icon name={categoryIcon(r.categoryName)} size={20} /></div>
                                        <div className="req-info"><div className="req-name">{r.serviceTitle}</div><div className="req-meta">{meta}</div></div>
                                        <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
                                            <span className={`sdot sdot-${r.status === 'PENDING' ? 'pending' : 'accepted'}`} />
                                            <span className={`badge ${st.badge}`}>{st.label}</span>
                                        </div>
                                    </div>
                                    <div className="req-actions">
                                        <button className="btn btn-ghost btn-sm" style={{ border: '1px solid var(--c-border)' }} onClick={() => navigate(`/services/${r.serviceId}`)}>Ver detalle</button>
                                        {canReschedule && <button className="btn btn-ghost btn-sm" style={{ border: '1px solid var(--c-border)' }} onClick={() => setReschedOpen(true)}><Icon name="reschedule" size={13} />Reprogramar</button>}
                                        {canConfirm && <button className="btn btn-primary btn-sm" onClick={() => setConfirmOpen(true)}><Icon name="check" size={13} />Confirmar servicio</button>}
                                        {(r.status === 'PENDING' || r.status === 'ACCEPTED') && <button className="btn btn-danger btn-sm" onClick={() => { setCancelTarget(r); setCancelOpen(true); }}><Icon name="close" size={13} />Cancelar</button>}
                                    </div>
                                </div>
                            );
                        })
                    )}
                </div>

                <div>
                    <div style={{ fontSize: '15px', fontWeight: 700, marginBottom: '14px' }}>Próximos servicios</div>
                    <div className="card" style={{ marginBottom: '18px' }}>
                        {AGENDA.map((a, i) => (
                            <div className="agenda-row" key={i}>
                                <div className="agenda-date"><div className="agenda-day">{a.day}</div><div className="agenda-month">{a.month}</div></div>
                                <div className="agenda-info"><div className="agenda-title">{a.title}</div><div className="agenda-sub">{a.sub}</div><span className={`badge ${a.badge}`} style={{ marginTop: '5px' }}>{a.label}</span></div>
                            </div>
                        ))}
                    </div>

                    <div style={{ fontSize: '15px', fontWeight: 700, marginBottom: '12px' }}>Notificaciones recientes</div>
                    {loadingNotifications ? (
                        <div style={{ padding: '12px 0', color: 'var(--c-soft)', fontSize: '13px' }}>Cargando notificaciones...</div>
                    ) : notifications.length === 0 ? (
                        <div style={{ padding: '12px 0', color: 'var(--c-soft)', fontSize: '13px' }}>No hay notificaciones</div>
                    ) : notifications.map((n) => {
                        const meta = metaForType(n.notificationType);
                        const isUnread = !n.readAt;
                        return (
                            <div className={`notif-item ${isUnread ? 'unread' : 'read'}`} key={n.deliveryId}>
                                <div className={`notif-ico ${meta.cls}`}><Icon name={meta.icon} size={16} /></div>
                                <div className="notif-body">
                                    <div className="notif-title">{n.title}</div>
                                    <div className="notif-msg">{n.message}</div>
                                    <div className="notif-time">{timeAgo(n.createdAt)}</div>
                                </div>
                            </div>
                        );
                    })}
                    <Link to="/notifications" className="link-more" style={{ display: 'block', textAlign: 'center', marginTop: '10px' }}>Ver todas las notificaciones →</Link>
                </div>
            </div>

            <ReviewModal
                open={confirmOpen}
                onClose={() => setConfirmOpen(false)}
                title="Confirmar cumplimiento del servicio"
                sub="¿El servicio de limpieza fue realizado correctamente por María L.?"
                ratingLabel="Calificación del servicio (RF-041)"
                reviewLabel="Reseña del servicio (RF-045)"
                confirmLabel="Confirmar servicio"
                onConfirm={() => { setConfirmOpen(false); showToast('Servicio confirmado y reseña enviada', 'success'); }}
            />

            <Modal open={reschedOpen} onClose={() => setReschedOpen(false)}>
                <div className="modal-title">Reprogramar solicitud</div>
                <div className="modal-sub">Elige una nueva fecha y hora disponible del oferente</div>
                <div className="input-group"><label className="label">Nueva fecha</label><input className="input" type="date" /></div>
                <div className="input-group"><label className="label">Nueva hora</label><select className="input"><option>9:00 AM</option><option>10:00 AM</option><option>2:00 PM</option><option>3:00 PM</option></select></div>
                <div style={{ display: 'flex', gap: '8px' }}>
                    <button className="btn btn-ghost btn-full" onClick={() => setReschedOpen(false)}>Cancelar</button>
                    <button className="btn btn-primary btn-full" onClick={() => { setReschedOpen(false); showToast('Solicitud reprogramada', 'success'); }}>Confirmar reprogramación</button>
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
            <ToastContainer toasts={toasts} />
        </DashboardLayout>
    );
}

export default ClientDashboardPage;
