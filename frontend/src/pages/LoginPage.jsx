import { ShieldCheck, UserPlus } from 'lucide-react'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Field, Input } from '../components/ui'
import { useAuth } from '../context/AuthContext'
import { getHomePath } from '../utils/access'

export default function LoginPage() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const { login } = useAuth()
  const navigate = useNavigate()

  async function handleSubmit(event) {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      const user = await login(username.trim(), password)
      navigate(getHomePath(user), { replace: true })
    } catch (ex) {
      setError(ex.message || 'Login failed')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="auth-wrap">
      <form className="auth-card" onSubmit={handleSubmit}>
        <div className="brand" style={{ marginBottom: 10 }}>
          <div className="brand-mark">
            <ShieldCheck size={18} />
          </div>
          <div>
            <div className="brand-title">KEEN ERP</div>
            <div className="brand-subtitle">Unified stock, transfer, and POS control</div>
          </div>
        </div>
        <h1>Sign in</h1>
        <p>Access the warehouse, shop, and cashier workspace from one backend.</p>

        {error ? <div className="error-banner">{error}</div> : null}

        <div className="auth-grid">
          <Field label="Username">
            <Input value={username} onChange={(event) => setUsername(event.target.value)} autoComplete="username" placeholder="Username" />
          </Field>
          <Field label="Password">
            <Input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              autoComplete="current-password"
              placeholder="Password"
            />
          </Field>
        </div>

        <div className="form-actions" style={{ justifyContent: 'space-between', marginTop: 18 }}>
          <div className="badge badge-slate">Sign in with your assigned account</div>
          <Button type="submit" busy={busy} icon={ShieldCheck} disabled={busy}>
            Enter system
          </Button>
        </div>

        <div className="form-actions" style={{ justifyContent: 'space-between', marginTop: 10 }}>
          <span className="auth-footnote">Need a new account?</span>
          <Button type="button" variant="secondary" icon={UserPlus} onClick={() => navigate('/register')}>
            Create account
          </Button>
        </div>
      </form>
    </div>
  )
}
