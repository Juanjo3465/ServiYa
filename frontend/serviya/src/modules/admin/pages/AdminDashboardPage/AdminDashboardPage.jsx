import { useState } from "react";
import { Link } from 'react-router-dom';
import { Icon, Modal, StatCard, ToastContainer, useToast } from '../../../../shared';
import { AdminNavbar } from '../../components/AdminNavbar/AdminNavbar';
import { AdminSidebar } from '../../components/AdminSidebar/AdminSidebar';

import './AdminDashboardPage.css';

const STATS = [
    { icon: 'users', value: '248', label: 'Usuarios registrados', change: '+12 esta semana', changeUp: true },
    { icon: 'wrench', value: '312', label: 'Servicios activos', variant: 'success', change: '+8 hoy', changeUp: true },
    { icon: 'alertTriangle', value: '7', label: 'Reportes pendientes', variant: 'danger', change: '3 críticos', changeUp: false },
    { icon: 'alertCircle', value: '3', label: 'Usuarios baneados', variant: 'warn' },
];

const REPORTS = [
    { type: 'Solicitud', typeBadge: 'badge-danger', reporter: 'JP', reporterName: 'Juan P.', reported: 'CM', reportedName: 'Carlos M.', reason: 'No se presentó', date: '12 mayo', priority: 'Alta', prioBadge: 'badge-danger' },
    { type: 'Reseña', typeBadge: 'badge-warn', reporter: 'SR', reporterName: 'Sandra R.', reported: 'ML', reportedName: 'María L.', reason: 'Contenido inapropiado', date: '11 mayo', priority: 'Media', prioBadge: 'badge-warn' },
    { type: 'Solicitud', typeBadge: 'badge-primary', reporter: 'MV', reporterName: 'Mario V.', reported: 'AR', reportedName: 'Ana R.', reason: 'Fraude', date: '10 mayo', priority: 'Alta', prioBadge: 'badge-danger' },
];

const USERS = [
    { initials: 'JP', name: 'Juan Pablo Bernal', roles: [{ label: 'Cliente', cls: 'badge-primary' }], email: 'jp@email.com', date: '12 mayo', status: 'Activo', statusBadge: 'badge-success', banned: false },
    { initials: 'CM', name: 'Carlos Martínez', roles: [{ label: 'Oferente', cls: 'badge-primary' }, { label: 'Cliente', cls: 'badge-gray' }], email: 'carlos@email.com', date: '3 enero', status: 'Activo', statusBadge: 'badge-success', banned: false },
    { initials: 'XX', name: 'Usuario Baneado', roles: [{ label: 'Cliente', cls: 'badge-primary' }], email: 'bad@email.com', date: '1 abril', status: 'Baneado', statusBadge: 'badge-danger', banned: true },
];

