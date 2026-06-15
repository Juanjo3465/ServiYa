import { useState } from "react";
import { Link, useNavigate } from 'react-router-dom';
import { Icon, ToastContainer, useToast } from '../../../../shared';

import './RegisterPage.css';

export function RegisterPage() {
    const navigate = useNavigate();
    const { toasts, showToast } = useToast();
    const [step, setStep] = useState(1);
    const [type, setType] = useState('c');
    const [terms, setTerms] = useState(false);
    const [dataConsent, setDataConsent] = useState(false);

    const handleRegister = () => {
        if (!terms || !dataConsent) {
            showToast('Debes aceptar los términos y política de datos', 'danger');
            return;
        }
        showToast('¡Cuenta creada! Redirigiendo...', 'success');
        setTimeout(() => navigate('/dashboard'), 1200);
    };

    return (
        <>
            <nav className="nav">
                <Link to="/" className="nav-logo"><img src="/logo.svg" alt="ServiYa" style={{ height: '24px' }} /></Link>
                <div className="nav-spacer" />
                <div className="nav-actions">
                    <span style={{ fontSize: '13px', color: 'var(--c-mid)' }} className="hide-mob">¿Ya tienes cuenta?</span>
                    <Link to="/login" className="btn btn-outline btn-sm">Iniciar sesión</Link>
                </div>
            </nav>

            <div className="reg-wrap">
                <div className="reg-card">
                    <div style={{ textAlign: 'center', marginBottom: '22px' }}>
                        <img src="/logo.svg" style={{ height: '30px', margin: '0 auto 16px' }} alt="ServiYa" />
                        <div style={{ fontSize: '22px', fontWeight: 800, marginBottom: '3px' }}>Crear cuenta</div>
                        <div style={{ fontSize: '13px', color: 'var(--c-mid)' }}>Únete a miles de usuarios en ServiYa</div>
                    </div>

                    <div className="step-dots">
                        {[1, 2, 3].map((n) => (
                            <div key={n} className={`sdot-step ${step === n ? 'active' : ''}`} />
                        ))}
                    </div>

                    {step === 1 && (
                        <div>
                            <p className="reg-q">¿Cómo quieres unirte?</p>
                            <div className="type-sel">
                                <div className={`type-card ${type === 'c' ? 'active' : ''}`} onClick={() => setType('c')}>
                                    <Icon name="user" size={30} style={{ color: 'var(--c-primary)', margin: '0 auto 8px' }} />
                                    <div className="type-card-name">Cliente</div>
                                    <div className="type-card-desc">Busca y contrata servicios para tu hogar</div>
                                </div>
                                <div className={`type-card ${type === 'o' ? 'active' : ''}`} onClick={() => setType('o')}>
                                    <Icon name="wrench" size={30} style={{ color: 'var(--c-primary)', margin: '0 auto 8px' }} />
                                    <div className="type-card-name">Oferente</div>
                                    <div className="type-card-desc">Ofrece tus servicios y llega a más clientes</div>
                                </div>
                            </div>
                            <p className="reg-note">Puedes cambiar de rol más adelante desde tu perfil</p>
                            <button className="btn btn-primary btn-full btn-lg" onClick={() => setStep(2)}>
                                Continuar<Icon name="chevronRight" size={17} />
                            </button>
                        </div>
                    )}

                    {step === 2 && (
                        <div>
                            <div className="g2">
                                <div className="input-group"><label className="label">Nombre</label><input className="input" placeholder="Juan" /></div>
                                <div className="input-group"><label className="label">Apellido</label><input className="input" placeholder="Pérez" /></div>
                            </div>
                            <div className="input-group">
                                <label className="label">Correo electrónico</label>
                                <div className="input-wrap"><div className="input-ico"><Icon name="mail" size={15} /></div><input className="input" type="email" placeholder="tucorreo@ejemplo.com" /></div>
                            </div>
                            <div className="input-group">
                                <label className="label">Teléfono</label>
                                <div className="input-wrap"><div className="input-ico"><Icon name="phone" size={15} /></div><input className="input" type="tel" placeholder="+57 300 000 0000" /></div>
                            </div>
                            <div className="input-group">
                                <label className="label">Contraseña</label>
                                <div className="input-wrap"><div className="input-ico"><Icon name="lock" size={15} /></div><input className="input" type="password" placeholder="Mínimo 8 caracteres" /></div>
                            </div>
                            <div className="input-group" style={{ marginBottom: '20px' }}>
                                <label className="label">Confirmar contraseña</label>
                                <div className="input-wrap"><div className="input-ico"><Icon name="lock" size={15} /></div><input className="input" type="password" placeholder="Repite tu contraseña" /></div>
                            </div>
                            <div style={{ display: 'flex', gap: '8px' }}>
                                <button className="btn btn-ghost btn-full" onClick={() => setStep(1)}><Icon name="chevronLeft" size={15} />Atrás</button>
                                <button className="btn btn-primary btn-full btn-lg" onClick={() => setStep(3)}>Continuar<Icon name="chevronRight" size={17} /></button>
                            </div>
                        </div>
                    )}

                    {step === 3 && (
                        <div>
                            <p className="reg-q">Consentimiento de uso de datos</p>
                            <div className="consent-box">
                                <label><input type="checkbox" checked={terms} onChange={(e) => setTerms(e.target.checked)} /> Acepto los <a>Términos de uso</a> y la <a>Política de privacidad</a> de ServiYa.</label>
                            </div>
                            <div className="consent-box">
                                <label><input type="checkbox" checked={dataConsent} onChange={(e) => setDataConsent(e.target.checked)} /> Autorizo el uso de mis <strong>datos personales (PII)</strong> conforme a la <a>Política de tratamiento de datos</a>. Entiendo que mis datos como documento, teléfono y dirección serán cifrados con AES-256-GCM.</label>
                            </div>
                            <div className="consent-box">
                                <label><input type="checkbox" /> Deseo recibir notificaciones y novedades de ServiYa por correo. <span style={{ color: 'var(--c-soft)' }}>(Opcional)</span></label>
                            </div>
                            <div style={{ display: 'flex', gap: '8px', marginTop: '16px' }}>
                                <button className="btn btn-ghost btn-full" onClick={() => setStep(2)}><Icon name="chevronLeft" size={15} />Atrás</button>
                                <button className="btn btn-primary btn-full btn-lg" onClick={handleRegister}><Icon name="check" size={17} />Crear cuenta</button>
                            </div>
                            <div className="or-line">o regístrate con</div>
                            <button className="google-reg-btn" onClick={() => showToast('Google OAuth no disponible en mockup', 'warn')}>
                                <svg width="16" height="16" viewBox="0 0 24 24"><path fill="#EA4335" d="M21.35 11.1H12v2.8h5.35c-.5 2.4-2.6 4.1-5.35 4.1-3.3 0-6-2.7-6-6s2.7-6 6-6c1.55 0 2.95.6 4 1.55l2.1-2.1C16.95 4 14.6 3 12 3 6.48 3 2 7.48 2 13s4.48 10 10 10c5.52 0 9.72-3.88 9.72-9.9 0-.66-.06-1.3-.17-1.9l-.2-1z" /></svg>
                                Continuar con Google
                            </button>
                            <p className="reg-foot">¿Ya tienes cuenta? <Link to="/login">Inicia sesión</Link></p>
                        </div>
                    )}
                </div>
            </div>
            <ToastContainer toasts={toasts} />
        </>
    );
}

export default RegisterPage;
