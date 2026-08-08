-- =========================================================
-- Prop-OS — Ultimate Property Operating System
-- Phase 1 DDL — PostgreSQL 16
-- Domains: Organization/RBAC, Portfolio/CRM, Financial, Tenant, Maintenance, Communication, IoT
-- =========================================================

-- Enable pgcrypto for uuid
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =========================================================
-- COMMON & ORGANIZATION & SECURITY
-- =========================================================

CREATE TABLE organizations (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    owner_user_id BIGINT,
    subscription_plan VARCHAR(50) NOT NULL DEFAULT 'TRIAL', -- FREE, TRIAL, PRO, GROWTH, ENTERPRISE
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, SUSPENDED, DELETED
    billing_email VARCHAR(255),
    max_properties INT DEFAULT 5,
    logo_s3_key VARCHAR(500),
    address TEXT,
    city VARCHAR(100),
    state VARCHAR(100),
    pincode VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT,
    updated_by BIGINT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_org_status ON organizations(status);
CREATE INDEX idx_org_slug ON organizations(slug);

CREATE TABLE app_users (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    org_id BIGINT REFERENCES organizations(id),
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(500) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    avatar_s3_key VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INVITED, BLOCKED, DELETED
    last_login TIMESTAMP,
    mfa_enabled BOOLEAN DEFAULT FALSE,
    email_verified BOOLEAN DEFAULT FALSE,
    phone_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT,
    updated_by BIGINT,
    is_deleted BOOLEAN DEFAULT FALSE,
    version BIGINT DEFAULT 0,
    UNIQUE(org_id, email)
);
CREATE INDEX idx_user_org ON app_users(org_id);
CREATE INDEX idx_user_email ON app_users(email);
CREATE INDEX idx_user_phone ON app_users(phone);
CREATE INDEX idx_user_status ON app_users(status);

CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    org_id BIGINT REFERENCES organizations(id), -- NULL for system roles
    name VARCHAR(100) NOT NULL, -- SUPER_ADMIN, PROPERTY_MANAGER, STAFF, TENANT, VENDOR, ACCOUNTANT, LEAD_AGENT
    description VARCHAR(500),
    hierarchy_level INT NOT NULL, -- 100 SUPER_ADMIN, 80 PROPERTY_MANAGER, 60 ACCOUNTANT, 50 STAFF, 30 TENANT, 20 VENDOR
    is_system BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(org_id, name)
);
CREATE INDEX idx_roles_org ON roles(org_id);
CREATE INDEX idx_roles_name ON roles(name);

CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    name VARCHAR(150) NOT NULL UNIQUE, -- PROPERTY_READ, PROPERTY_WRITE, UNIT_MANAGE, LEAD_MANAGE, INVOICE_MANAGE, LEASE_MANAGE, TICKET_MANAGE, VENDOR_MANAGE, USER_MANAGE, REPORT_VIEW, COMMUNICATION_SEND, IOT_MANAGE, SETTINGS_MANAGE...
    description VARCHAR(500),
    category VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    granted_at TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE user_roles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    org_id BIGINT NOT NULL REFERENCES organizations(id),
    assigned_at TIMESTAMP DEFAULT NOW(),
    assigned_by BIGINT REFERENCES app_users(id),
    UNIQUE(user_id, role_id, org_id)
);
CREATE INDEX idx_user_roles_user ON user_roles(user_id);
CREATE INDEX idx_user_roles_role ON user_roles(role_id);

-- =========================================================
-- PILLAR 1: PORTFOLIO & CRM
-- =========================================================

CREATE TABLE amenities (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    org_id BIGINT NOT NULL REFERENCES organizations(id),
    name VARCHAR(150) NOT NULL,
    category VARCHAR(100), -- COMMON, UNIT, SAFETY, LIFESTYLE
    icon VARCHAR(100),
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(org_id, name)
);

CREATE TABLE properties (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    org_id BIGINT NOT NULL REFERENCES organizations(id),
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL, -- RESIDENTIAL, COMMERCIAL, MIXED, CO_LIVING, PLOT
    address TEXT NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100),
    pincode VARCHAR(20),
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    total_floors INT,
    total_units INT,
    year_built INT,
    manager_id BIGINT REFERENCES app_users(id),
    status VARCHAR(30) DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, UNDER_CONSTRUCTION
    description TEXT,
    thumbnail_s3_key VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT,
    updated_by BIGINT,
    is_deleted BOOLEAN DEFAULT FALSE,
    version BIGINT DEFAULT 0
);
CREATE INDEX idx_properties_org ON properties(org_id);
CREATE INDEX idx_properties_city ON properties(city);
CREATE INDEX idx_properties_status ON properties(status) WHERE is_deleted=false;

CREATE TABLE property_amenities (
    property_id BIGINT NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    amenity_id BIGINT NOT NULL REFERENCES amenities(id) ON DELETE CASCADE,
    PRIMARY KEY (property_id, amenity_id)
);

CREATE TABLE units (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    org_id BIGINT NOT NULL REFERENCES organizations(id),
    property_id BIGINT NOT NULL REFERENCES properties(id),
    unit_number VARCHAR(50) NOT NULL,
    floor INT,
    type VARCHAR(50) NOT NULL, -- FLAT, BED, SHOP, OFFICE, VILLA, PENTHOUSE, STUDIO
    size_sqft DECIMAL(10,2),
    bedrooms INT,
    bathrooms INT,
    rent_amount DECIMAL(12,2) NOT NULL,
    deposit_amount DECIMAL(12,2),
    status VARCHAR(30) NOT NULL DEFAULT 'VACANT', -- VACANT, OCCUPIED, MAINTENANCE, RESERVED, NOTICE_PERIOD
    description TEXT,
    current_tenant_id BIGINT,
    current_lease_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT,
    updated_by BIGINT,
    is_deleted BOOLEAN DEFAULT FALSE,
    version BIGINT DEFAULT 0,
    UNIQUE(property_id, unit_number)
);
CREATE INDEX idx_units_org ON units(org_id);
CREATE INDEX idx_units_property ON units(property_id);
CREATE INDEX idx_units_status ON units(status) WHERE is_deleted=false;
CREATE INDEX idx_units_rent ON units(rent_amount);