export function AdminDashboardPage() {
    const { toasts, showToast } = useToast();
    const [reportOpen, setReportOpen] = useState(false);
    const [userOpen, setUserOpen] = useState(false);
    const [banOpen, setBanOpen] = useState(false);

    return (
        <>
            <AdminNavbar />
            <div className="admin-layout-container">
                <AdminSidebar />
                <main className="main-content">
                    <div className="ph"><h1>Panel de administración</h1><p>Gestión y moderación de la plataforma ServiYa</p></div>

                    <div className="g4" style={{ marginBottom: '22px' }}>
                        {STATS.map((s) => <StatCard key={s.label} {...s} />)}
                    </div>

                    <div className="sec-title">Reportes pendientes <span className="badge badge-danger">7</span></div>
                    <div className="tbl-wrap" style={{ marginBottom: '14px' }}>
                        <table>
                            <thead><tr><th>Tipo</th><th>Reportante</th><th>Reportado</th><th>Motivo</th><th>Fecha</th><th>Prioridad</th><th>Acciones</th></tr></thead>
                            <tbody>
                                {REPORTS.map((r, i) => (
                                    <tr key={i}>
                                        <td><span className={`badge ${r.typeBadge}`}>{r.type}</span></td>
                                        <td><div className="cell-user"><div className="av av-xs">{r.reporter}</div>{r.reporterName}</div></td>
                                        <td><div className="cell-user"><div className="av av-xs">{r.reported}</div>{r.reportedName}</div></td>
                                        <td style={{ color: 'var(--c-mid)', fontSize: '12px' }}>{r.reason}</td>
                                        <td style={{ fontSize: '12px', color: 'var(--c-soft)' }}>{r.date}</td>
                                        <td><span className={`badge ${r.prioBadge}`}>{r.priority}</span></td>
                                        <td><button className="btn btn-primary btn-sm" onClick={() => setReportOpen(true)}><Icon name="search" size={13} />Revisar</button></td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                    <Link to="/admin/reports" className="btn btn-outline btn-sm" style={{ marginBottom: '22px' }}>Ver todos los reportes →</Link>

                    <div className="sec-title" style={{ marginTop: '22px' }}>Usuarios recientes</div>
                    <div className="tbl-wrap" style={{ marginBottom: '14px' }}>
                        <table>
                            <thead><tr><th>Usuario</th><th>Roles</th><th>Correo</th><th>Registro</th><th>Estado</th><th>Acciones</th></tr></thead>
                            <tbody>
                                {USERS.map((u, i) => (
                                    <tr key={i}>
                                        <td><div className="cell-user"><div className="av av-sm" style={u.banned ? { background: 'var(--c-danger-bg)', color: 'var(--c-danger)' } : undefined}>{u.initials}</div><strong>{u.name}</strong></div></td>
                                        <td>{u.roles.map((r, j) => <span key={j} className={`badge ${r.cls}`} style={j > 0 ? { marginLeft: '4px' } : undefined}>{r.label}</span>)}</td>
                                        <td style={{ fontSize: '12px', color: 'var(--c-mid)' }}>{u.email}</td>
                                        <td style={{ fontSize: '12px', color: 'var(--c-soft)' }}>{u.date}</td>
                                        <td><span className={`badge ${u.statusBadge}`}>{u.status}</span></td>
                                        <td>
                                            <div style={{ display: 'flex', gap: '5px' }}>
                                                <button className="btn btn-ghost btn-sm" style={{ border: '1px solid var(--c-border)' }} onClick={() => setUserOpen(true)}><Icon name="search" size={13} />Ver</button>
                                                {u.banned
                                                    ? <button className="btn btn-success btn-sm" onClick={() => showToast('Usuario desbaneado', 'success')}><Icon name="check" size={13} />Desbanear</button>
                                                    : <button className="btn btn-danger btn-sm" onClick={() => setBanOpen(true)}><Icon name="ban" size={13} />Banear</button>}
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                    <Link to="/admin/users" className="btn btn-outline btn-sm">Ver todos los usuarios →</Link>
                </main>
            </div>

            <Modal open={reportOpen} onClose={() => setReportOpen(false)} maxWidth={520}>
                <div className="modal-title">Gestionar reporte</div>
                <div className="modal-sub">Reporte de Juan P. contra Carlos M. — "No se presentó al servicio"</div>
                <div className="report-context">Solicitud #SR-4821 · Reparación de tuberías · 12 mayo, 9am · Calle 45 #12-34</div>
                <div className="input-group"><label className="label">Acción tomada (RF-071)</label><select className="input"><option>Advertencia</option><option>Penalización en métricas</option><option>Baneo</option><option>Sin consecuencias</option></select></div>
                <div className="input-group"><label className="label">Registro de acciones</label><textarea className="input" placeholder="Describe las acciones tomadas para resolver el reporte..." /></div>
                <div className="input-group">
                    <label className="label">Notificar usuarios afectados (RF-060)</label>
                    <label className="ck-row"><input type="checkbox" defaultChecked /> Notificar al reportante (Juan P.)</label>
                    <label className="ck-row"><input type="checkbox" defaultChecked /> Notificar al reportado (Carlos M.)</label>
                </div>
                <label className="ck-row" style={{ marginBottom: '16px' }}><input type="checkbox" /> Marcar solicitud como no prestado (RF-074)</label>
                <div style={{ display: 'flex', gap: '7px', flexWrap: 'wrap' }}>
                    <button className="btn btn-ghost btn-full" onClick={() => setReportOpen(false)}>Cancelar</button>
                    <button className="btn btn-danger" onClick={() => { setReportOpen(false); showToast('Acción ejecutada. Usuarios notificados.', 'success'); }}><Icon name="shield" size={15} />Ejecutar y cerrar reporte</button>
                </div>
            </Modal>

            <Modal open={userOpen} onClose={() => setUserOpen(false)}>
                <div className="modal-title">Detalle de usuario</div>
                <div className="user-head"><div className="av av-lg">JP</div><div><div style={{ fontSize: '16px', fontWeight: 700 }}>Juan Pablo Bernal</div><div style={{ fontSize: '12px', color: 'var(--c-mid)' }}>jp@email.com · Registrado 12 mayo</div></div></div>
                <div className="input-group">
                    <label className="label">Roles asignados (RF-067)</label>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                        <label className="ck-row"><input type="checkbox" defaultChecked /> Cliente</label>
                        <label className="ck-row"><input type="checkbox" /> Oferente</label>
                        <label className="ck-row"><input type="checkbox" disabled /> Administrador (solo via otro admin)</label>
                    </div>
                </div>
                <div style={{ display: 'flex', gap: '7px' }}>
                    <button className="btn btn-ghost btn-full" onClick={() => setUserOpen(false)}>Cancelar</button>
                    <button className="btn btn-primary btn-full" onClick={() => { setUserOpen(false); showToast('Roles actualizados', 'success'); }}>Guardar roles</button>
                </div>
            </Modal>

            <Modal open={banOpen} onClose={() => setBanOpen(false)}>
                <div className="modal-title">Banear usuario</div>
                <div className="modal-sub">El usuario no podrá acceder a ninguna función de la plataforma. Se le notificará por correo.</div>
                <div className="input-group"><label className="label">Motivo del baneo</label><select className="input"><option>Comportamiento inapropiado</option><option>Fraude</option><option>Suplantación de identidad</option><option>Violación de términos</option></select></div>
                <div className="input-group"><label className="label">Descripción (se enviará al usuario por correo)</label><textarea className="input" placeholder="Describe detalladamente el motivo del baneo..." /></div>
                <div style={{ display: 'flex', gap: '8px' }}>
                    <button className="btn btn-ghost btn-full" onClick={() => setBanOpen(false)}>Cancelar</button>
                    <button className="btn btn-danger btn-full" onClick={() => { setBanOpen(false); showToast('Usuario baneado. Notificación enviada por correo', 'danger'); }}><Icon name="ban" size={15} />Confirmar baneo</button>
                </div>
            </Modal>
            <ToastContainer toasts={toasts} />
        </>
    );
}

export default AdminDashboardPage;
