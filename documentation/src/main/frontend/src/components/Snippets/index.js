import css from './Snippets.scss';

const Snippet = ({ value, language }) => (
    <pre>
        <code
            class={`hljs language-${language}`}
            dangerouslySetInnerHTML={{ __html: value }}
        />
    </pre>
);

export const Snippets = ({ xml, java }) => (
    <div className={`${css.ConnectorConfigurationSample} row`}>
        <div className="col-sm-7">
            <Snippet value={java} language="java" />
        </div>
        <div className="col-sm-5">
            <Snippet value={xml} language="xml" />
        </div>
    </div>
);
