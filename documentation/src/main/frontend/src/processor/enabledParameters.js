export const enabledParams = params => Object
    .keys(params)
    .sort()
    .filter(p => params[p].checked);
