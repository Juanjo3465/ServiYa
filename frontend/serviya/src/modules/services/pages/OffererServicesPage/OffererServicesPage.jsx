import { useForm } from 'react-hook-form';
import { useState, useEffect } from "react";
import { useNavigate } from 'react-router-dom';
import { DashboardLayout, Icon, Modal, Stars, ToastContainer, useToast, OFFERER_NAV, serviceApi, profileApi, categoryApi, isAuthenticated } from '../../../../shared';

import './OffererServicesPage.css';

function ServiceModal({
    onClose,
    title,
    sub,
    onSave,
    edit,
    categories,
    editedService
}) {

    const {
        register,
        handleSubmit,
        reset
    } = useForm({
        defaultValues: editedService || {}
    });

    useEffect(() => {
        if (!editedService) return;

        reset(editedService);
    }, [editedService, reset]);

    return (
        <Modal open={true} onClose={onClose} maxWidth={520}>
            <form onSubmit={handleSubmit(onSave)}>
                <div className="modal-title">{title}</div>
                <div className="modal-sub">{sub}</div>
                <div className="input-group">
                    <label className="label">
                        Título del servicio
                    </label>
                    <input
                        className="input"
                        placeholder="ej: Reparación de tuberías"
                        {...register("title", {
                            required: true
                        })}
                    />
                </div>
                <div className="g2">
                    <div className="input-group">
                        <label className="label">
                            Categoría
                        </label>
                        <select
                            className="input"
                            {...register("categoryId", {
                                required: true,
                                valueAsNumber: true
                            })}
                        >
                            <option value="">
                                Seleccione una categoría
                            </option>
                            {categories.map(category => (
                                <option
                                    key={category.id}
                                    value={category.id}
                                >
                                    {category.name}
                                </option>
                            ))}
                        </select>
                    </div>
                    <div className="input-group">
                        <label className="label">
                            Precio / hora (COP)
                        </label>
                        <input
                            className="input"
                            type="number"
                            {...register("priceHourly", {
                                valueAsNumber: true
                            })}
                        />
                    </div>
                </div>
                <div className="g2">
                    <div className="input-group">
                        <label className="label">
                            Duración estimada (min)
                        </label>
                        <input
                            className="input"
                            type="number"
                            {...register(
                                "averageDurationMinutes",
                                {
                                    valueAsNumber: true
                                }
                            )}
                        />
                    </div>
                    <div className="input-group">
                        <label className="label">
                            Radio operación (km)
                        </label>
                        <input
                            className="input"
                            type="number"
                            {...register(
                                "operationRadiusKm",
                                {
                                    valueAsNumber: true
                                }
                            )}
                        />
                    </div>
                </div>
                <div className="input-group">
                    <label className="label">
                        Descripción
                    </label>
                    <textarea
                        className="input"
                        rows="3"
                        {...register("description")}
                    />
                </div>
                <div
                    style={{
                        display: 'flex',
                        gap: '8px'
                    }}
                >
                    <button
                        type="button"
                        className="btn btn-ghost btn-full"
                        onClick={onClose}>
                        Cancelar
                    </button>
                    <button
                        type="submit"
                        className="btn btn-primary btn-full">
                        {edit
                            ? 'Guardar cambios'
                            : 'Crear servicio'}
                    </button>
                </div>
            </form>
        </Modal>
    );
}

