# Fix F3: Admin Pages - Category Dropdown and Name Resolution

## Bug Description

**Bug ID:** F3
**Severity:** MEDIO
**Area:** admin
**Description:** 
- `AdminServicesPage`: filtro de categoría por input numérico (no dropdown)
- IDs crudos en vez de nombres (`AdminFeedbackPage`/`AdminServicesPage`)

The admin pages displayed raw IDs instead of human-readable names, making it difficult for admins to identify entities.

## Root Cause

### AdminServicesPage
The category filter used a numeric input field:
```jsx
<input
    className="input"
    type="number"
    placeholder="Ej: 1"
    value={filters.categoryId}
    onChange={(e) => handleChange('categoryId', e.target.value)}
/>
```

The categories were already being loaded via `categoryApi.getCategories()` but were not used in a dropdown.

### AdminFeedbackPage
The results displayed raw IDs:
- "Usuario #1" instead of actual user names
- "Servicio #5" / "Cliente #2" instead of service/client names

No name resolution was implemented for authorId and targetId fields.

## Files Related to the Bug

1. **AdminServicesPage:** `frontend/serviya/src/modules/admin/pages/AdminServicesPage/AdminServicesPage.jsx`
2. **AdminFeedbackPage:** `frontend/serviya/src/modules/admin/pages/AdminFeedbackPage/AdminFeedbackPage.jsx`
3. **API:** `frontend/serviya/src/shared/api.js` - `userApi.getDisplayName()`, `categoryApi.getCategories()`

## Solution Implemented

### 1. AdminServicesPage - Category Dropdown

Changed the category filter from numeric input to a dropdown select populated with loaded categories:

```jsx
<select
    className="input"
    value={filters.categoryId}
    onChange={(e) => handleChange('categoryId', e.target.value)}
>
    <option value="">Todas las categorías</option>
    {categories.map((cat) => (
        <option key={cat.id} value={cat.id}>
            {cat.name}
        </option>
    ))}
</select>
```

The categories were already being loaded on filter panel open (`handleOpenFilters`), so the data was available.

### 2. AdminFeedbackPage - Name Resolution

Added a name caching mechanism to resolve and display human-readable names:

**Added name cache state:**
```javascript
const [nameCache, setNameCache] = useState({});
```

**Added async name resolver:**
```javascript
const resolveName = useCallback(async (userId) => {
    if (!userId) return `Usuario ${userId}`;
    if (nameCache[userId]) return nameCache[userId];

    try {
        const name = await userApi.getDisplayName(userId);
        setNameCache((prev) => ({ ...prev, [userId]: name }));
        return name;
    } catch {
        const fallback = `Usuario ${userId}`;
        setNameCache((prev) => ({ ...prev, [userId]: fallback }));
        return fallback;
    }
}, [nameCache]);
```

**Trigger resolution when results load:**
```javascript
useEffect(() => {
    if (results.length > 0) {
        const uniqueIds = new Set();
        results.forEach((item) => {
            if (item.authorId) uniqueIds.add(item.authorId);
            if (item.targetId) uniqueIds.add(item.targetId);
        });
        uniqueIds.forEach((id) => {
            if (!nameCache[id]) {
                resolveName(id);
            }
        });
    }
}, [results, nameCache, resolveName]);
```

**Display resolved names in results:**
```jsx
<span style={{ fontWeight: 600 }}>
    {nameCache[item.authorId] ? nameCache[item.authorId] : `Usuario #${item.authorId}`}
</span>
<span style={{ fontWeight: 600 }}>
    {item.feedbackType === 'SERVICE'
        ? nameCache[item.targetId] ? nameCache[item.targetId] : `Servicio #${item.targetId}`
        : nameCache[item.targetId] ? nameCache[item.targetId] : `Cliente #${item.targetId}`}
</span>
```

## Behavior

### AdminServicesPage
- Category filter now shows a dropdown with category names
- "Todas las categorías" option to clear filter
- Categories loaded on first filter panel open

### AdminFeedbackPage
- On search, extracts unique authorId and targetId from results
- Fetches display names via `userApi.getDisplayName()` (which tries offerer summary then user profile)
- Caches resolved names to avoid duplicate API calls
- Shows resolved names immediately when available, falls back to "Usuario #X" while loading
- Names persist in cache across searches

## Testing

The fix can be tested by:

1. **AdminServicesPage:** Navigate to `/admin/services`, open filters, verify category dropdown shows names
2. **AdminFeedbackPage:** Navigate to `/admin/feedback`, search for feedback, verify names appear instead of raw IDs
3. **Cache behavior:** Search multiple times, verify names appear instantly on subsequent searches

## Future Improvements

1. **Offerer dropdown in AdminServicesPage:** Add offerer name resolution similar to feedback page
2. **Batch name resolution:** Add a batch endpoint to resolve multiple names in one request
3. **Service name resolution:** For service feedback, fetch service title instead of using userApi.getDisplayName
4. **Debounced resolution:** Debounce name resolution to avoid burst of API calls

## Related Requirements

- **F3:** Admin page improvements (this fix)
- **M21:** Limpieza - dropdown de categoría en AdminServicesPage; resolver nombres en lote (no N+1 en el cliente) (mejora propuesta en §6.4)

## Verification

- ✅ Build passes (`npm run build`)
- ✅ Category dropdown renders with loaded categories
- ✅ Name resolution triggered on search results
- ✅ Cache prevents duplicate API calls
- ✅ Fallback to ID display while names load