import { useEffect, useState } from "react";
import { AdminNavbar } from '../../components/AdminNavbar/AdminNavbar';
import { AdminSidebar } from '../../components/AdminSidebar/AdminSidebar';
import { ReportCard } from '../../components/ReportCard/ReportCard';
import { ManagementModal } from '../../components/ManagementModal/ManagementModal';
import { ToastContainer } from '../../../../shared/components/Toast/Toast';
import { useToast } from '../../../../shared/hooks/useToast';
import { feedbackApi, Stars } from '../../../../shared';

import './AdminReportsPage.css';

const REPORTS_DATA = [
    {
        id: '#RPT-001',
        date: '12 mayo',
        type: 'Solicitud',
        priority: 'Alta',
        status: 'Pendiente',
        reporterInitials: 'JP',
        reporterName: 'Juan Pablo B.',
        reportedInitials: 'CM',
        reportedName: 'Carlos M. (Oferente)',
        contextInfo: '#SR-4821 · Reparación tuberías · 12 mayo',
        reason: 'No se presentó al servicio',
        description: 'El oferente no llegó a la cita acordada ni notificó su ausencia.'
    },
    {
        id: '#RPT-002',
        date: '11 mayo',
        type: 'Reseña',
        priority: 'Media',
        status: 'Pendiente',
        reporterInitials: 'SR',
        reporterName: 'Sandra R.',
        reportedInitials: 'ML',
        reportedName: 'María L. (Oferente)',
        reason: 'Contenido inapropiado',
        description: '"Lorem ipsum texto ofensivo que viola los términos de uso de la plataforma..."'
    },
    {
        id: '#RPT-000',
        date: '5 mayo',
        type: 'Solicitud',
        priority: 'Baja',
        status: 'Resuelto',
        resolutionText: 'Reporte resuelto el 6 mayo. Acción: Advertencia al oferente. Usuarios notificados.'
    }
];

export function AdminReportsPage() {
    const { toasts, showToast } = useToast();
    const [activeTab, setActiveTab] = useState('Todos');
    const [selectedReport, setSelectedReport] = useState(null);
    const [clientReviews, setClientReviews] = useState([]);

    useEffect(() => {
        feedbackApi.searchAdminFeedback({ type: 'CLIENT', size: 5 })
            .then((page) => setClientReviews(page?.content ?? []))
            .catch(() => setClientReviews([]));
    }, []);

    const handleOpenManagement = (report) => {
        setSelectedReport(report);
    };

    const handleExecuteManagement = () => {
        setSelectedReport(null);
        showToast('Reporte cerrado. Usuarios notificados.', 'success');
    };

    return (
        <>
            <AdminNavbar />
            
            <div className="admin-layout-container">
                <AdminSidebar />

                <main className="main-content">
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px', flexWrap: 'wrap', gap: '10px' }}>
                        <div className="ph">
                            <h1>Gestión de reportes</h1>
                            <p>Modera los reportes enviados por usuarios</p>
                        </div>
                    </div>

                    {/* Filtros */}
                    <div style={{ display: 'flex', gap: '8px', marginBottom: '18px', flexWrap: 'wrap' }}>
                        <div className={`chip ${activeTab === 'Todos' ? 'active' : ''}`} onClick={() => setActiveTab('Todos')}>Todos (7)</div>
                        <div className={`chip ${activeTab === 'Pendientes' ? 'active' : ''}`} onClick={() => setActiveTab('Pendientes')}>Pendientes (5)</div>
                        <div className={`chip ${activeTab === 'Resueltos' ? 'active' : ''}`} onClick={() => setActiveTab('Resueltos')}>Resueltos (2)</div>
                        
                        <select className="input-filter" style={{ width: 'auto', padding: '5px 10px', fontSize: '12px' }}>
                            <option>Todos los tipos</option>
                            <option>Solicitud</option>
                            <option>Reseña servicio</option>
                            <option>Reseña cliente</option>
                        </select>
                        <select className="input-filter" style={{ width: 'auto', padding: '5px 10px', fontSize: '12px' }}>
                            <option>Toda prioridad</option>
                            <option>Alta</option>
                            <option>Media</option>
                            <option>Baja</option>
                        </select>
                    </div>

                    {/* Lista Mapeada de Reportes */}
                    <div>
                        {REPORTS_DATA.map((report) => (
                            <ReportCard 
                                key={report.id} 
                                report={report} 
                                onManage={handleOpenManagement}
                                onNotify={() => showToast('Notificación enviada por correo', 'success')}
                                onDelete={() => showToast('Reseña eliminada (RF-049)', 'danger')}
                            />
                        ))}
                    </div>

                    <div className="card" style={{ marginTop: '18px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '10px', marginBottom: '12px' }}>
                            <div>
                                <div style={{ fontSize: '15px', fontWeight: 700 }}>Reseñas de clientes</div>
                                <div style={{ fontSize: '12px', color: 'var(--c-soft)' }}>Vista administrativa (RF-048)</div>
                            </div>
                            <span className="badge badge-primary">Cliente</span>
                        </div>

                        {clientReviews.length === 0 ? (
                            <div style={{ fontSize: '13px', color: 'var(--c-soft)' }}>
                                No hay reseñas de clientes disponibles.
                            </div>
                        ) : (
                            clientReviews.map((review) => (
                                <div className="review-admin-row" key={`${review.type}-${review.requestId}`}>
                                    <div>
                                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
                                            <strong style={{ fontSize: '13px' }}>Cliente #{review.clientId}</strong>
                                            <Stars rating={review.rating ?? 0} size={11} />
                                            <span style={{ fontSize: '11px', color: 'var(--c-soft)' }}>Solicitud #{review.requestId}</span>
                                        </div>
                                        <div style={{ fontSize: '12px', color: 'var(--c-mid)' }}>
                                            {review.comment || 'Sin comentario adicional.'}
                                        </div>
                                    </div>
                                    <span className="badge badge-gray">Oferente #{review.offererId}</span>
                                </div>
                            ))
                        )}
                    </div>
                </main>
            </div>

            {/* Modal de Gestión Controlado */}
            {selectedReport && (
                <ManagementModal 
                    report={selectedReport} 
                    onClose={() => setSelectedReport(null)} 
                    onExecute={handleExecuteManagement}
                />
            )}

            <ToastContainer toasts={toasts} />
        </>
    );
}

export default AdminReportsPage;
