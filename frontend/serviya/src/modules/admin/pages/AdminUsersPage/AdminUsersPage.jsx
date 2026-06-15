import { useState } from "react";
import { Icon, Modal, ToastContainer, useToast } from '../../../../shared';
import { AdminNavbar } from '../../components/AdminNavbar/AdminNavbar';
import { AdminSidebar } from '../../components/AdminSidebar/AdminSidebar';

import './AdminUsersPage.css';

const USERS = [
    { id: 'USR-001', initials: 'JP', name: 'Juan Pablo Bernal', roles: [{ label: 'Cliente', cls: 'badge-primary' }], email: 'jp@email.com', date: '12 mayo', status: 'Activo', statusBadge: 'badge-success', banned: false },
    { id: 'USR-002', initials: 'CM', name: 'Carlos Martínez', roles: [{ label: 'Oferente', cls: 'badge-primary' }, { label: 'Cliente', cls: 'badge-gray' }], email: 'carlos@email.com', date: '3 enero', status: 'Activo', statusBadge: 'badge-success', banned: false },
    { id: 'USR-003', initials: 'ML', name: 'María López', roles: [{ label: 'Oferente', cls: 'badge-primary' }], email: 'maria@email.com', date: '15 marzo', status: 'Activo', statusBadge: 'badge-success', banned: false },
    { id: 'USR-010', initials: 'XX', name: 'Usuario Baneado', roles: [{ label: 'Cliente', cls: 'badge-primary' }], email: 'bad@email.com', date: '1 abril', status: 'Baneado', statusBadge: 'badge-danger', banned: true },
];

export function AdminUsersPage() {
    const { toasts, showToast } = useToast();
    const [mgmtOpen, setMgmtOpen] = useState(false);
    const [banOpen, setBanOpen] = useState(false);

    return (
        <>
            <AdminNavbar />
            <div className="admin-layout-container">
                <AdminSidebar />
                <main className="main-content">
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px', flexWrap: 'wrap', gap: '10px' }}>
                        <div className="ph" style={{ margin: 0 }}><h1>Gestión de usuarios</h1><p>248 usuarios registrados en la plataforma</p></div>
                    </div>

                    <div className="users-filters">
                        <div className="input-wrap" style={{ flex: 1, minWidth: '200px' }}><div className="input-ico"><Icon name="search" size={15} /></div><input className="input" placeholder="Buscar por nombre o correo..." /></div>
                        <select className="input" style={{ width: 'auto', padding: '9px 12px', fontSize: '13px' }}><option>Todos los roles</option><option>Cliente</option><option>Oferente</option><option>Administrador</option></select>
                        <select className="input" style={{ width: 'auto', padding: '9px 12px', fontSize: '13px' }}><option>Todos los estados</option><option>Activos</option><option>Baneados</option></select>
                    </div>

                    <div className="tbl-wrap">
                        <table>
                            <thead><tr><th>Usuario</th><th>Roles (RF-067)</th><th>Correo</th><th>Registro</th><th>Estado</th><th>Acciones</th></tr></thead>
                            <tbody>
                                {USERS.map((u) => (
                                    <tr key={u.id}>
                                        <td>
                                            <div style={{ display: 'flex', alignItems: 'center', gap: '9px' }}>
                                                <div className="av av-sm" style={u.banned ? { background: 'var(--c-danger-bg)', color: 'var(--c-danger)' } : undefined}>{u.initials}</div>
                                                <div><div style={{ fontWeight: 700, fontSize: '13px' }}>{u.name}</div><div style={{ fontSize: '11px', color: 'var(--c-soft)' }}>ID: {u.id}</div></div>
                                            </div>
                                        </td>
                                        <td>{u.roles.map((r, j) => <span key={j} className={`badge ${r.cls}`} style={j > 0 ? { marginLeft: '4px' } : undefined}>{r.label}</span>)}</td>
                                        <td style={{ fontSize: '12px', color: 'var(--c-mid)' }}>{u.email}</td>
                                        <td style={{ fontSize: '12px', color: 'var(--c-soft)' }}>{u.date}</td>
                                        <td><span className={`badge ${u.statusBadge}`}>{u.status}</span></td>
                                        <td>
                                            <div style={{ display: 'flex', gap: '5px' }}>
                                                <button className="btn btn-ghost btn-sm" style={{ border: '1px solid var(--c-border)' }} onClick={() => setMgmtOpen(true)}><Icon name="search" size={13} /></button>
                                                {u.banned
                                                    ? <button className="btn btn-success btn-sm" onClick={() => showToast('Usuario desbaneado (RF-070)', 'success')}><Icon name="check" size={13} /></button>
                                                    : <button className="btn btn-danger btn-sm" onClick={() => setBanOpen(true)}><Icon name="ban" size={13} /></button>}
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'center', marginTop: '18px', gap: '5px' }}>
                        <button className="btn btn-primary btn-sm">1</button><button className="btn btn-ghost btn-sm">2</button><button className="btn btn-ghost btn-sm">3</button>
                    </div>
                </main>
            </div>

            <Modal open={mgmtOpen} onClose={() => setMgmtOpen(false)}>
                <div className="modal-title">Gestionar usuario</div>
                <div className="user-head"><div className="av av-md">JP</div><div><div style={{ fontWeight: 700 }}>Juan Pablo Bernal</div><div style={{ fontSize: '12px', color: 'var(--c-mid)' }}>jp@email.com</div></div></div>
                <div className="input-group">
                    <label className="label">Roles asignados (RF-065/066/067)</label>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '9px' }}>
                        <label className="ck-row"><input type="checkbox" defaultChecked /> <strong>Cliente</strong> — puede contratar servicios</label>
                        <label className="ck-row"><input type="checkbox" /> <strong>Oferente</strong> — puede publicar y prestar servicios</label>
                        <label className="ck-row"><input type="checkbox" disabled /> <strong>Administrador</strong> — solo se asigna vía otro admin</label>
                    </div>
                </div>
                <div style={{ display: 'flex', gap: '7px' }}>
                    <button className="btn btn-ghost btn-full" onClick={() => setMgmtOpen(false)}>Cancelar</button>
                    <button className="btn btn-primary btn-full" onClick={() => { setMgmtOpen(false); showToast('Roles actualizados', 'success'); }}>Guardar</button>
                </div>
            </Modal>

            <Modal open={banOpen} onClose={() => setBanOpen(false)}>
                <div className="modal-title">Banear usuario (RF-069)</div>
                <div className="modal-sub">El usuario será bloqueado y recibirá una notificación por correo (RF-063).</div>
                <div className="input-group"><label className="label">Motivo del baneo</label><select className="input"><option>Comportamiento inapropiado</option><option>Fraude</option><option>Suplantación</option><option>Reincidencia</option><option>Violación de términos</option></select></div>
                <div className="input-group"><label className="label">Descripción (se envía al usuario)</label><textarea className="input" rows="3" placeholder="Detalla el motivo del baneo..." /></div>
                <div style={{ display: 'flex', gap: '8px' }}>
                    <button className="btn btn-ghost btn-full" onClick={() => setBanOpen(false)}>Cancelar</button>
                    <button className="btn btn-danger btn-full" onClick={() => { setBanOpen(false); showToast('Usuario baneado. Correo enviado.', 'danger'); }}><Icon name="ban" size={15} />Confirmar baneo</button>
                </div>
            </Modal>
            <ToastContainer toasts={toasts} />
        </>
    );
}

export default AdminUsersPage;
