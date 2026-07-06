# KEEN ERP
# Inventory, Warehouse, and Shop POS Management System

## 1. System Overview

This system is a unified inventory management platform for a business that manages one or more warehouses/stores and multiple shops/branches. The warehouse/store holds stock, sends stock to shops, and tracks inventory movement. The shop side operates as a POS system, receives inventory from the warehouse, sells to customers, and reports sales, stock levels, losses, and cashier activity.

The recommended approach is **one system**, not two separate systems. The system should have one backend, one database, and separate modules for warehouse operations, shop/POS operations, reporting, RBAC, notifications, security, and audit tracking.

## 2. Technology Stack

### Frontend

- React
- Role-based dashboards
- Barcode scanner integration
- RFID/security dashboard
- POS interface for cashiers
- Admin/manager dashboards

### Backend

- Java Spring Boot
- Spring Security
- REST APIs
- PostgreSQL
- Docker for production

### Production Infrastructure

- Dockerized frontend and backend
- PostgreSQL container or managed PostgreSQL database
- Reverse proxy such as Nginx
- SSL/TLS certificate
- Centralized logs
- Backup and restore strategy
- Environment-based configuration

## 3. Core Business Model

The system should support these major entities:

- Store Owner/Admin
- Store Manager
- Warehouse/Store
- Shop/Branch
- Shop Manager
- Cashier
- Inventory Item/Product
- Supplier
- Stock Transfer
- Stock Request
- Sale
- Receipt
- Payment
- RFID/Barcode Event
- Theft/Loss Incident
- Audit Log
- Notification

## 4. Main User Roles

### 4.1 Store Owner/Admin

The Store Owner/Admin has full system control.

Permissions:

- Manage all branches and warehouses
- Add, edit, deactivate, or delete users
- Assign roles and permissions
- Add suppliers
- Add inventory categories and products
- Approve or reject stock requests
- Transfer inventory to shops
- View all reports
- View theft/loss reports
- View audit logs
- Delegate duties to Store Managers
- Configure email/SMS/notification settings
- Configure system security settings

### 4.2 Store Manager

The Store Manager manages warehouse/store operations on behalf of the Admin.

Permissions:

- Add stock to store inventory
- Scan barcodes when adding stock
- Process supplier deliveries
- Approve shop stock requests if allowed
- Send inventory to shops
- Generate transfer receipts
- View store inventory reports
- View transfer history
- View warehouse theft/loss alerts
- Receive and act on RFID alerts

### 4.3 Shop Manager

The Shop Manager controls shop inventory and supervises shop operations.

Permissions:

- Request stock from warehouse/store
- Receive transferred stock
- Approve received stock before it enters shop inventory
- Report discrepancies, damaged items, or missing stock
- View shop inventory reports
- View cashier sales reports
- Manage shop-level stock counts
- Generate shop reports

### 4.4 Cashier

The Cashier operates the POS.

Permissions:

- Sell inventory to customers
- Print or send receipts
- Process returns if allowed
- Open and close shifts
- Record payment methods
- View own sales history
- Cannot approve inventory transfers unless explicitly permitted

### 4.5 Auditor/Accountant Optional Role

This optional role is useful as the system grows.

Permissions:

- View financial reports
- View stock movement reports
- View audit logs
- Export reports
- Cannot modify stock or sales records

## 5. Role-Based Access Control Requirements

The system should implement RBAC with both role permissions and branch/location restrictions.

Example:

A cashier in Shop A should not be able to sell from Shop B's stock. A Shop Manager in Shop A should not approve inventory for Shop B. A Store Manager should only manage warehouses or stores assigned to them unless the Admin gives wider access.

Recommended RBAC structure:

```text
users
roles
permissions
user_roles
role_permissions
locations
user_locations
```

Important RBAC rules:

- Every user must belong to at least one role.
- Every user should be assigned to one or more locations.
- Admin can access all locations.
- Store Manager access can be limited to selected warehouses/stores.
- Shop Manager access should be limited to selected shops.
- Cashier access should be limited to assigned shop/POS terminal.
- Sensitive actions should require permission checks on the backend, not only on the frontend.

## 6. Location and Branch Management

The system should support multiple locations.

Location types:

- Main Warehouse
- Store
- Shop
- Branch
- Damaged Goods Area
- Returns Area
- Transit Area

