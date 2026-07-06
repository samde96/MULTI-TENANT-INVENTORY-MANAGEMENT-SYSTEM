export const APP_NAV_ITEMS = [
  { to: '/app/dashboard', permissions: [] },
  { to: '/app/locations', permissions: ['catalog:manage'] },
  { to: '/app/suppliers', permissions: ['catalog:view'] },
  { to: '/app/inventory', permissions: ['inventory:receive'] },
  { to: '/app/operations', permissions: ['requests:manage', 'transfers:manage'] },
  { to: '/app/pos', permissions: ['sales:manage'] },
  { to: '/app/approvals', permissions: ['admin:all'] },
  { to: '/app/reports', permissions: ['reports:view'] },
  { to: '/app/audit', permissions: ['audit:view'] }
]

export const ROUTE_PERMISSIONS = {
  '/app/dashboard': [],
  '/app/locations': ['catalog:manage'],
  '/app/suppliers': ['catalog:view'],
  '/app/inventory': ['inventory:receive'],
  '/app/operations': ['requests:manage', 'transfers:manage'],
  '/app/pos': ['sales:manage'],
  '/app/approvals': ['admin:all'],
  '/app/reports': ['reports:view'],
  '/app/audit': ['audit:view']
}

const ROLE_HOME_PATHS = {
  ADMIN: '/app/dashboard',
  STORE_MANAGER: '/app/inventory',
  SHOP_MANAGER: '/app/operations',
  CASHIER: '/app/pos',
  AUDITOR: '/app/reports'
}

const ROLE_PRIORITY = ['ADMIN', 'STORE_MANAGER', 'SHOP_MANAGER', 'CASHIER', 'AUDITOR']

export function hasAnyPermission(user, permissions = []) {
  if (!user) return false
  if (!permissions.length) return true
  const granted = user.permissions || []
  if (granted.includes('admin:all')) return true
  return permissions.some((permission) => granted.includes(permission))
}

export function getHomePath(user) {
  const roleCodes = new Set((user?.roles || []).map((role) => role.code))
  for (const roleCode of ROLE_PRIORITY) {
    if (roleCodes.has(roleCode)) {
      return ROLE_HOME_PATHS[roleCode]
    }
  }
  return '/app/dashboard'
}
