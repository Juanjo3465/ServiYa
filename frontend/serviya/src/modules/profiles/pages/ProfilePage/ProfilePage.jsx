import { useState } from "react";
import { useNavigate } from 'react-router-dom';
import { DashboardLayout, Icon, Modal, ToastContainer, useToast, CLIENT_NAV } from '../../../../shared';

import './ProfilePage.css';

const TABS = ['Información personal', 'Mis direcciones', 'Credenciales', 'Mis métricas', 'Roles'];

const ADDRESSES = [
    { line: 'Calle 45 #12-34, Bogotá', sector: 'Chapinero, Bogotá D.C.', main: true },
    { line: 'Carrera 7 #80-21, Bogotá', sector: 'Chapinero Alto, Bogotá D.C.', main: false },
];

const METRICS = [
    { icon: 'check', cls: 'success', n: '96%', l: 'Cumplimiento' },
    { icon: 'close', cls: 'danger', n: '3%', l: 'Cancelaciones' },
    { icon: 'reschedule', cls: 'warn', n: '4%', l: 'Reprogramaciones' },
    { icon: 'star', cls: '', n: '4.8★', l: 'Mi calificación' },
];

const TAGS = [
    { label: 'Puntual (14)', pos: true }, { label: 'Respetuoso (11)', pos: true },
    { label: 'Buen trato (9)', pos: true }, { label: 'No estaba en casa (2)', pos: false },
];