Each inventory balance must belong to a specific location.

Example:

```text
Product: 1kg Sugar
Main Warehouse: 500 units
Shop A: 100 units
Shop B: 60 units
Damaged Goods Area: 5 units
Transit: 30 units
```

## 7. Inventory Management Features

### 7.1 Product Catalog

Each product should have:

- Product name
- SKU
- Barcode
- RFID tag where applicable
- Category
- Brand
- Unit of measure
- Cost price
- Selling price
- Wholesale price
- Reorder level
- Expiry date if applicable
- Batch number if applicable
- Supplier
- Tax/VAT information if applicable
- Product image
- Product status: active, inactive, discontinued

### 7.2 Barcode Scanning

When adding stock, the Store Manager should scan the barcode to identify or register products.

Barcode use cases:

- Add new product
- Receive supplier stock
- Transfer stock
- Sell item at POS
- Perform stock count
- Verify picked items before dispatch
- Verify received items at shop

### 7.3 RFID Tracking

RFID can be used for high-value goods or theft-prone products.

RFID use cases:

- Track product movement
- Detect unauthorized exit from store/warehouse
- Match RFID scans against approved transfers
- Trigger theft/loss alerts
- Support stock count automation
- Create security incident reports

### 7.4 Stock Ledger

The system should not only update stock quantity directly. Every inventory change should create a stock ledger record.

Stock movement types:

- Supplier received stock
- Warehouse to shop transfer
- Shop sale
- Customer return
- Supplier return
- Damaged stock
- Lost stock
- Theft adjustment
- Manual correction
- Stock count adjustment
- Transfer in transit
- Transfer received
- Transfer rejected

The stock ledger should store:

- Product
- Quantity in
- Quantity out
- Source location
- Destination location
- Movement type
- Reference document
- User/attendant responsible
- Date and time
- Notes

## 8. Supplier and Purchase Management

Before sending stock to shops, the store/warehouse must receive stock from suppliers.

Recommended features:

- Supplier registration
- Purchase orders
- Supplier invoices
- Goods received notes
- Partial deliveries
- Damaged goods recording
- Supplier returns
- Supplier payment status
- Cost price history
- Batch and expiry tracking

Recommended workflow:

```text
Admin/Store Manager creates Purchase Order
Supplier delivers stock
Store Manager scans items
System creates Goods Received Note
Stock is added to warehouse inventory
Owner/Admin receives notification
Purchase report is updated
```

## 9. Stock Request Workflow

The Shop Manager can request inventory from the warehouse/store.

Workflow:

```text
Shop Manager creates stock request
System notifies Store Manager/Admin
Store Manager/Admin reviews request
Request is approved, edited, or rejected
Approved items are picked from warehouse stock
Items are scanned and packed
Transfer document is generated
Inventory moves from Warehouse to In-Transit
Owner/Admin receives email notification
Shop Manager receives notification
Shop Manager receives stock
Shop Manager approves received stock
Inventory moves from In-Transit to Shop Stock
Receipt/delivery note is generated
Reports are updated
```

Request statuses:

- Draft
- Submitted
- Approved
- Rejected
- Picking
- Packed
- Dispatched
- In Transit
- Partially Received
- Received
- Discrepancy Reported
- Cancelled

## 10. Inventory Transfer Workflow

When inventory is transferred to a shop, the system must track every step.

### 10.1 Transfer Creation

The Store Manager/Admin selects:

- Source warehouse/store
- Destination shop
- Products
- Quantities
- Attendant responsible
- Vehicle/dispatch method if applicable
- Notes

### 10.2 Transfer Approval

Approval should be required for sensitive or high-value transfers.

Approval rules can be based on:

- Product value
- Quantity
- Location
- Role
- Transfer amount
- Risk level

### 10.3 Transfer Receipt

After transfer approval, the system generates a receipt/delivery note.

The receipt should include:

- Receipt number
- Transfer number
- Source location
- Destination shop
- Product names
- SKU/barcode/RFID code
- Quantity sent
- Quantity received
- Unit cost
- Total value
- Attendant name
- Store Manager/Admin name
- Shop Manager name
- Date and time sent
- Date and time received
- Status
- Signature/approval section
- QR code or verification code

### 10.4 Email Notification

The owner/admin should immediately receive an email when stock is transferred.

