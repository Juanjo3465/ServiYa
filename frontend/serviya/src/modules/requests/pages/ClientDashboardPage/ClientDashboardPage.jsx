import { useEffect, useState } from "react";
import { Link, useNavigate } from 'react-router-dom';
import { DashboardLayout, Icon, Modal, StatCard, ToastContainer, useToast, CLIENT_NAV, feedbackApi } from '../../../../shared';
import { ReviewModal } from '../../components/ReviewModal/ReviewModal';

import './ClientDashboardPage.css';

const STATS = [
    { icon: 'tasks', value: '3', label: 'Solicitudes activas' },
    { icon: 'checkCircle', value: '18', label: 'Servicios completados', variant: 'success' },
    { icon: 'star', value: '4.8★', label: 'Mi calificación', variant: 'warn', fill: 'currentColor' },
    { icon: 'xCircle', value: '5%', label: 'Tasa cancelaciones', variant: 'danger' },
];

const REQUESTS = [
    { id: 1, name: 'Reparación de tuberías', icon: 'wrench', meta: 'Carlos M. · Lun 12 mayo, 9:00 AM · Calle 45 #12-34', status: 'Pendiente', dot: 'sdot-pending', badge: 'badge-warn', isNew: true, canConfirm: false },
    { id: 2, name: 'Limpieza de hogar', icon: 'home', meta: 'María L. · Mié 14 mayo, 10:00 AM', status: 'Aceptada', dot: 'sdot-accepted', badge: 'badge-success', isNew: false, canConfirm: true },
    { id: 3, name: 'Instalación eléctrica', icon: 'bolt', meta: 'Ana R. · Vie 16 mayo, 2:00 PM', status: 'Aceptada', dot: 'sdot-accepted', badge: 'badge-success', isNew: false, canConfirm: false },
];

const AGENDA = [
    { day: '12', month: 'Mayo', title: 'Reparación de tuberías', sub: 'Carlos M. · 9:00 AM · Calle 45 #12-34', badge: 'badge-warn', label: 'Pendiente' },
    { day: '14', month: 'Mayo', title: 'Limpieza de hogar', sub: 'María L. · 10:00 AM · Carrera 7', badge: 'badge-success', label: 'Confirmada' },
    { day: '16', month: 'Mayo', title: 'Instalación eléctrica', sub: 'Ana R. · 2:00 PM · Calle 45 #12-34', badge: 'badge-success', label: 'Confirmada' },
];

const NOTIFS = [
    { icon: 'check', cls: 'success', title: 'Solicitud aceptada', msg: 'María L. aceptó tu solicitud de limpieza para el 14 de mayo.', time: 'Hace 20 min', unread: true },
    { icon: 'reschedule', cls: 'warn', title: 'Propuesta de reprogramación', msg: 'Carlos M. propone cambiar la fecha de tu servicio de plomería.', time: 'Hace 1 hora', unread: true },
    { icon: 'check', cls: 'success', title: 'Solicitud aceptada', msg: 'Ana R. confirmó tu solicitud de electricidad para el 16 de mayo.', time: 'Ayer', unread: false },
];

export function ClientDashboardPage() {
    const navigate = useNavigate();
    const { toasts, showToast } = useToast();
    const [confirmOpen, setConfirmOpen] = useState(false);
    const [reschedOpen, setReschedOpen] = useState(false);
    const [activeRequest, setActiveRequest] = useState(null);
    const [serviceTags, setServiceTags] = useState([]);
    const [savingFeedback, setSavingFeedback] = useState(false);

    useEffect(() => {
        feedbackApi.getServiceFeedbackTags()
            .then(setServiceTags)
            .catch(() => setServiceTags([]));
    }, []);

    const openConfirm = (request) => {
        setActiveRequest(request);
        setConfirmOpen(true);
    };

    const submitServiceFeedback = async (payload) => {
        if (!activeRequest) return;
        setSavingFeedback(true);
        try {
            await feedbackApi.submitServiceFeedback(activeRequest.id, payload);
            setConfirmOpen(false);
            showToast('Servicio confirmado y reseña enviada', 'success');
        } catch (error) {
            showToast(error.message || 'No se pudo enviar la reseña', 'danger');
        } finally {
            setSavingFeedback(false);
        }
    };

    return (
        <DashboardLayout sections={CLIENT_NAV} avatar="JP">
            <div className="ph"><h1>¡Hola, Juan Pablo!</h1><p>Aquí tienes un resumen de tu actividad en ServiYa</p></div>

            <div className="g4" style={{ marginBottom: '22px' }}>
                {STATS.map((s) => <StatCard key={s.label} {...s} />)}
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
                    {REQUESTS.map((r) => (
                        <div className={`req-card ${r.isNew ? 'new' : ''}`} key={r.id}>
                            <div className="req-header">
                                <div className="req-icon"><Icon name={r.icon} size={20} /></div>
                                <div className="req-info"><div className="req-name">{r.name}</div><div className="req-meta">{r.meta}</div></div>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}><span className={`sdot ${r.dot}`} /><span className={`badge ${r.badge}`}>{r.status}</span></div>
                            </div>
                            <div className="req-actions">
                                <button className="btn btn-ghost btn-sm" style={{ border: '1px solid var(--c-border)' }} onClick={() => navigate('/services/1')}>Ver detalle</button>
                                {r.status === 'Pendiente' && <button className="btn btn-ghost btn-sm" style={{ border: '1px solid var(--c-border)' }} onClick={() => setReschedOpen(true)}><Icon name="reschedule" size={13} />Reprogramar</button>}
                                {r.canConfirm && <button className="btn btn-primary btn-sm" onClick={() => openConfirm(r)}><Icon name="check" size={13} />Confirmar servicio</button>}
                                <button className="btn btn-danger btn-sm" onClick={() => showToast('Solicitud cancelada', 'danger')}><Icon name="close" size={13} />Cancelar</button>
                            </div>
                        </div>
                    ))}
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
                    {NOTIFS.map((n, i) => (
                        <div className={`notif-item ${n.unread ? 'unread' : 'read'}`} key={i}>
                            <div className={`notif-ico ${n.cls}`}><Icon name={n.icon} size={16} /></div>
                            <div className="notif-body"><div className="notif-title">{n.title}</div><div className="notif-msg">{n.msg}</div><div className="notif-time">{n.time}</div></div>
                        </div>
                    ))}
                    <Link to="/notifications" className="link-more" style={{ display: 'block', textAlign: 'center', marginTop: '10px' }}>Ver todas las notificaciones →</Link>
                </div>
            </div>

            <ReviewModal
                open={confirmOpen}
                onClose={() => setConfirmOpen(false)}
                title="Confirmar cumplimiento del servicio"
                sub={`¿El servicio ${activeRequest?.name ? `de ${activeRequest.name.toLowerCase()}` : ''} fue realizado correctamente?`}
                ratingLabel="Calificación del servicio (RF-041)"
                reviewLabel="Reseña del servicio (RF-045)"
                confirmLabel="Confirmar servicio"
                tags={serviceTags}
                loading={savingFeedback}
                onConfirm={submitServiceFeedback}
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
            <ToastContainer toasts={toasts} />
        </DashboardLayout>
    );
}

export default ClientDashboardPage;