CREATE TABLE unit_amenities (
    unit_id BIGINT NOT NULL REFERENCES units(id) ON DELETE CASCADE,
    amenity_id BIGINT NOT NULL REFERENCES amenities(id) ON DELETE CASCADE,
    PRIMARY KEY (unit_id, amenity_id)
);

-- CRM
CREATE TABLE crm_leads (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    org_id BIGINT NOT NULL REFERENCES organizations(id),
    property_id BIGINT REFERENCES properties(id),
    unit_id BIGINT REFERENCES units(id),
    interested_unit_type VARCHAR(50),
    customer_name VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(255),
    source VARCHAR(50) DEFAULT 'WEBSITE', -- WALKIN, WEBSITE, REFERRAL, VOICE_AGENT, WHATSAPP, 99ACRES
    status VARCHAR(50) DEFAULT 'NEW', -- NEW, CONTACTED, VISIT_SCHEDULED, VISITED, NEGOTIATION, CONVERTED, LOST, JUNK
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    budget_min DECIMAL(12,2),
    budget_max DECIMAL(12,2),
    configuration VARCHAR(50), -- 1BHK, 2BHK
    timeline VARCHAR(100),
    purpose VARCHAR(100),
    assigned_to_staff_id BIGINT REFERENCES app_users(id),
    notes TEXT,
    conversation_summary TEXT,
    lost_reason VARCHAR(255),
    next_followup_at TIMESTAMP,
    ai_score DECIMAL(3,2),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT,
    is_deleted BOOLEAN DEFAULT FALSE
);
CREATE INDEX idx_crm_leads_org ON crm_leads(org_id);
CREATE INDEX idx_crm_leads_status ON crm_leads(status);
CREATE INDEX idx_crm_leads_phone ON crm_leads(phone);
CREATE INDEX idx_crm_leads_property ON crm_leads(property_id);

CREATE TABLE lead_visits (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    org_id BIGINT NOT NULL REFERENCES organizations(id),
    lead_id BIGINT NOT NULL REFERENCES crm_leads(id) ON DELETE CASCADE,
    property_id BIGINT NOT NULL REFERENCES properties(id),
    unit_id BIGINT REFERENCES units(id),
    scheduled_at TIMESTAMP NOT NULL,
    visited_at TIMESTAMP,
    status VARCHAR(30) DEFAULT 'SCHEDULED', -- SCHEDULED, COMPLETED, CANCELLED, NO_SHOW
    notes TEXT,
    feedback TEXT,
    staff_id BIGINT REFERENCES app_users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT
);
CREATE INDEX idx_visits_lead ON lead_visits(lead_id);
CREATE INDEX idx_visits_scheduled ON lead_visits(scheduled_at);

