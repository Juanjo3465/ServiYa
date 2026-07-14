import { useEffect, useState } from "react";
import { AdminNavbar } from '../../components/AdminNavbar/AdminNavbar';
import { AdminSidebar } from '../../components/AdminSidebar/AdminSidebar';
import { ReportCard } from '../../components/ReportCard/ReportCard';
import { ManagementModal } from '../../components/ManagementModal/ManagementModal';
import { ToastContainer } from '../../../../shared/components/Toast/Toast';
import { useToast } from '../../../../shared/hooks/useToast';
import { reportApi, userApi } from '../../../../shared/api';

import './AdminReportsPage.css';

const REPORT_TYPE_LABELS = {
    REQUEST: 'Solicitud',
    SERVICE_FEEDBACK: 'Reseña servicio',
    CLIENT_FEEDBACK: 'Reseña cliente'
};

const PRIORITY_LABELS = {
    LOW: 'Baja',
    MEDIUM: 'Media',
    HIGH: 'Alta',
    CRITICAL: 'Crítica'
};

const getInitials = (name = '') => {
    const parts = name.trim().split(/\s+/).filter(Boolean);
    if (!parts.length) return 'U';
    if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
    return `${parts[0][0]}${parts[parts.length - 1][0]}`.toUpperCase();
};

export function AdminReportsPage() {
    const { toasts, showToast } = useToast();
    const [activeTab, setActiveTab] = useState('Todos');
    const [selectedReport, setSelectedReport] = useState(null);
    const [reports, setReports] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const loadReports = async () => {
            try {
                const data = await reportApi.getAll();
                const mapped = await Promise.all((data.content ?? []).map(async (report) => {
                    const [reporterName, reportedName] = await Promise.all([
                        userApi.getDisplayName(report.reporterId),
                        userApi.getDisplayName(report.reportedUserId),
                    ]);

                    return {
                        id: `#RPT-${String(report.id).padStart(3, '0')}`,
                        rawId: report.id,
                        date: new Date(report.createdAt).toLocaleDateString('es-CO', { day: 'numeric', month: 'short' }),
                        type: REPORT_TYPE_LABELS[report.reportType] ?? report.reportType,
                        priority: PRIORITY_LABELS[report.priority] ?? report.priority,
                        status: report.status === 'PENDING' ? 'Pendiente' : report.status === 'RESOLVED' ? 'Resuelto' : 'Cerrado',
                        reporterInitials: getInitials(reporterName),
                        reporterName,
                        reportedInitials: getInitials(reportedName),
                        reportedName,
                        reason: report.category,
                        description: report.reason,
                        category: report.category,
                        rawStatus: report.status,
                    };
                }));

                setReports(mapped);
            } catch {
                showToast('No fue posible cargar los reportes', 'danger');
            } finally {
                setLoading(false);
            }
        };

        loadReports();
    }, [showToast]);

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

                    <div style={{ display: 'flex', gap: '8px', marginBottom: '18px', flexWrap: 'wrap' }}>
                        <div className={`chip ${activeTab === 'Todos' ? 'active' : ''}`} onClick={() => setActiveTab('Todos')}>Todos ({reports.length})</div>
                        <div className={`chip ${activeTab === 'Pendientes' ? 'active' : ''}`} onClick={() => setActiveTab('Pendientes')}>Pendientes ({reports.filter((r) => r.rawStatus === 'PENDING').length})</div>
                        <div className={`chip ${activeTab === 'Resueltos' ? 'active' : ''}`} onClick={() => setActiveTab('Resueltos')}>Resueltos ({reports.filter((r) => r.rawStatus === 'RESOLVED').length})</div>
                    </div>

                    <div>
                        {loading ? <div className="card">Cargando reportes...</div> : reports.filter((report) => activeTab === 'Todos' || (activeTab === 'Pendientes' ? report.rawStatus === 'PENDING' : report.rawStatus === 'RESOLVED')).map((report) => (
                            <ReportCard 
                                key={report.rawId} 
                                report={report} 
                                onManage={handleOpenManagement}
                                onNotify={() => showToast('Notificación enviada por correo', 'success')}
                                onDelete={() => showToast('Reseña eliminada (RF-049)', 'danger')}
                            />
                        ))}
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