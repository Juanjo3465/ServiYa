import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from 'react-router-dom';
import { AppNavbar, Icon, Modal, Stars, WhatsAppButton, ToastContainer, useToast, feedbackApi, metricsApi } from '../../../../shared';

import './ServiceDetailPage.css';

const SLOTS = [
    { lbl: 'Hoy', day: 'Mayo 10', hr: '9am–5pm' },
    { lbl: 'Mañana', day: 'Mayo 11', hr: '8am–3pm' },
    { lbl: 'Miér', day: 'Mayo 12', hr: '10am–6pm' },
    { lbl: 'Jue', day: 'Mayo 13', hr: 'No disponible', off: true },
    { lbl: 'Vie', day: 'Mayo 14', hr: '9am–5pm' },
];

const REVIEWS = [
    { initials: 'MA', name: 'María A.', rating: 5, time: 'Hace 2 días', text: 'Excelente servicio. Carlos llegó puntual y resolvió el problema rápidamente. Totalmente recomendado.' },
    { initials: 'RL', name: 'Roberto L.', rating: 4, time: 'Hace 1 semana', text: 'Buen trabajo, llegó un poco tarde pero el resultado fue perfecto. Lo volvería a contratar.' },
    { initials: 'SG', name: 'Sara G.', rating: 5, time: 'Hace 2 semanas', text: 'Super profesional y honesto con los precios. El mejor plomero que he contratado.' },
];

