import css from './Parameters.scss';

const ParameterInput = ({
    name,
    type,
    value,
    allowedvalues,
    onChange,
}) => {
    switch (type || '') {
        case 'string':
            if (allowedvalues && allowedvalues.length > 0) {
                return (
                    <div class="form-group">
                        <select onChange={e => onChange(e)} className="form-control">
                            {allowedvalues.map(it => (<option value={it} key={`${name}__${it}`}>{it}</option>))}
                        </select>
                    </div>
                );
            }
            return (
                <div class="form-group">
                    <input
                        className="form-control"
                        placeholder={`${name} value...`}
                        value={value}
                        onKeyUp={e => onChange(e)}
                        onChange={e => onChange(e)}
                    />
                </div>
            );
        case 'integer':
            return (
                <div class="form-group">
                    <input
                        className="form-control"
                        placeholder={`${name} value...`}
                        type="number"
                        value={value}
                        onKeyUp={e => onChange(e)}
                        onChange={e => onChange(e)}
                    />
                </div>
            );
        case 'boolean':
            const id = `${name}_value`;
            return (
                <div className={`form-check ${css.BooleanValue}`}>
                    <input
                        className="form-check-input"
                        type="checkbox"
                        checked={value && value !== 'false'}
                        onChange={e => onChange({
                            ...e,
                            target: {
                                ...e.target,
                                value: e.target.checked,
                            },
                        })}
                        id={id} />
                    <label
                        className="form-check-label"
                        for={id}>
                        Enabled
                    </label>
                </div>
            );
        default:
            return undefined;
    }
};
const Parameter = ({
    name, required, description,
    type, defaultValue, allowedvalues,
    onCheck, onValue,
    enabledParameters,
}) => {
    const id = `param_${name}`;
    const { checked, value } = enabledParameters[name] || { checked: false };
    return (
        <div>
            <div className="form-check" key={name}>
                <input
                    className="form-check-input"
                    type="checkbox"
                    checked={checked}
                    onChange={e => onCheck(e)}
                    id={id} />
                <label
                    className="form-check-label"
                    for={id}>
                    <div>
                        <code>{name}{required && '*'}</code>
                    </div>
                    <div>{description}</div>
                </label>
            </div>
            {checked &&
                <ParameterInput
                    name={name}
                    type={type}
                    allowedvalues={allowedvalues}
                    value={value !== undefined ? value : (defaultValue === undefined ? '' : defaultValue)}
                    onChange={e => onValue(e)}
                />}
        </div>
    );
};

export const Parameters = ({
    parameters,
    enabledParameters,
    setEnabledParameters,
}) => (
    <div className={css.Parameters}>
        {parameters.map((p, i) => {
            const Item = (
                <Parameter
                    {...p}
                    enabledParameters={enabledParameters}
                    onCheck={e => setEnabledParameters({
                        ...enabledParameters,
                        [p.name]: {
                            ...enabledParameters[p.name],
                            checked: e.target.checked,
                        },
                    })}
                    onValue={e => setEnabledParameters({
                        ...enabledParameters,
                        [p.name]: {
                            ...enabledParameters[p.name],
                            value: e.target.value,
                        },
                    })} />
            );
            if (i == 0 || parameters[i - 1].section !== p.section) {
                return (
                    <>
                        <h3>{p.section}</h3>
                        {Item}
                    </>
                );
            }
            return (<>{Item}</>);
        })}
    </div>
);
