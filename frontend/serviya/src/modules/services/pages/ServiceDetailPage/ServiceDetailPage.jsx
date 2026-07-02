import { useState, useEffect } from "react";
import { Link, useNavigate, useParams } from 'react-router-dom';
import { AppNavbar, Icon, Modal, Stars, WhatsAppButton, ToastContainer, useToast, serviceApi } from '../../../../shared';

import './ServiceDetailPage.css';

const DAYS = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];

const getWeekDayName = (dayNum) => {
    const idx = (dayNum - 1) % 7;
    return DAYS[idx] || 'Día';
};

export function ServiceDetailPage() {
    const { id } = useParams();
    const navigate = useNavigate();
    const { toasts, showToast } = useToast();
    const [activeSlot, setActiveSlot] = useState(0);
    const [reportOpen, setReportOpen] = useState(false);

    // Estados para la carga del detalle del servicio
    const [service, setService] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const loadServiceDetail = async () => {
            setLoading(true);
            try {
                const data = await serviceApi.getServiceDetail(id);
                setService(data);
            } catch (err) {
                setError(err.message);
                showToast("Error al cargar detalle del servicio: " + err.message, "error");
            } finally {
                setLoading(false);
            }
        };

        if (id) {
            loadServiceDetail();
        }
    }, [id]);

    if (loading) {
        return (
            <>
                <AppNavbar avatar="JP" links={[{ to: '/services', label: '← Resultados' }]} />
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', minHeight: '60vh', color: 'var(--c-soft)' }}>
                    <div className="loading-spinner" style={{ width: '40px', height: '40px', border: '4px solid var(--c-border)', borderTop: '4px solid var(--c-primary)', borderRadius: '50%', animation: 'spin 1s linear infinite', marginBottom: '15px' }} />
                    <span>Cargando detalles del servicio...</span>
                    <style>{`
                        @keyframes spin {
                            0% { transform: rotate(0deg); }
                            100% { transform: rotate(360deg); }
                        }
                    `}</style>
                </div>
                <ToastContainer toasts={toasts} />
            </>
        );
    }

    if (error || !service) {
        return (
            <>
                <AppNavbar avatar="JP" links={[{ to: '/services', label: '← Resultados' }]} />
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', minHeight: '60vh', color: 'var(--c-soft)', padding: '20px', textAlign: 'center' }}>
                    <Icon name="alertTriangle" size={48} style={{ color: 'var(--c-danger)', marginBottom: '15px' }} />
                    <h3>Error al cargar servicio</h3>
                    <p>{error || "El servicio no existe o no se pudo encontrar."}</p>
                    <Link to="/services" className="btn btn-primary" style={{ marginTop: '15px' }}>Volver al buscador</Link>
                </div>
                <ToastContainer toasts={toasts} />
            </>
        );
    }

    // Datos del servicio
    const rating = service.averageRating !== null && service.averageRating !== undefined ? service.averageRating : 4.5;
    const feedbacksCount = service.feedbacks ? service.feedbacks.length : 0;
    const initials = service.fullName ? service.fullName.substring(0, 2).toUpperCase() : "OF";

    return (
        <>
            <AppNavbar avatar="JP" links={[{ to: '/services', label: '← Resultados' }]} />

            <div className="breadcrumb">
                <Link to="/">Inicio</Link> / <Link to="/services">{service.category?.name || "Categoría"}</Link> / {service.title}
            </div>

            <div className="detail-layout">
                <div>
                    <div className="service-hero">
                        <Icon name="wrench" size={64} strokeWidth={1.2} />
                        <div className="service-hero-av">
                            <span className={`badge ${service.active ? 'badge-success' : 'badge-warn'}`}>
                                {service.active ? 'Disponible' : 'Inactivo'}
                            </span>
                        </div>
                    </div>
                    <div className="detail-title">{service.title}</div>
                    <div className="detail-meta">
                        <span style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
                            <Stars rating={rating} size={12} />
                            <strong style={{ color: 'var(--c-text)' }}>{rating.toFixed(1)}</strong>
                            ({feedbacksCount} reseñas)
                        </span>
                        <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                            <Icon name="mapPin" size={13} />
                            {service.operationRadiusKm ? `${service.operationRadiusKm} km` : 'Sin límite'}
                        </span>
                        {service.averageDurationMinutes && (
                            <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                                <Icon name="clock" size={13} />
                                Duración est.: {Math.floor(service.averageDurationMinutes / 60)}h {service.averageDurationMinutes % 60}m
                            </span>
                        )}
                        <span className="badge badge-primary">{service.category?.name || "General"}</span>
                    </div>

                    <div className="sec-card">
                        <div className="oferer-row" onClick={() => navigate(`/offerers/${service.userId}`)}>
                            <div className="av av-lg">{initials}</div>
                            <div style={{ flex: 1 }}>
                                <div style={{ fontSize: '15px', fontWeight: 700, color: 'var(--c-text)' }}>{service.fullName || "Oferente"}</div>
                                <div style={{ fontSize: '12px', color: 'var(--c-mid)', margin: '2px 0' }}>{service.specialty || "Especialista en ServiYa"}</div>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                    <Stars rating={rating} size={11} />
                                    <span style={{ fontSize: '12px', color: 'var(--c-soft)' }}>
                                        {rating.toFixed(1)} · {feedbacksCount} servicios
                                    </span>
                                    <span className="badge badge-success" style={{ fontSize: '10px' }}>Verificado</span>
                                </div>
                            </div>
                            <span style={{ color: 'var(--c-primary)', fontSize: '12px', fontWeight: 600 }}>Ver perfil →</span>
                        </div>
                    </div>

                    <div className="sec-card">
                        <h3>Descripción del servicio</h3>
                        <div style={{ fontSize: '13px', color: 'var(--c-mid)', lineHeight: 1.7 }}>
                            {service.description || "Sin descripción proporcionada."}
                            {service.operationRadiusKm && (
                                <>
                                    <br /><br />
                                    <strong style={{ color: 'var(--c-text)' }}>Radio de operación:</strong> hasta {service.operationRadiusKm} km desde la ubicación principal.
                                </>
                            )}
                        </div>
                    </div>

                    <div className="sec-card">
                        <h3>Disponibilidad</h3>
                        {service.availability && service.availability.length > 0 ? (
                            <div className="avail-grid">
                                {service.availability.map((avail, i) => {
                                    const dayName = getWeekDayName(avail.weekDay);
                                    const start = avail.startTime.substring(0, 5);
                                    const end = avail.endTime.substring(0, 5);
                                    return (
                                        <div
                                            key={avail.id || i}
                                            className={`avail-slot ${activeSlot === i ? 'active' : ''}`}
                                            onClick={() => setActiveSlot(i)}
                                        >
                                            <div className="avail-lbl">{dayName}</div>
                                            <div className="avail-day">Día {avail.weekDay}</div>
                                            <div className="avail-hr">{start}–{end}</div>
                                        </div>
                                    );
                                })}
                            </div>
                        ) : (
                            <div style={{ fontSize: '13px', color: 'var(--c-soft)', textAlign: 'center', padding: '10px' }}>
                                No hay horarios de disponibilidad configurados por el oferente.
                            </div>
                        )}
                    </div>

                    <div className="sec-card">
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
                            <h3 style={{ margin: 0 }}>Reseñas ({feedbacksCount})</h3>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                                <Stars rating={rating} />
                                <strong style={{ fontSize: '18px' }}>{rating.toFixed(1)}</strong>
                            </div>
                        </div>
                        {service.feedbacks && service.feedbacks.length > 0 ? (
                            service.feedbacks.map((r, i) => {
                                const fbInitials = r.userName ? r.userName.substring(0, 2).toUpperCase() : "US";
                                return (
                                    <div className="review-item" key={r.id || i}>
                                        <div style={{ display: 'flex', alignItems: 'center', gap: '9px', marginBottom: '7px' }}>
                                            <div className="av av-sm">{fbInitials}</div>
                                            <strong style={{ fontSize: '13px' }}>{r.userName || "Usuario"}</strong>
                                            <Stars rating={5} size={11} />
                                            <span style={{ fontSize: '11px', color: 'var(--c-soft)', marginLeft: 'auto' }}>
                                                {r.createdAt ? new Date(r.createdAt).toLocaleDateString() : 'Hace poco'}
                                            </span>
                                        </div>
                                        <div style={{ fontSize: '13px', color: 'var(--c-mid)', lineHeight: 1.6 }}>{r.comment}</div>
                                    </div>
                                );
                            })
                        ) : (
                            <div style={{ fontSize: '13px', color: 'var(--c-soft)', textAlign: 'center', padding: '10px' }}>
                                Aún no hay reseñas para este servicio.
                            </div>
                        )}
                    </div>
                </div>

                <div>
                    <div className="booking-card">
                        <div className="booking-price">desde ${service.priceHourly ? service.priceHourly.toLocaleString() : '0'}</div>
                        <div style={{ fontSize: '12px', color: 'var(--c-mid)', marginBottom: '16px' }}>/ por hora de servicio</div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '18px', fontSize: '12px', color: 'var(--c-mid)' }}>
                            <Stars rating={rating} size={12} /> {rating.toFixed(1)} · {feedbacksCount} reseñas
                        </div>

                        <div className="input-group"><label className="label">Fecha</label><input className="input" type="date" /></div>
                        <div className="input-group">
                            <label className="label">Hora</label>
                            <select className="input">
                                <option>9:00 AM</option>
                                <option>10:00 AM</option>
                                <option>11:00 AM</option>
                                <option>2:00 PM</option>
                                <option>3:00 PM</option>
                            </select>
                        </div>
                        <div className="input-group">
                            <label className="label">Dirección</label>
                            <select className="input">
                                <option>Calle 45 #12-34, Bogotá</option>
                                <option>Carrera 7 #80-21, Bogotá</option>
                                <option>+ Agregar dirección</option>
                            </select>
                        </div>

                        <div className="metrics-mini">
                            <div className="mm"><div className="mm-val">100%</div><div className="mm-lbl">Cumplimiento</div></div>
                            <div className="mm"><div className="mm-val">0%</div><div className="mm-lbl">Cancelaciones</div></div>
                            <div className="mm"><div className="mm-val">{feedbacksCount}</div><div className="mm-lbl">Servicios</div></div>
                            <div className="mm"><div className="mm-val">{rating.toFixed(1)}★</div><div className="mm-lbl">Calificación</div></div>
                        </div>

                        <button className="btn btn-primary btn-full btn-lg" onClick={() => navigate('/request-service')} style={{ marginBottom: '8px' }}>
                            <Icon name="calendar" size={17} />Solicitar servicio
                        </button>
                        {service.whatsappNumber && (
                            <WhatsAppButton 
                                block 
                                label="Contactar por WhatsApp" 
                                iconSize={16} 
                                onClick={() => {
                                    showToast('Abriendo WhatsApp...', 'success');
                                    window.open(`https://wa.me/${service.whatsappNumber}`, '_blank');
                                }} 
                            />
                        )}
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
