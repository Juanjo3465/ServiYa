import { useState, useEffect } from "react";
import { Link } from 'react-router-dom';
import { DashboardLayout, Icon, Modal, Stars, StatCard, ToastContainer, useToast, OFFERER_NAV } from '../../../../shared';
import { ReviewModal } from '../../components/ReviewModal/ReviewModal';
import { metricsApi, notificationApi } from '../../../../shared/api';
import { timeAgo } from '../../utils';

import './OffererDashboardPage.css';

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

const AGENDA = [
    { day: '12', month: 'Mayo', title: 'Reparación de tuberías', sub: 'Juan P. · 9:00 AM · Calle 45 #12-34', badge: 'badge-warn', label: 'Pendiente' },
    { day: '14', month: 'Mayo', title: 'Reparación de tuberías', sub: 'Sandra R. · 10:00 AM · Carrera 7', badge: 'badge-success', label: 'Aceptada' },
    { day: '16', month: 'Mayo', title: 'Destape de cañerías', sub: 'Mario V. · 2:00 PM · Usaquén', badge: 'badge-success', label: 'Aceptada' },
];

export function OffererDashboardPage() {
    const { toasts, showToast } = useToast();
    const [proposalOpen, setProposalOpen] = useState(false);
    const [completeOpen, setCompleteOpen] = useState(false);
    const [offererMetrics, setOffererMetrics] = useState(null);
    const [loading, setLoading] = useState(true);
    const [notifications, setNotifications] = useState([]);
    const [loadingNotifications, setLoadingNotifications] = useState(true);

    useEffect(() => {
        metricsApi.getMyMetrics()
            .then(data => setOffererMetrics(data.offererMetrics))
            .catch(() => showToast('Error al cargar métricas', 'danger'))
            .finally(() => setLoading(false));
    }, []);

    useEffect(() => {
        notificationApi.getNotifications({ page: 0, size: 3 })
            .then(data => setNotifications(data.content || []))
            .catch(() => {})
            .finally(() => setLoadingNotifications(false));
    }, []);

    const stats = offererMetrics ? [
        { icon: 'tasks', value: String(offererMetrics.totalRequestsReceived ?? 0), label: 'Solicitudes recibidas' },
        { icon: 'checkCircle', value: String(offererMetrics.totalCompletedServices ?? 0), label: 'Servicios completados', variant: 'success' },
        { icon: 'star', value: (offererMetrics.averageRating ?? 0).toFixed(1) + '★', label: 'Calificación promedio', variant: 'warn', fill: 'currentColor' },
        {
            icon: 'xCircle',
            value: (offererMetrics.totalRequestsReceived > 0 ? (offererMetrics.totalCancelledServices / offererMetrics.totalRequestsReceived * 100) : 0).toFixed(0) + '%',
            label: 'Cancelaciones',
            variant: 'danger',
        },
    ] : [];

    return (
        <DashboardLayout sections={OFFERER_NAV} avatar="CM">
            <div className="ph"><h1>¡Hola, Carlos!</h1><p>Gestiona tus servicios y solicitudes recibidas</p></div>

            <div className="g4" style={{ marginBottom: '22px' }}>
                {loading ? <div className="loading-pulse" style={{ height: 80 }} /> : stats.map((s) => <StatCard key={s.label} {...s} />)}
            </div>

            <div style={{ fontSize: '15px', fontWeight: 700, marginBottom: '14px' }}>Solicitudes recibidas</div>

            <div className="req-card new" style={{ marginBottom: '10px' }}>
                <div className="inc-head">
                    <span className="badge badge-primary">Nueva</span>
                    <span style={{ fontSize: '11px', color: 'var(--c-soft)' }}>Recibida hace 5 min</span>
                </div>
                <div className="inc-body">
                    <div className="av av-md">JP</div>
                    <div style={{ flex: 1 }}>
                        <div style={{ fontSize: '14px', fontWeight: 700, color: 'var(--c-text)' }}>Juan Pablo Bernal</div>
                        <div style={{ fontSize: '12px', color: 'var(--c-mid)' }}>Reparación de tuberías · Lun 12 mayo, 9:00 AM · Calle 45 #12-34</div>
                        <div className="client-metrics">
                            <div className="cm-pill"><strong>96%</strong> Cumplimiento</div>
                            <div className="cm-pill"><strong>3%</strong> Cancelaciones</div>
                            <div className="cm-pill"><Stars rating={5} size={11} /><strong>4.8</strong></div>
                            <div className="cm-pill"><strong>18</strong> servicios</div>
                        </div>
                    </div>
                </div>
                <div style={{ display: 'flex', gap: '7px', flexWrap: 'wrap' }}>
                    <button className="btn btn-success btn-sm" onClick={() => showToast('Solicitud aceptada. Cliente notificado', 'success')}><Icon name="check" size={13} />Aceptar</button>
                    <button className="btn btn-danger btn-sm" onClick={() => showToast('Solicitud rechazada. Cliente notificado', 'danger')}><Icon name="close" size={13} />Rechazar</button>
                    <button className="btn btn-ghost btn-sm" style={{ border: '1px solid var(--c-border)' }} onClick={() => setProposalOpen(true)}><Icon name="reschedule" size={13} />Proponer reprogramación</button>
                </div>
            </div>

            <div className="req-card" style={{ marginBottom: '10px' }}>
                <div className="inc-head">
                    <span className="badge badge-success">Aceptada</span>
                    <span style={{ fontSize: '11px', color: 'var(--c-soft)' }}>Mié 14 mayo, 10:00 AM</span>
                </div>
                <div className="inc-body" style={{ alignItems: 'center' }}>
                    <div className="av av-md">SR</div>
                    <div style={{ flex: 1 }}>
                        <div style={{ fontSize: '14px', fontWeight: 700, color: 'var(--c-text)' }}>Sandra Rivera</div>
                        <div style={{ fontSize: '12px', color: 'var(--c-mid)' }}>Reparación de tuberías · Carrera 7 #80-21</div>
                    </div>
                </div>
                <div style={{ display: 'flex', gap: '7px', flexWrap: 'wrap' }}>
                    <button className="btn btn-primary btn-sm" onClick={() => setCompleteOpen(true)}><Icon name="check" size={13} />Marcar como realizado</button>
                    <button className="btn btn-danger btn-sm" onClick={() => showToast('Servicio cancelado. Cliente notificado', 'danger')}>Cancelar</button>
                    <button className="btn btn-ghost btn-sm" style={{ border: '1px solid var(--c-border)' }} onClick={() => setProposalOpen(true)}>Reprogramar</button>
                </div>
            </div>

            <div style={{ fontSize: '15px', fontWeight: 700, margin: '20px 0 14px' }}>Próximos servicios</div>
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

            <Modal open={proposalOpen} onClose={() => setProposalOpen(false)}>
                <div className="modal-title">Proponer reprogramación</div>
                <div className="modal-sub">El cliente recibirá tu propuesta y podrá aceptarla o rechazarla.</div>
                <div className="input-group"><label className="label">Nueva fecha propuesta</label><input className="input" type="date" /></div>
                <div className="input-group"><label className="label">Nueva hora propuesta</label><select className="input"><option>9:00 AM</option><option>10:00 AM</option><option>11:00 AM</option><option>2:00 PM</option><option>3:00 PM</option></select></div>
                <div className="input-group"><label className="label">Motivo de la reprogramación</label><textarea className="input" placeholder="Explica brevemente el motivo..." /></div>
                <div style={{ display: 'flex', gap: '8px' }}>
                    <button className="btn btn-ghost btn-full" onClick={() => setProposalOpen(false)}>Cancelar</button>
                    <button className="btn btn-primary btn-full" onClick={() => { setProposalOpen(false); showToast('Propuesta enviada al cliente', 'success'); }}><Icon name="send" size={15} />Enviar propuesta</button>
                </div>
            </Modal>

            <ReviewModal
                open={completeOpen}
                onClose={() => setCompleteOpen(false)}
                title="Confirmar servicio realizado"
                sub="Confirma que prestaste el servicio a Sandra R. correctamente."
                ratingLabel="Califica al cliente (RF-043)"
                reviewLabel="Reseña del cliente (RF-044)"
                confirmLabel="Confirmar realizado"
                confirmClass="btn-success"
                onConfirm={() => { setCompleteOpen(false); showToast('Servicio marcado como realizado. Cliente notificado', 'success'); }}
            />
            <ToastContainer toasts={toasts} />
        </DashboardLayout>
    );
}

export default OffererDashboardPage;
