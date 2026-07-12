import { useEffect, useState } from "react";
import { Modal, StarRating, Icon } from '../../../../shared';

/**
 * Shared confirm-and-review modal used both by clients (confirm service +
 * review offerer) and offerers (mark done + review client).
 */
export function ReviewModal({
    open,
    onClose,
    title,
    sub,
    ratingLabel,
    reviewLabel,
    confirmLabel,
    confirmClass = 'btn-primary',
    tags = [],
    loading = false,
    onConfirm,
}) {
    const [rating, setRating] = useState(0);
    const [comment, setComment] = useState('');
    const [tagIds, setTagIds] = useState([]);

    useEffect(() => {
        if (!open) {
            setRating(0);
            setComment('');
            setTagIds([]);
        }
    }, [open]);

    const handleConfirm = () => {
        onConfirm?.({
            rating: rating || null,
            comment: comment.trim() || null,
            tagIds,
        });
    };

    const toggleTag = (id) => {
        setTagIds((current) =>
            current.includes(id) ? current.filter((tagId) => tagId !== id) : [...current, id]
        );
    };

    return (
        <Modal open={open} onClose={onClose}>
            <div className="modal-title">{title}</div>
            <div className="modal-sub">{sub}</div>
            <div className="input-group">
                <label className="label">{ratingLabel}</label>
                <StarRating value={rating} onChange={setRating} />
            </div>
            <div className="input-group">
                <label className="label">{reviewLabel}</label>
                <textarea
                    className="input"
                    rows="3"
                    placeholder="¿Cómo fue la experiencia?"
                    value={comment}
                    onChange={(event) => setComment(event.target.value)}
                />
            </div>
            {tags.length > 0 && (
                <div className="input-group">
                    <label className="label">Etiquetas</label>
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
                        {tags.map((tag) => (
                            <label
                                key={tag.id}
                                className={`tag-chip ${tagIds.includes(tag.id) ? 'pos' : ''}`}
                                style={{ cursor: 'pointer' }}
                            >
                                <input
                                    type="checkbox"
                                    checked={tagIds.includes(tag.id)}
                                    onChange={() => toggleTag(tag.id)}
                                    style={{ marginRight: '5px' }}
                                />
                                {tag.tagName}
                            </label>
                        ))}
                    </div>
                </div>
            )}
            <div style={{ display: 'flex', gap: '8px' }}>
                <button className="btn btn-ghost btn-full" onClick={onClose} disabled={loading}>Cancelar</button>
                <button className={`btn ${confirmClass} btn-full`} onClick={handleConfirm} disabled={loading}>
                    <Icon name="check" size={15} />{loading ? 'Enviando...' : confirmLabel}
                </button>
            </div>
        </Modal>
    );
}
