import './ManagementModal.css';

export function ManagementModal({ report, onClose, onExecute }) {
    if (!report) return null;

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal" style={{ maxWidth: '520px' }} onClick={(e) => e.stopPropagation()}>
                <div className="modal-title">Gestionar reporte {report.id}</div>
                <div className="modal-sub">Reporte contra {report.reportedName} — {report.reason}</div>
                
                <div className="input-group">
                    <label className="label">Acción tomada (RF-071)</label>
                    <select className="input">
                        <option>Advertencia</option>
                        <option>Penalización en métricas</option>
                        <option>Baneo temporal</option>
                        <option>Baneo permanente</option>
                        <option>Sin consecuencias</option>
                    </select>
                </div>

                <div className="input-group">
                    <label className="label">Registro de acciones</label>
                    <textarea className="input" rows="3" placeholder="Describe detalladamente las acciones tomadas..."></textarea>
                </div>

                <label style={{ display: 'flex', alignItems: 'center', gap: '7px', fontSize: '13px', color: 'var(--c-mid)', marginBottom: '10px', cursor: 'pointer' }}>
                    <input type="checkbox" style={{ accentColor: 'var(--c-primary)' }} /> 
                    Marcar solicitud como no prestada (RF-074)
                </label>

                <div style={{ marginBottom: '14px' }}>
                    <div style={{ fontSize: '12px', fontWeight: 600, color: 'var(--c-text)', marginBottom: '8px' }}>Notificar usuarios afectados (RF-060)</div>
                    <label style={{ display: 'flex', alignItems: 'center', gap: '7px', fontSize: '13px', color: 'var(--c-mid)', marginBottom: '6px', cursor: 'pointer' }}>
                        <input type="checkbox" defaultChecked style={{ accentColor: 'var(--c-primary)' }} /> {report.reporterName.split(' ')[0]} (reportante)
                    </label>
                    <label style={{ display: 'flex', alignItems: 'center', gap: '7px', fontSize: '13px', color: 'var(--c-mid)', cursor: 'pointer' }}>
                        <input type="checkbox" defaultChecked style={{ accentColor: 'var(--c-primary)' }} /> {report.reportedName.split(' ')[0]} (reportado)
                    </label>
                </div>

                <div style={{ display: 'flex', gap: '7px' }}>
                    <button className="btn btn-ghost btn-full" style={{ border: '1px solid var(--c-border)' }} onClick={onClose}>Cancelar</button>
                    <button className="btn btn-primary btn-full" onClick={onExecute}>
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ width: '14px', marginRight: '4px' }}><path d="M20 6 9 17l-5-5" /></svg>
                        Ejecutar y cerrar (RF-059)
                    </button>
                </div>
            </div>
        </div>
    );
}