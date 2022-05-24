export const defaultFor = (all, name) => {
    const selected = all.filter(it => it.name === name);
    if (selected.length === 1) {
        const { type, defaultValue } = selected[0];
        switch (type || '') {
            case 'integer':
                return defaultValue === undefined ? '0' : defaultValue;
            case 'string':
                return defaultValue === undefined ? '' : defaultValue;
            case 'boolean':
                if (defaultValue !== undefined) {
                    return defaultValue;
                }
            default:
            // use global default
        }
    }
    return 'true';
};
