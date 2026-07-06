import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { getHomePath, hasAnyPermission } from '../utils/access'

export default function AccessGate({ permissions = [], children }) {
  const { user } = useAuth()

  if (!user) {
    return <Navigate to="/login" replace />
  }

  if (!hasAnyPermission(user, permissions)) {
    return <Navigate to={getHomePath(user)} replace />
  }

  return children
}
