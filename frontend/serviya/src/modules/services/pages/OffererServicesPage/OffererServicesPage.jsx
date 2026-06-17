import { useState } from "react";
import { DashboardLayout, Icon, Modal, Stars, ToastContainer, useToast, OFFERER_NAV } from '../../../../shared';

import './OffererServicesPage.css';

const SERVICES = [
    { id: 1, name: 'Reparación de tuberías', category: 'Plomería', desc: 'Servicio profesional de reparación de tuberías y filtraciones...', price: '$50.000/hr', duration: '2-4 horas', radius: '10 km radio', rating: 4.9, requests: 32, active: true },
    { id: 2, name: 'Destape de cañerías', category: 'Plomería', desc: 'Destape profesional de tuberías obstruidas...', price: '$35.000/hr', duration: '1-2 horas', radius: '8 km radio', rating: 5.0, requests: 9, active: true },
    { id: 3, name: 'Instalación de grifos', category: 'Plomería', desc: 'Instalación y cambio de grifería...', price: null, active: false },
];

function ServiceModal({ open, onClose, title, sub, onSave, edit }) {
    return (
        <Modal open={open} onClose={onClose} maxWidth={520}>
            <div className="modal-title">{title}</div>
            <div className="modal-sub">{sub}</div>
            <div className="input-group"><label className="label">Título del servicio</label><input className="input" placeholder="ej: Reparación de tuberías" defaultValue={edit ? 'Reparación de tuberías' : ''} /></div>
            <div className="g2">
                <div className="input-group"><label className="label">Categoría</label><select className="input"><option>Plomería</option><option>Electricidad</option><option>Limpieza</option><option>Jardinería</option><option>Pintura</option></select></div>
                <div className="input-group"><label className="label">Precio / hora (COP)</label><input className="input" type="number" placeholder="50000" defaultValue={edit ? 50000 : ''} /></div>
            </div>
            <div className="g2">
                <div className="input-group"><label className="label">Duración estimada (min)</label><input className="input" type="number" placeholder="120" defaultValue={edit ? 120 : ''} /></div>
                <div className="input-group"><label className="label">Radio operación (km)</label><input className="input" type="number" placeholder="10" defaultValue={edit ? 10 : ''} /></div>
            </div>
            <div className="input-group"><label className="label">Descripción</label><textarea className="input" rows="3" placeholder="Describe tu servicio detalladamente..." defaultValue={edit ? 'Servicio profesional de reparación de tuberías...' : ''} /></div>
            <div style={{ display: 'flex', gap: '8px' }}>
                <button className="btn btn-ghost btn-full" onClick={onClose}>Cancelar</button>
                <button className="btn btn-primary btn-full" onClick={onSave}>{!edit && <Icon name="check" size={15} />}{edit ? 'Guardar cambios' : 'Crear servicio'}</button>
            </div>
        </Modal>
    );
}

export function OffererServicesPage() {
    const { toasts, showToast } = useToast();
    const [newOpen, setNewOpen] = useState(false);
    const [editOpen, setEditOpen] = useState(false);

    return (
        <DashboardLayout sections={OFFERER_NAV} avatar="CM">
            <div className="page-head">
                <div className="ph" style={{ margin: 0 }}><h1>Mis servicios</h1><p>Crea y administra tu catálogo de servicios</p></div>
                <button className="btn btn-primary" onClick={() => setNewOpen(true)}><Icon name="plus" size={15} />Nuevo servicio</button>
            </div>

            <div className="g2" style={{ gap: '14px' }}>
                {SERVICES.map((s) => (
                    <div className={`card svc-card ${!s.active ? 'svc-inactive' : ''}`} key={s.id}>
                        <div className="svc-actions">
                            {s.active ? <span className="badge badge-success">Activo</span> : <span className="badge badge-gray">Inactivo</span>}
                            {s.active && (
                                <>
                                    <button className="btn btn-ghost btn-sm" onClick={() => setEditOpen(true)} style={{ border: '1px solid var(--c-border)' }}><Icon name="edit" size={13} /></button>
                                    <button className="btn btn-danger btn-sm" onClick={() => showToast('Servicio eliminado', 'danger')}><Icon name="trash" size={13} /></button>
                                </>
                            )}
                        </div>
                        <div className={`svc-ico ${!s.active ? 'svc-ico-off' : ''}`}><Icon name="wrench" size={22} /></div>
                        <div className="svc-name">{s.name}</div>
                        <span className={`badge ${s.active ? 'badge-primary' : 'badge-gray'}`} style={{ marginBottom: '10px' }}>{s.category}</span>
                        <div className="svc-desc">{s.desc}</div>
                        {s.active && (
                            <div className="svc-stats">
                                <span><Icon name="dollar" size={12} />desde {s.price}</span>
                                <span><Icon name="clock" size={12} />{s.duration}</span>
                                <span><Icon name="mapPin" size={12} />{s.radius}</span>
                            </div>
                        )}
                        <div className="divider" />
                        <div className="svc-foot">
                            {s.active
                                ? <span><Stars rating={s.rating} size={12} /> {s.rating} · {s.requests} sol.</span>
                                : <span style={{ color: 'var(--c-soft)' }}>Sin solicitudes aún</span>}
                            <label className="svc-visible"><input type="checkbox" defaultChecked={s.active} /> Visible</label>
                        </div>
                    </div>
                ))}
            </div>

            <ServiceModal open={newOpen} onClose={() => setNewOpen(false)} title="Nuevo servicio" sub="Crea una nueva oferta de servicio para tu catálogo" onSave={() => { setNewOpen(false); showToast('Servicio creado exitosamente', 'success'); }} />
            <ServiceModal open={editOpen} onClose={() => setEditOpen(false)} edit title="Editar servicio" sub="Actualiza la información de tu servicio" onSave={() => { setEditOpen(false); showToast('Servicio actualizado', 'success'); }} />
            <ToastContainer toasts={toasts} />
        </DashboardLayout>
    );
}

export default OffererServicesPage;