Email should include:

- Transfer number
- Source warehouse/store
- Destination shop
- Product summary
- Total quantity
- Total stock value
- Attendant responsible
- Approval status
- Link to view transfer
- PDF receipt attachment if needed

### 10.5 Shop Receiving and Approval

The transferred stock should not automatically become available for sale until the Shop Manager approves it.

Receiving process:

```text
Shop Manager opens pending transfer
Shop Manager scans/verifies items
System compares sent quantity vs received quantity
Shop Manager approves if correct
If correct: stock enters shop inventory
If not correct: discrepancy report is created
Admin/Store Manager is notified
```

Possible receiving outcomes:

- Fully received
- Partially received
- Damaged on arrival
- Missing items
- Extra items found
- Rejected transfer

## 11. Shop POS Features

The shop side should behave like a POS system.

Required POS features:

- Product search
- Barcode scanning at checkout
- Cart management
- Discounts with permission control
- Tax/VAT calculation if needed
- Multiple payment methods
- Cash payment
- M-Pesa/mobile money payment
- Card payment
- Split payment
- Receipt printing
- Email/SMS receipt
- Refunds and returns
- Cashier shift opening
- Cashier shift closing
- Cash drawer reconciliation
- Daily sales summary
- Offline mode if required

POS receipt should include:

- Receipt number
- Shop name
- Cashier name
- Products sold
- Quantity
- Unit price
- Discount
- Tax/VAT
- Total amount
- Payment method
- Date and time
- Return policy note
- QR code or receipt verification code

## 12. Reporting and Analytics

Both warehouse/store and shop sides should have reporting.

### 12.1 Admin Reports

- Total stock value across all locations
- Total sales across all shops
- Profit/loss report
- Branch performance
- Fast-moving products
- Slow-moving products
- Low-stock products
- Dead stock report
- Theft/loss report
- Damaged goods report
- Supplier performance
- Stock transfer report
- Cashier performance
- Store Manager activity report
- Shop Manager activity report
- Audit log report

### 12.2 Warehouse/Store Reports

- Current warehouse stock
- Stock received from suppliers
- Stock sent to shops
- Pending stock requests
- Rejected stock requests
- Stock in transit
- Damaged stock
- Theft/loss incidents
- Reorder report
- Inventory valuation

### 12.3 Shop Reports

- Current shop stock
- Stock received from warehouse
- Daily sales
- Cashier sales
- Product sales
- Returns/refunds
- Low stock
- Damaged stock
- Theft/loss
- Cash drawer report
- End-of-day report

### 12.4 Export Options

The system should support:

- PDF export
- Excel export
- CSV export
- Email scheduled reports
- Printable reports

## 13. Notifications

The system should support real-time or near-real-time notifications.

Notification channels:

- Email
- In-app notification
- SMS optional
- WhatsApp optional
- Push notification optional

Important notification events:

- Stock transferred to shop
- Stock request submitted
- Stock request approved
- Stock request rejected
- Transfer received by shop
- Transfer discrepancy reported
- Low stock alert
- Theft/RFID alert
- New supplier stock received
- Large discount applied
- Refund processed
- Cashier shift closed
- Failed login attempts
- New user created
- Role/permission changed

## 14. Theft, Loss, and Security Monitoring

The system should treat security events as first-class records.

### 14.1 RFID Theft Detection

The RFID system should detect when a tagged item leaves a store/warehouse without an approved transaction.

Example logic:

```text
RFID scan detected at exit gate
System checks if product is part of an approved sale or transfer
If no approved transaction exists, create theft alert
Notify Store Manager/Admin
Create security incident record
```

### 14.2 Theft/Loss Incident Record

Each theft/loss incident should include:

- Incident number
- Product
- RFID tag or barcode
- Location
- Quantity
- Estimated value
- Time detected
- Detection method
- Responsible shift/cashier/attendant if known
- CCTV reference optional
- Notes
- Status: open, under review, resolved, written off

### 14.3 Stock Variance Detection

During stock counts, the system should compare expected stock with counted stock.

Variance example:

```text
Expected stock: 100 units
Counted stock: 92 units
Variance: -8 units
System creates variance report
Manager reviews and approves adjustment
```

Variance approval should be required before stock is adjusted.

## 15. Audit Trail