export function ServiceDetailPage() {
    const navigate = useNavigate();
    const { id } = useParams();
    const { toasts, showToast } = useToast();
    const [activeSlot, setActiveSlot] = useState(0);
    const [reportOpen, setReportOpen] = useState(false);
    const [metrics, setMetrics] = useState(null);
    const [reviews, setReviews] = useState(REVIEWS);
    const [loadingReviews, setLoadingReviews] = useState(false);
    const serviceId = id ?? 1;

    useEffect(() => {
        let active = true;
        metricsApi.getServiceMetrics(serviceId)
            .then((data) => active && setMetrics(data))
            .catch(() => active && setMetrics(null));

        setLoadingReviews(true);
        feedbackApi.getServiceFeedbackList(serviceId, { size: 6 })
            .then((page) => {
                if (!active) return;
                const content = page?.content ?? [];
                if (content.length === 0) return;
                setReviews(content.map((item) => ({
                    initials: `C${item.clientId ?? ''}`.slice(0, 2),
                    name: `Cliente ${item.clientId ?? ''}`,
                    rating: item.rating ?? 0,
                    time: formatDate(item.createdAt),
                    text: item.comment || 'Sin comentario adicional.',
                    tags: item.tags ?? [],
                })));
            })
            .catch(() => active && setReviews(REVIEWS))
            .finally(() => active && setLoadingReviews(false));

        return () => {
            active = false;
        };
    }, [serviceId]);

    const ratingValue = Number(metrics?.averageRating ?? 4.9);
    const ratingCount = metrics?.totalRatings ?? 32;
    const reviewCount = metrics?.totalComments ?? reviews.length;
    const visibleTags = useMemo(
        () => [...new Set(reviews.flatMap((review) => review.tags ?? []))].slice(0, 6),
        [reviews]
    );

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
                        <span style={{ display: 'flex', alignItems: 'center', gap: '5px' }}><Stars rating={ratingValue} size={12} /><strong style={{ color: 'var(--c-text)' }}>{ratingValue.toFixed(1)}</strong>({reviewCount} reseñas)</span>
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
                                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}><Stars rating={ratingValue} size={11} /><span style={{ fontSize: '12px', color: 'var(--c-soft)' }}>{ratingValue.toFixed(1)} · 42 servicios</span><span className="badge badge-success" style={{ fontSize: '10px' }}>Verificado</span></div>
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
                            <h3 style={{ margin: 0 }}>Reseñas ({reviewCount})</h3>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}><Stars rating={ratingValue} /><strong style={{ fontSize: '18px' }}>{ratingValue.toFixed(1)}</strong></div>
                        </div>
                        <div className="tag-chips" style={{ marginBottom: '16px' }}>
                            {(visibleTags.length ? visibleTags : ['Puntual', 'Profesional', 'Buen trabajo', 'Tardó un poco']).map((tag, index) => (
                                <span className={`tag-chip ${index === 3 ? 'neg' : 'pos'}`} key={tag}>{tag}</span>
                            ))}
                        </div>
                        {loadingReviews && <div className="reviews-empty">Cargando reseñas...</div>}
                        {reviews.map((r, i) => (
                            <div className="review-item" key={`${r.name}-${i}`}>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '9px', marginBottom: '7px' }}>
                                    <div className="av av-sm">{r.initials}</div>
                                    <strong style={{ fontSize: '13px' }}>{r.name}</strong>
                                    <Stars rating={r.rating} size={11} />
                                    <span style={{ fontSize: '11px', color: 'var(--c-soft)', marginLeft: 'auto' }}>{r.time}</span>
                                </div>
                                <div style={{ fontSize: '13px', color: 'var(--c-mid)', lineHeight: 1.6 }}>{r.text}</div>
                            </div>
                        ))}
                        {!loadingReviews && reviews.length === 0 && <div className="reviews-empty">Este servicio aún no tiene reseñas.</div>}
                    </div>
                </div>

                <div>
                    <div className="booking-card">
                        <div className="booking-price">desde $50.000</div>
                        <div style={{ fontSize: '12px', color: 'var(--c-mid)', marginBottom: '16px' }}>/ por servicio</div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '18px', fontSize: '12px', color: 'var(--c-mid)' }}><Stars rating={ratingValue} size={12} /> {ratingValue.toFixed(1)} · {reviewCount} reseñas</div>

                        <div className="input-group"><label className="label">Fecha</label><input className="input" type="date" /></div>
                        <div className="input-group"><label className="label">Hora</label><select className="input"><option>9:00 AM</option><option>10:00 AM</option><option>11:00 AM</option><option>2:00 PM</option><option>3:00 PM</option></select></div>
                        <div className="input-group"><label className="label">Dirección</label><select className="input"><option>Calle 45 #12-34, Bogotá</option><option>Carrera 7 #80-21, Bogotá</option><option>+ Agregar dirección</option></select></div>

                        <div className="metrics-mini">
                            <div className="mm"><div className="mm-val">98%</div><div className="mm-lbl">Cumplimiento</div></div>
                            <div className="mm"><div className="mm-val">2%</div><div className="mm-lbl">Cancelaciones</div></div>
                            <div className="mm"><div className="mm-val">42</div><div className="mm-lbl">Servicios</div></div>
                            <div className="mm"><div className="mm-val">{ratingValue.toFixed(1)}★</div><div className="mm-lbl">Calificación</div></div>
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
                <div className="input-group"><label className="label">Motivo</label><select className="input"><option>Comportamiento inapropiado</option><option>No se presentó</option><option>Fraude</option><option>Otro</option></select></div>
                <div className="input-group"><label className="label">Descripción</label><textarea className="input" placeholder="Describe lo que ocurrió..." /></div>
                <div style={{ display: 'flex', gap: '8px' }}>
                    <button className="btn btn-ghost btn-full" onClick={() => setReportOpen(false)}>Cancelar</button>
                    <button className="btn btn-danger btn-full" onClick={() => { setReportOpen(false); showToast('Reporte enviado al administrador', 'info'); }}>Enviar reporte</button>
                </div>
            </Modal>
            <ToastContainer toasts={toasts} />
        </>
    );
}

export default ServiceDetailPage;

function formatDate(value) {
    if (!value) return 'Reciente';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return 'Reciente';
    return new Intl.DateTimeFormat('es-CO', { day: 'numeric', month: 'short' }).format(date);
}
