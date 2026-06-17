import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AppNavbar, Icon, Modal } from '../../../../shared';

import './RequestServicePage.css';

const STEPS = ['Servicio', 'Fecha/Hora', 'Dirección', 'Confirmar'];
const TIME_SLOTS = [
    { t: '9:00 AM' }, { t: '10:00 AM' }, { t: '11:00 AM' }, { t: '12:00 PM', na: true },
    { t: '2:00 PM' }, { t: '3:00 PM' }, { t: '4:00 PM' }, { t: '5:00 PM', na: true },
];

function StepBar({ step }) {
    return (
        <div className="step-bar">
            {STEPS.map((label, i) => {
                const n = i + 1;
                const cls = n < step ? 'step-done' : n === step ? 'step-active' : 'step-pending';
                return (
                    <React.Fragment key={label}>
                        {i > 0 && <div className={`step-line ${n <= step ? 'done' : ''}`} />}
                        <div className="step-item">
                            <div className={`step-circle ${cls}`}>{n < step ? <Icon name="check" size={15} strokeWidth={2.5} /> : n}</div>
                            <div className={`step-label ${n === step ? 'active' : ''}`}>{label}</div>
                        </div>
                    </React.Fragment>
                );
            })}
        </div>
    );
}

export function RequestServicePage() {
    const navigate = useNavigate();
    const [step, setStep] = useState(2);
    const [hora, setHora] = useState('9:00 AM');
    const [successOpen, setSuccessOpen] = useState(false);

    return (
        <>
            <AppNavbar avatar="JP" showBell={false} />
            <div className="req-wrap">
                <div className="req-card-wizard">
                    <StepBar step={step} />

                    <div className="service-summary">
                        <div className="service-summary-ico"><Icon name="wrench" size={20} /></div>
                        <div>
                            <div style={{ fontSize: '13px', fontWeight: 700, color: 'var(--c-text)' }}>Reparación de tuberías</div>
                            <div style={{ fontSize: '12px', color: 'var(--c-mid)', marginTop: '2px' }}>Carlos Martínez · Plomería · desde $50.000</div>
                        </div>
                    </div>

                    {step === 2 && (
                        <div>
                            <div className="wiz-title">Elige fecha y hora</div>
                            <div className="wiz-sub">Selecciona según la disponibilidad del oferente</div>
                            <div className="input-group"><label className="label">Fecha</label><input className="input" type="date" /></div>
                            <div className="input-group">
                                <label className="label">Horario disponible</label>
                                <div className="time-slots">
                                    {TIME_SLOTS.map((s) => (
                                        <div
                                            key={s.t}
                                            className={`time-slot ${s.na ? 'na' : ''} ${hora === s.t && !s.na ? 'active' : ''}`}
                                            onClick={() => !s.na && setHora(s.t)}
                                        >{s.t}</div>
                                    ))}
                                </div>
                            </div>
                            <div className="wiz-actions">
                                <button className="btn btn-ghost btn-full" onClick={() => navigate('/services/1')}><Icon name="chevronLeft" size={15} />Atrás</button>
                                <button className="btn btn-primary btn-full btn-lg" onClick={() => setStep(3)}>Continuar<Icon name="chevronRight" size={17} /></button>
                            </div>
                        </div>
                    )}

                    {step === 3 && (
                        <div>
                            <div className="wiz-title">Dirección del servicio</div>
                            <div className="wiz-sub">¿Dónde necesitas el servicio?</div>
                            <div className="input-group">
                                <label className="label">Selecciona una dirección</label>
                                <select className="input">
                                    <option>Calle 45 #12-34, Bogotá (Principal)</option>
                                    <option>Carrera 7 #80-21, Bogotá</option>
                                    <option>+ Ingresar nueva dirección</option>
                                </select>
                            </div>
                            <div className="map-placeholder"><Icon name="mapPin" size={20} />&nbsp;Google Maps · Vista de ubicación</div>
                            <div className="input-group"><label className="label">Referencia adicional (opcional)</label><input className="input" placeholder="Piso, apartamento, indicaciones..." /></div>
                            <div className="wiz-actions">
                                <button className="btn btn-ghost btn-full" onClick={() => setStep(2)}><Icon name="chevronLeft" size={15} />Atrás</button>
                                <button className="btn btn-primary btn-full btn-lg" onClick={() => setStep(4)}>Continuar<Icon name="chevronRight" size={17} /></button>
                            </div>
                        </div>
                    )}

                    {step === 4 && (
                        <div>
                            <div className="wiz-title">Confirma tu solicitud</div>
                            <div className="wiz-sub">Revisa los detalles antes de enviar</div>
                            <div className="confirm-box">
                                <div className="confirm-row"><span className="confirm-label">Servicio</span><span className="confirm-value">Reparación de tuberías</span></div>
                                <div className="confirm-row"><span className="confirm-label">Oferente</span><span className="confirm-value">Carlos Martínez</span></div>
                                <div className="confirm-row"><span className="confirm-label">Fecha</span><span className="confirm-value">Lunes 12 de mayo, 2025</span></div>
                                <div className="confirm-row"><span className="confirm-label">Hora</span><span className="confirm-value">{hora}</span></div>
                                <div className="confirm-row"><span className="confirm-label">Dirección</span><span className="confirm-value">Calle 45 #12-34, Bogotá</span></div>
                                <div className="confirm-row"><span className="confirm-label">Precio estimado</span><span className="confirm-value" style={{ color: 'var(--c-primary-d)' }}>desde $50.000</span></div>
                            </div>
                            <div className="confirm-note"><strong>Nota:</strong> El servicio está sujeto a confirmación del oferente. Recibirás una notificación por el canal interno y por correo cuando acepte o rechace tu solicitud.</div>
                            <div className="wiz-actions">
                                <button className="btn btn-ghost btn-full" onClick={() => setStep(3)}><Icon name="chevronLeft" size={15} />Atrás</button>
                                <button className="btn btn-primary btn-full btn-lg" onClick={() => setSuccessOpen(true)}><Icon name="check" size={17} />Enviar solicitud</button>
                            </div>
                        </div>
                    )}
                </div>
            </div>

            <Modal open={successOpen} onClose={() => setSuccessOpen(false)}>
                <div style={{ textAlign: 'center' }}>
                    <div className="success-ico"><Icon name="check" size={30} /></div>
                    <div style={{ fontSize: '20px', fontWeight: 800, marginBottom: '8px' }}>¡Solicitud enviada!</div>
                    <div style={{ fontSize: '13px', color: 'var(--c-mid)', marginBottom: '22px', lineHeight: 1.65 }}>Tu solicitud fue enviada a Carlos Martínez. Te notificaremos por el sistema interno y por correo cuando acepte o rechace.</div>
                    <button className="btn btn-primary btn-full" onClick={() => navigate('/requests')}>Ver mis solicitudes</button>
                </div>
            </Modal>
        </>
    );
}

export default RequestServicePage;
