import css from './Switch.scss';

export const Switch = ({ value, onChange }) => (
    <div className={css.Switch} onClick={() => onChange(!value)}>
        <input type="checkbox" checked={value} value={value} />
        <span className={css.slider} />
    </div>
);
