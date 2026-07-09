import { useState, useEffect } from "react";
import { Link, useNavigate } from 'react-router-dom';
import { DashboardLayout, Icon, Modal, ToastContainer, useToast, CLIENT_NAV, requestApi } from '../../../../shared';
import { ReviewModal } from '../../components/ReviewModal/ReviewModal';
import { STATUS_MAP, formatDate, timeAgo, getInitials, formatPrice } from '../../utils';

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
    const [requests, setRequests] = useState([]);
    const [loading, setLoading] = useState(true);
    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(1);
    const [confirmOpen, setConfirmOpen] = useState(false);
    const [reschedOpen, setReschedOpen] = useState(false);
    const [reportOpen, setReportOpen] = useState(false);

    const fetchRequests = (page = 0) => {
        setLoading(true);
        const params = { page, size: 10 };
        const statusKey = FILTERS[filter].key;
        if (statusKey) {
            params.statuses = statusKey;
        }
        requestApi.getMyClientRequests(params)
            .then(data => {
                setRequests(data.content || []);
                setTotalPages(data.totalPages || 1);
                setCurrentPage(data.number || 0);
            })
            .catch(err => showToast('Error al cargar solicitudes: ' + err.message, 'error'))
            .finally(() => setLoading(false));
    };

    useEffect(() => {
        fetchRequests(0);
    }, [filter]);

    return (
        <DashboardLayout sections={CLIENT_NAV} avatar="JP">
            <div className="page-head">
                <div className="ph" style={{ margin: 0 }}><h1>Solicitudes activas</h1><p>Gestiona tus solicitudes en curso</p></div>
                <Link to="/services" className="btn btn-primary btn-sm"><Icon name="plus" size={13} />Nueva solicitud</Link>
            </div>

            <div className="filter-chips">
                {FILTERS.map((f, i) => (
                    <div key={f.label} className={`chip ${filter === i ? 'active' : ''}`} onClick={() => setFilter(i)}>{f.label}</div>
                ))}
            </div>

            {loading ? (
                <div style={{ textAlign: 'center', padding: '40px 0', color: 'var(--c-soft)' }}>Cargando solicitudes...</div>
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
                                    <button className="btn btn-ghost btn-sm" onClick={() => setReportOpen(true)} style={{ color: 'var(--c-danger)' }}><Icon name="alertTriangle" size={13} />Reportar</button>
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
                                    <button className="btn btn-ghost btn-sm" style={{ border: '1px solid var(--c-border)' }} onClick={() => navigate(`/services/${r.serviceId}`)}>Ver detalle</button>
                                    {canReschedule && <button className="btn btn-ghost btn-sm" style={{ border: '1px solid var(--c-border)' }} onClick={() => setReschedOpen(true)}><Icon name="reschedule" size={13} />Reprogramar</button>}
                                    {canConfirm && <button className="btn btn-primary btn-sm" onClick={() => setConfirmOpen(true)}><Icon name="check" size={13} />Confirmar servicio</button>}
                                    <button className="btn btn-danger btn-sm" onClick={() => showToast('Solicitud cancelada. Oferente notificado', 'danger')}><Icon name="close" size={13} />Cancelar</button>
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
                                    onClick={() => fetchRequests(i)}
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
