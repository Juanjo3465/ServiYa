import { useState, useEffect } from "react";
import { Link, useNavigate } from 'react-router-dom';
import { AppNavbar, Icon, Modal, Stars, WhatsAppButton, ToastContainer, useToast, reportApi } from '../../../../shared';

import './ServiceDetailPage.css';

const SLOTS = [
    { lbl: 'Hoy', day: 'Mayo 10', hr: '9am–5pm' },
    { lbl: 'Mañana', day: 'Mayo 11', hr: '8am–3pm' },
    { lbl: 'Miér', day: 'Mayo 12', hr: '10am–6pm' },
    { lbl: 'Jue', day: 'Mayo 13', hr: 'No disponible', off: true },
    { lbl: 'Vie', day: 'Mayo 14', hr: '9am–5pm' },
];

const REVIEWS = [
    { id: 1, userId: 101, initials: 'MA', name: 'María A.', rating: 5, time: 'Hace 2 días', text: 'Excelente servicio. Carlos llegó puntual y resolvió el problema rápidamente. Totalmente recomendado.' },
    { id: 2, userId: 102, initials: 'RL', name: 'Roberto L.', rating: 4, time: 'Hace 1 semana', text: 'Buen trabajo, llegó un poco tarde pero el resultado fue perfecto. Lo volvería a contratar.' },
    { id: 3, userId: 103, initials: 'SG', name: 'Sara G.', rating: 5, time: 'Hace 2 semanas', text: 'Super profesional y honesto con los precios. El mejor plomero que he contratado.' },
];

