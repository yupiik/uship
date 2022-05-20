import { ConnectorConfigurer } from '../ConnectorConfigurer';
import css from './App.scss';

const {
    tomcatVersion,
    sections,
} = window.tomcatPreact || {
    tomcatVersion: '1.0.21',
    sections: [],
};

export const App = () => {
    return (
        <div className={css.App}>
            <h2>Tomcat v{tomcatVersion}</h2>

            <ConnectorConfigurer
                sections={sections}
            />
        </div>
    );
};
