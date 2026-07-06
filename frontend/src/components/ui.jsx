import { Loader2 } from 'lucide-react'

export function PageSection({ title, subtitle, actions, children }) {
  return (
    <section className="section">
      <div className="section-head">
        <div>
          <h2>{title}</h2>
          {subtitle ? <p>{subtitle}</p> : null}
        </div>
        {actions ? <div className="section-actions">{actions}</div> : null}
      </div>
      <div className="section-body">{children}</div>
    </section>
  )
}

export function MetricCard({ label, value, note, tone = 'teal' }) {
  return (
    <div className={`metric metric-${tone}`}>
      <div className="metric-label">{label}</div>
      <div className="metric-value">{value}</div>
      {note ? <div className="metric-note">{note}</div> : null}
    </div>
  )
}

export function Badge({ children, tone = 'slate' }) {
  return <span className={`badge badge-${tone}`}>{children}</span>
}

export function Button({ children, icon: Icon, variant = 'primary', busy = false, ...props }) {
  const { className = '', ...rest } = props
  return (
    <button className={`btn btn-${variant} ${className}`.trim()} {...rest}>
      {busy ? <Loader2 className="spin" size={16} /> : Icon ? <Icon size={16} /> : null}
      <span>{children}</span>
    </button>
  )
}

export function Field({ label, children, hint }) {
  return (
    <label className="field">
      <span>{label}</span>
      {children}
      {hint ? <small>{hint}</small> : null}
    </label>
  )
}

export function Input(props) {
  return <input className="input" {...props} />
}

export function Select(props) {
  return <select className="input" {...props} />
}

export function TextArea(props) {
  return <textarea className="input" rows={props.rows || 3} {...props} />
}

export function Table({ columns, rows, rowKey, renderRow, emptyMessage = 'No records yet.' }) {
  return (
    <div className="table-wrap">
      <table className="table">
        <thead>
          <tr>
            {columns.map((column) => (
              <th key={column}>{column}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.length ? rows.map((row) => renderRow(row, rowKey ? row[rowKey] : row)) : (
            <tr>
              <td colSpan={columns.length} className="empty-cell">
                {emptyMessage}
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  )
}
