import { useNavigate } from 'react-router-dom';
import { Icon } from '../Icon/Icon';
import { rolesFromToken, setActiveRole } from '../../api';
import { sidebarStore } from '../../hooks/useSidebar';

/**
 * Selector de rol/área para usuarios con más de un rol. CLIENT y OFFERER comparten el shell de dashboard
 * (cambian el "rol activo" que decide el menú de las páginas compartidas); ADMIN es un área propia
 * (AdminSidebar), así que ir a Admin solo navega, sin tocar el rol activo. No se muestra si hay un solo rol.
 *
 * @param {('CLIENT'|'OFFERER'|'ADMIN')} current  Rol/área resaltado como activo.
 */
const ROLE_META = {
    CLIENT: { label: 'Cliente', icon: 'user', to: '/dashboard' },
    OFFERER: { label: 'Oferente', icon: 'wrench', to: '/offerer/dashboard' },
    ADMIN: { label: 'Admin', icon: 'shield', to: '/admin/dashboard' },
};
const ORDER = ['CLIENT', 'OFFERER', 'ADMIN'];

export function RoleSwitcher({ current }) {
    const navigate = useNavigate();
    const roles = ORDER.filter((r) => rolesFromToken().includes(r));
    if (roles.length < 2) return null;

    const go = (role) => {
        if (role !== 'ADMIN') setActiveRole(role);  // ADMIN es un área aparte, no un "modo" del shell
        sidebarStore.close();
        navigate(ROLE_META[role].to);
    };

    return (
        <div className="sb-role-switch" role="group" aria-label="Cambiar de rol"
            style={{ display: 'flex', gap: '4px', padding: '4px', margin: '0 0 12px', background: 'var(--c-bg-s)', borderRadius: '8px', flexWrap: 'wrap' }}>
            {roles.map((role) => {
                const m = ROLE_META[role];
                const active = current === role;
                return (
                    <button key={role} type="button" onClick={() => go(role)} aria-pressed={active}
                        style={{
                            flex: '1 1 0', minWidth: '72px', display: 'flex', alignItems: 'center', justifyContent: 'center',
                            gap: '6px', padding: '7px 4px', border: 'none', borderRadius: '6px', cursor: 'pointer',
                            font: 'inherit', fontSize: '13px', fontWeight: 600,
                            background: active ? 'var(--c-primary)' : 'transparent',
                            color: active ? '#fff' : 'var(--c-mid)',
                        }}>
                        <Icon name={m.icon} size={14} />{m.label}
                    </button>
                );
            })}
        </div>
    );
}

export default RoleSwitcher;
