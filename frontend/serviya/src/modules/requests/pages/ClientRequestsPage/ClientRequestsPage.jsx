import { useState } from "react";
import { Link, useNavigate } from 'react-router-dom';
import { DashboardLayout, Icon, Modal, ToastContainer, useToast, CLIENT_NAV } from '../../../../shared';
import { ReviewModal } from '../../components/ReviewModal/ReviewModal';

import './ClientRequestsPage.css';

const FILTERS = ['Todas (3)', 'Pendientes (1)', 'Aceptadas (2)'];

const REQUESTS = [
    { id: 'SR-4821', initials: 'CM', name: 'Reparación de tuberías', offerer: 'Carlos Martínez · Plomería', date: 'Lun 12 mayo, 9:00 AM', addr: 'Calle 45 #12-34', price: 'desde $50.000', status: 'Pendiente', badge: 'badge-warn', age: 'Hace 5 min', isNew: true, canConfirm: false, canReschedule: true },
    { id: 'SR-4815', initials: 'ML', name: 'Limpieza de hogar', offerer: 'María López · Limpieza', date: 'Mié 14 mayo, 10:00 AM', addr: 'Carrera 7 #80-21', price: 'desde $60.000', status: 'Aceptada', badge: 'badge-success', age: 'Hace 2 días', isNew: false, canConfirm: true, canReschedule: false },
    { id: 'SR-4810', initials: 'AR', name: 'Instalación eléctrica', offerer: 'Ana Rodríguez · Electricidad', date: 'Vie 16 mayo, 2:00 PM', addr: 'Calle 45 #12-34', price: 'desde $80.000', status: 'Aceptada', badge: 'badge-success', age: '', isNew: false, canConfirm: false, canReschedule: false },
];

export function ClientRequestsPage() {
    const navigate = useNavigate();
    const { toasts, showToast } = useToast();
    const [filter, setFilter] = useState(0);
    const [confirmOpen, setConfirmOpen] = useState(false);
    const [reschedOpen, setReschedOpen] = useState(false);
    const [reportOpen, setReportOpen] = useState(false);

    return (
        <DashboardLayout sections={CLIENT_NAV} avatar="JP">
            <div className="page-head">
                <div className="ph" style={{ margin: 0 }}><h1>Solicitudes activas</h1><p>Gestiona tus solicitudes en curso</p></div>
                <Link to="/services" className="btn btn-primary btn-sm"><Icon name="plus" size={13} />Nueva solicitud</Link>
            </div>

            <div className="filter-chips">
                {FILTERS.map((f, i) => (
                    <div key={f} className={`chip ${filter === i ? 'active' : ''}`} onClick={() => setFilter(i)}>{f}</div>
                ))}
            </div>

            {REQUESTS.map((r) => (
                <div className={`req-card ${r.isNew ? 'new' : ''}`} key={r.id}>
                    <div className="rq-top">
                        <div style={{ display: 'flex', alignItems: 'center', gap: '7px' }}>
                            <span className={`badge ${r.badge}`}>{r.status}</span>
                            <span style={{ fontSize: '11px', color: 'var(--c-soft)' }}>#{r.id}{r.age && ` · ${r.age}`}</span>
                        </div>
                        <button className="btn btn-ghost btn-sm" onClick={() => setReportOpen(true)} style={{ color: 'var(--c-danger)' }}><Icon name="alertTriangle" size={13} />Reportar</button>
                    </div>
                    <div className="rq-main">
                        <div className="av av-md">{r.initials}</div>
                        <div style={{ flex: 1 }}>
                            <div style={{ fontSize: '14px', fontWeight: 700 }}>{r.name}</div>
                            <div style={{ fontSize: '12px', color: 'var(--c-mid)', marginTop: '2px' }}>{r.offerer}</div>
                            <div className="rq-meta">
                                <span><Icon name="calendar" size={12} />{r.date}</span>
                                <span><Icon name="mapPin" size={12} />{r.addr}</span>
                                <span><Icon name="dollar" size={12} />{r.price}</span>
                            </div>
                        </div>
                    </div>
                    <div style={{ display: 'flex', gap: '7px', flexWrap: 'wrap' }}>
                        <button className="btn btn-ghost btn-sm" style={{ border: '1px solid var(--c-border)' }} onClick={() => navigate('/services/1')}>Ver detalle</button>
                        {r.canReschedule && <button className="btn btn-ghost btn-sm" style={{ border: '1px solid var(--c-border)' }} onClick={() => setReschedOpen(true)}><Icon name="reschedule" size={13} />Reprogramar</button>}
                        {r.canConfirm && <button className="btn btn-primary btn-sm" onClick={() => setConfirmOpen(true)}><Icon name="check" size={13} />Confirmar servicio</button>}
                        <button className="btn btn-danger btn-sm" onClick={() => showToast('Solicitud cancelada. Oferente notificado', 'danger')}><Icon name="close" size={13} />Cancelar</button>
                    </div>
                </div>
            ))}

            <ReviewModal
                open={confirmOpen}
                onClose={() => setConfirmOpen(false)}
                title="Confirmar servicio"
                sub="¿El servicio fue realizado correctamente?"
                ratingLabel="Calificación"
                reviewLabel="Reseña"
                confirmLabel="Confirmar"
                onConfirm={() => { setConfirmOpen(false); showToast('Servicio confirmado y reseña enviada', 'success'); }}
            />

            <Modal open={reschedOpen} onClose={() => setReschedOpen(false)}>
                <div className="modal-title">Reprogramar solicitud</div>
                <div className="modal-sub">Elige nueva fecha y hora disponible</div>
                <div className="input-group"><label className="label">Nueva fecha</label><input className="input" type="date" /></div>
                <div className="input-group"><label className="label">Nueva hora</label><select className="input"><option>9:00 AM</option><option>10:00 AM</option><option>2:00 PM</option></select></div>
                <div style={{ display: 'flex', gap: '8px' }}>
                    <button className="btn btn-ghost btn-full" onClick={() => setReschedOpen(false)}>Cancelar</button>
                    <button className="btn btn-primary btn-full" onClick={() => { setReschedOpen(false); showToast('Solicitud reprogramada', 'success'); }}>Confirmar</button>
                </div>
            </Modal>

            <Modal open={reportOpen} onClose={() => setReportOpen(false)}>
                <div className="modal-title">Reportar oferente</div>
                <div className="modal-sub">El administrador revisará el caso.</div>
                <div className="input-group"><label className="label">Motivo</label><select className="input"><option>No se presentó</option><option>Comportamiento inapropiado</option><option>Fraude</option><option>Otro</option></select></div>
                <div className="input-group"><label className="label">Descripción</label><textarea className="input" rows="3" placeholder="Describe lo que ocurrió..." /></div>
                <div style={{ display: 'flex', gap: '8px' }}>
                    <button className="btn btn-ghost btn-full" onClick={() => setReportOpen(false)}>Cancelar</button>
                    <button className="btn btn-danger btn-full" onClick={() => { setReportOpen(false); showToast('Reporte enviado', 'info'); }}>Enviar reporte</button>
                </div>
            </Modal>
            <ToastContainer toasts={toasts} />
        </DashboardLayout>
    );
}

export default ClientRequestsPage;