CREATE TABLE waitlist_entries (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    org_id BIGINT NOT NULL REFERENCES organizations(id),
    property_id BIGINT NOT NULL REFERENCES properties(id),
    unit_type VARCHAR(50) NOT NULL,
    lead_id BIGINT NOT NULL REFERENCES crm_leads(id),
    position INT,
    status VARCHAR(30) DEFAULT 'WAITING', -- WAITING, OFFERED, ACCEPTED, EXPIRED, CANCELLED
    priority_score INT DEFAULT 0,
    desired_move_in DATE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_waitlist_property ON waitlist_entries(property_id, status);

-- =========================================================
-- PILLAR 2: FINANCIAL
-- =========================================================

CREATE TABLE late_fee_rules (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    org_id BIGINT NOT NULL REFERENCES organizations(id),
    property_id BIGINT REFERENCES properties(id), -- NULL = org-wide
    name VARCHAR(255) NOT NULL,
    fee_type VARCHAR(30) NOT NULL, -- FIXED, PERCENTAGE_PER_DAY, SLAB
    amount_value DECIMAL(12,2),
    percentage_rate DECIMAL(5,2),
    grace_period_days INT DEFAULT 3,
    max_cap_amount DECIMAL(12,2),
    compounding BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE invoices (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    org_id BIGINT NOT NULL REFERENCES organizations(id),
    property_id BIGINT NOT NULL REFERENCES properties(id),
    unit_id BIGINT NOT NULL REFERENCES units(id),
    tenant_id BIGINT NOT NULL, -- FK TenantProfile later
    lease_id BIGINT, -- FK LeaseAgreement
    invoice_number VARCHAR(100) NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL, -- RENT, UTILITY, MAINTENANCE, SECURITY_DEPOSIT, OTHER
    billing_period_start DATE,
    billing_period_end DATE,
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL DEFAULT 0,
    tax_amount DECIMAL(12,2) DEFAULT 0,
    late_fee_amount DECIMAL(12,2) DEFAULT 0,
    discount_amount DECIMAL(12,2) DEFAULT 0,
    total_amount DECIMAL(12,2) NOT NULL,
    amount_paid DECIMAL(12,2) DEFAULT 0,
    balance_due DECIMAL(12,2) GENERATED ALWAYS AS (total_amount - amount_paid) STORED,
    status VARCHAR(30) DEFAULT 'DRAFT', -- DRAFT, ISSUED, PAID, PARTIALLY_PAID, OVERDUE, CANCELLED, VOID
    notes TEXT,
    pdf_s3_key VARCHAR(500),
    auto_generated BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT,
    is_deleted BOOLEAN DEFAULT FALSE
);
CREATE INDEX idx_invoices_org ON invoices(org_id);
CREATE INDEX idx_invoices_tenant ON invoices(tenant_id);
CREATE INDEX idx_invoices_due_status ON invoices(due_date, status);
CREATE INDEX idx_invoices_period ON invoices(billing_period_start, billing_period_end);

CREATE TABLE invoice_line_items (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    description VARCHAR(500) NOT NULL,
    quantity DECIMAL(10,2) DEFAULT 1,
    unit_price DECIMAL(12,2) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    type VARCHAR(50) -- RENT, BASE, UTILITY, LATE_FEE, DAMAGES, CREDIT
);

CREATE TABLE utility_types (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    org_id BIGINT NOT NULL REFERENCES organizations(id),
    name VARCHAR(100) NOT NULL, -- ELECTRICITY, WATER, GAS, INTERNET
    unit_label VARCHAR(50), -- kWh, KL, GB
    default_rate DECIMAL(10,4),
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(org_id, name)
);

CREATE TABLE utility_meters (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    org_id BIGINT NOT NULL REFERENCES organizations(id),
    property_id BIGINT NOT NULL REFERENCES properties(id),
    unit_id BIGINT REFERENCES units(id), -- NULL = master meter
    utility_type_id BIGINT NOT NULL REFERENCES utility_types(id),
    meter_number VARCHAR(100) NOT NULL UNIQUE,
    is_shared BOOLEAN DEFAULT FALSE,
    location VARCHAR(255),
    total_units_sharing INT DEFAULT 1,
    ratio_config JSONB, -- {"type":"EQUAL|RATIO|SUBMETER","ratios":{"101":0.3}}
    status VARCHAR(30) DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_meters_property ON utility_meters(property_id);
CREATE INDEX idx_meters_unit ON utility_meters(unit_id);

CREATE TABLE utility_bills (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    org_id BIGINT NOT NULL REFERENCES organizations(id),
    property_id BIGINT NOT NULL REFERENCES properties(id),
    utility_type_id BIGINT NOT NULL REFERENCES utility_types(id),
    meter_id BIGINT REFERENCES utility_meters(id), -- building meter if null unit reading
    billing_month DATE NOT NULL, -- YYYY-MM-01
    total_amount DECIMAL(12,2) NOT NULL,
    total_units_consumed DECIMAL(12,2),
    due_date DATE,
    provider_name VARCHAR(255),
    bill_document_s3_key VARCHAR(500),
    status VARCHAR(30) DEFAULT 'PENDING', -- PENDING, SPLIT, PAID
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(property_id, utility_type_id, billing_month, provider_name)
);
CREATE INDEX idx_bills_month ON utility_bills(billing_month);

CREATE TABLE utility_readings (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    meter_id BIGINT NOT NULL REFERENCES utility_meters(id),
    reading_date DATE NOT NULL,
    previous_reading DECIMAL(12,2) DEFAULT 0,
    current_reading DECIMAL(12,2) NOT NULL,
    units_consumed DECIMAL(12,2) GENERATED ALWAYS AS (current_reading - previous_reading) STORED,
    rate_per_unit DECIMAL(10,4) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    recorded_by_user_id BIGINT REFERENCES app_users(id),
    photo_s3_key VARCHAR(500),
    source VARCHAR(20) DEFAULT 'MANUAL', -- MANUAL, IOT
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_readings_meter_date ON utility_readings(meter_id, reading_date DESC);

CREATE TABLE utility_bill_splits (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    utility_bill_id BIGINT NOT NULL REFERENCES utility_bills(id) ON DELETE CASCADE,
    invoice_id BIGINT REFERENCES invoices(id),
    tenant_id BIGINT NOT NULL,
    unit_id BIGINT NOT NULL REFERENCES units(id),
    share_ratio DECIMAL(5,4) NOT NULL, -- 0.25 = 25%
    units_allocated DECIMAL(12,2),
    amount_share DECIMAL(12,2) NOT NULL,
    calculation_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_splits_bill ON utility_bill_splits(utility_bill_id);
CREATE INDEX idx_splits_tenant ON utility_bill_splits(tenant_id);

CREATE TABLE security_deposits (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    org_id BIGINT NOT NULL REFERENCES organizations(id),
    lease_id BIGINT NOT NULL UNIQUE,
    tenant_id BIGINT NOT NULL,
    unit_id BIGINT NOT NULL REFERENCES units(id),
    total_deposited DECIMAL(12,2) NOT NULL,
    currency VARCHAR(10) DEFAULT 'INR',
    status VARCHAR(30) DEFAULT 'HELD', -- HELD, PARTIALLY_REFUNDED, REFUNDED, FORFEITED
    held_in_account VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE security_deposit_ledger (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    deposit_id BIGINT NOT NULL REFERENCES security_deposits(id) ON DELETE CASCADE,
    transaction_type VARCHAR(30) NOT NULL, -- DEPOSIT, DEDUCTION, REFUND, ADJUSTMENT, FORFEITURE
    description TEXT,
    amount DECIMAL(12,2) NOT NULL, -- +deposit, -deduction
    balance_after DECIMAL(12,2) NOT NULL,
    reference_type VARCHAR(100), -- CONDITION_REPORT, TICKET, MANUAL
    reference_id BIGINT,
    created_by_user_id BIGINT REFERENCES app_users(id),
    receipt_s3_key VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_ledger_deposit ON security_deposit_ledger(deposit_id, created_at);

CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    org_id BIGINT NOT NULL REFERENCES organizations(id),
    property_id BIGINT REFERENCES properties(id),
    unit_id BIGINT REFERENCES units(id),
    type VARCHAR(20) NOT NULL, -- INCOME, EXPENSE
    category VARCHAR(100) NOT NULL, -- RENT, DEPOSIT_REFUND, UTILITY_COLLECTION, MAINTENANCE_VENDOR, VENDOR_PAYOUT, TAX, OTHER
    amount DECIMAL(12,2) NOT NULL,
    date DATE NOT NULL,
    description TEXT,
    payment_method VARCHAR(50), -- CASH, UPI, BANK_TRANSFER, CHEQUE, ONLINE
    invoice_id BIGINT REFERENCES invoices(id),
    vendor_payout_id BIGINT,
    ledger_reference_type VARCHAR(100),
    ledger_reference_id BIGINT,
    receipt_s3_key VARCHAR(500),
    created_by BIGINT REFERENCES app_users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_trans_org_date ON transactions(org_id, date DESC);
CREATE INDEX idx_trans_type ON transactions(type, category);

CREATE TABLE tax_report_snapshots (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    org_id BIGINT NOT NULL REFERENCES organizations(id),
    financial_year VARCHAR(20) NOT NULL, -- 2025-26
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_income DECIMAL(14,2) DEFAULT 0,
    total_expense DECIMAL(14,2) DEFAULT 0,
    net_profit DECIMAL(14,2) GENERATED ALWAYS AS (total_income - total_expense) STORED,
    total_tds DECIMAL(12,2) DEFAULT 0,
    total_gst DECIMAL(12,2) DEFAULT 0,
    report_json JSONB,
    report_pdf_s3_key VARCHAR(500),
    generated_at TIMESTAMP DEFAULT NOW(),
    generated_by BIGINT REFERENCES app_users(id),
    UNIQUE(org_id, financial_year)
);

-- =========================================================
-- PILLAR 3: TENANT LIFECYCLE
-- =========================================================

CREATE TABLE tenant_profiles (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    org_id BIGINT NOT NULL REFERENCES organizations(id),
    user_id BIGINT REFERENCES app_users(id),
    property_id BIGINT REFERENCES properties(id),
    unit_id BIGINT REFERENCES units(id),
    tenancy_type VARCHAR(20) DEFAULT 'PRIMARY', -- PRIMARY, CO_TENANT
    employer_name VARCHAR(255),
    occupation VARCHAR(100),
    monthly_income DECIMAL(12,2),
    emergency_contact_name VARCHAR(255),
    emergency_contact_phone VARCHAR(20),
    move_in_date DATE,
    expected_move_out_date DATE,
    actual_move_out_date DATE,
    status VARCHAR(30) DEFAULT 'PROSPECT', -- PROSPECT, ACTIVE, NOTICE_PERIOD, MOVED_OUT, BLACKLISTED
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE
);
CREATE INDEX idx_tenant_org ON tenant_profiles(org_id);
CREATE INDEX idx_tenant_unit ON tenant_profiles(unit_id);
CREATE INDEX idx_tenant_status ON tenant_profiles(status);

CREATE TABLE kyc_documents (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    org_id BIGINT NOT NULL REFERENCES organizations(id),
    tenant_id BIGINT NOT NULL REFERENCES tenant_profiles(id) ON DELETE CASCADE,
    document_type VARCHAR(50) NOT NULL, -- AADHAAR, PAN, PASSPORT, DRIVING_LICENSE, VOTER_ID, PHOTO, SALARY_SLIP
    document_number VARCHAR(255), -- encrypted at app layer
    s3_key VARCHAR(500),
    front_s3_key VARCHAR(500),
    back_s3_key VARCHAR(500),
    verification_status VARCHAR(30) DEFAULT 'PENDING', -- PENDING, VERIFIED, REJECTED, EXPIRED
    verified_by_user_id BIGINT REFERENCES app_users(id),
    verified_at TIMESTAMP,
    rejection_reason TEXT,
    expiry_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_kyc_tenant ON kyc_documents(tenant_id);

CREATE TABLE lease_agreements (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    org_id BIGINT NOT NULL REFERENCES organizations(id),
    property_id BIGINT NOT NULL REFERENCES properties(id),
    unit_id BIGINT NOT NULL REFERENCES units(id),
    tenant_id BIGINT NOT NULL REFERENCES tenant_profiles(id),
    lease_number VARCHAR(100) NOT NULL UNIQUE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    rent_amount DECIMAL(12,2) NOT NULL,
    deposit_amount DECIMAL(12,2) NOT NULL,
    rent_due_day INT NOT NULL DEFAULT 5 CHECK (rent_due_day BETWEEN 1 AND 28),
    notice_period_days INT DEFAULT 30,
    lock_in_period_months INT DEFAULT 6,
    escalation_percent DECIMAL(5,2) DEFAULT 0,
    status VARCHAR(30) DEFAULT 'DRAFT', -- DRAFT, PENDING_SIGNATURE, ACTIVE, EXPIRED, TERMINATED, RENEWED, CANCELLED
    terms TEXT,
    document_template_id BIGINT,
    final_pdf_s3_key VARCHAR(500),
    version INT DEFAULT 1,
    parent_lease_id BIGINT REFERENCES lease_agreements(id),
    termination_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT,
    is_deleted BOOLEAN DEFAULT FALSE
);
CREATE INDEX idx_lease_org ON lease_agreements(org_id);
CREATE INDEX idx_lease_unit ON lease_agreements(unit_id);
CREATE INDEX idx_lease_tenant ON lease_agreements(tenant_id);
CREATE INDEX idx_lease_end_date ON lease_agreements(end_date) WHERE status='ACTIVE';

CREATE TABLE esign_trackings (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    lease_id BIGINT NOT NULL REFERENCES lease_agreements(id) ON DELETE CASCADE,
    signer_user_id BIGINT NOT NULL REFERENCES app_users(id),
    signer_role VARCHAR(30) NOT NULL, -- TENANT, OWNER, MANAGER, WITNESS
    status VARCHAR(30) DEFAULT 'PENDING', -- PENDING, SENT, VIEWED, SIGNED, DECLINED, EXPIRED
    signature_order INT NOT NULL DEFAULT 1,
    signature_data_s3_key VARCHAR(500),
    signed_at TIMESTAMP,
    ip_address VARCHAR(100),
    user_agent TEXT,
    otp_verified BOOLEAN DEFAULT FALSE,
    otp_hash VARCHAR(255),
    expiry_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_esign_lease ON esign_trackings(lease_id);

CREATE TABLE checklist_templates (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    org_id BIGINT NOT NULL REFERENCES organizations(id),
    type VARCHAR(30) NOT NULL, -- MOVE_IN, MOVE_OUT, PERIODIC
    name VARCHAR(255) NOT NULL,
    description TEXT,
    items JSONB NOT NULL, -- [{"key":"wall_paint","label":"Wall Paint","type":"CONDITION","required":true}]
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE unit_condition_reports (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    org_id BIGINT NOT NULL REFERENCES organizations(id),
    lease_id BIGINT NOT NULL REFERENCES lease_agreements(id),
    unit_id BIGINT NOT NULL REFERENCES units(id),
    tenant_id BIGINT NOT NULL REFERENCES tenant_profiles(id),
    type VARCHAR(30) NOT NULL, -- MOVE_IN, MOVE_OUT, PERIODIC
    template_id BIGINT REFERENCES checklist_templates(id),
    inspected_by_user_id BIGINT REFERENCES app_users(id),
    inspected_at TIMESTAMP NOT NULL DEFAULT NOW(),
    overall_condition VARCHAR(30), -- EXCELLENT, GOOD, FAIR, POOR, DAMAGED
    notes TEXT,
    status VARCHAR(30) DEFAULT 'DRAFT', -- DRAFT, COMPLETED, DISPUTED, ADJUSTED
    pdf_s3_key VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_condition_lease ON unit_condition_reports(lease_id);
CREATE INDEX idx_condition_unit ON unit_condition_reports(unit_id);

CREATE TABLE condition_report_items (
    id BIGSERIAL PRIMARY KEY,
    report_id BIGINT NOT NULL REFERENCES unit_condition_reports(id) ON DELETE CASCADE,
    area VARCHAR(100) NOT NULL, -- LIVING_ROOM, BEDROOM, KITCHEN, BATHROOM, BALCONY, ELECTRICAL, PLUMBING, EXTERIOR
    item_name VARCHAR(255) NOT NULL,
    condition VARCHAR(30) NOT NULL, -- EXCELLENT, GOOD, FAIR, DAMAGED, MISSING
    description TEXT,
    estimated_repair_cost DECIMAL(12,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE condition_photos (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    report_item_id BIGINT REFERENCES condition_report_items(id) ON DELETE CASCADE,
    report_id BIGINT REFERENCES unit_condition_reports(id) ON DELETE CASCADE,
    s3_key VARCHAR(500) NOT NULL,
    caption VARCHAR(500),
    taken_at TIMESTAMP DEFAULT NOW(),
    metadata JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_cond_photos_report ON condition_photos(report_id);
CREATE INDEX idx_cond_photos_item ON condition_photos(report_item_id);

-- =========================================================
-- PILLAR 4: MAINTENANCE & VENDOR BIDDING
-- =========================================================

CREATE TABLE vendor_profiles (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    org_id BIGINT NOT NULL REFERENCES organizations(id),
    user_id BIGINT NOT NULL REFERENCES app_users(id),
    company_name VARCHAR(255) NOT NULL,
    specialization VARCHAR(100) NOT NULL, -- PLUMBING, ELECTRICAL, CARPENTRY, PAINTING, CLEANING, SECURITY, HVAC, PEST_CONTROL, GENERAL, APPLIANCE
    years_experience INT,
    rating DECIMAL(3,2) DEFAULT 0,
    total_jobs_completed INT DEFAULT 0,
    is_verified BOOLEAN DEFAULT FALSE,
    verification_docs_s3 VARCHAR(500),
    bank_account_encrypted VARCHAR(500),
    bank_ifsc VARCHAR(20),
    status VARCHAR(30) DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, BLACKLISTED
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE
);
CREATE INDEX idx_vendor_org ON vendor_profiles(org_id);
CREATE INDEX idx_vendor_spec ON vendor_profiles(specialization);

CREATE TABLE maintenance_tickets (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    org_id BIGINT NOT NULL REFERENCES organizations(id),
    property_id BIGINT NOT NULL REFERENCES properties(id),
    unit_id BIGINT REFERENCES units(id),
    tenant_id BIGINT REFERENCES tenant_profiles(id),
    raised_by_user_id BIGINT NOT NULL REFERENCES app_users(id),
    category VARCHAR(100) NOT NULL,
    priority VARCHAR(20) DEFAULT 'MEDIUM', -- LOW, MEDIUM, HIGH, URGENT
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(30) DEFAULT 'OPEN', -- OPEN, BROADCASTED, BIDDING, ASSIGNED, IN_PROGRESS, PENDING_PARTS, COMPLETED, CANCELLED, CLOSED
    assigned_vendor_id BIGINT REFERENCES vendor_profiles(id),
    assigned_bid_id BIGINT,
    estimated_cost DECIMAL(12,2),
    actual_cost DECIMAL(12,2),
    scheduled_at TIMESTAMP,
    completed_at TIMESTAMP,
    completion_notes TEXT,
    rating_by_tenant INT CHECK (rating_by_tenant BETWEEN 1 AND 5),
    feedback TEXT,
    sla_due_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE
);
CREATE INDEX idx_ticket_org_status ON maintenance_tickets(org_id, status);
CREATE INDEX idx_ticket_property ON maintenance_tickets(property_id);
CREATE INDEX idx_ticket_priority ON maintenance_tickets(priority) WHERE status NOT IN ('COMPLETED','CLOSED','CANCELLED');

CREATE TABLE ticket_media (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    ticket_id BIGINT NOT NULL REFERENCES maintenance_tickets(id) ON DELETE CASCADE,
    s3_key VARCHAR(500) NOT NULL,
    media_type VARCHAR(20) NOT NULL, -- IMAGE, VIDEO, DOCUMENT
    file_size BIGINT,
    uploaded_by BIGINT REFERENCES app_users(id),
    caption VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE vendor_bids (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    org_id BIGINT NOT NULL REFERENCES organizations(id),
    ticket_id BIGINT NOT NULL REFERENCES maintenance_tickets(id) ON DELETE CASCADE,
    vendor_id BIGINT NOT NULL REFERENCES vendor_profiles(id) ON DELETE CASCADE,
    bid_amount DECIMAL(12,2) NOT NULL,
    estimated_days INT NOT NULL,
    proposal TEXT,
    status VARCHAR(30) DEFAULT 'SUBMITTED', -- SUBMITTED, APPROVED, REJECTED, WITHDRAWN, EXPIRED
    submitted_at TIMESTAMP NOT NULL DEFAULT NOW(),
    approved_at TIMESTAMP,
    rejection_reason TEXT,
    includes_material BOOLEAN DEFAULT FALSE,
    warranty_days INT DEFAULT 0,
    UNIQUE(ticket_id, vendor_id)
);
CREATE INDEX idx_bids_ticket ON vendor_bids(ticket_id, status);
CREATE INDEX idx_bids_vendor ON vendor_bids(vendor_id);

CREATE TABLE work_orders (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    org_id BIGINT NOT NULL REFERENCES organizations(id),
    ticket_id BIGINT NOT NULL UNIQUE REFERENCES maintenance_tickets(id),
    vendor_id BIGINT NOT NULL REFERENCES vendor_profiles(id),
    bid_id BIGINT NOT NULL REFERENCES vendor_bids(id),
    assigned_by_user_id BIGINT NOT NULL REFERENCES app_users(id),
    status VARCHAR(30) DEFAULT 'CREATED', -- CREATED, ACCEPTED, IN_PROGRESS, PENDING_APPROVAL, COMPLETED, CANCELLED
    scheduled_date DATE,
    start_date TIMESTAMP,
    completed_date TIMESTAMP,
    completion_notes TEXT,
    checklist_completed BOOLEAN DEFAULT FALSE,
    otp_verified_for_completion BOOLEAN DEFAULT FALSE,
    invoice_s3_key VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE vendor_payouts (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    org_id BIGINT NOT NULL REFERENCES organizations(id),
    work_order_id BIGINT NOT NULL REFERENCES work_orders(id),
    ticket_id BIGINT NOT NULL REFERENCES maintenance_tickets(id),
    vendor_id BIGINT NOT NULL REFERENCES vendor_profiles(id),
    amount DECIMAL(12,2) NOT NULL,
    tds_deducted DECIMAL(12,2) DEFAULT 0,
    net_payable DECIMAL(12,2) NOT NULL,
    status VARCHAR(30) DEFAULT 'PENDING', -- PENDING, APPROVED, PAID, FAILED, ON_HOLD
    payment_method VARCHAR(30), -- UPI, BANK_TRANSFER, CASH
    utr_number VARCHAR(100),
    transaction_id BIGINT REFERENCES transactions(id),
    paid_at TIMESTAMP,
    paid_by_user_id BIGINT REFERENCES app_users(id),
    notes TEXT,
    invoice_s3_key VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_payout_vendor ON vendor_payouts(vendor_id, status);
CREATE INDEX idx_payout_status ON vendor_payouts(status);

-- =========================================================
-- PILLAR 5: COMMUNICATION & AUTOMATION
-- =========================================================

CREATE TABLE notification_templates (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    org_id BIGINT NOT NULL REFERENCES organizations(id),
    name VARCHAR(255) NOT NULL,
    code VARCHAR(100) NOT NULL, -- RENT_REMINDER_3D, LEASE_EXPIRY_60D etc
    channel VARCHAR(20) NOT NULL, -- EMAIL, SMS, WHATSAPP, PUSH, IN_APP
    subject VARCHAR(500), -- templated with {{var}}
    body TEXT NOT NULL, -- template
    body_whatsapp_template_id VARCHAR(100),
    variables JSONB, -- ["tenant_name","rent_amount"]
    category VARCHAR(50), -- RENT, LEASE, MAINTENANCE, ANNOUNCEMENT, GENERAL
    is_active BOOLEAN DEFAULT TRUE,
    locale VARCHAR(20) DEFAULT 'en', -- en, hi, en_HI
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(org_id, code),
    UNIQUE(org_id, name)
);

CREATE TABLE notification_logs (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    org_id BIGINT NOT NULL REFERENCES organizations(id),
    template_id BIGINT REFERENCES notification_templates(id),
    channel VARCHAR(20) NOT NULL,
    recipient_type VARCHAR(30) NOT NULL, -- TENANT, VENDOR, STAFF, LEAD, USER
    recipient_id BIGINT REFERENCES app_users(id),
    recipient_contact VARCHAR(255) NOT NULL, -- phone/email
    subject_rendered VARCHAR(500),
    body_rendered TEXT,
    status VARCHAR(20) DEFAULT 'QUEUED', -- QUEUED, SENT, FAILED, DELIVERED, READ, BOUNCED
    provider_message_id VARCHAR(255),
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    failure_reason TEXT,
    related_entity_type VARCHAR(100), -- INVOICE, LEASE, TICKET
    related_entity_id BIGINT,
    retry_count INT DEFAULT 0,
    next_retry_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_notif_org ON notification_logs(org_id, created_at DESC);
CREATE INDEX idx_notif_status ON notification_logs(status);
CREATE INDEX idx_notif_recipient ON notification_logs(recipient_contact);
CREATE INDEX idx_notif_related ON notification_logs(related_entity_type, related_entity_id);

CREATE TABLE broadcast_announcements (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    org_id BIGINT NOT NULL REFERENCES organizations(id),
    property_id BIGINT REFERENCES properties(id), -- NULL = org-wide
    unit_id BIGINT REFERENCES units(id),
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    priority VARCHAR(20) DEFAULT 'MEDIUM', -- LOW, MEDIUM, HIGH, CRITICAL
    category VARCHAR(50), -- WATER, ELECTRICITY, MAINTENANCE, EVENT, SAFETY, GENERAL
    created_by_user_id BIGINT NOT NULL REFERENCES app_users(id),
    expires_at TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    attachment_s3_key VARCHAR(500),
    action_required BOOLEAN DEFAULT FALSE,
    action_label VARCHAR(100),
    send_push BOOLEAN DEFAULT TRUE,
    send_sms BOOLEAN DEFAULT FALSE,
    send_whatsapp BOOLEAN DEFAULT TRUE,
    send_email BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_broadcast_org ON broadcast_announcements(org_id, is_active);
CREATE INDEX idx_broadcast_property ON broadcast_announcements(property_id);

CREATE TABLE announcement_recipients (
    id BIGSERIAL PRIMARY KEY,
    announcement_id BIGINT NOT NULL REFERENCES broadcast_announcements(id) ON DELETE CASCADE,
    recipient_user_id BIGINT NOT NULL REFERENCES app_users(id),
    read_at TIMESTAMP,
    status VARCHAR(20) DEFAULT 'SENT', -- SENT, DELIVERED, READ, ARCHIVED
    delivered_via JSONB, -- ["push","sms"]
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(announcement_id, recipient_user_id)
);

CREATE TABLE automation_rules (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    org_id BIGINT NOT NULL REFERENCES organizations(id),
    name VARCHAR(255) NOT NULL,
    code VARCHAR(100) NOT NULL, -- AUTO_RENT_DUE_T_MINUS_3 etc
    description TEXT,
    trigger_event VARCHAR(100) NOT NULL, -- RENT_DUE_7D, RENT_DUE_3D, RENT_OVERDUE_1D, RENT_OVERDUE_5D, LEASE_EXPIRY_60D, LEASE_EXPIRY_30D, LEASE_EXPIRED, TICKET_CREATED, TICKET_COMPLETED, LEAD_NO_FOLLOWUP_2D, UTILITY_BILL_GENERATED
    conditions JSONB, -- {"property_id_in":[],"unit_status":"occupied"}
    template_id BIGINT REFERENCES notification_templates(id),
    is_active BOOLEAN DEFAULT TRUE,
    cooldown_hours INT DEFAULT 24,
    last_triggered_at TIMESTAMP,
    execution_count BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(org_id, code)
);

CREATE TABLE automation_execution_logs (
    id BIGSERIAL PRIMARY KEY,
    rule_id BIGINT NOT NULL REFERENCES automation_rules(id),
    org_id BIGINT NOT NULL REFERENCES organizations(id),
    triggered_at TIMESTAMP NOT NULL DEFAULT NOW(),
    status VARCHAR(20) DEFAULT 'SUCCESS', -- SUCCESS, PARTIAL, FAILED
    context JSONB, -- {"invoice_id":1}
    affected_recipients_count INT DEFAULT 0,
    details TEXT,
    error TEXT
);
CREATE INDEX idx_auto_exec_rule ON automation_execution_logs(rule_id, triggered_at DESC);

-- =========================================================
-- PILLAR 6: IOT & SMART TECH
-- =========================================================

CREATE TABLE smart_lock_devices (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    org_id BIGINT NOT NULL REFERENCES organizations(id),
    property_id BIGINT NOT NULL REFERENCES properties(id),
    unit_id BIGINT REFERENCES units(id),
    device_name VARCHAR(255) NOT NULL,
    provider VARCHAR(50) NOT NULL, -- TTLOCK, AUGUST, YALE, SMARTTHINGS, AQARA, CUSTOM
    device_id_external VARCHAR(255) NOT NULL UNIQUE, -- external ID
    mac_address VARCHAR(100),
    api_key_encrypted VARCHAR(500),
    api_secret_encrypted VARCHAR(500),
    firmware_version VARCHAR(50),
    status VARCHAR(30) DEFAULT 'ACTIVE', -- ACTIVE, OFFLINE, MAINTENANCE, DECOMMISSIONED
    battery_level INT,
    signal_strength INT,
    last_seen_at TIMESTAMP,
    config_json JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE
);
CREATE INDEX idx_lock_property ON smart_lock_devices(property_id);
CREATE INDEX idx_lock_unit ON smart_lock_devices(unit_id);

CREATE TABLE access_pins (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    org_id BIGINT NOT NULL REFERENCES organizations(id),
    device_id BIGINT NOT NULL REFERENCES smart_lock_devices(id),
    generated_for_user_id BIGINT REFERENCES app_users(id),
    generated_for_type VARCHAR(50) NOT NULL, -- TENANT, VENDOR, PROSPECTIVE, STAFF, HOUSEKEEPING, EMERGENCY
    pin_code_encrypted VARCHAR(500) NOT NULL,
    pin_hash VARCHAR(255) NOT NULL, -- SHA256 for quick lookup
    label VARCHAR(255), -- "Vendor Plumbing Ticket#123"
    valid_from TIMESTAMP NOT NULL DEFAULT NOW(),
    valid_to TIMESTAMP NOT NULL,
    max_uses INT DEFAULT 1,
    used_count INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_by_user_id BIGINT REFERENCES app_users(id),
    revoked_at TIMESTAMP,
    revoke_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_pins_device_active ON access_pins(device_id, is_active) WHERE is_deleted=false OR is_deleted IS NULL;
CREATE INDEX idx_pins_valid_to ON access_pins(valid_to) WHERE is_active=true;

CREATE TABLE access_logs (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    org_id BIGINT NOT NULL REFERENCES organizations(id),
    device_id BIGINT NOT NULL REFERENCES smart_lock_devices(id),
    pin_id BIGINT REFERENCES access_pins(id),
    user_id BIGINT REFERENCES app_users(id),
    access_type VARCHAR(30) NOT NULL, -- PIN, FINGERPRINT, CARD, APP, MANUAL
    accessed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    success BOOLEAN NOT NULL,
    failure_reason VARCHAR(255),
    ip_address VARCHAR(100),
    location_latlon VARCHAR(100),
    provider_event_id VARCHAR(255),
    raw_payload JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_access_device_time ON access_logs(device_id, accessed_at DESC);
CREATE INDEX idx_access_user ON access_logs(user_id);

CREATE TABLE iot_webhook_configs (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    org_id BIGINT NOT NULL REFERENCES organizations(id),
    provider VARCHAR(50) NOT NULL,
    webhook_url VARCHAR(500) NOT NULL,
    secret_encrypted VARCHAR(500),
    events_subscribed JSONB, -- ["lock.unlocked","lock.locked","battery.low"]
    is_active BOOLEAN DEFAULT TRUE,
    last_received_at TIMESTAMP,
    failure_count INT DEFAULT 0,
    headers_json JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =========================================================
-- ADD FK CONSTRAINTS THAT WERE DEFERRED DUE TO ORDER
-- =========================================================

ALTER TABLE organizations ADD CONSTRAINT fk_org_owner FOREIGN KEY (owner_user_id) REFERENCES app_users(id) DEFERRABLE;
ALTER TABLE units ADD CONSTRAINT fk_units_current_tenant FOREIGN KEY (current_tenant_id) REFERENCES tenant_profiles(id);
ALTER TABLE units ADD CONSTRAINT fk_units_current_lease FOREIGN KEY (current_lease_id) REFERENCES lease_agreements(id);
ALTER TABLE transactions ADD CONSTRAINT fk_trans_vendor_payout FOREIGN KEY (vendor_payout_id) REFERENCES vendor_payouts(id);
ALTER TABLE maintenance_tickets ADD CONSTRAINT fk_ticket_assigned_bid FOREIGN KEY (assigned_bid_id) REFERENCES vendor_bids(id);
ALTER TABLE invoices ADD CONSTRAINT fk_invoice_tenant FOREIGN KEY (tenant_id) REFERENCES tenant_profiles(id);
ALTER TABLE invoices ADD CONSTRAINT fk_invoice_lease FOREIGN KEY (lease_id) REFERENCES lease_agreements(id);

-- =========================================================
-- SEED DATA FOR RBAC (System Permissions)
-- =========================================================

INSERT INTO permissions (name, description, category) VALUES
('PROPERTY_READ','View properties','PORTFOLIO'),
('PROPERTY_WRITE','Create/Edit properties','PORTFOLIO'),
('PROPERTY_DELETE','Delete properties','PORTFOLIO'),
('UNIT_MANAGE','Manage units','PORTFOLIO'),
('AMENITY_MANAGE','Manage amenities','PORTFOLIO'),
('LEAD_MANAGE','Manage CRM leads','CRM'),
('LEAD_VISIT_MANAGE','Manage visits & waitlist','CRM'),
('TENANT_READ','View tenants','TENANT'),
('TENANT_WRITE','Create/Edit tenants','TENANT'),
('KYC_VERIFY','Verify KYC docs','TENANT'),
('LEASE_MANAGE','Manage leases','TENANT'),
('LEASE_ESIGN','Manage e-sign','TENANT'),
('INVOICE_MANAGE','Manage invoices','FINANCIAL'),
('INVOICE_VIEW','View invoices','FINANCIAL'),
('LATE_FEE_MANAGE','Manage late fee rules','FINANCIAL'),
('UTILITY_MANAGE','Manage utility meters & bills','FINANCIAL'),
('DEPOSIT_MANAGE','Manage security deposits','FINANCIAL'),
('TRANSACTION_MANAGE','Manage transactions','FINANCIAL'),
('REPORT_VIEW','View tax/account reports','FINANCIAL'),
('TICKET_CREATE','Create tickets (tenant)','MAINTENANCE'),
('TICKET_MANAGE','Manage all tickets','MAINTENANCE'),
('VENDOR_MANAGE','Manage vendors','MAINTENANCE'),
('VENDOR_BID','Submit/view bids (vendor)','MAINTENANCE'),
('VENDOR_PAYOUT_MANAGE','Manage payouts','MAINTENANCE'),
('COMMUNICATION_SEND','Send notifications/broadcasts','COMMUNICATION'),
('COMMUNICATION_TEMPLATE_MANAGE','Manage templates & automation','COMMUNICATION'),
('IOT_MANAGE','Manage smart locks & pins','IOT'),
('IOT_PIN_GENERATE','Generate temporary PINs','IOT'),
('USER_MANAGE','Manage users & roles','ADMIN'),
('SETTINGS_MANAGE','Manage org settings','ADMIN'),
('ORG_MANAGE','Manage organizations (super admin)','ADMIN')
ON CONFLICT (name) DO NOTHING;

-- System roles seed (org_id NULL)
INSERT INTO roles (org_id, name, description, hierarchy_level, is_system) VALUES
(NULL,'SUPER_ADMIN','Platform super admin - all access',100,true),
(NULL,'PROPERTY_MANAGER','Property owner/tycoon - org admin',80,true),
(NULL,'ACCOUNTANT','Financial/accounting role',60,true),
(NULL,'STAFF','Property manager staff',50,true),
(NULL,'LEAD_AGENT','CRM agent handling leads',51,true),
(NULL,'TENANT','Tenant - limited self service',30,true),
(NULL,'VENDOR','Vendor - ticket bidding',20,true)
ON CONFLICT (org_id, name) DO NOTHING;

-- Example auto-increment sequences for invoice_number & lease_number to be generated in app layer, not DB sequence, but we can create helper.
-- These will be generated as INV-YYYY-XXXXX in service.

-- =========================================================
-- END OF PROP-OS DDL
-- =========================================================