Every sensitive action must be recorded.

Audit log should capture:

- User ID
- User name
- Role
- Action performed
- Entity affected
- Old value
- New value
- IP address
- Device/browser
- Location/branch
- Date and time
- Success/failure status

Actions to audit:

- Login/logout
- Failed login attempts
- Password changes
- Role changes
- Permission changes
- Product creation/editing/deactivation
- Stock receiving
- Stock transfer
- Transfer approval
- Transfer receiving
- Stock adjustment
- Sale
- Refund
- Discount approval
- Theft/loss adjustment
- Report export
- Data deletion

Audit logs should be tamper-resistant. Normal users should not be able to edit or delete audit logs.

## 16. Data Integrity and No Data Loss Requirements

Since the system will support many branches and shops, data integrity is very important.

Recommended controls:

- Use database transactions for all stock movements
- Use optimistic locking or row-level locking when updating stock
- Prevent negative stock unless explicitly allowed
- Use unique receipt numbers and transfer numbers
- Use stock ledger as the source of truth
- Never delete important business records permanently
- Use soft deletes where possible
- Validate all quantities and prices on the backend
- Use idempotency keys for payment and stock transfer APIs
- Use database constraints and foreign keys
- Use scheduled backups
- Test restore process regularly

Important rule:

Do not update shop stock until the Shop Manager confirms receipt. Before confirmation, transferred items should be marked as **In Transit**.

## 17. Security Measures to Add Before Building

### 17.1 Authentication Security

Recommended authentication features:

- Secure login
- Strong password policy
- Password hashing using a strong algorithm such as BCrypt or Argon2
- Multi-factor authentication for Admin and Store Manager accounts
- Account lockout or throttling after repeated failed login attempts
- Password reset using secure one-time tokens
- Session timeout
- Refresh token rotation if using JWT
- Logout/token revocation

### 17.2 Authorization Security

- Enforce RBAC on backend APIs
- Use branch/location-level access control
- Prevent users from accessing records from unauthorized branches
- Require approval for sensitive actions
- Separate cashier, manager, and admin permissions
- Deny by default when permission is missing

### 17.3 API Security

- Use HTTPS only in production
- Use rate limiting for login and sensitive endpoints
- Validate all request payloads
- Use DTOs instead of exposing database models directly
- Use pagination for large data endpoints
- Prevent mass assignment vulnerabilities
- Implement CORS correctly
- Disable debug endpoints in production
- Document all API endpoints and versions
- Version APIs, for example `/api/v1/...`

### 17.4 Database Security

- Use least-privilege database users
- Do not use the PostgreSQL superuser for the application
- Encrypt backups
- Use database migrations with Flyway or Liquibase
- Add constraints and indexes
- Use foreign keys
- Keep audit logs separate from normal editable records
- Avoid permanent deletion of financial and inventory records

### 17.5 Docker and Production Security

- Do not store secrets inside Docker images
- Use environment variables or Docker secrets
- Use non-root container users where possible
- Keep base images updated
- Use multi-stage Docker builds
- Scan images for vulnerabilities
- Do not expose PostgreSQL publicly
- Use private Docker networks
- Use health checks
- Use log rotation

### 17.6 Frontend Security

- Protect routes based on roles
- Do not rely only on frontend route guards
- Store tokens safely
- Avoid exposing sensitive environment variables
- Sanitize user input displayed in the UI
- Use secure HTTP-only cookies if possible
- Add Content Security Policy headers

### 17.7 Device and POS Security

- Register POS terminals/devices
- Restrict cashiers to assigned devices if needed
- Track device ID in sales records
- Auto-lock POS after inactivity
- Require manager PIN for refunds, discounts, voids, or stock adjustments
- Log all device activity

## 18. Extra Functionalities Recommended Before Building

### 18.1 Stock Count and Reconciliation

Add a module for physical stock counts.

Features:

- Scheduled stock count
- Barcode/RFID-based counting
- Expected vs counted quantity
- Variance report
- Manager approval before adjustment
- Count history

### 18.2 Returns and Refunds

Add controlled return/refund workflows.

Features:

- Customer return
- Supplier return
- Damaged goods return
- Refund approval
- Return receipt
- Reason codes
- Stock restocking rules

### 18.3 Damaged Goods Management

Track damaged stock separately.

Features:

- Move damaged items to damaged stock location
- Record damage reason
- Attach photo evidence optional
- Approve write-off
- Report damaged stock value

### 18.4 Reorder Management

The system should automatically alert managers when stock is low.

Features:

- Reorder level per product/location
- Suggested reorder quantity
- Supplier recommendation
- Purchase order creation from low-stock report

### 18.5 Cashier Shift Management

For POS accountability, every cashier should work in shifts.

Features:

- Open shift
- Opening cash amount
- Sales during shift
- Cash expected
- Cash counted
- Cash difference
- Close shift
- Manager approval for cash variance

### 18.6 Pricing and Discount Control

Features:

- Product price levels
- Wholesale price
- Retail price
- Branch-specific pricing
- Discount limits per role
- Manager approval for high discounts
- Price change history

### 18.7 Customer Management Optional

If shops sell to repeat customers, add a simple CRM.

Features:

- Customer profile
- Customer purchase history
- Credit sales optional
- Loyalty points optional
- Customer receipts

### 18.8 Payment Tracking

Features:

- Cash
- M-Pesa/mobile money
- Card
- Split payment
- Payment reference number
- Failed payment tracking
- Payment reconciliation

### 18.9 Offline Mode Optional

If shops may lose internet, offline mode can be added later.

Offline mode should support:

- Local sales recording
- Local receipt generation
- Sync when internet returns
- Conflict resolution
- Prevention of duplicate receipts
- Server-generated sync confirmation

Offline mode is powerful but complex, so it can be planned for version 2 if not required immediately.

## 19. Recommended Database Tables

This is a starting structure, not the final schema.

```text
users
roles
permissions
user_roles
role_permissions
locations
user_locations
suppliers
categories
products
product_barcodes
product_rfid_tags
inventory_balances
stock_ledger
stock_requests
stock_request_items
stock_transfers
stock_transfer_items
purchase_orders
purchase_order_items
goods_received_notes
goods_received_note_items
sales
sale_items
payments
receipts
cashier_shifts
stock_counts
stock_count_items
stock_adjustments
theft_incidents
notifications
audit_logs
system_settings
```

## 20. Key Backend Design Rules

### 20.1 Use Stock Ledger as Source of Truth

Inventory should be calculated from movements or updated using transaction-safe balance records backed by a ledger.

Recommended approach:

- `inventory_balances` stores current quantity for fast reads.
- `stock_ledger` stores every movement for audit and reporting.
- Every stock change must update both inside one database transaction.

### 20.2 Use Transactions

Operations that must be atomic:

- Receive supplier stock
- Transfer stock
- Receive transfer at shop
- POS sale
- Refund
- Stock adjustment
- Theft/loss write-off

If one step fails, the entire transaction should roll back.

### 20.3 Use In-Transit Inventory

When warehouse sends stock to shop:

```text
Warehouse stock decreases
In-transit stock increases
Shop stock does not increase yet
```

When shop approves receipt:

```text
In-transit stock decreases
Shop stock increases
```

If shop reports discrepancy, the system should keep the transfer open until reviewed.

## 21. Suggested API Modules

```text
/api/v1/auth
/api/v1/users
/api/v1/roles
/api/v1/permissions
/api/v1/locations
/api/v1/suppliers
/api/v1/products
/api/v1/inventory
/api/v1/stock-requests
/api/v1/stock-transfers
/api/v1/purchases
/api/v1/sales
/api/v1/pos
/api/v1/payments
/api/v1/receipts
/api/v1/reports
/api/v1/notifications
/api/v1/rfid
/api/v1/audit-logs
/api/v1/settings
```

## 22. Suggested Development Phases

### Phase 1: Foundation

- Project setup
- Docker setup
- PostgreSQL setup
- Authentication
- RBAC
- Locations/branches
- Product catalog
- Supplier module

### Phase 2: Warehouse Inventory

- Add stock
- Barcode scanning support
- Purchase receiving
- Inventory balances
- Stock ledger
- Warehouse reports

### Phase 3: Stock Requests and Transfers

- Shop stock requests
- Approval workflow
- Transfer dispatch
- In-transit stock
- Shop receiving approval
- Transfer receipt generation
- Email notifications

### Phase 4: Shop POS

- POS screen
- Barcode checkout
- Sales
- Payments
- Receipts
- Cashier shifts
- Returns/refunds

