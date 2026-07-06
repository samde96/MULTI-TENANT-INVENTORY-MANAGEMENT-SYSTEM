import { ArrowLeft, ShieldCheck, UserPlus } from 'lucide-react'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { apiFetch } from '../api/client'
import { Badge, Button, Field, Input } from '../components/ui'

export default function RegisterPage() {
  const [username, setUsername] = useState('')
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [busy, setBusy] = useState(false)
  const navigate = useNavigate()

  const locked = Boolean(success)

  async function handleSubmit(event) {
    event.preventDefault()

    if (password !== confirmPassword) {
      setError('Passwords do not match')
      return
    }

    setBusy(true)
    setError('')
    try {
      const response = await apiFetch('/auth/register', {
        method: 'POST',
        body: {
          username: username.trim(),
          fullName: fullName.trim(),
          email: email.trim(),
          password
        }
      })
      setSuccess(response.message || 'Registration received. Your account is pending approval.')
      setUsername('')
      setFullName('')
      setEmail('')
      setPassword('')
      setConfirmPassword('')
    } catch (ex) {
      setError(ex.message || 'Registration failed')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="auth-wrap">
      <form className="auth-card auth-card-wide" onSubmit={handleSubmit}>
        <div className="brand" style={{ marginBottom: 10 }}>
          <div className="brand-mark">
            <ShieldCheck size={18} />
          </div>
          <div>
            <div className="brand-title">KEEN ERP</div>
            <div className="brand-subtitle">Request a new account</div>
          </div>
        </div>

        <Badge tone="warning">Pending approval</Badge>
        <h1>Create account</h1>
        <p>Submit your details and wait for admin approval before first sign-in.</p>

        {error ? <div className="error-banner">{error}</div> : null}
        {success ? <div className="success-banner">{success}</div> : null}

        <div className="auth-grid">
          <Field label="Username">
            <Input
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              autoComplete="username"
              placeholder="Username"
              disabled={busy || locked}
            />
          </Field>
          <Field label="Full name">
            <Input
              value={fullName}
              onChange={(event) => setFullName(event.target.value)}
              autoComplete="name"
              placeholder="Full name"
              disabled={busy || locked}
            />
          </Field>
          <Field label="Email">
            <Input
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              autoComplete="email"
              placeholder="Email address"
              disabled={busy || locked}
            />
          </Field>
          <Field label="Password" hint="Use at least 8 characters.">
            <Input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              autoComplete="new-password"
              placeholder="Password"
              disabled={busy || locked}
            />
          </Field>
          <Field label="Confirm password">
            <Input
              type="password"
              value={confirmPassword}
              onChange={(event) => setConfirmPassword(event.target.value)}
              autoComplete="new-password"
              placeholder="Confirm password"
              disabled={busy || locked}
            />
          </Field>
        </div>

        <div className="form-actions" style={{ justifyContent: 'space-between', marginTop: 18 }}>
          <Button type="button" variant="secondary" icon={ArrowLeft} onClick={() => navigate('/login')}>
            Back to sign in
          </Button>
          {locked ? (
            <Button type="button" variant="secondary" icon={ShieldCheck} onClick={() => navigate('/login')}>
              Go to login
            </Button>
          ) : (
            <Button type="submit" busy={busy} icon={UserPlus} disabled={busy || locked}>
              Request access
            </Button>
          )}
        </div>
      </form>
    </div>
  )
}
