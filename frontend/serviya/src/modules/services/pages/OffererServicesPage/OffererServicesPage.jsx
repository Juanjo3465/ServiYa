import { useForm } from 'react-hook-form';
import { useState, useEffect } from "react";
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { DashboardLayout, Icon, Modal, ToastContainer, useToast, useMyProfile, OFFERER_NAV, serviceApi, categoryApi, getApiImageUrl, ServiceImage } from '../../../../shared';

import './OffererServicesPage.css';

function ServiceModal({
    onClose,
    title,
    sub,
    onSave,
    edit,
    categories,
    editedService,
    selectedPhotos,
    setSelectedPhotos,
    removePhoto
}) {

    const {
        register,
        handleSubmit,
        reset
    } = useForm({
        defaultValues: editedService || {}
    });
    const [previewPhoto, setPreviewPhoto] = useState(null);

    useEffect(() => {
        if (!editedService) {
            setSelectedPhotos([]);
            return;
        }
        setSelectedPhotos(editedService.photos || []);
    }, [editedService, setSelectedPhotos]);

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
                <div className="input-group">
                    <label className="label">Fotos del servicio (máx. 15)</label>
                    <input
                        className="input"
                        type="file"
                        accept="image/*"
                        multiple
                        onChange={(e) => {
                            const files = Array.from(e.target.files || []);
                            if (!files.length) return;
                            setSelectedPhotos(prev => [...prev, ...files]);
                            e.target.value = '';
                        }}
                    />
                    {selectedPhotos.length > 0 && (
                        <div style={{ marginTop: '8px', display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                            {selectedPhotos.map((photo, index) => {
                                const src = photo instanceof File ? URL.createObjectURL(photo) : getApiImageUrl(photo);
                                return (
                                    <div key={`${index}-${typeof photo === 'string' ? photo : photo.name}`} style={{ position: 'relative', cursor: 'pointer' }}>
                                        <button
                                            type="button"
                                            onClick={(event) => {
                                                event.stopPropagation();
                                                removePhoto(index);
                                            }}
                                            style={{
                                                position: 'absolute',
                                                top: '4px',
                                                right: '4px',
                                                width: '20px',
                                                height: '20px',
                                                border: 'none',
                                                borderRadius: '50%',
                                                background: '#dc2626',
                                                color: '#fff',
                                                fontSize: '12px',
                                                lineHeight: '1',
                                                cursor: 'pointer',
                                                boxShadow: '0 0 0 2px rgba(255,255,255,0.8)'
                                            }}
                                            aria-label="Eliminar foto"
                                        >
                                            ×
                                        </button>
                                        <ServiceImage src={src} alt={`Foto ${index + 1}`} onClick={() => setPreviewPhoto(src)} style={{ width: '72px', height: '72px', objectFit: 'cover', borderRadius: '8px', border: '1px solid var(--c-border)' }} />
                                    </div>
                                );
                            })}
                        </div>
                    )}
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
            {previewPhoto && (
                <Modal open={true} onClose={() => setPreviewPhoto(null)} maxWidth={760}>
                    <div style={{ textAlign: 'center' }}>
                        <ServiceImage src={previewPhoto} alt="Vista previa" style={{ maxWidth: '100%', maxHeight: '70vh', objectFit: 'contain', borderRadius: '12px' }} />
                    </div>
                </Modal>
            )}
        </Modal>
    );
}

export function OffererServicesPage() {
    const navigate = useNavigate();
    const { toasts, showToast } = useToast();
    const [newOpen, setNewOpen] = useState(false);
    const [editOpen, setEditOpen] = useState(false);
    const [editedService, setEditedService] = useState(null);
    const [selectedPhotos, setSelectedPhotos] = useState([]);

    // M49: perfil (hook compartido), categorías y servicios como queries; CRUD con useMutation.
    const queryClient = useQueryClient();
    const { data: profile } = useMyProfile();
    const categoriesQuery = useQuery({ queryKey: ['categories'], queryFn: () => categoryApi.getCategories() });
    const categories = categoriesQuery.data ?? [];
    const servicesQuery = useQuery({
        queryKey: ['my-services', profile?.id],
        queryFn: () => serviceApi.getMyServices(profile.id),
        enabled: !!profile?.id,
    });
    const services = servicesQuery.data ?? [];
    const invalidateServices = () => queryClient.invalidateQueries({ queryKey: ['my-services'] });

    const createMutation = useMutation({
        mutationFn: (formDataToSend) => serviceApi.createService(formDataToSend, true),
        onSuccess: () => { invalidateServices(); setNewOpen(false); setSelectedPhotos([]); showToast('Servicio creado exitosamente', 'success'); },
        onError: (e) => showToast(e.message || 'No se pudo crear el servicio', 'danger'),
    });
    const updateMutation = useMutation({
        mutationFn: ({ id, formDataToSend }) => serviceApi.updateService(id, formDataToSend, true),
        onSuccess: () => { invalidateServices(); setEditOpen(false); setEditedService(null); setSelectedPhotos([]); showToast('Servicio actualizado exitosamente', 'success'); },
        onError: (e) => showToast(e.message || 'No se pudo actualizar el servicio', 'danger'),
    });
    const deleteMutation = useMutation({
        mutationFn: (id) => serviceApi.deleteService(id),
        onMutate: async (id) => {
            await queryClient.cancelQueries({ queryKey: ['my-services'] });
            const snapshot = queryClient.getQueriesData({ queryKey: ['my-services'] });
            queryClient.setQueriesData({ queryKey: ['my-services'] }, (old) => (Array.isArray(old) ? old.filter((s) => s.id !== id) : old));
            return { snapshot };
        },
        onError: (e, _id, ctx) => { ctx?.snapshot?.forEach(([key, data]) => queryClient.setQueryData(key, data)); showToast(e.message || 'No se pudo eliminar el servicio', 'danger'); },
        onSuccess: () => showToast('Servicio eliminado', 'danger'),
        onSettled: invalidateServices,
    });

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
        setSelectedPhotos([]);
    };

    const normalizeNumericValue = (value) => {
        if (value === null || value === undefined || value === '' || value === 'NaN') return null;
        const numericValue = typeof value === 'number' ? value : Number(value);
        return Number.isFinite(numericValue) ? numericValue : null;
    };

    const createService = (formData) => {
        const categoryId = normalizeNumericValue(formData.categoryId);
        if (categoryId === null) {
            showToast('Selecciona una categoría antes de crear el servicio', 'danger');
            return;
        }

        const formDataToSend = new FormData();
        formDataToSend.append('title', formData.title || '');
        formDataToSend.append('description', formData.description || '');
        const priceHourly = normalizeNumericValue(formData.priceHourly);
        formDataToSend.append('priceHourly', String(priceHourly ?? 0));
        formDataToSend.append('categoryId', String(categoryId));

        const averageDurationMinutes = normalizeNumericValue(formData.averageDurationMinutes);
        if (averageDurationMinutes !== null) formDataToSend.append('averageDurationMinutes', String(averageDurationMinutes));

        const operationRadiusKm = normalizeNumericValue(formData.operationRadiusKm);
        if (operationRadiusKm !== null) formDataToSend.append('operationRadiusKm', String(operationRadiusKm));

        selectedPhotos.forEach((photo) => formDataToSend.append('photos', photo));

        createMutation.mutate(formDataToSend);
    };

    const removePhoto = (index) => {
        setSelectedPhotos(prev => prev.filter((_, idx) => idx !== index));
    };

    const saveEditedService = (formData) => {
        const categoryId = normalizeNumericValue(formData.categoryId);
        const formDataToSend = new FormData();
        if (formData.title != null) formDataToSend.append('title', formData.title || '');
        if (formData.description != null) formDataToSend.append('description', formData.description || '');

        const priceHourly = normalizeNumericValue(formData.priceHourly);
        if (priceHourly !== null) formDataToSend.append('priceHourly', String(priceHourly));

        if (categoryId !== null) formDataToSend.append('categoryId', String(categoryId));

        const averageDurationMinutes = normalizeNumericValue(formData.averageDurationMinutes);
        if (averageDurationMinutes !== null) formDataToSend.append('averageDurationMinutes', String(averageDurationMinutes));

        const operationRadiusKm = normalizeNumericValue(formData.operationRadiusKm);
        if (operationRadiusKm !== null) formDataToSend.append('operationRadiusKm', String(operationRadiusKm));

        const retainedExistingPhotos = selectedPhotos.filter((photo) => typeof photo === 'string');
        const removedPhotos = (editedService?.photos || []).filter((photo) => !retainedExistingPhotos.includes(photo));
        retainedExistingPhotos.forEach((photo) => formDataToSend.append('existingPhotos', photo));
        removedPhotos.forEach((photo) => formDataToSend.append('removedPhotos', photo));
        selectedPhotos.filter((photo) => photo instanceof File).forEach((photo) => formDataToSend.append('photos', photo));

        updateMutation.mutate({ id: editedService.id, formDataToSend });
    }

    const deleteService = (service) => () => deleteMutation.mutate(service.id);

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
                        <ServiceImage
                            src={s.photos && s.photos.length > 0 ? getApiImageUrl(s.photos[0]) : null}
                            alt={s.title}
                            style={{ width: '100%', height: '138px', objectFit: 'cover', borderRadius: '12px', marginBottom: '10px' }}
                            fallback={<div className={`svc-ico ${!s.active ? 'svc-ico-off' : ''}`}><Icon name="wrench" size={22} /></div>}
                        />
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
                        <button className="btn btn-outline btn-sm btn-full" onClick={() => navigate(`/offerer/services/${s.id}`)}>
                            <Icon name="search" size={13} />Ver detalle
                        </button>
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
                    selectedPhotos={selectedPhotos}
                    setSelectedPhotos={setSelectedPhotos}
                    removePhoto={removePhoto}
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
                    selectedPhotos={selectedPhotos}
                    setSelectedPhotos={setSelectedPhotos}
                    removePhoto={removePhoto}
                />
            )}
            <ToastContainer toasts={toasts} />
        </DashboardLayout>
    );
}

export default OffererServicesPage;