### Phase 5: Reporting and Audit

- Admin dashboard
- Store reports
- Shop reports
- Cashier reports
- Stock reports
- Audit logs
- PDF/Excel exports

### Phase 6: Security and RFID

- RFID event tracking
- Theft alerts
- Stock variance detection
- Manager approval rules
- Device/POS terminal tracking
- Security hardening

### Phase 7: Production Deployment

- Docker production build
- Reverse proxy
- SSL/TLS
- Backups
- Monitoring
- Log management
- Error tracking
- Load testing

## 23. Minimum Viable Product Scope

For the first version, build the most important features first.

MVP features:

- Login/authentication
- RBAC
- Branch/location management
- Product catalog
- Supplier stock receiving
- Barcode support
- Warehouse inventory
- Shop stock request
- Store Manager/Admin approval
- Stock transfer
- Shop Manager receiving approval
- Transfer receipt
- Email notification to owner/admin
- Shop POS sale
- POS receipt
- Basic reports
- Audit logs

Features that can come after MVP:

- RFID theft detection
- Advanced analytics
- Offline mode
- Loyalty system
- WhatsApp notifications
- Advanced accounting integration
- AI anomaly detection
- Multi-currency
- E-commerce integration

## 24. Critical Business Rules

- Stock cannot be sold from a shop until it has been received and approved by the Shop Manager.
- Every stock movement must create a ledger record.
- Every receipt must have a unique receipt number.
- Every transfer must have a unique transfer number.
- Cashiers can only sell from assigned shops.
- Store Managers can only manage assigned warehouses/stores unless given extra permission.
- Discounts, refunds, voids, and stock adjustments should require permission.
- Theft/loss adjustments should require manager/admin approval.
- Audit logs must not be editable by normal users.
- Stock transfers should support discrepancies.
- Reports should always be filterable by branch, date, product, user, and status.

## 25. Recommended Security Standards to Follow

Use these standards and references during development:

- OWASP API Security Top 10 for API risks such as broken authentication, broken authorization, misconfiguration, rate limiting, and poor API inventory.
- OWASP ASVS as a checklist for application security controls.
- NIST SP 800-63B for authentication and password guidance.
- Docker secrets or a proper secrets manager for production secrets.

## 26. Final Recommendation

Build one unified system with separate warehouse/store and shop/POS modules. The most important design decision is to track inventory as movements between locations instead of only updating quantities. This gives you accurate reports, transfer history, theft tracking, cashier accountability, stock reconciliation, and protection against data loss.

The system should be designed around:

- Multi-branch inventory
- Stock ledger
- Transfer approval workflow
- Shop receiving approval
- POS sales
- RBAC
- Audit logs
- Receipts and notifications
- Theft/loss tracking
- Production-grade security
- Reliable backups

This structure will allow the business to manage many shops and warehouses under one platform while keeping stock, sales, users, and reports accurate.

## COLORS
/* CSS HEX */
--#10e39f: #10e39f;
--#00ff97: #00ff97;
--#5855c5: #5855c5;
--#2d98b1: #2d98b1;
--#7c7ccf: #7c7ccf;
--#347ab6: #347ab6;
--#25b1ac: #25b1ac;
--#cbcaeb: #cbcaeb;
--#3f3cbf: #3f3cbf;
--#ffffff: #ffffff

/* SCSS HEX */
$#10e39f: #10e39f;
$#00ff97: #00ff97;
$#5855c5: #5855c5;
$#2d98b1: #2d98b1;
$#7c7ccf: #7c7ccf;
$#347ab6: #347ab6;
$#25b1ac: #25b1ac;
$#cbcaeb: #cbcaeb;
$#3f3cbf: #3f3cbf;

/* SCSS RGB */
$#10e39f: rgba(16,227,159,1);
$#00ff97: rgba(0,255,151,1);
$#5855c5: rgba(88,85,197,1);
$#2d98b1: rgba(45,152,177,1);
$#7c7ccf: rgba(124,124,207,1);
$#347ab6: rgba(52,122,182,1);
$#25b1ac: rgba(37,177,172,1);
$#cbcaeb: rgba(203,202,235,1);
$#3f3cbf: rgba(63,60,191,1);
$#ffffff : (rgba(255, 255, 255))