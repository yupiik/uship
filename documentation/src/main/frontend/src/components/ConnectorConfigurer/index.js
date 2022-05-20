import { useMemo, useState } from 'preact/hooks';
import css from './ConnectorConfigurer.scss';

const CONNECTOR_TYPES = [ // /!\ sorted by importance!
    {
        name: 'NIO',
        protocol: 'HTTP/1.1',
        description: 'Default connector (use it when you are not sure about which one to pick). It uses Java NIO API.',
        sectionsMatchers: [ // sorted too
            name => name.indexOf('Common') >= 0,
            name => name.indexOf('Standard') >= 0,
            name => name.indexOf('TCP') >= 0,
            name => name.indexOf('NIO') >= 0 && name.indexOf('NIO2') < 0,
        ],
    },
    /* don't make it over complex for now, NIO is sufficient in 99.9% of cases
    {
        name: 'NIO2',
        protocol: 'org.apache.coyote.http11.Http11Nio2Protocol',
        description: 'Default connector (use it when you are not sure about which one to pick). It uses Java NIO API.',
        sectionsMatchers: [],
    },
    */
    {
        name: 'APR',
        protocol: 'AJP/1.3',
        description: 'Connector using Apache Portable Runtime (APR) to hopefully better scale behind httpd v2.',
        sectionsMatchers: [
            name => name.indexOf('Common') >= 0,
            name => name.indexOf('Standard') >= 0,
            name => name.indexOf('APR') >= 0,
        ],
    },
];

const enabledParams = params => Object
    .keys(params)
    .sort()
    .filter(p => params[p].checked);

const defaultFor = (all, name) => {
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

const asXml = (connector, params, paramsSpec) => {
    const active = enabledParams(params);
    return [
        '<Connector',
        `  protocol="${connector.protocol}"`,
        ...(active.indexOf('port') >= 0 ? [] : ['  port="8080"']),
        ...active.map(p => `  ${p}="${params[p].value === undefined ? defaultFor(paramsSpec, p) : params[p].value}"`),
        '/>',
    ].join('\n');
};

const asJava = (connector, enabledParameters, paramsSpec) => {
    const params = enabledParams(enabledParameters);
    const port = (enabledParameters['port'] || {}).value || '8080';
    const needsConfigure = params.length > 0 || connector.protocol !== CONNECTOR_TYPES[0].protocol;
    return ([
        '@ApplicationScoped',
        'public class ServerConfiguration {',
        '  @Produces',
        '  @ApplicationScoped',
        '  public TomcatWebServerConfiguration configuration() {',
        '    final var conf = new TomcatWebServerConfiguration();',
        `    conf.setPort(${port});`,
        ...(needsConfigure ? [
            `    conf.setCompression(null); // use configure() instead`,
            '    conf.setTomcatCustomizers(List.of(this::configure));',
        ] : []),
        '    return conf;',
        '  }',
        ...(needsConfigure ? [
            '',
            '  private void configure(final Tomcat tomcat) {',
            `    final var connector = new Connector("${connector.protocol}");`,
            `    connector.setPort(${port});`,
            ...params
                .filter(it => it !== 'port')
                .map(param => `    connector.setProperty("${param}", "${enabledParameters[param].value === undefined ? defaultFor(paramsSpec, param) : enabledParameters[param].value}");`),
            '    tomcat.setConnector(connector);',
            '  }',
        ] : []),
        '}',
    ].join('\n'));
};

const findConnector = name => CONNECTOR_TYPES.filter(it => it.name === name)[0];

const Selector = ({ selected, setSelected }) => (
    <div>
        <select value={selected} onChange={it => setSelected(it.target.value)}>
            <option selected disabled value="">Select a connector type</option>
            {CONNECTOR_TYPES
                .map(it => (
                    <option value={it.name} key={it.name}>
                        <div>{it.name}</div>
                    </option>
                ))}
        </select>
    </div>
);

const ConnectorDescription = ({ selected }) => (
    <div className={css.ConnectorDescription}>
        {findConnector(selected).description}
    </div>
);

const Snippet = ({ value, language }) => (
    <pre>
        <code
            class={`hljs language-${language}`}
            dangerouslySetInnerHTML={{ __html: value }}
        />
    </pre>
);
const Snippets = ({ xml, java }) => (
    <div className={`${css.ConnectorConfigurationSample} row`}>
        <div className="col-sm-7">
            <Snippet value={java} language="java" />
        </div>
        <div className="col-sm-5">
            <Snippet value={xml} language="xml" />
        </div>
    </div>
);

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

const Configuration = ({ parameters, enabledParameters, setEnabledParameters }) => (
    <div className={css.Configuration}>
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
                    onValue={e => {
                        setEnabledParameters({
                            ...enabledParameters,
                            [p.name]: {
                                ...enabledParameters[p.name],
                                value: e.target.value,
                            },
                        });
                    }} />
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

export const ConnectorConfigurer = ({
    sections,
}) => {
    const [selected, setSelected] = useState('');
    const [enabledParameters, setEnabledParameters] = useState({});
    const parameters = useMemo(() => {
        if (!selected || selected.length === 0) {
            return [];
        }
        const filters = findConnector(selected).sectionsMatchers;
        return sections
            .filter(it => filters.some(f => f(it.name)))
            .map(section => (section.attributes || []).map(it => ({ ...it, section: section.name })))
            .reduce((agg, attributes) => ([...agg, ...attributes]), []);
    }, [sections, selected]);

    const previewXmlSnippet = useMemo(() => !selected || selected.length === 0 ?
        undefined :
        window.hljs.highlight(asXml(findConnector(selected), enabledParameters, parameters), { language: 'xml' }).value,
        [selected, enabledParameters]);
    const previewJavaSnippet = useMemo(() => !selected || selected.length === 0 ?
        undefined :
        window.hljs.highlight(asJava(findConnector(selected), enabledParameters, parameters), { language: 'java' }).value, [selected, enabledParameters]);

    return (
        <div className={css.ConnectorConfigurer}>
            <Selector
                selected={selected}
                setSelected={setSelected}
            />

            {selected && selected.length > 0 &&
                <ConnectorDescription
                    selected={selected}
                />}

            {previewXmlSnippet &&
                <Snippets
                    java={previewJavaSnippet}
                    xml={previewXmlSnippet}
                />}

            {parameters && parameters.length > 0 &&
                <Configuration
                    parameters={parameters}
                    enabledParameters={enabledParameters}
                    setEnabledParameters={setEnabledParameters}
                />}
        </div>
    );
};