export function ProfilePage() {
    const navigate = useNavigate();
    const { toasts, showToast } = useToast();
    const [tab, setTab] = useState(0);
    const [addrOpen, setAddrOpen] = useState(false);
    const [deleteOpen, setDeleteOpen] = useState(false);

    return (
        <DashboardLayout sections={CLIENT_NAV} avatar="JP">
            <div className="ph"><h1>Mi perfil</h1><p>Gestiona tu información personal y configuración de cuenta</p></div>

            <div className="tabs">
                {TABS.map((t, i) => (
                    <div key={t} className={`tab ${tab === i ? 'active' : ''}`} onClick={() => setTab(i)}>{t}</div>
                ))}
            </div>

            {tab === 0 && (
                <div className="card">
                    <div className="profile-id">
                        <div style={{ position: 'relative' }}>
                            <div className="av av-xl">JP</div>
                            <button className="avatar-edit" onClick={() => showToast('Foto actualizada', 'success')}><Icon name="camera" size={12} strokeWidth={2.5} /></button>
                        </div>
                        <div>
                            <div style={{ fontSize: '18px', fontWeight: 700 }}>Juan Pablo Bernal</div>
                            <div style={{ fontSize: '13px', color: 'var(--c-mid)' }}>jp@email.com · Cliente</div>
                            <span className="badge badge-success" style={{ marginTop: '4px' }}>Cuenta activa</span>
                        </div>
                    </div>
                    <div className="g2">
                        <div className="input-group"><label className="label">Nombre</label><input className="input" defaultValue="Juan Pablo" /></div>
                        <div className="input-group"><label className="label">Apellido</label><input className="input" defaultValue="Bernal Orjuela" /></div>
                    </div>
                    <div className="g2">
                        <div className="input-group"><label className="label">Teléfono</label><div className="input-wrap"><div className="input-ico"><Icon name="phone" size={15} /></div><input className="input" defaultValue="+57 300 000 0000" /></div></div>
                        <div className="input-group"><label className="label">Tipo de perfil</label><select className="input"><option>Persona natural</option><option>Empresa</option></select></div>
                    </div>
                    <div className="input-group"><label className="label">Descripción personal</label><textarea className="input" rows="3" defaultValue="Residente en Bogotá, apasionado por mantener mi hogar en perfectas condiciones." /></div>
                    <div className="note-box"><strong style={{ color: 'var(--c-text)' }}>Datos no editables:</strong> Tipo y número de documento — establecidos al registrarse por seguridad (AES-256-GCM).</div>
                    <button className="btn btn-primary" onClick={() => showToast('Perfil actualizado', 'success')}><Icon name="save" size={15} />Guardar cambios</button>
                </div>
            )}

            {tab === 1 && (
                <div>
                    <div className="page-head">
                        <div style={{ fontSize: '14px', fontWeight: 700 }}>Mis direcciones</div>
                        <button className="btn btn-primary btn-sm" onClick={() => setAddrOpen(true)}><Icon name="plus" size={13} />Agregar dirección</button>
                    </div>
                    {ADDRESSES.map((a, i) => (
                        <div className="card addr-card" key={i} style={a.main ? { borderLeft: '3px solid var(--c-primary)' } : undefined}>
                            <div className="addr-row">
                                <div className="stat-ico" style={{ margin: 0, flexShrink: 0, ...(a.main ? {} : { background: 'var(--c-bg-s)', color: 'var(--c-soft)' }) }}><Icon name="mapPin" size={18} /></div>
                                <div style={{ flex: 1 }}>
                                    <div style={{ fontSize: '13px', fontWeight: 700 }}>{a.line} {a.main && <span className="badge badge-primary">Principal</span>}</div>
                                    <div style={{ fontSize: '12px', color: 'var(--c-mid)', marginTop: '3px' }}>{a.sector}</div>
                                </div>
                                <div style={{ display: 'flex', gap: '5px' }}>
                                    <button className="btn btn-ghost btn-sm" style={{ border: '1px solid var(--c-border)' }} onClick={() => showToast('Dirección editada', 'success')}><Icon name="edit" size={13} /></button>
                                    {!a.main && <button className="btn btn-danger btn-sm" onClick={() => showToast('Dirección eliminada', 'danger')}><Icon name="trash" size={13} /></button>}
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {tab === 2 && (
                <div>
                    <div className="card" style={{ marginBottom: '14px' }}>
                        <div className="card-title">Cambiar correo electrónico</div>
                        <div className="input-group"><label className="label">Correo actual</label><input className="input" defaultValue="jp@email.com" disabled /></div>
                        <div className="input-group"><label className="label">Nuevo correo</label><input className="input" type="email" placeholder="nuevocorreo@ejemplo.com" /></div>
                        <div className="input-group"><label className="label">Confirmar contraseña actual</label><input className="input" type="password" placeholder="••••••••" /></div>
                        <button className="btn btn-primary" onClick={() => showToast('Correo actualizado', 'success')}>Cambiar correo</button>
                    </div>
                    <div className="card">
                        <div className="card-title">Cambiar contraseña</div>
                        <div className="input-group"><label className="label">Contraseña actual</label><input className="input" type="password" placeholder="••••••••" /></div>
                        <div className="input-group"><label className="label">Nueva contraseña</label><input className="input" type="password" placeholder="Mínimo 8 caracteres" /></div>
                        <div className="input-group"><label className="label">Confirmar nueva contraseña</label><input className="input" type="password" placeholder="Repite la contraseña" /></div>
                        <button className="btn btn-primary" onClick={() => showToast('Contraseña actualizada', 'success')}>Cambiar contraseña</button>
                    </div>
                    <div className="danger-zone">
                        <div className="danger-title">Zona de peligro</div>
                        <div style={{ fontSize: '13px', color: 'var(--c-mid)', marginBottom: '14px' }}>Eliminar permanentemente tu cuenta. Esta acción no se puede deshacer (RF-008).</div>
                        <button className="btn btn-danger" onClick={() => setDeleteOpen(true)}><Icon name="trash" size={15} />Eliminar mi cuenta</button>
                    </div>
                </div>
            )}

            {tab === 3 && (
                <div>
                    <div className="g4" style={{ marginBottom: '18px' }}>
                        {METRICS.map((m) => (
                            <div className="stat-card" key={m.l}>
                                <div className={`stat-ico ${m.cls}`}><Icon name={m.icon} size={18} fill={m.icon === 'star' ? 'currentColor' : 'none'} /></div>
                                <div className="stat-n">{m.n}</div>
                                <div className="stat-l">{m.l}</div>
                            </div>
                        ))}
                    </div>
                    <div className="card">
                        <div className="card-title">Etiquetas recibidas de oferentes</div>
                        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '7px' }}>
                            {TAGS.map((t) => (
                                <span key={t.label} className={`profile-tag ${t.pos ? 'pos' : 'neg'}`}>{t.label}</span>
                            ))}
                        </div>
                    </div>
                </div>
            )}

            {tab === 4 && (
                <div className="card">
                    <div className="card-title" style={{ marginBottom: '4px' }}>Tus roles actuales</div>
                    <div style={{ fontSize: '13px', color: 'var(--c-mid)', marginBottom: '16px' }}>Un mismo usuario puede ser cliente y oferente sin crear otra cuenta.</div>
                    <div className="role-row role-active">
                        <div className="stat-ico" style={{ margin: 0 }}><Icon name="user" size={18} /></div>
                        <div style={{ flex: 1 }}><div style={{ fontWeight: 700 }}>Cliente</div><div style={{ fontSize: '12px', color: 'var(--c-mid)' }}>Puedes buscar y contratar servicios</div></div>
                        <span className="badge badge-success">Activo</span>
                    </div>
                    <div className="role-row role-inactive">
                        <div className="stat-ico" style={{ margin: 0, background: 'var(--c-bg-s)', color: 'var(--c-soft)' }}><Icon name="wrench" size={18} /></div>
                        <div style={{ flex: 1 }}><div style={{ fontWeight: 700, color: 'var(--c-text)' }}>Oferente</div><div style={{ fontSize: '12px', color: 'var(--c-mid)' }}>Ofrece tus servicios y llega a más clientes</div></div>
                        <button className="btn btn-primary btn-sm" onClick={() => { showToast('¡Rol de oferente adquirido! Redirigiendo...', 'success'); setTimeout(() => navigate('/offerer/dashboard'), 1200); }}>Adquirir rol</button>
                    </div>
                </div>
            )}

            <Modal open={addrOpen} onClose={() => setAddrOpen(false)}>
                <div className="modal-title">Agregar dirección</div>
                <div className="modal-sub">La dirección se validará con Google Maps.</div>
                <div className="input-group"><label className="label">Dirección</label><div className="input-wrap"><div className="input-ico"><Icon name="mapPin" size={15} /></div><input className="input" placeholder="Calle 80 #45-12" /></div></div>
                <div className="g2"><div className="input-group"><label className="label">Ciudad</label><input className="input" defaultValue="Bogotá" /></div><div className="input-group"><label className="label">Sector</label><input className="input" placeholder="Chapinero" /></div></div>
                <label className="check-line"><input type="checkbox" /> Establecer como dirección principal</label>
                <div style={{ display: 'flex', gap: '8px' }}>
                    <button className="btn btn-ghost btn-full" onClick={() => setAddrOpen(false)}>Cancelar</button>
                    <button className="btn btn-primary btn-full" onClick={() => { setAddrOpen(false); showToast('Dirección agregada', 'success'); }}>Guardar</button>
                </div>
            </Modal>

            <Modal open={deleteOpen} onClose={() => setDeleteOpen(false)}>
                <div className="modal-title" style={{ color: 'var(--c-danger)' }}>Eliminar cuenta</div>
                <div className="modal-sub">Esta acción es irreversible. Tu cuenta será marcada como eliminada y no podrás volver a iniciar sesión.</div>
                <div className="input-group"><label className="label">Confirma tu contraseña</label><input className="input" type="password" placeholder="••••••••" /></div>
                <div style={{ display: 'flex', gap: '8px' }}>
                    <button className="btn btn-ghost btn-full" onClick={() => setDeleteOpen(false)}>Cancelar</button>
                    <button className="btn btn-danger btn-full" onClick={() => navigate('/login')}>Eliminar definitivamente</button>
                </div>
            </Modal>
            <ToastContainer toasts={toasts} />
        </DashboardLayout>
    );
}

export default ProfilePage;
