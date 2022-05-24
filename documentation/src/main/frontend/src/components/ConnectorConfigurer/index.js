import { useMemo, useState } from 'preact/hooks';
import { defaultFor } from '../../processor/defaultFor';
import { enabledParams } from '../../processor/enabledParameters';
import { Parameters } from '../Parameters';
import { Snippets } from '../Snippets';
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

export const ConnectorConfigurer = ({
    connectors,
}) => {
    const [selected, setSelected] = useState('');
    const [enabledParameters, setEnabledParameters] = useState({});
    const parameters = useMemo(() => {
        if (!selected || selected.length === 0) {
            return [];
        }
        const filters = findConnector(selected).sectionsMatchers;
        return connectors
            .filter(it => filters.some(f => f(it.name)))
            .map(section => (section.attributes || []).map(it => ({ ...it, section: section.name })))
            .reduce((agg, attributes) => ([...agg, ...attributes]), []);
    }, [connectors, selected]);

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
                <Parameters
                    parameters={parameters}
                    enabledParameters={enabledParameters}
                    setEnabledParameters={setEnabledParameters}
                />}
        </div>
    );
};