export function OffererServicesPage() {
    const navigate = useNavigate();
    const { toasts, showToast } = useToast();
    const [newOpen, setNewOpen] = useState(false);
    const [editOpen, setEditOpen] = useState(false);
    const [profile, setProfile] = useState(null);
    const [services, setServices] = useState([]);
    const [categories, setCategories] = useState([]);
    const [editedService, setEditedService] = useState(null);

    useEffect(() => {
        if (!isAuthenticated()) {
            navigate('/login');
            return;
        }
        profileApi.getMyProfile()
            .then(setProfile)
            .catch((e) => showToast(e.message || 'No se pudo cargar tu perfil', 'danger'));
        categoryApi.getCategories()
            .then(setCategories)
            .catch((e) => showToast(e.message || 'No se pudieron cargar las categorías', 'danger'));
    }, []);

    useEffect(() => {
        if (!profile?.id) return;
        serviceApi.getMyServices(profile.id)
            .then(setServices)
            .catch((e) => showToast(e.message || 'No se pudieron cargar tus servicios', 'danger'));
    }, [profile?.id]);

    const openEdit = (service) => () => {
        setEditedService({
            ...service,
            categoryId: service.category?.id
        });
        setEditOpen(true);
    }

    const closeModal = () => {
        setNewOpen(false);
        setEditOpen(false);
        setEditedService(null);
    };

    const createService = (formData) => {
        const category = categories.find(c => c.id === formData.categoryId);
        const payload = {
            ...formData,
            active: true
        };

        serviceApi
            .createService(payload)
            .then((created) => {
                setServices(prev => [
                    ...prev,
                    { ...created, category: created.category || category }
                ]);
                setNewOpen(false);
                showToast('Servicio creado exitosamente', 'success');
            })
            .catch(e =>
                showToast(
                    e.message ||
                    'No se pudo crear el servicio',
                    'danger'
                )
            );
    };

    const saveEditedService = (formData) => {
        const category = categories.find(c => c.id === formData.categoryId);
        const payload = {
            ...editedService,
            ...formData,
            category: category || editedService.category
        };

        serviceApi
            .updateService(payload.id, payload)
            .then(() => {
                setServices(prev =>
                    prev.map(s =>
                        s.id === payload.id
                            ? payload
                            : s
                    )
                );
                setEditOpen(false);
                setEditedService(null);
                showToast(
                    'Servicio actualizado exitosamente',
                    'success'
                );
            })
            .catch(e =>
                showToast(
                    e.message ||
                    'No se pudo actualizar el servicio',
                    'danger'
                )
            );
    }

    const deleteService = (service) => () => {
        serviceApi
            .deleteService(service.id)
            .then(() => {
                setServices(prev => prev.filter(s => s.id !== service.id));
                showToast('Servicio eliminado', 'danger');
            })
            .catch(e =>
                showToast(
                    e.message ||
                    'No se pudo eliminar el servicio',
                    'danger'
                )
            );
    };

    return (
        <DashboardLayout sections={OFFERER_NAV} avatar="CM">
            <div className="page-head">
                <div className="ph" style={{ margin: 0 }}><h1>Mis servicios</h1><p>Crea y administra tu catálogo de servicios</p></div>
                <button className="btn btn-primary" onClick={() => setNewOpen(true)}><Icon name="plus" size={15} />Nuevo servicio</button>
            </div>

            <div className="g2" style={{ gap: '14px' }}>
                {services.map((s) => (
                    <div className={`card svc-card ${!s.active ? 'svc-inactive' : ''}`} key={s.id}>
                        <div className="svc-actions">
                            {s.active ? <span className="badge badge-success">Activo</span> : <span className="badge badge-gray">Inactivo</span>}
                            {s.active && (
                                <>
                                    <button className="btn btn-ghost btn-sm" onClick={openEdit(s)} style={{ border: '1px solid var(--c-border)' }}><Icon name="edit" size={13} /></button>
                                    <button className="btn btn-danger btn-sm" onClick={deleteService(s)}><Icon name="trash" size={13} /></button>
                                </>
                            )}
                        </div>
                        <div className={`svc-ico ${!s.active ? 'svc-ico-off' : ''}`}><Icon name="wrench" size={22} /></div>
                        <div className="svc-name">{s.title}</div>
                        <span className={`badge ${s.active ? 'badge-primary' : 'badge-gray'}`} style={{ marginBottom: '10px' }}>{s.category?.name}</span>
                        <div className="svc-desc">{s.description}</div>
                        {s.active && (
                            <div className="svc-stats">
                                <span><Icon name="dollar" size={12} />desde {s.priceHourly}/hr</span>
                                <span><Icon name="clock" size={12} />{s.averageDurationMinutes} min</span>
                                {s.operationRadiusKm && (
                                    <span><Icon name="mapPin" size={12} />{s.operationRadiusKm} km</span>
                                )}
                            </div>
                        )}
                        <div className="divider" />
                        {/* <div className="svc-foot">
                            {s.active
                                ? <span><Stars rating={s.rating} size={12} /> {s.rating} · {s.requests} sol.</span>
                                : <span style={{ color: 'var(--c-soft)' }}>Sin solicitudes aún</span>}
                            <label className="svc-visible"><input type="checkbox" defaultChecked={s.active} /> Visible</label>
                        </div> */}
                    </div>
                ))}
            </div>

            {newOpen && (
                <ServiceModal
                    onClose={closeModal}
                    title="Nuevo servicio"
                    sub="Crea una nueva oferta de servicio para tu catálogo"
                    onSave={createService}
                    categories={categories}
                    editedService={null}
                />
            )}
            {editOpen && editedService && (
                <ServiceModal
                    onClose={closeModal}
                    edit
                    title="Editar servicio"
                    sub="Actualiza la información de tu servicio"
                    onSave={saveEditedService}
                    categories={categories}
                    editedService={editedService}
                />
            )}
            <ToastContainer toasts={toasts} />
        </DashboardLayout>
    );
}

export default OffererServicesPage;