export function ServiceDetailPage() {
    const navigate = useNavigate();
    const { toasts, showToast } = useToast();
    const [activeSlot, setActiveSlot] = useState(0);
    const [reportOpen, setReportOpen] = useState(false);
    const [reportCategory, setReportCategory] = useState('Comportamiento inapropiado');
    const [customCategory, setCustomCategory] = useState('');
    const [reportReason, setReportReason] = useState('');
    const [feedbackReportOpen, setFeedbackReportOpen] = useState(false);
    const [feedbackTarget, setFeedbackTarget] = useState(null);
    const [feedbackReportCategory, setFeedbackReportCategory] = useState('Contenido inapropiado');
    const [feedbackCustomCategory, setFeedbackCustomCategory] = useState('');
    const [feedbackReportReason, setFeedbackReportReason] = useState('');

    const handleOpenFeedbackReport = (review) => {
        setFeedbackTarget(review);
        setFeedbackReportCategory('Contenido inapropiado');
        setFeedbackCustomCategory('');
        setFeedbackReportReason('');
        setFeedbackReportOpen(true);
    };

    const handleSubmitFeedbackReport = async () => {
        if (!feedbackTarget) return;

        try {
            await reportApi.createServiceFeedbackReport({
                reportedUserId: feedbackTarget.userId,
                category: feedbackReportCategory,
                customCategory: feedbackCustomCategory,
                reason: feedbackReportReason,
                serviceFeedbackId: feedbackTarget.id,
            });
            setFeedbackReportOpen(false);
            setFeedbackTarget(null);
            setFeedbackReportCategory('Contenido inapropiado');
            setFeedbackCustomCategory('');
            setFeedbackReportReason('');
            showToast('Reporte de reseña enviado al administrador', 'success');
        } catch (error) {
            showToast(error.message || 'No se pudo enviar el reporte de la reseña', 'danger');
        }
    };

    return (
        <>
            <AppNavbar avatar="JP" links={[{ to: '/services', label: '← Resultados' }]} />

            <div className="breadcrumb">
                <Link to="/">Inicio</Link> / <Link to="/services">Plomería</Link> / Reparación de tuberías
            </div>

            <div className="detail-layout">
                <div>
                    <div className="service-hero">
                        <Icon name="wrench" size={64} strokeWidth={1.2} />
                        <div className="service-hero-av"><span className="badge badge-success">Disponible hoy</span></div>
                    </div>
                    <div className="detail-title">Reparación de tuberías y filtraciones</div>
                    <div className="detail-meta">
                        <span style={{ display: 'flex', alignItems: 'center', gap: '5px' }}><Stars rating={4.9} size={12} /><strong style={{ color: 'var(--c-text)' }}>4.9</strong>(32 reseñas)</span>
                        <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}><Icon name="mapPin" size={13} />2.3 km</span>
                        <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}><Icon name="clock" size={13} />Duración est.: 2–4 hrs</span>
                        <span className="badge badge-primary">Plomería</span>
                    </div>

                    <div className="sec-card">
                        <div className="oferer-row" onClick={() => navigate('/offerers/1')}>
                            <div className="av av-lg">CM</div>
                            <div style={{ flex: 1 }}>
                                <div style={{ fontSize: '15px', fontWeight: 700, color: 'var(--c-text)' }}>Carlos Martínez</div>
                                <div style={{ fontSize: '12px', color: 'var(--c-mid)', margin: '2px 0' }}>Plomero profesional · 8 años de experiencia</div>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}><Stars rating={5} size={11} /><span style={{ fontSize: '12px', color: 'var(--c-soft)' }}>4.9 · 42 servicios</span><span className="badge badge-success" style={{ fontSize: '10px' }}>Verificado</span></div>
                            </div>
                            <span style={{ color: 'var(--c-primary)', fontSize: '12px', fontWeight: 600 }}>Ver perfil →</span>
                        </div>
                    </div>

                    <div className="sec-card">
                        <h3>Descripción del servicio</h3>
                        <div style={{ fontSize: '13px', color: 'var(--c-mid)', lineHeight: 1.7 }}>
                            Servicio profesional de reparación de tuberías, filtraciones y fugas de agua en hogares y apartamentos. Más de 8 años de experiencia con las herramientas adecuadas para resolver cualquier problema hidráulico.<br /><br />
                            <strong style={{ color: 'var(--c-text)' }}>Incluye:</strong> diagnóstico, mano de obra y materiales básicos. Materiales especiales se cotizan aparte.<br /><br />
                            <strong style={{ color: 'var(--c-text)' }}>Radio de operación:</strong> hasta 10 km desde su dirección principal.
                        </div>
                    </div>

                    <div className="sec-card">
                        <h3>Disponibilidad</h3>
                        <div className="avail-grid">
                            {SLOTS.map((s, i) => (
                                <div
                                    key={i}
                                    className={`avail-slot ${s.off ? 'off' : ''} ${activeSlot === i && !s.off ? 'active' : ''}`}
                                    onClick={() => !s.off && setActiveSlot(i)}
                                >
                                    <div className="avail-lbl">{s.lbl}</div>
                                    <div className="avail-day">{s.day}</div>
                                    <div className="avail-hr">{s.hr}</div>
                                </div>
                            ))}
                        </div>
                    </div>

                    <div className="sec-card">
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
                            <h3 style={{ margin: 0 }}>Reseñas (32)</h3>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}><Stars rating={5} /><strong style={{ fontSize: '18px' }}>4.9</strong></div>
                        </div>
                        <div className="tag-chips" style={{ marginBottom: '16px' }}>
                            <span className="tag-chip pos">Puntual</span><span className="tag-chip pos">Profesional</span>
                            <span className="tag-chip pos">Buen trabajo</span><span className="tag-chip neg">Tardó un poco</span>
                        </div>
                        {REVIEWS.map((r, i) => (
                            <div className="review-item" key={i}>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '9px', marginBottom: '7px', flexWrap: 'wrap' }}>
                                    <div className="av av-sm">{r.initials}</div>
                                    <strong style={{ fontSize: '13px' }}>{r.name}</strong>
                                    <Stars rating={r.rating} size={11} />
                                    <span style={{ fontSize: '11px', color: 'var(--c-soft)', marginLeft: 'auto' }}>{r.time}</span>
                                    <button className="btn btn-ghost btn-sm" style={{ color: 'var(--c-danger)', padding: '4px 8px', marginLeft: '4px' }} onClick={() => handleOpenFeedbackReport(r)}>
                                        <Icon name="alertTriangle" size={13} />Reportar reseña
                                    </button>
                                </div>
                                <div style={{ fontSize: '13px', color: 'var(--c-mid)', lineHeight: 1.6 }}>{r.text}</div>
                            </div>
                        ))}
                        <button className="btn btn-ghost btn-full" style={{ marginTop: '8px', border: '1px solid var(--c-border)' }}>Ver todas las reseñas →</button>
                    </div>
                </div>

                <div>
                    <div className="booking-card">
                        <div className="booking-price">desde $50.000</div>
                        <div style={{ fontSize: '12px', color: 'var(--c-mid)', marginBottom: '16px' }}>/ por servicio</div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '18px', fontSize: '12px', color: 'var(--c-mid)' }}><Stars rating={5} size={12} /> 4.9 · 32 reseñas</div>

                        <div className="input-group"><label className="label">Fecha</label><input className="input" type="date" /></div>
                        <div className="input-group"><label className="label">Hora</label><select className="input"><option>9:00 AM</option><option>10:00 AM</option><option>11:00 AM</option><option>2:00 PM</option><option>3:00 PM</option></select></div>
                        <div className="input-group"><label className="label">Dirección</label><select className="input"><option>Calle 45 #12-34, Bogotá</option><option>Carrera 7 #80-21, Bogotá</option><option>+ Agregar dirección</option></select></div>

                        <div className="metrics-mini">
                            <div className="mm"><div className="mm-val">98%</div><div className="mm-lbl">Cumplimiento</div></div>
                            <div className="mm"><div className="mm-val">2%</div><div className="mm-lbl">Cancelaciones</div></div>
                            <div className="mm"><div className="mm-val">42</div><div className="mm-lbl">Servicios</div></div>
                            <div className="mm"><div className="mm-val">4.9★</div><div className="mm-lbl">Calificación</div></div>
                        </div>

                        <button className="btn btn-primary btn-full btn-lg" onClick={() => navigate('/request-service')} style={{ marginBottom: '8px' }}>
                            <Icon name="calendar" size={17} />Solicitar servicio
                        </button>
                        <WhatsAppButton block label="Contactar por WhatsApp" iconSize={16} onClick={() => showToast('Abriendo WhatsApp...', 'success')} />
                        <div className="verified-note">
                            <Icon name="shield" size={13} style={{ color: 'var(--c-success)' }} />Perfil verificado por ServiYa
                        </div>
                        <button className="btn btn-ghost btn-sm" style={{ marginTop: '10px', color: 'var(--c-danger)' }} onClick={() => setReportOpen(true)}>
                            <Icon name="alertTriangle" size={13} />Reportar oferente
                        </button>
                    </div>
                </div>
            </div>

            <Modal open={reportOpen} onClose={() => setReportOpen(false)}>
                <div className="modal-title">Reportar oferente</div>
                <div className="modal-sub">Indica el motivo del reporte. El administrador revisará el caso.</div>
                <div className="input-group"><label className="label">Categoría</label><select className="input" value={reportCategory} onChange={(e) => setReportCategory(e.target.value)}><option>Comportamiento inapropiado</option><option>No se presentó</option><option>Fraude</option><option>Otra</option></select></div>
                {reportCategory === 'Otra' && <div className="input-group"><label className="label">Categoría personalizada</label><input className="input" value={customCategory} onChange={(e) => setCustomCategory(e.target.value)} placeholder="Escribe la categoría" /></div>}
                <div className="input-group"><label className="label">Descripción</label><textarea className="input" value={reportReason} onChange={(e) => setReportReason(e.target.value)} placeholder="Describe lo que ocurrió..." /></div>
                <div style={{ display: 'flex', gap: '8px' }}>
                    <button className="btn btn-ghost btn-full" onClick={() => setReportOpen(false)}>Cancelar</button>
                    <button className="btn btn-danger btn-full" onClick={async () => {
                        try {
                            await reportApi.createRequestReport({
                                reportedUserId: 2,
                                category: reportCategory,
                                customCategory,
                                reason: reportReason,
                                requestId: 1,
                            });
                            setReportOpen(false);
                            setReportCategory('Comportamiento inapropiado');
                            setCustomCategory('');
                            setReportReason('');
                            showToast('Reporte enviado al administrador', 'success');
                        } catch (error) {
                            showToast(error.message || 'No se pudo enviar el reporte', 'danger');
                        }
                    }}>Enviar reporte</button>
                </div>
            </Modal>

            <Modal open={feedbackReportOpen} onClose={() => setFeedbackReportOpen(false)}>
                <div className="modal-title">Reportar reseña</div>
                <div className="modal-sub">{feedbackTarget ? `Estás reportando la reseña de ${feedbackTarget.name}.` : 'Indica el motivo del reporte.'}</div>
                <div className="input-group"><label className="label">Categoría</label><select className="input" value={feedbackReportCategory} onChange={(e) => setFeedbackReportCategory(e.target.value)}><option>Contenido inapropiado</option><option>Spam</option><option>Amenaza o acoso</option><option>Otra</option></select></div>
                {feedbackReportCategory === 'Otra' && <div className="input-group"><label className="label">Categoría personalizada</label><input className="input" value={feedbackCustomCategory} onChange={(e) => setFeedbackCustomCategory(e.target.value)} placeholder="Escribe la categoría" /></div>}
                <div className="input-group"><label className="label">Descripción</label><textarea className="input" value={feedbackReportReason} onChange={(e) => setFeedbackReportReason(e.target.value)} placeholder="Describe lo que ocurrió..." /></div>
                <div style={{ display: 'flex', gap: '8px' }}>
                    <button className="btn btn-ghost btn-full" onClick={() => setFeedbackReportOpen(false)}>Cancelar</button>
                    <button className="btn btn-danger btn-full" onClick={handleSubmitFeedbackReport}>Enviar reporte</button>
                </div>
            </Modal>
            <ToastContainer toasts={toasts} />
        </>
    );
}

export default ServiceDetailPage;
