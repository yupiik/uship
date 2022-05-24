import { useEffect, useMemo, useState } from "preact/hooks";
import { defaultFor } from "../../processor/defaultFor";
import { enabledParams } from "../../processor/enabledParameters";
import { Parameters } from "../Parameters";
import { Snippets } from "../Snippets";
import css from './ValveConfigurer.scss';

const compare = (a, b) => {
    if (a === 'className') {
        return -1;
    }
    if (b === 'className') {
        return 1;
    }
    return a.localeCompare(b);
};

const findValve = (valves, name) => {
    const found = valves.filter(it => it.name === name);
    return found.length === 1 ? found[0] : undefined;
};

const asXml = (enabledParameters, paramsSpec) => {
    const active = enabledParams(enabledParameters).sort(compare);
    return [
        '<Valve',
        ...active.map(p => `  ${p}="${enabledParameters[p].value === undefined ?
            defaultFor(paramsSpec, p) :
            enabledParameters[p].value}"`),
        '/>',
    ].join('\n');
};

const asJava = (enabledParameters, paramsSpec) => {
    const params = enabledParams(enabledParameters);
    let clazz = enabledParameters.className;
    if (clazz) {
        clazz = clazz.value;
    }
    if (!clazz) {
        clazz = paramsSpec.filter(it => it.name === 'className')[0].defaultValue;
    }
    const valve = clazz.substring(clazz.lastIndexOf('.') + 1);
    return ([
        '@ApplicationScoped',
        'public class ServerConfiguration {',
        '  @Produces',
        '  @ApplicationScoped',
        '  public TomcatWebServerConfiguration configuration() {',
        '    final var conf = new TomcatWebServerConfiguration();',
        '    // connector configuration etc if needed',
        '',
        '    conf.setContextCustomizers(List.of(;',
        '      // enable the valve', ,
        `      this::configure${valve}));`,
        '    return conf;',
        '  }',
        '',
        `  private void configure${valve}(final StandardContext context) {`,
        `    final var valve = new ${valve}();`,
        ...params.filter(it => it !== 'className').map(it => {
            let value = enabledParameters[it].value === undefined ?
                defaultFor(paramsSpec, it) :
                enabledParameters[it].value;
            const type = paramsSpec.filter(s => s.name === it)[0].type;
            if (type === 'string') {
                value = `"${value}"`;
            }
            const capitalizedName = it.charAt(0).toUpperCase() + it.substring(1);
            return `    valve.set${capitalizedName}(${value});`;
        }),
        '    ctx.getPipeline().addValve(valve);',
        '  }',
        '}',
    ].join('\n'));
};

const Selector = ({
    valves,
    selected,
    setSelected,
}) => (
    <div>
        <select value={selected} onChange={it => setSelected(it.target.value)}>
            <option selected disabled value="">Select a valve</option>
            {valves.map(it => (
                <option value={it.name} key={it.name}>
                    <div>{it.name}</div>
                </option>
            ))}
        </select>
    </div>
);

const ValveDescription = ({ valves, selected }) => (
    <div className={css.Description}>
        {findValve(valves, selected).description}
    </div>
);

export const ValveConfigurer = ({
    valves,
}) => {
    const [selected, setSelected] = useState(undefined);
    const [enabledParameters, setEnabledParameters] = useState({});
    const parameters = useMemo(
        () => !selected || selected.length === 0 ? [] : (findValve(valves, selected) || {}).attributes,
        [valves, selected]);
    useEffect(
        () => {
            const params = parameters
                .filter(it => it.required && it.defaultValue)
                .reduce((a, it) => ({
                    ...a,
                    [it.name]: {
                        value: it.defaultValue,
                        checked: true,
                    },
                }), {});
            setEnabledParameters(params);
        },
        [parameters]);
    const previewXmlSnippet = useMemo(() => !selected || selected.length === 0 ?
        undefined :
        window.hljs.highlight(asXml(enabledParameters, parameters), { language: 'xml' }).value,
        [selected, enabledParameters]);
    const previewJavaSnippet = useMemo(() => !selected || selected.length === 0 ?
        undefined :
        window.hljs.highlight(asJava(enabledParameters, parameters), { language: 'java' }).value,
        [selected, enabledParameters]);

    return (
        <div className={css.ValveConfigurer}>
            <Selector
                valves={valves}
                selected={selected}
                setSelected={setSelected}
            />

            {selected && selected.length > 0 &&
                <ValveDescription
                    valves={valves}
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