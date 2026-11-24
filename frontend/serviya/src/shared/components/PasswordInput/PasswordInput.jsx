import { useState } from 'react';
import { Icon } from '../Icon/Icon';

/**
 * Campo de contraseña reutilizable con botón para mostrar/ocultar (ojo).
 * Controlado: pásale `value`/`onChange` como a un <input>. Reenvía el resto de props
 * (placeholder, onKeyDown, autoComplete, disabled, name, ...).
 *
 * @param {boolean} [withIcon=false]  Muestra el icono de candado a la izquierda (como en login/registro).
 * @param {string}  [className='input']
 */
export function PasswordInput({ withIcon = false, className = 'input', style, ...props }) {
    const [show, setShow] = useState(false);
    return (
        <div className="input-wrap">
            {withIcon && <div className="input-ico"><Icon name="lock" size={15} /></div>}
            <input
                {...props}
                className={className}
                type={show ? 'text' : 'password'}
                style={{ paddingRight: '40px', ...style }}
            />
            <button
                type="button"
                onClick={() => setShow((v) => !v)}
                aria-label={show ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                title={show ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                style={{ position: 'absolute', right: '10px', top: '50%', transform: 'translateY(-50%)', background: 'none', border: 'none', padding: 0, cursor: 'pointer', color: 'var(--c-soft)', display: 'flex', alignItems: 'center' }}
            >
                <Icon name={show ? 'eyeOff' : 'eye'} size={16} />
            </button>
        </div>
    );
}

export default PasswordInput;
