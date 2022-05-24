import { useState } from 'preact/hooks';
import { processValves } from '../../processor/valveProcessor';
import { ConnectorConfigurer } from '../ConnectorConfigurer';
import { Switch } from '../Switch';
import { ValveConfigurer } from '../ValveConfigurer';
import css from './App.scss';

const {
    tomcatVersion,
    connectors,
    valves,
} = processValves(window.tomcatPreact || {
    tomcatVersion: '1.0.21',
    connectors: [],
    valves: [],
});

export const App = () => {
    const [connectorToggle, setConnectorToggle] = useState(true);
    return (
        <div className={css.App}>
            <h2>Tomcat v{tomcatVersion}</h2>
            <div>
                <h3>Configuration Element</h3>
                <span>Valve</span>
                <Switch
                    value={connectorToggle}
                    onChange={value => setConnectorToggle(value)}
                />
                <span>Connector</span>
            </div>
            {connectorToggle &&
                <ConnectorConfigurer
                    connectors={connectors}
                />}
            {!connectorToggle &&
                <ValveConfigurer
                    valves={valves}
                />}
        </div>
    );
};
