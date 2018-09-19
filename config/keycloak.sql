--
-- PostgreSQL database dump
--

-- Dumped from database version 10.3 (Debian 10.3-1.pgdg90+1)
-- Dumped by pg_dump version 10.4

-- Started on 2018-09-19 11:15:22 CEST

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 3826 (class 1262 OID 16384)
-- Name: keycloak; Type: DATABASE; Schema: -; Owner: postgres
--

-- CREATE DATABASE keycloak WITH TEMPLATE = template0 ENCODING = 'UTF8' LC_COLLATE = 'en_US.utf8' LC_CTYPE = 'en_US.utf8';


-- ALTER DATABASE keycloak OWNER TO postgres;

\connect keycloak

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 1 (class 3079 OID 12980)
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- TOC entry 3828 (class 0 OID 0)
-- Dependencies: 1
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET default_tablespace = '';

SET default_with_oids = false;

--
-- TOC entry 239 (class 1259 OID 17093)
-- Name: admin_event_entity; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.admin_event_entity (
    id character varying(36) NOT NULL,
    admin_event_time bigint,
    realm_id character varying(255),
    operation_type character varying(255),
    auth_realm_id character varying(255),
    auth_client_id character varying(255),
    auth_user_id character varying(255),
    ip_address character varying(255),
    resource_path character varying(2550),
    representation text,
    error character varying(255),
    resource_type character varying(64)
);


ALTER TABLE public.admin_event_entity OWNER TO keycloak;

--
-- TOC entry 268 (class 1259 OID 17792)
-- Name: associated_policy; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.associated_policy (
    policy_id character varying(36) NOT NULL,
    associated_policy_id character varying(36) NOT NULL
);


ALTER TABLE public.associated_policy OWNER TO keycloak;

--
-- TOC entry 242 (class 1259 OID 17111)
-- Name: authentication_execution; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.authentication_execution (
    id character varying(36) NOT NULL,
    alias character varying(255),
    authenticator character varying(36),
    realm_id character varying(36),
    flow_id character varying(36),
    requirement integer,
    priority integer,
    authenticator_flow boolean DEFAULT false NOT NULL,
    auth_flow_id character varying(36),
    auth_config character varying(36)
);


ALTER TABLE public.authentication_execution OWNER TO keycloak;

--
-- TOC entry 241 (class 1259 OID 17105)
-- Name: authentication_flow; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.authentication_flow (
    id character varying(36) NOT NULL,
    alias character varying(255),
    description character varying(255),
    realm_id character varying(36),
    provider_id character varying(36) DEFAULT 'basic-flow'::character varying NOT NULL,
    top_level boolean DEFAULT false NOT NULL,
    built_in boolean DEFAULT false NOT NULL
);


ALTER TABLE public.authentication_flow OWNER TO keycloak;

--
-- TOC entry 240 (class 1259 OID 17099)
-- Name: authenticator_config; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.authenticator_config (
    id character varying(36) NOT NULL,
    alias character varying(255),
    realm_id character varying(36)
);


ALTER TABLE public.authenticator_config OWNER TO keycloak;

--
-- TOC entry 243 (class 1259 OID 17116)
-- Name: authenticator_config_entry; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.authenticator_config_entry (
    authenticator_id character varying(36) NOT NULL,
    value text,
    name character varying(255) NOT NULL
);


ALTER TABLE public.authenticator_config_entry OWNER TO keycloak;

--
-- TOC entry 269 (class 1259 OID 17807)
-- Name: broker_link; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.broker_link (
    identity_provider character varying(255) NOT NULL,
    storage_provider_id character varying(255),
    realm_id character varying(36) NOT NULL,
    broker_user_id character varying(255),
    broker_username character varying(255),
    token text,
    user_id character varying(255) NOT NULL
);


ALTER TABLE public.broker_link OWNER TO keycloak;

--
-- TOC entry 199 (class 1259 OID 16400)
-- Name: client; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.client (
    id character varying(36) NOT NULL,
    enabled boolean DEFAULT false NOT NULL,
    full_scope_allowed boolean DEFAULT false NOT NULL,
    client_id character varying(255),
    not_before integer,
    public_client boolean DEFAULT false NOT NULL,
    secret character varying(255),
    base_url character varying(255),
    bearer_only boolean DEFAULT false NOT NULL,
    management_url character varying(255),
    surrogate_auth_required boolean DEFAULT false NOT NULL,
    realm_id character varying(36),
    protocol character varying(255),
    node_rereg_timeout integer DEFAULT 0,
    frontchannel_logout boolean DEFAULT false NOT NULL,
    consent_required boolean DEFAULT false NOT NULL,
    name character varying(255),
    service_accounts_enabled boolean DEFAULT false NOT NULL,
    client_authenticator_type character varying(255),
    root_url character varying(255),
    description character varying(255),
    registration_token character varying(255),
    standard_flow_enabled boolean DEFAULT true NOT NULL,
    implicit_flow_enabled boolean DEFAULT false NOT NULL,
    direct_access_grants_enabled boolean DEFAULT false NOT NULL
);


ALTER TABLE public.client OWNER TO keycloak;

--
-- TOC entry 223 (class 1259 OID 16774)
-- Name: client_attributes; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.client_attributes (
    client_id character varying(36) NOT NULL,
    value character varying(4000),
    name character varying(255) NOT NULL
);


ALTER TABLE public.client_attributes OWNER TO keycloak;

--
-- TOC entry 282 (class 1259 OID 18109)
-- Name: client_auth_flow_bindings; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.client_auth_flow_bindings (
    client_id character varying(36) NOT NULL,
    flow_id character varying(36),
    binding_name character varying(255) NOT NULL
);


ALTER TABLE public.client_auth_flow_bindings OWNER TO keycloak;

--
-- TOC entry 198 (class 1259 OID 16397)
-- Name: client_default_roles; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.client_default_roles (
    client_id character varying(36) NOT NULL,
    role_id character varying(36) NOT NULL
);


ALTER TABLE public.client_default_roles OWNER TO keycloak;

--
-- TOC entry 281 (class 1259 OID 17965)
-- Name: client_initial_access; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.client_initial_access (
    id character varying(36) NOT NULL,
    realm_id character varying(36) NOT NULL,
    "timestamp" integer,
    expiration integer,
    count integer,
    remaining_count integer
);


ALTER TABLE public.client_initial_access OWNER TO keycloak;

--
-- TOC entry 225 (class 1259 OID 16786)
-- Name: client_node_registrations; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.client_node_registrations (
    client_id character varying(36) NOT NULL,
    value integer,
    name character varying(255) NOT NULL
);


ALTER TABLE public.client_node_registrations OWNER TO keycloak;

--
-- TOC entry 257 (class 1259 OID 17566)
-- Name: client_scope; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.client_scope (
    id character varying(36) NOT NULL,
    name character varying(255),
    realm_id character varying(36),
    description character varying(255),
    protocol character varying(255)
);


ALTER TABLE public.client_scope OWNER TO keycloak;

--
-- TOC entry 258 (class 1259 OID 17581)
-- Name: client_scope_attributes; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.client_scope_attributes (
    scope_id character varying(36) NOT NULL,
    value character varying(2048),
    name character varying(255) NOT NULL
);


ALTER TABLE public.client_scope_attributes OWNER TO keycloak;

--
-- TOC entry 283 (class 1259 OID 18167)
-- Name: client_scope_client; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.client_scope_client (
    client_id character varying(36) NOT NULL,
    scope_id character varying(36) NOT NULL,
    default_scope boolean DEFAULT false NOT NULL
);


ALTER TABLE public.client_scope_client OWNER TO keycloak;

--
-- TOC entry 259 (class 1259 OID 17587)
-- Name: client_scope_role_mapping; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.client_scope_role_mapping (
    scope_id character varying(36) NOT NULL,
    role_id character varying(36) NOT NULL
);


ALTER TABLE public.client_scope_role_mapping OWNER TO keycloak;

--
-- TOC entry 200 (class 1259 OID 16412)
-- Name: client_session; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.client_session (
    id character varying(36) NOT NULL,
    client_id character varying(36),
    redirect_uri character varying(255),
    state character varying(255),
    "timestamp" integer,
    session_id character varying(36),
    auth_method character varying(255),
    realm_id character varying(255),
    auth_user_id character varying(36),
    current_action character varying(36)
);


ALTER TABLE public.client_session OWNER TO keycloak;

--
-- TOC entry 246 (class 1259 OID 17161)
-- Name: client_session_auth_status; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.client_session_auth_status (
    authenticator character varying(36) NOT NULL,
    status integer,
    client_session character varying(36) NOT NULL
);


ALTER TABLE public.client_session_auth_status OWNER TO keycloak;

--
-- TOC entry 224 (class 1259 OID 16780)
-- Name: client_session_note; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.client_session_note (
    name character varying(255) NOT NULL,
    value character varying(255),
    client_session character varying(36) NOT NULL
);


ALTER TABLE public.client_session_note OWNER TO keycloak;

--
-- TOC entry 238 (class 1259 OID 16999)
-- Name: client_session_prot_mapper; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.client_session_prot_mapper (
    protocol_mapper_id character varying(36) NOT NULL,
    client_session character varying(36) NOT NULL
);


ALTER TABLE public.client_session_prot_mapper OWNER TO keycloak;

--
-- TOC entry 201 (class 1259 OID 16418)
-- Name: client_session_role; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.client_session_role (
    role_id character varying(255) NOT NULL,
    client_session character varying(36) NOT NULL
);


ALTER TABLE public.client_session_role OWNER TO keycloak;

--
-- TOC entry 247 (class 1259 OID 17298)
-- Name: client_user_session_note; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.client_user_session_note (
    name character varying(255) NOT NULL,
    value character varying(2048),
    client_session character varying(36) NOT NULL
);


ALTER TABLE public.client_user_session_note OWNER TO keycloak;

--
-- TOC entry 277 (class 1259 OID 17857)
-- Name: component; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.component (
    id character varying(36) NOT NULL,
    name character varying(255),
    parent_id character varying(36),
    provider_id character varying(36),
    provider_type character varying(255),
    realm_id character varying(36),
    sub_type character varying(255)
);


ALTER TABLE public.component OWNER TO keycloak;

--
-- TOC entry 276 (class 1259 OID 17851)
-- Name: component_config; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.component_config (
    id character varying(36) NOT NULL,
    component_id character varying(36) NOT NULL,
    name character varying(255) NOT NULL,
    value character varying(4000)
);


ALTER TABLE public.component_config OWNER TO keycloak;

--
-- TOC entry 202 (class 1259 OID 16421)
-- Name: composite_role; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.composite_role (
    composite character varying(36) NOT NULL,
    child_role character varying(36) NOT NULL
);


ALTER TABLE public.composite_role OWNER TO keycloak;

--
-- TOC entry 203 (class 1259 OID 16424)
-- Name: credential; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.credential (
    id character varying(36) NOT NULL,
    device character varying(255),
    hash_iterations integer,
    salt bytea,
    type character varying(255),
    value character varying(4000),
    user_id character varying(36),
    created_date bigint,
    counter integer DEFAULT 0,
    digits integer DEFAULT 6,
    period integer DEFAULT 30,
    algorithm character varying(36) DEFAULT NULL::character varying
);


ALTER TABLE public.credential OWNER TO keycloak;

--
-- TOC entry 278 (class 1259 OID 17895)
-- Name: credential_attribute; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.credential_attribute (
    id character varying(36) NOT NULL,
    credential_id character varying(36) NOT NULL,
    name character varying(255) NOT NULL,
    value character varying(4000)
);


ALTER TABLE public.credential_attribute OWNER TO keycloak;

--
-- TOC entry 197 (class 1259 OID 16391)
-- Name: databasechangelog; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.databasechangelog (
    id character varying(255) NOT NULL,
    author character varying(255) NOT NULL,
    filename character varying(255) NOT NULL,
    dateexecuted timestamp without time zone NOT NULL,
    orderexecuted integer NOT NULL,
    exectype character varying(10) NOT NULL,
    md5sum character varying(35),
    description character varying(255),
    comments character varying(255),
    tag character varying(255),
    liquibase character varying(20),
    contexts character varying(255),
    labels character varying(255),
    deployment_id character varying(10)
);


ALTER TABLE public.databasechangelog OWNER TO keycloak;

--
-- TOC entry 196 (class 1259 OID 16386)
-- Name: databasechangeloglock; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.databasechangeloglock (
    id integer NOT NULL,
    locked boolean NOT NULL,
    lockgranted timestamp without time zone,
    lockedby character varying(255)
);


ALTER TABLE public.databasechangeloglock OWNER TO keycloak;

--
-- TOC entry 284 (class 1259 OID 18183)
-- Name: default_client_scope; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.default_client_scope (
    realm_id character varying(36) NOT NULL,
    scope_id character varying(36) NOT NULL,
    default_scope boolean DEFAULT false NOT NULL
);


ALTER TABLE public.default_client_scope OWNER TO keycloak;

--
-- TOC entry 204 (class 1259 OID 16430)
-- Name: event_entity; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.event_entity (
    id character varying(36) NOT NULL,
    client_id character varying(255),
    details_json character varying(2550),
    error character varying(255),
    ip_address character varying(255),
    realm_id character varying(255),
    session_id character varying(255),
    event_time bigint,
    type character varying(255),
    user_id character varying(255)
);


ALTER TABLE public.event_entity OWNER TO keycloak;

--
-- TOC entry 279 (class 1259 OID 17901)
-- Name: fed_credential_attribute; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.fed_credential_attribute (
    id character varying(36) NOT NULL,
    credential_id character varying(36) NOT NULL,
    name character varying(255) NOT NULL,
    value character varying(4000)
);


ALTER TABLE public.fed_credential_attribute OWNER TO keycloak;

--
-- TOC entry 270 (class 1259 OID 17813)
-- Name: fed_user_attribute; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.fed_user_attribute (
    id character varying(36) NOT NULL,
    name character varying(255) NOT NULL,
    user_id character varying(255) NOT NULL,
    realm_id character varying(36) NOT NULL,
    storage_provider_id character varying(36),
    value character varying(2024)
);


ALTER TABLE public.fed_user_attribute OWNER TO keycloak;

--
-- TOC entry 271 (class 1259 OID 17819)
-- Name: fed_user_consent; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.fed_user_consent (
    id character varying(36) NOT NULL,
    client_id character varying(36),
    user_id character varying(255) NOT NULL,
    realm_id character varying(36) NOT NULL,
    storage_provider_id character varying(36),
    created_date bigint,
    last_updated_date bigint,
    client_storage_provider character varying(36),
    external_client_id character varying(255)
);


ALTER TABLE public.fed_user_consent OWNER TO keycloak;

--
-- TOC entry 286 (class 1259 OID 18209)
-- Name: fed_user_consent_cl_scope; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.fed_user_consent_cl_scope (
    user_consent_id character varying(36) NOT NULL,
    scope_id character varying(36) NOT NULL
);


ALTER TABLE public.fed_user_consent_cl_scope OWNER TO keycloak;

--
-- TOC entry 272 (class 1259 OID 17828)
-- Name: fed_user_credential; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.fed_user_credential (
    id character varying(36) NOT NULL,
    device character varying(255),
    hash_iterations integer,
    salt bytea,
    type character varying(255),
    value character varying(255),
    created_date bigint,
    counter integer DEFAULT 0,
    digits integer DEFAULT 6,
    period integer DEFAULT 30,
    algorithm character varying(36) DEFAULT 'HmacSHA1'::character varying,
    user_id character varying(255) NOT NULL,
    realm_id character varying(36) NOT NULL,
    storage_provider_id character varying(36)
);


ALTER TABLE public.fed_user_credential OWNER TO keycloak;

--
-- TOC entry 273 (class 1259 OID 17838)
-- Name: fed_user_group_membership; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.fed_user_group_membership (
    group_id character varying(36) NOT NULL,
    user_id character varying(255) NOT NULL,
    realm_id character varying(36) NOT NULL,
    storage_provider_id character varying(36)
);


ALTER TABLE public.fed_user_group_membership OWNER TO keycloak;

--
-- TOC entry 274 (class 1259 OID 17841)
-- Name: fed_user_required_action; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.fed_user_required_action (
    required_action character varying(255) DEFAULT ' '::character varying NOT NULL,
    user_id character varying(255) NOT NULL,
    realm_id character varying(36) NOT NULL,
    storage_provider_id character varying(36)
);


ALTER TABLE public.fed_user_required_action OWNER TO keycloak;

--
-- TOC entry 275 (class 1259 OID 17848)
-- Name: fed_user_role_mapping; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.fed_user_role_mapping (
    role_id character varying(36) NOT NULL,
    user_id character varying(255) NOT NULL,
    realm_id character varying(36) NOT NULL,
    storage_provider_id character varying(36)
);


ALTER TABLE public.fed_user_role_mapping OWNER TO keycloak;

--
-- TOC entry 228 (class 1259 OID 16832)
-- Name: federated_identity; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.federated_identity (
    identity_provider character varying(255) NOT NULL,
    realm_id character varying(36),
    federated_user_id character varying(255),
    federated_username character varying(255),
    token text,
    user_id character varying(36) NOT NULL
);


ALTER TABLE public.federated_identity OWNER TO keycloak;

--
-- TOC entry 280 (class 1259 OID 17917)
-- Name: federated_user; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.federated_user (
    id character varying(255) NOT NULL,
    storage_provider_id character varying(255),
    realm_id character varying(36) NOT NULL
);


ALTER TABLE public.federated_user OWNER TO keycloak;

--
-- TOC entry 254 (class 1259 OID 17471)
-- Name: group_attribute; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.group_attribute (
    id character varying(36) DEFAULT 'sybase-needs-something-here'::character varying NOT NULL,
    name character varying(255) NOT NULL,
    value character varying(255),
    group_id character varying(36) NOT NULL
);


ALTER TABLE public.group_attribute OWNER TO keycloak;

--
-- TOC entry 253 (class 1259 OID 17468)
-- Name: group_role_mapping; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.group_role_mapping (
    role_id character varying(36) NOT NULL,
    group_id character varying(36) NOT NULL
);


ALTER TABLE public.group_role_mapping OWNER TO keycloak;

--
-- TOC entry 229 (class 1259 OID 16838)
-- Name: identity_provider; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.identity_provider (
    internal_id character varying(36) NOT NULL,
    enabled boolean DEFAULT false NOT NULL,
    provider_alias character varying(255),
    provider_id character varying(255),
    store_token boolean DEFAULT false NOT NULL,
    authenticate_by_default boolean DEFAULT false NOT NULL,
    realm_id character varying(36),
    add_token_role boolean DEFAULT true NOT NULL,
    trust_email boolean DEFAULT false NOT NULL,
    first_broker_login_flow_id character varying(36),
    post_broker_login_flow_id character varying(36),
    provider_display_name character varying(255),
    link_only boolean DEFAULT false NOT NULL
);


ALTER TABLE public.identity_provider OWNER TO keycloak;

--
-- TOC entry 230 (class 1259 OID 16848)
-- Name: identity_provider_config; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.identity_provider_config (
    identity_provider_id character varying(36) NOT NULL,
    value text,
    name character varying(255) NOT NULL
);


ALTER TABLE public.identity_provider_config OWNER TO keycloak;

--
-- TOC entry 235 (class 1259 OID 16978)
-- Name: identity_provider_mapper; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.identity_provider_mapper (
    id character varying(36) NOT NULL,
    name character varying(255) NOT NULL,
    idp_alias character varying(255) NOT NULL,
    idp_mapper_name character varying(255) NOT NULL,
    realm_id character varying(36) NOT NULL
);


ALTER TABLE public.identity_provider_mapper OWNER TO keycloak;

--
-- TOC entry 236 (class 1259 OID 16984)
-- Name: idp_mapper_config; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.idp_mapper_config (
    idp_mapper_id character varying(36) NOT NULL,
    value text,
    name character varying(255) NOT NULL
);


ALTER TABLE public.idp_mapper_config OWNER TO keycloak;

--
-- TOC entry 252 (class 1259 OID 17465)
-- Name: keycloak_group; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.keycloak_group (
    id character varying(36) NOT NULL,
    name character varying(255),
    parent_group character varying(36),
    realm_id character varying(36)
);


ALTER TABLE public.keycloak_group OWNER TO keycloak;

--
-- TOC entry 205 (class 1259 OID 16439)
-- Name: keycloak_role; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.keycloak_role (
    id character varying(36) NOT NULL,
    client_realm_constraint character varying(36),
    client_role boolean DEFAULT false NOT NULL,
    description character varying(255),
    name character varying(255),
    realm_id character varying(255),
    client character varying(36),
    realm character varying(36)
);


ALTER TABLE public.keycloak_role OWNER TO keycloak;

--
-- TOC entry 234 (class 1259 OID 16975)
-- Name: migration_model; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.migration_model (
    id character varying(36) NOT NULL,
    version character varying(36)
);


ALTER TABLE public.migration_model OWNER TO keycloak;

--
-- TOC entry 251 (class 1259 OID 17455)
-- Name: offline_client_session; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.offline_client_session (
    user_session_id character varying(36) NOT NULL,
    client_id character varying(36) NOT NULL,
    offline_flag character varying(4) NOT NULL,
    "timestamp" integer,
    data text,
    client_storage_provider character varying(36) DEFAULT 'local'::character varying NOT NULL,
    external_client_id character varying(255) DEFAULT 'local'::character varying NOT NULL
);


ALTER TABLE public.offline_client_session OWNER TO keycloak;

--
-- TOC entry 250 (class 1259 OID 17449)
-- Name: offline_user_session; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.offline_user_session (
    user_session_id character varying(36) NOT NULL,
    user_id character varying(255) NOT NULL,
    realm_id character varying(36) NOT NULL,
    last_session_refresh integer,
    offline_flag character varying(4) NOT NULL,
    data text
);


ALTER TABLE public.offline_user_session OWNER TO keycloak;

--
-- TOC entry 264 (class 1259 OID 17734)
-- Name: policy_config; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.policy_config (
    policy_id character varying(36) NOT NULL,
    name character varying(255) NOT NULL,
    value text
);


ALTER TABLE public.policy_config OWNER TO keycloak;

--
-- TOC entry 226 (class 1259 OID 16819)
-- Name: protocol_mapper; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.protocol_mapper (
    id character varying(36) NOT NULL,
    name character varying(255) NOT NULL,
    protocol character varying(255) NOT NULL,
    protocol_mapper_name character varying(255) NOT NULL,
    client_id character varying(36),
    client_scope_id character varying(36)
);


ALTER TABLE public.protocol_mapper OWNER TO keycloak;

--
-- TOC entry 227 (class 1259 OID 16826)
-- Name: protocol_mapper_config; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.protocol_mapper_config (
    protocol_mapper_id character varying(36) NOT NULL,
    value text,
    name character varying(255) NOT NULL
);


ALTER TABLE public.protocol_mapper_config OWNER TO keycloak;

--
-- TOC entry 206 (class 1259 OID 16446)
-- Name: realm; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.realm (
    id character varying(36) NOT NULL,
    access_code_lifespan integer,
    user_action_lifespan integer,
    access_token_lifespan integer,
    account_theme character varying(255),
    admin_theme character varying(255),
    email_theme character varying(255),
    enabled boolean DEFAULT false NOT NULL,
    events_enabled boolean DEFAULT false NOT NULL,
    events_expiration bigint,
    login_theme character varying(255),
    name character varying(255),
    not_before integer,
    password_policy character varying(2550),
    registration_allowed boolean DEFAULT false NOT NULL,
    remember_me boolean DEFAULT false NOT NULL,
    reset_password_allowed boolean DEFAULT false NOT NULL,
    social boolean DEFAULT false NOT NULL,
    ssl_required character varying(255),
    sso_idle_timeout integer,
    sso_max_lifespan integer,
    update_profile_on_soc_login boolean DEFAULT false NOT NULL,
    verify_email boolean DEFAULT false NOT NULL,
    master_admin_client character varying(36),
    login_lifespan integer,
    internationalization_enabled boolean DEFAULT false NOT NULL,
    default_locale character varying(255),
    reg_email_as_username boolean DEFAULT false NOT NULL,
    admin_events_enabled boolean DEFAULT false NOT NULL,
    admin_events_details_enabled boolean DEFAULT false NOT NULL,
    edit_username_allowed boolean DEFAULT false NOT NULL,
    otp_policy_counter integer DEFAULT 0,
    otp_policy_window integer DEFAULT 1,
    otp_policy_period integer DEFAULT 30,
    otp_policy_digits integer DEFAULT 6,
    otp_policy_alg character varying(36) DEFAULT 'HmacSHA1'::character varying,
    otp_policy_type character varying(36) DEFAULT 'totp'::character varying,
    browser_flow character varying(36),
    registration_flow character varying(36),
    direct_grant_flow character varying(36),
    reset_credentials_flow character varying(36),
    client_auth_flow character varying(36),
    offline_session_idle_timeout integer DEFAULT 0,
    revoke_refresh_token boolean DEFAULT false NOT NULL,
    access_token_life_implicit integer DEFAULT 0,
    login_with_email_allowed boolean DEFAULT true NOT NULL,
    duplicate_emails_allowed boolean DEFAULT false NOT NULL,
    docker_auth_flow character varying(36),
    refresh_token_max_reuse integer DEFAULT 0,
    allow_user_managed_access boolean DEFAULT false NOT NULL
);


ALTER TABLE public.realm OWNER TO keycloak;

--
-- TOC entry 207 (class 1259 OID 16464)
-- Name: realm_attribute; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.realm_attribute (
    name character varying(255) NOT NULL,
    value character varying(255),
    realm_id character varying(36) NOT NULL
);


ALTER TABLE public.realm_attribute OWNER TO keycloak;

--
-- TOC entry 256 (class 1259 OID 17481)
-- Name: realm_default_groups; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.realm_default_groups (
    realm_id character varying(36) NOT NULL,
    group_id character varying(36) NOT NULL
);


ALTER TABLE public.realm_default_groups OWNER TO keycloak;

--
-- TOC entry 208 (class 1259 OID 16470)
-- Name: realm_default_roles; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.realm_default_roles (
    realm_id character varying(36) NOT NULL,
    role_id character varying(36) NOT NULL
);


ALTER TABLE public.realm_default_roles OWNER TO keycloak;

--
-- TOC entry 233 (class 1259 OID 16967)
-- Name: realm_enabled_event_types; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.realm_enabled_event_types (
    realm_id character varying(36) NOT NULL,
    value character varying(255) NOT NULL
);


ALTER TABLE public.realm_enabled_event_types OWNER TO keycloak;

--
-- TOC entry 209 (class 1259 OID 16473)
-- Name: realm_events_listeners; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.realm_events_listeners (
    realm_id character varying(36) NOT NULL,
    value character varying(255) NOT NULL
);


ALTER TABLE public.realm_events_listeners OWNER TO keycloak;

--
-- TOC entry 210 (class 1259 OID 16476)
-- Name: realm_required_credential; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.realm_required_credential (
    type character varying(255) NOT NULL,
    form_label character varying(255),
    input boolean DEFAULT false NOT NULL,
    secret boolean DEFAULT false NOT NULL,
    realm_id character varying(36) NOT NULL
);


ALTER TABLE public.realm_required_credential OWNER TO keycloak;

--
-- TOC entry 211 (class 1259 OID 16484)
-- Name: realm_smtp_config; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.realm_smtp_config (
    realm_id character varying(36) NOT NULL,
    value character varying(255),
    name character varying(255) NOT NULL
);


ALTER TABLE public.realm_smtp_config OWNER TO keycloak;

--
-- TOC entry 231 (class 1259 OID 16858)
-- Name: realm_supported_locales; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.realm_supported_locales (
    realm_id character varying(36) NOT NULL,
    value character varying(255) NOT NULL
);


ALTER TABLE public.realm_supported_locales OWNER TO keycloak;

--
-- TOC entry 212 (class 1259 OID 16496)
-- Name: redirect_uris; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.redirect_uris (
    client_id character varying(36) NOT NULL,
    value character varying(255) NOT NULL
);


ALTER TABLE public.redirect_uris OWNER TO keycloak;

--
-- TOC entry 249 (class 1259 OID 17312)
-- Name: required_action_config; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.required_action_config (
    required_action_id character varying(36) NOT NULL,
    value text,
    name character varying(255) NOT NULL
);


ALTER TABLE public.required_action_config OWNER TO keycloak;

--
-- TOC entry 248 (class 1259 OID 17304)
-- Name: required_action_provider; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.required_action_provider (
    id character varying(36) NOT NULL,
    alias character varying(255),
    name character varying(255),
    realm_id character varying(36),
    enabled boolean DEFAULT false NOT NULL,
    default_action boolean DEFAULT false NOT NULL,
    provider_id character varying(255),
    priority integer
);


ALTER TABLE public.required_action_provider OWNER TO keycloak;

--
-- TOC entry 288 (class 1259 OID 18266)
-- Name: resource_attribute; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.resource_attribute (
    id character varying(36) DEFAULT 'sybase-needs-something-here'::character varying NOT NULL,
    name character varying(255) NOT NULL,
    value character varying(255),
    resource_id character varying(36) NOT NULL
);


ALTER TABLE public.resource_attribute OWNER TO keycloak;

--
-- TOC entry 266 (class 1259 OID 17762)
-- Name: resource_policy; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.resource_policy (
    resource_id character varying(36) NOT NULL,
    policy_id character varying(36) NOT NULL
);


ALTER TABLE public.resource_policy OWNER TO keycloak;

--
-- TOC entry 265 (class 1259 OID 17747)
-- Name: resource_scope; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.resource_scope (
    resource_id character varying(36) NOT NULL,
    scope_id character varying(36) NOT NULL
);


ALTER TABLE public.resource_scope OWNER TO keycloak;

--
-- TOC entry 260 (class 1259 OID 17681)
-- Name: resource_server; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.resource_server (
    id character varying(36) NOT NULL,
    allow_rs_remote_mgmt boolean DEFAULT false NOT NULL,
    policy_enforce_mode character varying(15) NOT NULL
);


ALTER TABLE public.resource_server OWNER TO keycloak;

--
-- TOC entry 287 (class 1259 OID 18224)
-- Name: resource_server_perm_ticket; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.resource_server_perm_ticket (
    id character varying(36) NOT NULL,
    owner character varying(36) NOT NULL,
    requester character varying(36) NOT NULL,
    created_timestamp bigint NOT NULL,
    granted_timestamp bigint,
    resource_id character varying(36) NOT NULL,
    scope_id character varying(36),
    resource_server_id character varying(36) NOT NULL,
    policy_id character varying(36)
);


ALTER TABLE public.resource_server_perm_ticket OWNER TO keycloak;

--
-- TOC entry 263 (class 1259 OID 17719)
-- Name: resource_server_policy; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.resource_server_policy (
    id character varying(36) NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255),
    type character varying(255) NOT NULL,
    decision_strategy character varying(20),
    logic character varying(20),
    resource_server_id character varying(36) NOT NULL,
    owner character varying(36)
);


ALTER TABLE public.resource_server_policy OWNER TO keycloak;

--
-- TOC entry 261 (class 1259 OID 17689)
-- Name: resource_server_resource; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.resource_server_resource (
    id character varying(36) NOT NULL,
    name character varying(255) NOT NULL,
    type character varying(255),
    icon_uri character varying(255),
    owner character varying(36) NOT NULL,
    resource_server_id character varying(36) NOT NULL,
    owner_managed_access boolean DEFAULT false NOT NULL,
    display_name character varying(255)
);


ALTER TABLE public.resource_server_resource OWNER TO keycloak;

--
-- TOC entry 262 (class 1259 OID 17704)
-- Name: resource_server_scope; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.resource_server_scope (
    id character varying(36) NOT NULL,
    name character varying(255) NOT NULL,
    icon_uri character varying(255),
    resource_server_id character varying(36) NOT NULL,
    display_name character varying(255)
);


ALTER TABLE public.resource_server_scope OWNER TO keycloak;

--
-- TOC entry 289 (class 1259 OID 18285)
-- Name: resource_uris; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.resource_uris (
    resource_id character varying(36) NOT NULL,
    value character varying(255) NOT NULL
);


ALTER TABLE public.resource_uris OWNER TO keycloak;

--
-- TOC entry 213 (class 1259 OID 16499)
-- Name: scope_mapping; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.scope_mapping (
    client_id character varying(36) NOT NULL,
    role_id character varying(36) NOT NULL
);


ALTER TABLE public.scope_mapping OWNER TO keycloak;

--
-- TOC entry 267 (class 1259 OID 17777)
-- Name: scope_policy; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.scope_policy (
    scope_id character varying(36) NOT NULL,
    policy_id character varying(36) NOT NULL
);


ALTER TABLE public.scope_policy OWNER TO keycloak;

--
-- TOC entry 215 (class 1259 OID 16505)
-- Name: user_attribute; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.user_attribute (
    name character varying(255) NOT NULL,
    value character varying(255),
    user_id character varying(36) NOT NULL,
    id character varying(36) DEFAULT 'sybase-needs-something-here'::character varying NOT NULL
);


ALTER TABLE public.user_attribute OWNER TO keycloak;

--
-- TOC entry 237 (class 1259 OID 16990)
-- Name: user_consent; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.user_consent (
    id character varying(36) NOT NULL,
    client_id character varying(36),
    user_id character varying(36) NOT NULL,
    created_date bigint,
    last_updated_date bigint,
    client_storage_provider character varying(36),
    external_client_id character varying(255)
);


ALTER TABLE public.user_consent OWNER TO keycloak;

--
-- TOC entry 285 (class 1259 OID 18199)
-- Name: user_consent_client_scope; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.user_consent_client_scope (
    user_consent_id character varying(36) NOT NULL,
    scope_id character varying(36) NOT NULL
);


ALTER TABLE public.user_consent_client_scope OWNER TO keycloak;

--
-- TOC entry 216 (class 1259 OID 16511)
-- Name: user_entity; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.user_entity (
    id character varying(36) NOT NULL,
    email character varying(255),
    email_constraint character varying(255),
    email_verified boolean DEFAULT false NOT NULL,
    enabled boolean DEFAULT false NOT NULL,
    federation_link character varying(255),
    first_name character varying(255),
    last_name character varying(255),
    realm_id character varying(255),
    username character varying(255),
    created_timestamp bigint,
    service_account_client_link character varying(36),
    not_before integer DEFAULT 0 NOT NULL
);


ALTER TABLE public.user_entity OWNER TO keycloak;

--
-- TOC entry 217 (class 1259 OID 16520)
-- Name: user_federation_config; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.user_federation_config (
    user_federation_provider_id character varying(36) NOT NULL,
    value character varying(255),
    name character varying(255) NOT NULL
);


ALTER TABLE public.user_federation_config OWNER TO keycloak;

--
-- TOC entry 244 (class 1259 OID 17122)
-- Name: user_federation_mapper; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.user_federation_mapper (
    id character varying(36) NOT NULL,
    name character varying(255) NOT NULL,
    federation_provider_id character varying(36) NOT NULL,
    federation_mapper_type character varying(255) NOT NULL,
    realm_id character varying(36) NOT NULL
);


ALTER TABLE public.user_federation_mapper OWNER TO keycloak;

--
-- TOC entry 245 (class 1259 OID 17128)
-- Name: user_federation_mapper_config; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.user_federation_mapper_config (
    user_federation_mapper_id character varying(36) NOT NULL,
    value character varying(255),
    name character varying(255) NOT NULL
);


ALTER TABLE public.user_federation_mapper_config OWNER TO keycloak;

--
-- TOC entry 218 (class 1259 OID 16526)
-- Name: user_federation_provider; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.user_federation_provider (
    id character varying(36) NOT NULL,
    changed_sync_period integer,
    display_name character varying(255),
    full_sync_period integer,
    last_sync integer,
    priority integer,
    provider_name character varying(255),
    realm_id character varying(36)
);


ALTER TABLE public.user_federation_provider OWNER TO keycloak;

--
-- TOC entry 255 (class 1259 OID 17478)
-- Name: user_group_membership; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.user_group_membership (
    group_id character varying(36) NOT NULL,
    user_id character varying(36) NOT NULL
);


ALTER TABLE public.user_group_membership OWNER TO keycloak;

--
-- TOC entry 219 (class 1259 OID 16532)
-- Name: user_required_action; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.user_required_action (
    user_id character varying(36) NOT NULL,
    required_action character varying(255) DEFAULT ' '::character varying NOT NULL
);


ALTER TABLE public.user_required_action OWNER TO keycloak;

--
-- TOC entry 220 (class 1259 OID 16535)
-- Name: user_role_mapping; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.user_role_mapping (
    role_id character varying(255) NOT NULL,
    user_id character varying(36) NOT NULL
);


ALTER TABLE public.user_role_mapping OWNER TO keycloak;

--
-- TOC entry 221 (class 1259 OID 16538)
-- Name: user_session; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.user_session (
    id character varying(36) NOT NULL,
    auth_method character varying(255),
    ip_address character varying(255),
    last_session_refresh integer,
    login_username character varying(255),
    realm_id character varying(255),
    remember_me boolean DEFAULT false NOT NULL,
    started integer,
    user_id character varying(255),
    user_session_state integer,
    broker_session_id character varying(255),
    broker_user_id character varying(255)
);


ALTER TABLE public.user_session OWNER TO keycloak;

--
-- TOC entry 232 (class 1259 OID 16861)
-- Name: user_session_note; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.user_session_note (
    user_session character varying(36) NOT NULL,
    name character varying(255) NOT NULL,
    value character varying(2048)
);


ALTER TABLE public.user_session_note OWNER TO keycloak;

--
-- TOC entry 214 (class 1259 OID 16502)
-- Name: username_login_failure; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.username_login_failure (
    realm_id character varying(36) NOT NULL,
    username character varying(255) NOT NULL,
    failed_login_not_before integer,
    last_failure bigint,
    last_ip_failure character varying(255),
    num_failures integer
);


ALTER TABLE public.username_login_failure OWNER TO keycloak;

--
-- TOC entry 222 (class 1259 OID 16551)
-- Name: web_origins; Type: TABLE; Schema: public; Owner: keycloak
--

CREATE TABLE public.web_origins (
    client_id character varying(36) NOT NULL,
    value character varying(255) NOT NULL
);


ALTER TABLE public.web_origins OWNER TO keycloak;

--
-- TOC entry 3770 (class 0 OID 17093)
-- Dependencies: 239
-- Data for Name: admin_event_entity; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3799 (class 0 OID 17792)
-- Dependencies: 268
-- Data for Name: associated_policy; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3773 (class 0 OID 17111)
-- Dependencies: 242
-- Data for Name: authentication_execution; Type: TABLE DATA; Schema: public; Owner: keycloak
--

INSERT INTO public.authentication_execution VALUES ('16c883dc-32eb-4596-a958-1cac1db3cd5f', NULL, 'auth-cookie', 'master', '97e757c6-46b1-4391-9588-78e7c457dd46', 2, 10, false, NULL, NULL);
INSERT INTO public.authentication_execution VALUES ('bfc7fb4c-6add-4de3-b427-9ab860480ed0', NULL, 'auth-spnego', 'master', '97e757c6-46b1-4391-9588-78e7c457dd46', 3, 20, false, NULL, NULL);
INSERT INTO public.authentication_execution VALUES ('0ab91b5d-a512-47ba-b26b-81f76d002269', NULL, 'identity-provider-redirector', 'master', '97e757c6-46b1-4391-9588-78e7c457dd46', 2, 25, false, NULL, NULL);
INSERT INTO public.authentication_execution VALUES ('ad1f1f01-0e12-456b-be5f-2f2fe0f9c60b', NULL, NULL, 'master', '97e757c6-46b1-4391-9588-78e7c457dd46', 2, 30, true, '8ffa5bc0-54bb-4c81-a114-ab824af4f872', NULL);
INSERT INTO public.authentication_execution VALUES ('0d62d7b4-2066-464c-a68f-fb588387fbfa', NULL, 'auth-username-password-form', 'master', '8ffa5bc0-54bb-4c81-a114-ab824af4f872', 0, 10, false, NULL, NULL);
INSERT INTO public.authentication_execution VALUES ('870c117a-9267-40f4-8882-82e494dd53b5', NULL, 'auth-otp-form', 'master', '8ffa5bc0-54bb-4c81-a114-ab824af4f872', 1, 20, false, NULL, NULL);
INSERT INTO public.authentication_execution VALUES ('eb921edc-83d3-44b9-ba4a-81e1334a671d', NULL, 'direct-grant-validate-username', 'master', 'bff3e92c-2061-498c-9ecc-d6f2581998fe', 0, 10, false, NULL, NULL);
INSERT INTO public.authentication_execution VALUES ('ce9f0b9c-1ebc-41e7-9235-68596b4783bf', NULL, 'direct-grant-validate-password', 'master', 'bff3e92c-2061-498c-9ecc-d6f2581998fe', 0, 20, false, NULL, NULL);
INSERT INTO public.authentication_execution VALUES ('8857accd-559e-4771-a836-6590c440ace5', NULL, 'direct-grant-validate-otp', 'master', 'bff3e92c-2061-498c-9ecc-d6f2581998fe', 1, 30, false, NULL, NULL);
INSERT INTO public.authentication_execution VALUES ('1ef7b181-92e2-479b-95ba-be5dc451f3e6', NULL, 'registration-page-form', 'master', '0c7ad963-11b3-411c-ba19-7c0ded73aa6a', 0, 10, true, '5914ec20-cf5a-448a-b92a-c76420d35112', NULL);
INSERT INTO public.authentication_execution VALUES ('4beb770d-a598-4593-9142-afd4d477b4b4', NULL, 'registration-user-creation', 'master', '5914ec20-cf5a-448a-b92a-c76420d35112', 0, 20, false, NULL, NULL);
INSERT INTO public.authentication_execution VALUES ('d8348d32-c6a0-429b-84e1-b15a62c567ed', NULL, 'registration-profile-action', 'master', '5914ec20-cf5a-448a-b92a-c76420d35112', 0, 40, false, NULL, NULL);
INSERT INTO public.authentication_execution VALUES ('464bf7e3-9c93-4329-8ccb-def974bb450b', NULL, 'registration-password-action', 'master', '5914ec20-cf5a-448a-b92a-c76420d35112', 0, 50, false, NULL, NULL);
INSERT INTO public.authentication_execution VALUES ('1bbdc50c-5e95-4a63-8ad8-078b9e75327e', NULL, 'registration-recaptcha-action', 'master', '5914ec20-cf5a-448a-b92a-c76420d35112', 3, 60, false, NULL, NULL);
INSERT INTO public.authentication_execution VALUES ('eae11eaf-5727-41af-9279-130a813e5021', NULL, 'reset-credentials-choose-user', 'master', '2a0c1b2b-77a2-4282-8eda-246a8cf4890b', 0, 10, false, NULL, NULL);
INSERT INTO public.authentication_execution VALUES ('070d81a0-5396-4f70-8d70-c4e3022008ff', NULL, 'reset-credential-email', 'master', '2a0c1b2b-77a2-4282-8eda-246a8cf4890b', 0, 20, false, NULL, NULL);
INSERT INTO public.authentication_execution VALUES ('c96417a8-62a6-48da-9e37-4bc11338a85b', NULL, 'reset-password', 'master', '2a0c1b2b-77a2-4282-8eda-246a8cf4890b', 0, 30, false, NULL, NULL);
INSERT INTO public.authentication_execution VALUES ('579fd52d-9359-4f16-be6e-afd7c0509bcc', NULL, 'reset-otp', 'master', '2a0c1b2b-77a2-4282-8eda-246a8cf4890b', 1, 40, false, NULL, NULL);
INSERT INTO public.authentication_execution VALUES ('836c9b70-43f3-453b-852b-91848c72b49e', NULL, 'client-secret', 'master', '9829c39e-3c1c-4c30-8ddf-28791a94b456', 2, 10, false, NULL, NULL);
INSERT INTO public.authentication_execution VALUES ('0dacec9c-cf06-44ae-88c1-13147aa72f70', NULL, 'client-jwt', 'master', '9829c39e-3c1c-4c30-8ddf-28791a94b456', 2, 20, false, NULL, NULL);
INSERT INTO public.authentication_execution VALUES ('546933f3-ecb2-4e9c-b299-ee8b97ba3411', NULL, 'client-secret-jwt', 'master', '9829c39e-3c1c-4c30-8ddf-28791a94b456', 2, 30, false, NULL, NULL);
INSERT INTO public.authentication_execution VALUES ('c0c606d2-9ae6-40bf-9dc4-31c483506ccb', NULL, 'client-x509', 'master', '9829c39e-3c1c-4c30-8ddf-28791a94b456', 2, 40, false, NULL, NULL);
INSERT INTO public.authentication_execution VALUES ('3c6231db-d6c4-4794-96a8-ab2e36e922e5', NULL, 'idp-review-profile', 'master', 'a22eaa6a-4cf2-4402-a343-eb692a5b9e45', 0, 10, false, NULL, '31de9f5a-ab68-45d8-baad-53ffacbdc9df');
INSERT INTO public.authentication_execution VALUES ('6be6a4fa-121e-4c61-ba38-6ead449d1474', NULL, 'idp-create-user-if-unique', 'master', 'a22eaa6a-4cf2-4402-a343-eb692a5b9e45', 2, 20, false, NULL, '803f258b-290c-4d4d-8e6c-f46a688aa942');
INSERT INTO public.authentication_execution VALUES ('52c785eb-e6ca-4cfd-a718-6395608ca5dc', NULL, NULL, 'master', 'a22eaa6a-4cf2-4402-a343-eb692a5b9e45', 2, 30, true, '51603406-c77b-494e-a4e2-c28391504f11', NULL);
INSERT INTO public.authentication_execution VALUES ('22633e06-9df4-444e-be71-ee01e669bced', NULL, 'idp-confirm-link', 'master', '51603406-c77b-494e-a4e2-c28391504f11', 0, 10, false, NULL, NULL);
INSERT INTO public.authentication_execution VALUES ('b05cb851-2aab-4478-86e8-ff994cba0c61', NULL, 'idp-email-verification', 'master', '51603406-c77b-494e-a4e2-c28391504f11', 2, 20, false, NULL, NULL);
INSERT INTO public.authentication_execution VALUES ('d93d1788-ec25-4268-a9d3-9ef4d8ef7e09', NULL, NULL, 'master', '51603406-c77b-494e-a4e2-c28391504f11', 2, 30, true, 'e28d3908-1b09-4068-8a96-1bb8d0a97fad', NULL);
INSERT INTO public.authentication_execution VALUES ('358e162c-9f18-4117-8644-95344715a204', NULL, 'idp-username-password-form', 'master', 'e28d3908-1b09-4068-8a96-1bb8d0a97fad', 0, 10, false, NULL, NULL);
INSERT INTO public.authentication_execution VALUES ('2b90760f-9027-4adf-88b6-2687836d8ca9', NULL, 'auth-otp-form', 'master', 'e28d3908-1b09-4068-8a96-1bb8d0a97fad', 1, 20, false, NULL, NULL);
INSERT INTO public.authentication_execution VALUES ('8195d156-03ba-4a60-abf9-f6c215c9c7ca', NULL, 'http-basic-authenticator', 'master', 'a6108bcc-7b1a-4829-964a-74e56b28f3e4', 0, 10, false, NULL, NULL);
INSERT INTO public.authentication_execution VALUES ('289e94d9-e192-4abe-8cac-e1b0c34f1f68', NULL, 'docker-http-basic-authenticator', 'master', '930b1832-546f-4afa-9cef-0f10348ea8fe', 0, 10, false, NULL, NULL);


--
-- TOC entry 3772 (class 0 OID 17105)
-- Dependencies: 241
-- Data for Name: authentication_flow; Type: TABLE DATA; Schema: public; Owner: keycloak
--

INSERT INTO public.authentication_flow VALUES ('97e757c6-46b1-4391-9588-78e7c457dd46', 'browser', 'browser based authentication', 'master', 'basic-flow', true, true);
INSERT INTO public.authentication_flow VALUES ('8ffa5bc0-54bb-4c81-a114-ab824af4f872', 'forms', 'Username, password, otp and other auth forms.', 'master', 'basic-flow', false, true);
INSERT INTO public.authentication_flow VALUES ('bff3e92c-2061-498c-9ecc-d6f2581998fe', 'direct grant', 'OpenID Connect Resource Owner Grant', 'master', 'basic-flow', true, true);
INSERT INTO public.authentication_flow VALUES ('0c7ad963-11b3-411c-ba19-7c0ded73aa6a', 'registration', 'registration flow', 'master', 'basic-flow', true, true);
INSERT INTO public.authentication_flow VALUES ('5914ec20-cf5a-448a-b92a-c76420d35112', 'registration form', 'registration form', 'master', 'form-flow', false, true);
INSERT INTO public.authentication_flow VALUES ('2a0c1b2b-77a2-4282-8eda-246a8cf4890b', 'reset credentials', 'Reset credentials for a user if they forgot their password or something', 'master', 'basic-flow', true, true);
INSERT INTO public.authentication_flow VALUES ('9829c39e-3c1c-4c30-8ddf-28791a94b456', 'clients', 'Base authentication for clients', 'master', 'client-flow', true, true);
INSERT INTO public.authentication_flow VALUES ('a22eaa6a-4cf2-4402-a343-eb692a5b9e45', 'first broker login', 'Actions taken after first broker login with identity provider account, which is not yet linked to any Keycloak account', 'master', 'basic-flow', true, true);
INSERT INTO public.authentication_flow VALUES ('51603406-c77b-494e-a4e2-c28391504f11', 'Handle Existing Account', 'Handle what to do if there is existing account with same email/username like authenticated identity provider', 'master', 'basic-flow', false, true);
INSERT INTO public.authentication_flow VALUES ('e28d3908-1b09-4068-8a96-1bb8d0a97fad', 'Verify Existing Account by Re-authentication', 'Reauthentication of existing account', 'master', 'basic-flow', false, true);
INSERT INTO public.authentication_flow VALUES ('a6108bcc-7b1a-4829-964a-74e56b28f3e4', 'saml ecp', 'SAML ECP Profile Authentication Flow', 'master', 'basic-flow', true, true);
INSERT INTO public.authentication_flow VALUES ('930b1832-546f-4afa-9cef-0f10348ea8fe', 'docker auth', 'Used by Docker clients to authenticate against the IDP', 'master', 'basic-flow', true, true);


--
-- TOC entry 3771 (class 0 OID 17099)
-- Dependencies: 240
-- Data for Name: authenticator_config; Type: TABLE DATA; Schema: public; Owner: keycloak
--

INSERT INTO public.authenticator_config VALUES ('31de9f5a-ab68-45d8-baad-53ffacbdc9df', 'review profile config', 'master');
INSERT INTO public.authenticator_config VALUES ('803f258b-290c-4d4d-8e6c-f46a688aa942', 'create unique user config', 'master');


--
-- TOC entry 3774 (class 0 OID 17116)
-- Dependencies: 243
-- Data for Name: authenticator_config_entry; Type: TABLE DATA; Schema: public; Owner: keycloak
--

INSERT INTO public.authenticator_config_entry VALUES ('31de9f5a-ab68-45d8-baad-53ffacbdc9df', 'missing', 'update.profile.on.first.login');
INSERT INTO public.authenticator_config_entry VALUES ('803f258b-290c-4d4d-8e6c-f46a688aa942', 'false', 'require.password.update.after.registration');


--
-- TOC entry 3800 (class 0 OID 17807)
-- Dependencies: 269
-- Data for Name: broker_link; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3730 (class 0 OID 16400)
-- Dependencies: 199
-- Data for Name: client; Type: TABLE DATA; Schema: public; Owner: keycloak
--

INSERT INTO public.client VALUES ('cd5df352-ee3f-41ff-8a25-7ddfb4d39455', true, true, 'master-realm', 0, false, '855bd270-c263-4808-98d2-d8c1500702bb', NULL, true, NULL, false, 'master', NULL, 0, false, false, 'master Realm', false, 'client-secret', NULL, NULL, NULL, true, false, false);
INSERT INTO public.client VALUES ('1b950850-25cf-4739-8214-3014b9380d49', true, false, 'account', 0, false, '5418f4d2-dcde-470c-add1-2b0175f1d022', '/auth/realms/master/account', false, NULL, false, 'master', 'openid-connect', 0, false, false, '${client_account}', false, 'client-secret', NULL, NULL, NULL, true, false, false);
INSERT INTO public.client VALUES ('e26c74c2-d698-42ca-bdff-27a2c5b7bbc6', true, false, 'broker', 0, false, '53d26a9e-1d2f-4047-97b7-e70280fd339d', NULL, false, NULL, false, 'master', 'openid-connect', 0, false, false, '${client_broker}', false, 'client-secret', NULL, NULL, NULL, true, false, false);
INSERT INTO public.client VALUES ('8662309a-9b69-48e4-b315-fa9fb9d07e94', true, false, 'security-admin-console', 0, true, '938fb869-2061-4733-a15a-3a4ab3def241', '/auth/admin/master/console/index.html', false, NULL, false, 'master', 'openid-connect', 0, false, false, '${client_security-admin-console}', false, 'client-secret', NULL, NULL, NULL, true, false, false);
INSERT INTO public.client VALUES ('c93028bf-4468-4483-9d3c-9c69007ebf86', true, false, 'admin-cli', 0, true, 'c8c299a5-be40-404b-ad4e-d931ac3dd49c', NULL, false, NULL, false, 'master', 'openid-connect', 0, false, false, '${client_admin-cli}', false, 'client-secret', NULL, NULL, NULL, false, false, true);
INSERT INTO public.client VALUES ('8c7f88d9-5059-4e59-9827-85a76634e8e9', true, true, 'otoroshi', 0, false, '8c284230-0237-4346-977d-5c4225670e16', NULL, false, 'http://privateapps.foo.bar:8889', false, 'master', 'openid-connect', -1, false, false, 'Otoroshi', true, 'client-secret', 'http://privateapps.foo.bar:8889', 'Otoroshi', NULL, true, false, true);


--
-- TOC entry 3754 (class 0 OID 16774)
-- Dependencies: 223
-- Data for Name: client_attributes; Type: TABLE DATA; Schema: public; Owner: keycloak
--

INSERT INTO public.client_attributes VALUES ('8c7f88d9-5059-4e59-9827-85a76634e8e9', 'false', 'saml.assertion.signature');
INSERT INTO public.client_attributes VALUES ('8c7f88d9-5059-4e59-9827-85a76634e8e9', 'false', 'saml.multivalued.roles');
INSERT INTO public.client_attributes VALUES ('8c7f88d9-5059-4e59-9827-85a76634e8e9', 'false', 'saml.force.post.binding');
INSERT INTO public.client_attributes VALUES ('8c7f88d9-5059-4e59-9827-85a76634e8e9', 'false', 'saml.encrypt');
INSERT INTO public.client_attributes VALUES ('8c7f88d9-5059-4e59-9827-85a76634e8e9', 'false', 'saml.server.signature');
INSERT INTO public.client_attributes VALUES ('8c7f88d9-5059-4e59-9827-85a76634e8e9', 'false', 'saml.server.signature.keyinfo.ext');
INSERT INTO public.client_attributes VALUES ('8c7f88d9-5059-4e59-9827-85a76634e8e9', 'false', 'exclude.session.state.from.auth.response');
INSERT INTO public.client_attributes VALUES ('8c7f88d9-5059-4e59-9827-85a76634e8e9', 'false', 'saml_force_name_id_format');
INSERT INTO public.client_attributes VALUES ('8c7f88d9-5059-4e59-9827-85a76634e8e9', 'false', 'saml.client.signature');
INSERT INTO public.client_attributes VALUES ('8c7f88d9-5059-4e59-9827-85a76634e8e9', 'false', 'tls.client.certificate.bound.access.tokens');
INSERT INTO public.client_attributes VALUES ('8c7f88d9-5059-4e59-9827-85a76634e8e9', 'false', 'saml.authnstatement');
INSERT INTO public.client_attributes VALUES ('8c7f88d9-5059-4e59-9827-85a76634e8e9', 'false', 'display.on.consent.screen');
INSERT INTO public.client_attributes VALUES ('8c7f88d9-5059-4e59-9827-85a76634e8e9', 'false', 'saml.onetimeuse.condition');


--
-- TOC entry 3813 (class 0 OID 18109)
-- Dependencies: 282
-- Data for Name: client_auth_flow_bindings; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3729 (class 0 OID 16397)
-- Dependencies: 198
-- Data for Name: client_default_roles; Type: TABLE DATA; Schema: public; Owner: keycloak
--

INSERT INTO public.client_default_roles VALUES ('1b950850-25cf-4739-8214-3014b9380d49', '3cc4f4bf-e857-4b70-a86f-3230b7b89dd2');
INSERT INTO public.client_default_roles VALUES ('1b950850-25cf-4739-8214-3014b9380d49', 'de5b13b1-7ddc-4032-82ac-ce0e7b6aabcf');


--
-- TOC entry 3812 (class 0 OID 17965)
-- Dependencies: 281
-- Data for Name: client_initial_access; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3756 (class 0 OID 16786)
-- Dependencies: 225
-- Data for Name: client_node_registrations; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3788 (class 0 OID 17566)
-- Dependencies: 257
-- Data for Name: client_scope; Type: TABLE DATA; Schema: public; Owner: keycloak
--

INSERT INTO public.client_scope VALUES ('48e40558-b38c-45a0-99ad-768e5274205d', 'offline_access', 'master', 'OpenID Connect built-in scope: offline_access', 'openid-connect');
INSERT INTO public.client_scope VALUES ('e909616e-c008-4512-9423-3776f068fd6f', 'role_list', 'master', 'SAML role list', 'saml');
INSERT INTO public.client_scope VALUES ('56d81fbe-ee25-498a-8d46-753f683023d0', 'profile', 'master', 'OpenID Connect built-in scope: profile', 'openid-connect');
INSERT INTO public.client_scope VALUES ('8d25d33e-9811-4ef6-9bd8-7a011f9718db', 'email', 'master', 'OpenID Connect built-in scope: email', 'openid-connect');
INSERT INTO public.client_scope VALUES ('6866d376-ef81-436c-9c18-ab29c139d539', 'address', 'master', 'OpenID Connect built-in scope: address', 'openid-connect');
INSERT INTO public.client_scope VALUES ('678b7854-acc8-4bc9-9d2c-d161dbac7294', 'phone', 'master', 'OpenID Connect built-in scope: phone', 'openid-connect');


--
-- TOC entry 3789 (class 0 OID 17581)
-- Dependencies: 258
-- Data for Name: client_scope_attributes; Type: TABLE DATA; Schema: public; Owner: keycloak
--

INSERT INTO public.client_scope_attributes VALUES ('48e40558-b38c-45a0-99ad-768e5274205d', '${offlineAccessScopeConsentText}', 'consent.screen.text');
INSERT INTO public.client_scope_attributes VALUES ('48e40558-b38c-45a0-99ad-768e5274205d', 'true', 'display.on.consent.screen');
INSERT INTO public.client_scope_attributes VALUES ('e909616e-c008-4512-9423-3776f068fd6f', '${samlRoleListScopeConsentText}', 'consent.screen.text');
INSERT INTO public.client_scope_attributes VALUES ('e909616e-c008-4512-9423-3776f068fd6f', 'true', 'display.on.consent.screen');
INSERT INTO public.client_scope_attributes VALUES ('56d81fbe-ee25-498a-8d46-753f683023d0', '${profileScopeConsentText}', 'consent.screen.text');
INSERT INTO public.client_scope_attributes VALUES ('56d81fbe-ee25-498a-8d46-753f683023d0', 'true', 'display.on.consent.screen');
INSERT INTO public.client_scope_attributes VALUES ('8d25d33e-9811-4ef6-9bd8-7a011f9718db', '${emailScopeConsentText}', 'consent.screen.text');
INSERT INTO public.client_scope_attributes VALUES ('8d25d33e-9811-4ef6-9bd8-7a011f9718db', 'true', 'display.on.consent.screen');
INSERT INTO public.client_scope_attributes VALUES ('6866d376-ef81-436c-9c18-ab29c139d539', '${addressScopeConsentText}', 'consent.screen.text');
INSERT INTO public.client_scope_attributes VALUES ('6866d376-ef81-436c-9c18-ab29c139d539', 'true', 'display.on.consent.screen');
INSERT INTO public.client_scope_attributes VALUES ('678b7854-acc8-4bc9-9d2c-d161dbac7294', '${phoneScopeConsentText}', 'consent.screen.text');
INSERT INTO public.client_scope_attributes VALUES ('678b7854-acc8-4bc9-9d2c-d161dbac7294', 'true', 'display.on.consent.screen');


--
-- TOC entry 3814 (class 0 OID 18167)
-- Dependencies: 283
-- Data for Name: client_scope_client; Type: TABLE DATA; Schema: public; Owner: keycloak
--

INSERT INTO public.client_scope_client VALUES ('1b950850-25cf-4739-8214-3014b9380d49', 'e909616e-c008-4512-9423-3776f068fd6f', true);
INSERT INTO public.client_scope_client VALUES ('c93028bf-4468-4483-9d3c-9c69007ebf86', 'e909616e-c008-4512-9423-3776f068fd6f', true);
INSERT INTO public.client_scope_client VALUES ('e26c74c2-d698-42ca-bdff-27a2c5b7bbc6', 'e909616e-c008-4512-9423-3776f068fd6f', true);
INSERT INTO public.client_scope_client VALUES ('cd5df352-ee3f-41ff-8a25-7ddfb4d39455', 'e909616e-c008-4512-9423-3776f068fd6f', true);
INSERT INTO public.client_scope_client VALUES ('8662309a-9b69-48e4-b315-fa9fb9d07e94', 'e909616e-c008-4512-9423-3776f068fd6f', true);
INSERT INTO public.client_scope_client VALUES ('1b950850-25cf-4739-8214-3014b9380d49', '56d81fbe-ee25-498a-8d46-753f683023d0', true);
INSERT INTO public.client_scope_client VALUES ('1b950850-25cf-4739-8214-3014b9380d49', '8d25d33e-9811-4ef6-9bd8-7a011f9718db', true);
INSERT INTO public.client_scope_client VALUES ('1b950850-25cf-4739-8214-3014b9380d49', '48e40558-b38c-45a0-99ad-768e5274205d', false);
INSERT INTO public.client_scope_client VALUES ('1b950850-25cf-4739-8214-3014b9380d49', '6866d376-ef81-436c-9c18-ab29c139d539', false);
INSERT INTO public.client_scope_client VALUES ('1b950850-25cf-4739-8214-3014b9380d49', '678b7854-acc8-4bc9-9d2c-d161dbac7294', false);
INSERT INTO public.client_scope_client VALUES ('c93028bf-4468-4483-9d3c-9c69007ebf86', '56d81fbe-ee25-498a-8d46-753f683023d0', true);
INSERT INTO public.client_scope_client VALUES ('c93028bf-4468-4483-9d3c-9c69007ebf86', '8d25d33e-9811-4ef6-9bd8-7a011f9718db', true);
INSERT INTO public.client_scope_client VALUES ('c93028bf-4468-4483-9d3c-9c69007ebf86', '48e40558-b38c-45a0-99ad-768e5274205d', false);
INSERT INTO public.client_scope_client VALUES ('c93028bf-4468-4483-9d3c-9c69007ebf86', '6866d376-ef81-436c-9c18-ab29c139d539', false);
INSERT INTO public.client_scope_client VALUES ('c93028bf-4468-4483-9d3c-9c69007ebf86', '678b7854-acc8-4bc9-9d2c-d161dbac7294', false);
INSERT INTO public.client_scope_client VALUES ('e26c74c2-d698-42ca-bdff-27a2c5b7bbc6', '56d81fbe-ee25-498a-8d46-753f683023d0', true);
INSERT INTO public.client_scope_client VALUES ('e26c74c2-d698-42ca-bdff-27a2c5b7bbc6', '8d25d33e-9811-4ef6-9bd8-7a011f9718db', true);
INSERT INTO public.client_scope_client VALUES ('e26c74c2-d698-42ca-bdff-27a2c5b7bbc6', '48e40558-b38c-45a0-99ad-768e5274205d', false);
INSERT INTO public.client_scope_client VALUES ('e26c74c2-d698-42ca-bdff-27a2c5b7bbc6', '6866d376-ef81-436c-9c18-ab29c139d539', false);
INSERT INTO public.client_scope_client VALUES ('e26c74c2-d698-42ca-bdff-27a2c5b7bbc6', '678b7854-acc8-4bc9-9d2c-d161dbac7294', false);
INSERT INTO public.client_scope_client VALUES ('cd5df352-ee3f-41ff-8a25-7ddfb4d39455', '56d81fbe-ee25-498a-8d46-753f683023d0', true);
INSERT INTO public.client_scope_client VALUES ('cd5df352-ee3f-41ff-8a25-7ddfb4d39455', '8d25d33e-9811-4ef6-9bd8-7a011f9718db', true);
INSERT INTO public.client_scope_client VALUES ('cd5df352-ee3f-41ff-8a25-7ddfb4d39455', '48e40558-b38c-45a0-99ad-768e5274205d', false);
INSERT INTO public.client_scope_client VALUES ('cd5df352-ee3f-41ff-8a25-7ddfb4d39455', '6866d376-ef81-436c-9c18-ab29c139d539', false);
INSERT INTO public.client_scope_client VALUES ('cd5df352-ee3f-41ff-8a25-7ddfb4d39455', '678b7854-acc8-4bc9-9d2c-d161dbac7294', false);
INSERT INTO public.client_scope_client VALUES ('8662309a-9b69-48e4-b315-fa9fb9d07e94', '56d81fbe-ee25-498a-8d46-753f683023d0', true);
INSERT INTO public.client_scope_client VALUES ('8662309a-9b69-48e4-b315-fa9fb9d07e94', '8d25d33e-9811-4ef6-9bd8-7a011f9718db', true);
INSERT INTO public.client_scope_client VALUES ('8662309a-9b69-48e4-b315-fa9fb9d07e94', '48e40558-b38c-45a0-99ad-768e5274205d', false);
INSERT INTO public.client_scope_client VALUES ('8662309a-9b69-48e4-b315-fa9fb9d07e94', '6866d376-ef81-436c-9c18-ab29c139d539', false);
INSERT INTO public.client_scope_client VALUES ('8662309a-9b69-48e4-b315-fa9fb9d07e94', '678b7854-acc8-4bc9-9d2c-d161dbac7294', false);
INSERT INTO public.client_scope_client VALUES ('8c7f88d9-5059-4e59-9827-85a76634e8e9', 'e909616e-c008-4512-9423-3776f068fd6f', true);
INSERT INTO public.client_scope_client VALUES ('8c7f88d9-5059-4e59-9827-85a76634e8e9', '56d81fbe-ee25-498a-8d46-753f683023d0', true);
INSERT INTO public.client_scope_client VALUES ('8c7f88d9-5059-4e59-9827-85a76634e8e9', '8d25d33e-9811-4ef6-9bd8-7a011f9718db', true);
INSERT INTO public.client_scope_client VALUES ('8c7f88d9-5059-4e59-9827-85a76634e8e9', '48e40558-b38c-45a0-99ad-768e5274205d', false);
INSERT INTO public.client_scope_client VALUES ('8c7f88d9-5059-4e59-9827-85a76634e8e9', '6866d376-ef81-436c-9c18-ab29c139d539', false);
INSERT INTO public.client_scope_client VALUES ('8c7f88d9-5059-4e59-9827-85a76634e8e9', '678b7854-acc8-4bc9-9d2c-d161dbac7294', false);


--
-- TOC entry 3790 (class 0 OID 17587)
-- Dependencies: 259
-- Data for Name: client_scope_role_mapping; Type: TABLE DATA; Schema: public; Owner: keycloak
--

INSERT INTO public.client_scope_role_mapping VALUES ('48e40558-b38c-45a0-99ad-768e5274205d', '5603901b-9d26-49b6-8de2-e8a632c2a55b');


--
-- TOC entry 3731 (class 0 OID 16412)
-- Dependencies: 200
-- Data for Name: client_session; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3777 (class 0 OID 17161)
-- Dependencies: 246
-- Data for Name: client_session_auth_status; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3755 (class 0 OID 16780)
-- Dependencies: 224
-- Data for Name: client_session_note; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3769 (class 0 OID 16999)
-- Dependencies: 238
-- Data for Name: client_session_prot_mapper; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3732 (class 0 OID 16418)
-- Dependencies: 201
-- Data for Name: client_session_role; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3778 (class 0 OID 17298)
-- Dependencies: 247
-- Data for Name: client_user_session_note; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3808 (class 0 OID 17857)
-- Dependencies: 277
-- Data for Name: component; Type: TABLE DATA; Schema: public; Owner: keycloak
--

INSERT INTO public.component VALUES ('713b27cc-6ceb-4b45-807a-d73a88efedad', 'Trusted Hosts', 'master', 'trusted-hosts', 'org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy', 'master', 'anonymous');
INSERT INTO public.component VALUES ('bd3a2b63-6fb5-40b6-9ab6-d28e1cc3b4d3', 'Consent Required', 'master', 'consent-required', 'org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy', 'master', 'anonymous');
INSERT INTO public.component VALUES ('067d904d-fd44-4efb-b1cc-8b7a74a08521', 'Full Scope Disabled', 'master', 'scope', 'org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy', 'master', 'anonymous');
INSERT INTO public.component VALUES ('31596a9d-af3c-4b7f-8251-1bb1cca5d2a3', 'Max Clients Limit', 'master', 'max-clients', 'org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy', 'master', 'anonymous');
INSERT INTO public.component VALUES ('884b9a0a-2394-49ac-a3b3-d17f2bbb0864', 'Allowed Protocol Mapper Types', 'master', 'allowed-protocol-mappers', 'org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy', 'master', 'anonymous');
INSERT INTO public.component VALUES ('398269c6-0ddd-44a5-870e-75051798bfa6', 'Allowed Client Scopes', 'master', 'allowed-client-templates', 'org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy', 'master', 'anonymous');
INSERT INTO public.component VALUES ('9bc36a50-61f9-441e-9fb6-b73f4f6781b7', 'Allowed Protocol Mapper Types', 'master', 'allowed-protocol-mappers', 'org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy', 'master', 'authenticated');
INSERT INTO public.component VALUES ('19169d31-51b8-451c-8bf0-d1be3ef7a4db', 'Allowed Client Scopes', 'master', 'allowed-client-templates', 'org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy', 'master', 'authenticated');
INSERT INTO public.component VALUES ('f9090c17-4fe0-4b45-8b17-2cd0508e4b47', 'rsa-generated', 'master', 'rsa-generated', 'org.keycloak.keys.KeyProvider', 'master', NULL);
INSERT INTO public.component VALUES ('cb04f659-d0e7-4d7e-b048-3d5819e2c7f0', 'hmac-generated', 'master', 'hmac-generated', 'org.keycloak.keys.KeyProvider', 'master', NULL);
INSERT INTO public.component VALUES ('a6ad4aea-4bc9-4560-ae5a-b5de244202c8', 'aes-generated', 'master', 'aes-generated', 'org.keycloak.keys.KeyProvider', 'master', NULL);


--
-- TOC entry 3807 (class 0 OID 17851)
-- Dependencies: 276
-- Data for Name: component_config; Type: TABLE DATA; Schema: public; Owner: keycloak
--

INSERT INTO public.component_config VALUES ('85760059-7ee5-4ecc-b5e6-5947a457e84f', '31596a9d-af3c-4b7f-8251-1bb1cca5d2a3', 'max-clients', '200');
INSERT INTO public.component_config VALUES ('87e9e579-4bb1-4a55-92fe-ab039cb05088', '9bc36a50-61f9-441e-9fb6-b73f4f6781b7', 'allowed-protocol-mapper-types', 'oidc-usermodel-attribute-mapper');
INSERT INTO public.component_config VALUES ('01e69da6-78d1-4e81-a23e-56df8875a156', '9bc36a50-61f9-441e-9fb6-b73f4f6781b7', 'allowed-protocol-mapper-types', 'oidc-sha256-pairwise-sub-mapper');
INSERT INTO public.component_config VALUES ('230c56e2-fce5-447e-a601-fffff498ad56', '9bc36a50-61f9-441e-9fb6-b73f4f6781b7', 'allowed-protocol-mapper-types', 'oidc-usermodel-property-mapper');
INSERT INTO public.component_config VALUES ('c582a3d6-9476-43ec-9745-52fc2238fa03', '9bc36a50-61f9-441e-9fb6-b73f4f6781b7', 'allowed-protocol-mapper-types', 'saml-user-property-mapper');
INSERT INTO public.component_config VALUES ('08aa8e09-c085-45a8-836f-20fa636e647a', '9bc36a50-61f9-441e-9fb6-b73f4f6781b7', 'allowed-protocol-mapper-types', 'saml-user-attribute-mapper');
INSERT INTO public.component_config VALUES ('51585511-cacb-46c8-b3bc-f6dca4ee1938', '9bc36a50-61f9-441e-9fb6-b73f4f6781b7', 'allowed-protocol-mapper-types', 'saml-role-list-mapper');
INSERT INTO public.component_config VALUES ('fc42e909-ab2c-47a5-afe4-5f3d4a406a20', '9bc36a50-61f9-441e-9fb6-b73f4f6781b7', 'allowed-protocol-mapper-types', 'oidc-full-name-mapper');
INSERT INTO public.component_config VALUES ('b35f56d9-8cdf-4f85-8c65-9a16e6710dc3', '9bc36a50-61f9-441e-9fb6-b73f4f6781b7', 'allowed-protocol-mapper-types', 'oidc-address-mapper');
INSERT INTO public.component_config VALUES ('1e7968ff-68a3-4db5-bd17-6cc02788d169', '884b9a0a-2394-49ac-a3b3-d17f2bbb0864', 'allowed-protocol-mapper-types', 'oidc-address-mapper');
INSERT INTO public.component_config VALUES ('27e14f93-3b3e-4b3f-9bf5-ab959d6aaa4b', '884b9a0a-2394-49ac-a3b3-d17f2bbb0864', 'allowed-protocol-mapper-types', 'oidc-sha256-pairwise-sub-mapper');
INSERT INTO public.component_config VALUES ('17ab4c59-f198-47e2-b5c7-46af6dba12c4', '884b9a0a-2394-49ac-a3b3-d17f2bbb0864', 'allowed-protocol-mapper-types', 'saml-user-attribute-mapper');
INSERT INTO public.component_config VALUES ('68008d9c-550c-415b-9e8d-6d1485f69030', '884b9a0a-2394-49ac-a3b3-d17f2bbb0864', 'allowed-protocol-mapper-types', 'saml-user-property-mapper');
INSERT INTO public.component_config VALUES ('2797da5d-ba40-4cbe-ada5-e203825b1d06', '884b9a0a-2394-49ac-a3b3-d17f2bbb0864', 'allowed-protocol-mapper-types', 'oidc-usermodel-property-mapper');
INSERT INTO public.component_config VALUES ('b6b7ed25-f83c-4741-8a80-a8240a57bf8d', '884b9a0a-2394-49ac-a3b3-d17f2bbb0864', 'allowed-protocol-mapper-types', 'oidc-usermodel-attribute-mapper');
INSERT INTO public.component_config VALUES ('c1ab58c9-27db-4757-a8aa-d1cc2592d9a7', '884b9a0a-2394-49ac-a3b3-d17f2bbb0864', 'allowed-protocol-mapper-types', 'saml-role-list-mapper');
INSERT INTO public.component_config VALUES ('859ae374-cc61-4953-a8f2-44c476e4d096', '884b9a0a-2394-49ac-a3b3-d17f2bbb0864', 'allowed-protocol-mapper-types', 'oidc-full-name-mapper');
INSERT INTO public.component_config VALUES ('244130d6-4e7e-4aba-a603-e30825d90de4', '398269c6-0ddd-44a5-870e-75051798bfa6', 'allow-default-scopes', 'true');
INSERT INTO public.component_config VALUES ('b95f2860-e1ae-4551-88a0-1c582ef18b8c', '713b27cc-6ceb-4b45-807a-d73a88efedad', 'client-uris-must-match', 'true');
INSERT INTO public.component_config VALUES ('89ef5061-ac7c-4777-ba71-a869f3a5c4a2', '713b27cc-6ceb-4b45-807a-d73a88efedad', 'host-sending-registration-request-must-match', 'true');
INSERT INTO public.component_config VALUES ('dc94329e-9ac2-4d8a-a39e-a16ccdb1d7f8', '19169d31-51b8-451c-8bf0-d1be3ef7a4db', 'allow-default-scopes', 'true');
INSERT INTO public.component_config VALUES ('968f5578-7583-4c5a-af13-6e993dea2ebe', 'cb04f659-d0e7-4d7e-b048-3d5819e2c7f0', 'kid', 'dd23072d-4f77-40b7-87e7-cde668af9d59');
INSERT INTO public.component_config VALUES ('477bd39c-c66a-434d-bec5-1306ad44e328', 'cb04f659-d0e7-4d7e-b048-3d5819e2c7f0', 'priority', '100');
INSERT INTO public.component_config VALUES ('f575a389-3d2c-4e3a-8375-991e76e33d12', 'cb04f659-d0e7-4d7e-b048-3d5819e2c7f0', 'secret', 'd9E519XBgNv-1YEPpYU21WuKV0O2FqdlN_8c5av4pQo');
INSERT INTO public.component_config VALUES ('75a16b55-c6b6-4074-a9ad-b7af6904a6ce', 'a6ad4aea-4bc9-4560-ae5a-b5de244202c8', 'kid', 'ee8213ff-9149-427f-b61a-1fcf76c98213');
INSERT INTO public.component_config VALUES ('a86bf95c-9879-4a6c-b1b1-9bfd42fe82ea', 'a6ad4aea-4bc9-4560-ae5a-b5de244202c8', 'secret', '87dtI2_BSFEBR5Ss8b5rng');
INSERT INTO public.component_config VALUES ('ecb222a7-db29-4b70-bdb3-0018c5fed6dd', 'a6ad4aea-4bc9-4560-ae5a-b5de244202c8', 'priority', '100');
INSERT INTO public.component_config VALUES ('0387f200-534e-4f7e-a755-32fdac51e736', 'f9090c17-4fe0-4b45-8b17-2cd0508e4b47', 'priority', '100');
INSERT INTO public.component_config VALUES ('ea8d0005-10c9-48af-b412-1348559da752', 'f9090c17-4fe0-4b45-8b17-2cd0508e4b47', 'certificate', 'MIICmzCCAYMCBgFl8QqBvTANBgkqhkiG9w0BAQsFADARMQ8wDQYDVQQDDAZtYXN0ZXIwHhcNMTgwOTE5MDg1MzQyWhcNMjgwOTE5MDg1NTIyWjARMQ8wDQYDVQQDDAZtYXN0ZXIwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQC0fCoDLRbWWICiMTQFYt9nyFpUQrp7CpPZHyvM8kiKNDjn0U/AQAgTNDDtFpmFjrmJ+dysK0Pm4l5MjPk43LAp5IGAe+79Wf9Ek96dsS4yMfNI3Bl4TGeUgGXSTIrFAVKIPcWlaJyehUzeDxLl37baQAxUAzJ1N7oCkJ7ZAmi+V7ECQfkA6IOiGp6kkTmX6Y4NaIdopNGURD3dsZGzCi6VTah0wjTcVVy1GSMzsI45YYBWe1gcEHPW22rKGoHEwdrf3aQCgjeWek/ndlD7un0sd8KvW13fWd6oF3+Dy6/cSVFcNrYrAYunRd3HEebMeXB77H9IzJgVEigCGWJiFo43AgMBAAEwDQYJKoZIhvcNAQELBQADggEBAHTU1/+Pc6UvRBSitZaqkzBWJvxWQChtN2W+av4w96BLfuJInb6pMkTPiLhLq3LzD6WI0fOgLMRLtruVnP89KHGTi4Hm3oAUDYz3Qn6xe5TI5pdDvk12SMkc5M6D1h48ugdY57nMRA4ToJBxs+AOrQateuJm0VugILVsMHMqAoDcfTL6LskjTVLSfYHXPxpkE7M63Z3m5va/PO5ay2Y5f+lDqOaxoyXfRtL0WtTzJNeDpoO8/segESO9dEKUnaG0qdsJhGqF8frkHHaVCZiHJ4cb/3uJDf/cjUewHcwvNBqi1lvpyuZLFhxDNCUdtUwlSo+io9pWJtGIXb8orp+d3AY=');
INSERT INTO public.component_config VALUES ('a1f83c02-38a9-4bd7-9e90-dfd62aaec262', 'f9090c17-4fe0-4b45-8b17-2cd0508e4b47', 'privateKey', 'MIIEpAIBAAKCAQEAtHwqAy0W1liAojE0BWLfZ8haVEK6ewqT2R8rzPJIijQ459FPwEAIEzQw7RaZhY65ifncrCtD5uJeTIz5ONywKeSBgHvu/Vn/RJPenbEuMjHzSNwZeExnlIBl0kyKxQFSiD3FpWicnoVM3g8S5d+22kAMVAMydTe6ApCe2QJovlexAkH5AOiDohqepJE5l+mODWiHaKTRlEQ93bGRswoulU2odMI03FVctRkjM7COOWGAVntYHBBz1ttqyhqBxMHa392kAoI3lnpP53ZQ+7p9LHfCr1td31neqBd/g8uv3ElRXDa2KwGLp0XdxxHmzHlwe+x/SMyYFRIoAhliYhaONwIDAQABAoIBAQCmufChgHyRQ66IpgVVWHHiUH3JSt8znKDZuObga4zBRt6un3gZs80B2Hu9+NuXOjKBmDTXQxx14/WSp8PuWEfQW0uaYmJy3jlmo2bJq4xuSCBi1RgePg6Na1MkQxsKRF92hgHX/Fx6P8+zbp9ZhSFIWlRJI67wA6ushOaIt6YE9jWSX8RBKaWYyguyJgYWiyBaqviMCuzqd4W1cRVqN5/jAduWPiGWr7tMJieAfl1Vpz35FP7xvv8xJEvmR/lueXR0hGDidrooEms0+Bj004lEh+W8/HpDGYxv2YDJpRmyrXDY8Pj0GH+2w63sva946FtX48wejf9P8MVUMhPcuLdRAoGBAN/c3GD0sSUFLFO4Kbofw2wzR0x0Fe0qZeeXCtK4EtXP8E4szgHkjrdYv6s4cbHyr/u+8F4xnAL0lcmGu5hkec/5ppv5jK0m+mdGWBoPKB/EItH1Enaxe5BimKtfuf9uscHy7f7eS95yzqQ43ui+SS8aU76ai1w2uqy9sJi4Z6h5AoGBAM5lIuyTltNdiKqHqAMCIjz5i+YFU5mO690F1CcV8Jw55mn9Wu6irN1WsatFV8JCA6RY0AhLQTvNbFik3mP/Y58UgWfMpRgrJ9Kkv8H78ugAemma7eMMt6nAAC1PzAFwe9gZJlajoS6BDSYlCpImKBW1NP4wAc5r54egnGSBG6AvAoGBAJGLddivnjCUgXJY7QAuwI6rdDKX0t2kUCbXA9lmhhBvJiydYr7GS6eW0t7OTtVEFPjW9k43cNhXDr+8kmENCkmWZaVJBRZanjjg+kzPB+ZHTeA1tvBmihCgmePp0/LnlDil6ehnvOn+uKz6sKqfdNNkCYiF0A9/IbVHivZwBiz5AoGARgivZxNVlHcijqrlac/pikMrI2wfR/XlNuRpbrHVKU3ET6a2mk781UY7l2A6PSJlVfkE7iuLAR3da97Vz5yzUyGEH5KjWqYaJzHcF5jEfdDbuMXnfqmsEJ+j77wp/zsJ6vPtvGxdnwgPTCg5hAoKhAMu7imgE0kuESmAp+HfW+UCgYA/+VyUHJNKh2WE5YYU+RSBlwyxXfWIbkimQPQjZzoOLVGDz8OL2RXKruTdXJioihNXC5AVpTyGVMQjPTMaB71h7afv/3dSmlpU5QF1pwoEDvx93G7TqNRo0BbqvUqdFDur8ROIroGrJl8GLmMwq+xGwDMkLVSDjdiCwxwQardvOQ==');


--
-- TOC entry 3733 (class 0 OID 16421)
-- Dependencies: 202
-- Data for Name: composite_role; Type: TABLE DATA; Schema: public; Owner: keycloak
--

INSERT INTO public.composite_role VALUES ('186d3a91-8920-4fde-93b6-d98226e2e13f', '9f714ee2-d21d-4e49-8145-775d077c24b6');
INSERT INTO public.composite_role VALUES ('186d3a91-8920-4fde-93b6-d98226e2e13f', '9565e1bb-8351-4f1b-9194-b519a88aa17f');
INSERT INTO public.composite_role VALUES ('186d3a91-8920-4fde-93b6-d98226e2e13f', '357f7f68-664e-455a-9508-86dcf381be68');
INSERT INTO public.composite_role VALUES ('186d3a91-8920-4fde-93b6-d98226e2e13f', '06944daa-0da3-4c40-b475-0aacf9cba357');
INSERT INTO public.composite_role VALUES ('186d3a91-8920-4fde-93b6-d98226e2e13f', 'fb26b091-9eee-4a5b-9d91-5980f8fd23d7');
INSERT INTO public.composite_role VALUES ('186d3a91-8920-4fde-93b6-d98226e2e13f', 'fc7f3c4a-e702-470e-91fc-5166135a63a9');
INSERT INTO public.composite_role VALUES ('186d3a91-8920-4fde-93b6-d98226e2e13f', '105806c5-f58b-40a7-9f82-0a964742d222');
INSERT INTO public.composite_role VALUES ('186d3a91-8920-4fde-93b6-d98226e2e13f', '0582be99-3a7e-47f0-9104-740f3472e1d4');
INSERT INTO public.composite_role VALUES ('186d3a91-8920-4fde-93b6-d98226e2e13f', 'ca5a428a-5505-452d-a366-2f9d780335e2');
INSERT INTO public.composite_role VALUES ('186d3a91-8920-4fde-93b6-d98226e2e13f', '36c6f320-51b6-4e99-96e0-356dbb1b5ac3');
INSERT INTO public.composite_role VALUES ('186d3a91-8920-4fde-93b6-d98226e2e13f', '806c5df3-e925-4df0-9137-401adcc1935a');
INSERT INTO public.composite_role VALUES ('186d3a91-8920-4fde-93b6-d98226e2e13f', '6283833b-f5b0-43c7-8f5f-af461008159a');
INSERT INTO public.composite_role VALUES ('186d3a91-8920-4fde-93b6-d98226e2e13f', 'a6994cca-e8e5-461c-8d03-6a73232b5a2d');
INSERT INTO public.composite_role VALUES ('186d3a91-8920-4fde-93b6-d98226e2e13f', 'e643acbb-ccb0-4486-bad2-09c157fefb4f');
INSERT INTO public.composite_role VALUES ('186d3a91-8920-4fde-93b6-d98226e2e13f', 'a938128b-c816-43cb-b594-40a8ceb1e8fd');
INSERT INTO public.composite_role VALUES ('186d3a91-8920-4fde-93b6-d98226e2e13f', '2c732570-7691-4acd-a118-8952669777db');
INSERT INTO public.composite_role VALUES ('186d3a91-8920-4fde-93b6-d98226e2e13f', '5242b3e3-ee56-42d5-aa65-0654a8655303');
INSERT INTO public.composite_role VALUES ('186d3a91-8920-4fde-93b6-d98226e2e13f', '3b6c844d-cb84-4f10-a32a-9a469b35da5d');
INSERT INTO public.composite_role VALUES ('06944daa-0da3-4c40-b475-0aacf9cba357', 'a938128b-c816-43cb-b594-40a8ceb1e8fd');
INSERT INTO public.composite_role VALUES ('06944daa-0da3-4c40-b475-0aacf9cba357', '3b6c844d-cb84-4f10-a32a-9a469b35da5d');
INSERT INTO public.composite_role VALUES ('fb26b091-9eee-4a5b-9d91-5980f8fd23d7', '2c732570-7691-4acd-a118-8952669777db');
INSERT INTO public.composite_role VALUES ('de5b13b1-7ddc-4032-82ac-ce0e7b6aabcf', '03982a16-772c-48f6-8b5b-9629397e5729');
INSERT INTO public.composite_role VALUES ('186d3a91-8920-4fde-93b6-d98226e2e13f', '68317bfc-336c-4ecb-84b9-636c0423fa01');


--
-- TOC entry 3734 (class 0 OID 16424)
-- Dependencies: 203
-- Data for Name: credential; Type: TABLE DATA; Schema: public; Owner: keycloak
--

INSERT INTO public.credential VALUES ('5709f400-4c0c-4d56-808c-c01e44b78005', NULL, 27500, '\x7ce7466441bc480711ac4eafbd85b063', 'password', 'SrTI8bFIYKWwQafLQkwaf3sFsJedSi67sndYWA7OCtI5ESg4MYwtD4nZdTegtc6BcqP/SZOqpkH3mElvg+zICQ==', '581897a3-1caa-4406-9af6-5dfeffc5c75c', NULL, 0, 0, 0, 'pbkdf2-sha256');
INSERT INTO public.credential VALUES ('001a8e36-8160-4da3-8aae-2bac07701ed4', NULL, 27500, '\xa6bc549ed60b92c68f42e37e61157be0', 'password', 'fXRBzOM1VbKpcFvAj6Ixh/PExSpfOcEBUuoUXe9bR+vxuC/NSRIS2ZmG8m6cY5XS+nczq2g6r/G8UYY0+yQ/PA==', '614ef060-a249-4f4a-b24f-f5f63a230f80', 1537347753591, 0, 0, 0, 'pbkdf2-sha256');


--
-- TOC entry 3809 (class 0 OID 17895)
-- Dependencies: 278
-- Data for Name: credential_attribute; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3728 (class 0 OID 16391)
-- Dependencies: 197
-- Data for Name: databasechangelog; Type: TABLE DATA; Schema: public; Owner: keycloak
--

INSERT INTO public.databasechangelog VALUES ('1.0.0.Final-KEYCLOAK-5461', 'sthorger@redhat.com', 'META-INF/jpa-changelog-1.0.0.Final.xml', '2018-09-19 08:55:06.261802', 1, 'EXECUTED', '7:4e70412f24a3f382c82183742ec79317', 'createTable tableName=APPLICATION_DEFAULT_ROLES; createTable tableName=CLIENT; createTable tableName=CLIENT_SESSION; createTable tableName=CLIENT_SESSION_ROLE; createTable tableName=COMPOSITE_ROLE; createTable tableName=CREDENTIAL; createTable tab...', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('1.0.0.Final-KEYCLOAK-5461', 'sthorger@redhat.com', 'META-INF/db2-jpa-changelog-1.0.0.Final.xml', '2018-09-19 08:55:06.287646', 2, 'MARK_RAN', '7:cb16724583e9675711801c6875114f28', 'createTable tableName=APPLICATION_DEFAULT_ROLES; createTable tableName=CLIENT; createTable tableName=CLIENT_SESSION; createTable tableName=CLIENT_SESSION_ROLE; createTable tableName=COMPOSITE_ROLE; createTable tableName=CREDENTIAL; createTable tab...', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('1.1.0.Beta1', 'sthorger@redhat.com', 'META-INF/jpa-changelog-1.1.0.Beta1.xml', '2018-09-19 08:55:06.473352', 3, 'EXECUTED', '7:0310eb8ba07cec616460794d42ade0fa', 'delete tableName=CLIENT_SESSION_ROLE; delete tableName=CLIENT_SESSION; delete tableName=USER_SESSION; createTable tableName=CLIENT_ATTRIBUTES; createTable tableName=CLIENT_SESSION_NOTE; createTable tableName=APP_NODE_REGISTRATIONS; addColumn table...', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('1.1.0.Final', 'sthorger@redhat.com', 'META-INF/jpa-changelog-1.1.0.Final.xml', '2018-09-19 08:55:06.480567', 4, 'EXECUTED', '7:5d25857e708c3233ef4439df1f93f012', 'renameColumn newColumnName=EVENT_TIME, oldColumnName=TIME, tableName=EVENT_ENTITY', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('1.2.0.Beta1', 'psilva@redhat.com', 'META-INF/jpa-changelog-1.2.0.Beta1.xml', '2018-09-19 08:55:06.990393', 5, 'EXECUTED', '7:c7a54a1041d58eb3817a4a883b4d4e84', 'delete tableName=CLIENT_SESSION_ROLE; delete tableName=CLIENT_SESSION_NOTE; delete tableName=CLIENT_SESSION; delete tableName=USER_SESSION; createTable tableName=PROTOCOL_MAPPER; createTable tableName=PROTOCOL_MAPPER_CONFIG; createTable tableName=...', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('1.2.0.Beta1', 'psilva@redhat.com', 'META-INF/db2-jpa-changelog-1.2.0.Beta1.xml', '2018-09-19 08:55:07.006637', 6, 'MARK_RAN', '7:2e01012df20974c1c2a605ef8afe25b7', 'delete tableName=CLIENT_SESSION_ROLE; delete tableName=CLIENT_SESSION_NOTE; delete tableName=CLIENT_SESSION; delete tableName=USER_SESSION; createTable tableName=PROTOCOL_MAPPER; createTable tableName=PROTOCOL_MAPPER_CONFIG; createTable tableName=...', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('1.2.0.RC1', 'bburke@redhat.com', 'META-INF/jpa-changelog-1.2.0.CR1.xml', '2018-09-19 08:55:07.347805', 7, 'EXECUTED', '7:0f08df48468428e0f30ee59a8ec01a41', 'delete tableName=CLIENT_SESSION_ROLE; delete tableName=CLIENT_SESSION_NOTE; delete tableName=CLIENT_SESSION; delete tableName=USER_SESSION_NOTE; delete tableName=USER_SESSION; createTable tableName=MIGRATION_MODEL; createTable tableName=IDENTITY_P...', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('1.2.0.RC1', 'bburke@redhat.com', 'META-INF/db2-jpa-changelog-1.2.0.CR1.xml', '2018-09-19 08:55:07.363491', 8, 'MARK_RAN', '7:a77ea2ad226b345e7d689d366f185c8c', 'delete tableName=CLIENT_SESSION_ROLE; delete tableName=CLIENT_SESSION_NOTE; delete tableName=CLIENT_SESSION; delete tableName=USER_SESSION_NOTE; delete tableName=USER_SESSION; createTable tableName=MIGRATION_MODEL; createTable tableName=IDENTITY_P...', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('1.2.0.Final', 'keycloak', 'META-INF/jpa-changelog-1.2.0.Final.xml', '2018-09-19 08:55:07.385436', 9, 'EXECUTED', '7:a3377a2059aefbf3b90ebb4c4cc8e2ab', 'update tableName=CLIENT; update tableName=CLIENT; update tableName=CLIENT', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('1.3.0', 'bburke@redhat.com', 'META-INF/jpa-changelog-1.3.0.xml', '2018-09-19 08:55:08.13679', 10, 'EXECUTED', '7:04c1dbedc2aa3e9756d1a1668e003451', 'delete tableName=CLIENT_SESSION_ROLE; delete tableName=CLIENT_SESSION_PROT_MAPPER; delete tableName=CLIENT_SESSION_NOTE; delete tableName=CLIENT_SESSION; delete tableName=USER_SESSION_NOTE; delete tableName=USER_SESSION; createTable tableName=ADMI...', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('1.4.0', 'bburke@redhat.com', 'META-INF/jpa-changelog-1.4.0.xml', '2018-09-19 08:55:08.533579', 11, 'EXECUTED', '7:36ef39ed560ad07062d956db861042ba', 'delete tableName=CLIENT_SESSION_AUTH_STATUS; delete tableName=CLIENT_SESSION_ROLE; delete tableName=CLIENT_SESSION_PROT_MAPPER; delete tableName=CLIENT_SESSION_NOTE; delete tableName=CLIENT_SESSION; delete tableName=USER_SESSION_NOTE; delete table...', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('1.4.0', 'bburke@redhat.com', 'META-INF/db2-jpa-changelog-1.4.0.xml', '2018-09-19 08:55:08.539617', 12, 'MARK_RAN', '7:d909180b2530479a716d3f9c9eaea3d7', 'delete tableName=CLIENT_SESSION_AUTH_STATUS; delete tableName=CLIENT_SESSION_ROLE; delete tableName=CLIENT_SESSION_PROT_MAPPER; delete tableName=CLIENT_SESSION_NOTE; delete tableName=CLIENT_SESSION; delete tableName=USER_SESSION_NOTE; delete table...', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('1.5.0', 'bburke@redhat.com', 'META-INF/jpa-changelog-1.5.0.xml', '2018-09-19 08:55:09.042882', 13, 'EXECUTED', '7:cf12b04b79bea5152f165eb41f3955f6', 'delete tableName=CLIENT_SESSION_AUTH_STATUS; delete tableName=CLIENT_SESSION_ROLE; delete tableName=CLIENT_SESSION_PROT_MAPPER; delete tableName=CLIENT_SESSION_NOTE; delete tableName=CLIENT_SESSION; delete tableName=USER_SESSION_NOTE; delete table...', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('1.6.1_from15', 'mposolda@redhat.com', 'META-INF/jpa-changelog-1.6.1.xml', '2018-09-19 08:55:09.2596', 14, 'EXECUTED', '7:7e32c8f05c755e8675764e7d5f514509', 'addColumn tableName=REALM; addColumn tableName=KEYCLOAK_ROLE; addColumn tableName=CLIENT; createTable tableName=OFFLINE_USER_SESSION; createTable tableName=OFFLINE_CLIENT_SESSION; addPrimaryKey constraintName=CONSTRAINT_OFFL_US_SES_PK2, tableName=...', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('1.6.1_from16-pre', 'mposolda@redhat.com', 'META-INF/jpa-changelog-1.6.1.xml', '2018-09-19 08:55:09.261484', 15, 'MARK_RAN', '7:980ba23cc0ec39cab731ce903dd01291', 'delete tableName=OFFLINE_CLIENT_SESSION; delete tableName=OFFLINE_USER_SESSION', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('1.6.1_from16', 'mposolda@redhat.com', 'META-INF/jpa-changelog-1.6.1.xml', '2018-09-19 08:55:09.26373', 16, 'MARK_RAN', '7:2fa220758991285312eb84f3b4ff5336', 'dropPrimaryKey constraintName=CONSTRAINT_OFFLINE_US_SES_PK, tableName=OFFLINE_USER_SESSION; dropPrimaryKey constraintName=CONSTRAINT_OFFLINE_CL_SES_PK, tableName=OFFLINE_CLIENT_SESSION; addColumn tableName=OFFLINE_USER_SESSION; update tableName=OF...', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('1.6.1', 'mposolda@redhat.com', 'META-INF/jpa-changelog-1.6.1.xml', '2018-09-19 08:55:09.265713', 17, 'EXECUTED', '7:d41d8cd98f00b204e9800998ecf8427e', 'empty', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('1.7.0', 'bburke@redhat.com', 'META-INF/jpa-changelog-1.7.0.xml', '2018-09-19 08:55:10.139886', 18, 'EXECUTED', '7:91ace540896df890cc00a0490ee52bbc', 'createTable tableName=KEYCLOAK_GROUP; createTable tableName=GROUP_ROLE_MAPPING; createTable tableName=GROUP_ATTRIBUTE; createTable tableName=USER_GROUP_MEMBERSHIP; createTable tableName=REALM_DEFAULT_GROUPS; addColumn tableName=IDENTITY_PROVIDER; ...', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('1.8.0', 'mposolda@redhat.com', 'META-INF/jpa-changelog-1.8.0.xml', '2018-09-19 08:55:10.442188', 19, 'EXECUTED', '7:c31d1646dfa2618a9335c00e07f89f24', 'addColumn tableName=IDENTITY_PROVIDER; createTable tableName=CLIENT_TEMPLATE; createTable tableName=CLIENT_TEMPLATE_ATTRIBUTES; createTable tableName=TEMPLATE_SCOPE_MAPPING; dropNotNullConstraint columnName=CLIENT_ID, tableName=PROTOCOL_MAPPER; ad...', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('1.8.0-2', 'keycloak', 'META-INF/jpa-changelog-1.8.0.xml', '2018-09-19 08:55:10.455746', 20, 'EXECUTED', '7:df8bc21027a4f7cbbb01f6344e89ce07', 'dropDefaultValue columnName=ALGORITHM, tableName=CREDENTIAL; update tableName=CREDENTIAL', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('authz-3.4.0.CR1-resource-server-pk-change-part2-KEYCLOAK-6095', 'hmlnarik@redhat.com', 'META-INF/jpa-changelog-authz-3.4.0.CR1.xml', '2018-09-19 08:55:12.128588', 45, 'EXECUTED', '7:e64b5dcea7db06077c6e57d3b9e5ca14', 'customChange', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('1.8.0', 'mposolda@redhat.com', 'META-INF/db2-jpa-changelog-1.8.0.xml', '2018-09-19 08:55:10.460518', 21, 'MARK_RAN', '7:f987971fe6b37d963bc95fee2b27f8df', 'addColumn tableName=IDENTITY_PROVIDER; createTable tableName=CLIENT_TEMPLATE; createTable tableName=CLIENT_TEMPLATE_ATTRIBUTES; createTable tableName=TEMPLATE_SCOPE_MAPPING; dropNotNullConstraint columnName=CLIENT_ID, tableName=PROTOCOL_MAPPER; ad...', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('1.8.0-2', 'keycloak', 'META-INF/db2-jpa-changelog-1.8.0.xml', '2018-09-19 08:55:10.467877', 22, 'MARK_RAN', '7:df8bc21027a4f7cbbb01f6344e89ce07', 'dropDefaultValue columnName=ALGORITHM, tableName=CREDENTIAL; update tableName=CREDENTIAL', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('1.9.0', 'mposolda@redhat.com', 'META-INF/jpa-changelog-1.9.0.xml', '2018-09-19 08:55:10.524772', 23, 'EXECUTED', '7:ed2dc7f799d19ac452cbcda56c929e47', 'update tableName=REALM; update tableName=REALM; update tableName=REALM; update tableName=REALM; update tableName=CREDENTIAL; update tableName=CREDENTIAL; update tableName=CREDENTIAL; update tableName=REALM; update tableName=REALM; customChange; dr...', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('1.9.1', 'keycloak', 'META-INF/jpa-changelog-1.9.1.xml', '2018-09-19 08:55:10.529413', 24, 'EXECUTED', '7:80b5db88a5dda36ece5f235be8757615', 'modifyDataType columnName=PRIVATE_KEY, tableName=REALM; modifyDataType columnName=PUBLIC_KEY, tableName=REALM; modifyDataType columnName=CERTIFICATE, tableName=REALM', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('1.9.1', 'keycloak', 'META-INF/db2-jpa-changelog-1.9.1.xml', '2018-09-19 08:55:10.531218', 25, 'MARK_RAN', '7:1437310ed1305a9b93f8848f301726ce', 'modifyDataType columnName=PRIVATE_KEY, tableName=REALM; modifyDataType columnName=CERTIFICATE, tableName=REALM', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('1.9.2', 'keycloak', 'META-INF/jpa-changelog-1.9.2.xml', '2018-09-19 08:55:10.620703', 26, 'EXECUTED', '7:b82ffb34850fa0836be16deefc6a87c4', 'createIndex indexName=IDX_USER_EMAIL, tableName=USER_ENTITY; createIndex indexName=IDX_USER_ROLE_MAPPING, tableName=USER_ROLE_MAPPING; createIndex indexName=IDX_USER_GROUP_MAPPING, tableName=USER_GROUP_MEMBERSHIP; createIndex indexName=IDX_USER_CO...', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('authz-2.0.0', 'psilva@redhat.com', 'META-INF/jpa-changelog-authz-2.0.0.xml', '2018-09-19 08:55:10.934564', 27, 'EXECUTED', '7:9cc98082921330d8d9266decdd4bd658', 'createTable tableName=RESOURCE_SERVER; addPrimaryKey constraintName=CONSTRAINT_FARS, tableName=RESOURCE_SERVER; addUniqueConstraint constraintName=UK_AU8TT6T700S9V50BU18WS5HA6, tableName=RESOURCE_SERVER; createTable tableName=RESOURCE_SERVER_RESOU...', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('authz-2.5.1', 'psilva@redhat.com', 'META-INF/jpa-changelog-authz-2.5.1.xml', '2018-09-19 08:55:10.948406', 28, 'EXECUTED', '7:03d64aeed9cb52b969bd30a7ac0db57e', 'update tableName=RESOURCE_SERVER_POLICY', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('2.1.0-KEYCLOAK-5461', 'bburke@redhat.com', 'META-INF/jpa-changelog-2.1.0.xml', '2018-09-19 08:55:11.328483', 29, 'EXECUTED', '7:f1f9fd8710399d725b780f463c6b21cd', 'createTable tableName=BROKER_LINK; createTable tableName=FED_USER_ATTRIBUTE; createTable tableName=FED_USER_CONSENT; createTable tableName=FED_USER_CONSENT_ROLE; createTable tableName=FED_USER_CONSENT_PROT_MAPPER; createTable tableName=FED_USER_CR...', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('2.2.0', 'bburke@redhat.com', 'META-INF/jpa-changelog-2.2.0.xml', '2018-09-19 08:55:11.383481', 30, 'EXECUTED', '7:53188c3eb1107546e6f765835705b6c1', 'addColumn tableName=ADMIN_EVENT_ENTITY; createTable tableName=CREDENTIAL_ATTRIBUTE; createTable tableName=FED_CREDENTIAL_ATTRIBUTE; modifyDataType columnName=VALUE, tableName=CREDENTIAL; addForeignKeyConstraint baseTableName=FED_CREDENTIAL_ATTRIBU...', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('2.3.0', 'bburke@redhat.com', 'META-INF/jpa-changelog-2.3.0.xml', '2018-09-19 08:55:11.420325', 31, 'EXECUTED', '7:d6e6f3bc57a0c5586737d1351725d4d4', 'createTable tableName=FEDERATED_USER; addPrimaryKey constraintName=CONSTR_FEDERATED_USER, tableName=FEDERATED_USER; dropDefaultValue columnName=TOTP, tableName=USER_ENTITY; dropColumn columnName=TOTP, tableName=USER_ENTITY; addColumn tableName=IDE...', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('2.4.0', 'bburke@redhat.com', 'META-INF/jpa-changelog-2.4.0.xml', '2018-09-19 08:55:11.432603', 32, 'EXECUTED', '7:454d604fbd755d9df3fd9c6329043aa5', 'customChange', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('2.5.0', 'bburke@redhat.com', 'META-INF/jpa-changelog-2.5.0.xml', '2018-09-19 08:55:11.445274', 33, 'EXECUTED', '7:57e98a3077e29caf562f7dbf80c72600', 'customChange; modifyDataType columnName=USER_ID, tableName=OFFLINE_USER_SESSION', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('2.5.0-unicode-oracle', 'hmlnarik@redhat.com', 'META-INF/jpa-changelog-2.5.0.xml', '2018-09-19 08:55:11.447009', 34, 'MARK_RAN', '7:e4c7e8f2256210aee71ddc42f538b57a', 'modifyDataType columnName=DESCRIPTION, tableName=AUTHENTICATION_FLOW; modifyDataType columnName=DESCRIPTION, tableName=CLIENT_TEMPLATE; modifyDataType columnName=DESCRIPTION, tableName=RESOURCE_SERVER_POLICY; modifyDataType columnName=DESCRIPTION,...', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('2.5.0-unicode-other-dbs', 'hmlnarik@redhat.com', 'META-INF/jpa-changelog-2.5.0.xml', '2018-09-19 08:55:11.50594', 35, 'EXECUTED', '7:09a43c97e49bc626460480aa1379b522', 'modifyDataType columnName=DESCRIPTION, tableName=AUTHENTICATION_FLOW; modifyDataType columnName=DESCRIPTION, tableName=CLIENT_TEMPLATE; modifyDataType columnName=DESCRIPTION, tableName=RESOURCE_SERVER_POLICY; modifyDataType columnName=DESCRIPTION,...', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('2.5.0-duplicate-email-support', 'slawomir@dabek.name', 'META-INF/jpa-changelog-2.5.0.xml', '2018-09-19 08:55:11.592241', 36, 'EXECUTED', '7:26bfc7c74fefa9126f2ce702fb775553', 'addColumn tableName=REALM', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('2.5.0-unique-group-names', 'hmlnarik@redhat.com', 'META-INF/jpa-changelog-2.5.0.xml', '2018-09-19 08:55:11.602525', 37, 'EXECUTED', '7:a161e2ae671a9020fff61e996a207377', 'addUniqueConstraint constraintName=SIBLING_NAMES, tableName=KEYCLOAK_GROUP', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('2.5.1', 'bburke@redhat.com', 'META-INF/jpa-changelog-2.5.1.xml', '2018-09-19 08:55:11.605567', 38, 'EXECUTED', '7:37fc1781855ac5388c494f1442b3f717', 'addColumn tableName=FED_USER_CONSENT', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('3.0.0', 'bburke@redhat.com', 'META-INF/jpa-changelog-3.0.0.xml', '2018-09-19 08:55:11.644603', 39, 'EXECUTED', '7:13a27db0dae6049541136adad7261d27', 'addColumn tableName=IDENTITY_PROVIDER', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('3.2.0-fix', 'keycloak', 'META-INF/jpa-changelog-3.2.0.xml', '2018-09-19 08:55:11.646447', 40, 'MARK_RAN', '7:550300617e3b59e8af3a6294df8248a3', 'addNotNullConstraint columnName=REALM_ID, tableName=CLIENT_INITIAL_ACCESS', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('3.2.0-fix-with-keycloak-5416', 'keycloak', 'META-INF/jpa-changelog-3.2.0.xml', '2018-09-19 08:55:11.647881', 41, 'MARK_RAN', '7:e3a9482b8931481dc2772a5c07c44f17', 'dropIndex indexName=IDX_CLIENT_INIT_ACC_REALM, tableName=CLIENT_INITIAL_ACCESS; addNotNullConstraint columnName=REALM_ID, tableName=CLIENT_INITIAL_ACCESS; createIndex indexName=IDX_CLIENT_INIT_ACC_REALM, tableName=CLIENT_INITIAL_ACCESS', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('3.2.0-fixed', 'keycloak', 'META-INF/jpa-changelog-3.2.0.xml', '2018-09-19 08:55:12.032192', 42, 'EXECUTED', '7:a72a7858967bd414835d19e04d880312', 'addColumn tableName=REALM; dropPrimaryKey constraintName=CONSTRAINT_OFFL_CL_SES_PK2, tableName=OFFLINE_CLIENT_SESSION; dropColumn columnName=CLIENT_SESSION_ID, tableName=OFFLINE_CLIENT_SESSION; addPrimaryKey constraintName=CONSTRAINT_OFFL_CL_SES_P...', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('3.3.0', 'keycloak', 'META-INF/jpa-changelog-3.3.0.xml', '2018-09-19 08:55:12.111571', 43, 'EXECUTED', '7:94edff7cf9ce179e7e85f0cd78a3cf2c', 'addColumn tableName=USER_ENTITY', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('authz-3.4.0.CR1-resource-server-pk-change-part1', 'glavoie@gmail.com', 'META-INF/jpa-changelog-authz-3.4.0.CR1.xml', '2018-09-19 08:55:12.115751', 44, 'EXECUTED', '7:6a48ce645a3525488a90fbf76adf3bb3', 'addColumn tableName=RESOURCE_SERVER_POLICY; addColumn tableName=RESOURCE_SERVER_RESOURCE; addColumn tableName=RESOURCE_SERVER_SCOPE', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('authz-3.4.0.CR1-resource-server-pk-change-part3-fixed', 'glavoie@gmail.com', 'META-INF/jpa-changelog-authz-3.4.0.CR1.xml', '2018-09-19 08:55:12.130435', 46, 'MARK_RAN', '7:fd8cf02498f8b1e72496a20afc75178c', 'dropIndex indexName=IDX_RES_SERV_POL_RES_SERV, tableName=RESOURCE_SERVER_POLICY; dropIndex indexName=IDX_RES_SRV_RES_RES_SRV, tableName=RESOURCE_SERVER_RESOURCE; dropIndex indexName=IDX_RES_SRV_SCOPE_RES_SRV, tableName=RESOURCE_SERVER_SCOPE', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('authz-3.4.0.CR1-resource-server-pk-change-part3-fixed-nodropindex', 'glavoie@gmail.com', 'META-INF/jpa-changelog-authz-3.4.0.CR1.xml', '2018-09-19 08:55:12.247366', 47, 'EXECUTED', '7:542794f25aa2b1fbabb7e577d6646319', 'addNotNullConstraint columnName=RESOURCE_SERVER_CLIENT_ID, tableName=RESOURCE_SERVER_POLICY; addNotNullConstraint columnName=RESOURCE_SERVER_CLIENT_ID, tableName=RESOURCE_SERVER_RESOURCE; addNotNullConstraint columnName=RESOURCE_SERVER_CLIENT_ID, ...', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('authn-3.4.0.CR1-refresh-token-max-reuse', 'glavoie@gmail.com', 'META-INF/jpa-changelog-authz-3.4.0.CR1.xml', '2018-09-19 08:55:12.306333', 48, 'EXECUTED', '7:edad604c882df12f74941dac3cc6d650', 'addColumn tableName=REALM', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('3.4.0', 'keycloak', 'META-INF/jpa-changelog-3.4.0.xml', '2018-09-19 08:55:12.430924', 49, 'EXECUTED', '7:0f88b78b7b46480eb92690cbf5e44900', 'addPrimaryKey constraintName=CONSTRAINT_REALM_DEFAULT_ROLES, tableName=REALM_DEFAULT_ROLES; addPrimaryKey constraintName=CONSTRAINT_COMPOSITE_ROLE, tableName=COMPOSITE_ROLE; addPrimaryKey constraintName=CONSTR_REALM_DEFAULT_GROUPS, tableName=REALM...', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('3.4.0-KEYCLOAK-5230', 'hmlnarik@redhat.com', 'META-INF/jpa-changelog-3.4.0.xml', '2018-09-19 08:55:12.513605', 50, 'EXECUTED', '7:d560e43982611d936457c327f872dd59', 'createIndex indexName=IDX_FU_ATTRIBUTE, tableName=FED_USER_ATTRIBUTE; createIndex indexName=IDX_FU_CONSENT, tableName=FED_USER_CONSENT; createIndex indexName=IDX_FU_CONSENT_RU, tableName=FED_USER_CONSENT; createIndex indexName=IDX_FU_CREDENTIAL, t...', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('3.4.1', 'psilva@redhat.com', 'META-INF/jpa-changelog-3.4.1.xml', '2018-09-19 08:55:12.51761', 51, 'EXECUTED', '7:c155566c42b4d14ef07059ec3b3bbd8e', 'modifyDataType columnName=VALUE, tableName=CLIENT_ATTRIBUTES', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('3.4.2', 'keycloak', 'META-INF/jpa-changelog-3.4.2.xml', '2018-09-19 08:55:12.525853', 52, 'EXECUTED', '7:b40376581f12d70f3c89ba8ddf5b7dea', 'update tableName=REALM', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('3.4.2-KEYCLOAK-5172', 'mkanis@redhat.com', 'META-INF/jpa-changelog-3.4.2.xml', '2018-09-19 08:55:12.531379', 53, 'EXECUTED', '7:a1132cc395f7b95b3646146c2e38f168', 'update tableName=CLIENT', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('4.0.0-KEYCLOAK-6335', 'bburke@redhat.com', 'META-INF/jpa-changelog-4.0.0.xml', '2018-09-19 08:55:12.543212', 54, 'EXECUTED', '7:d8dc5d89c789105cfa7ca0e82cba60af', 'createTable tableName=CLIENT_AUTH_FLOW_BINDINGS; addPrimaryKey constraintName=C_CLI_FLOW_BIND, tableName=CLIENT_AUTH_FLOW_BINDINGS', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('4.0.0-CLEANUP-UNUSED-TABLE', 'bburke@redhat.com', 'META-INF/jpa-changelog-4.0.0.xml', '2018-09-19 08:55:12.565734', 55, 'EXECUTED', '7:7822e0165097182e8f653c35517656a3', 'dropTable tableName=CLIENT_IDENTITY_PROV_MAPPING', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('4.0.0-KEYCLOAK-6228', 'bburke@redhat.com', 'META-INF/jpa-changelog-4.0.0.xml', '2018-09-19 08:55:12.702796', 56, 'EXECUTED', '7:c6538c29b9c9a08f9e9ea2de5c2b6375', 'dropUniqueConstraint constraintName=UK_JKUWUVD56ONTGSUHOGM8UEWRT, tableName=USER_CONSENT; dropNotNullConstraint columnName=CLIENT_ID, tableName=USER_CONSENT; addColumn tableName=USER_CONSENT; addUniqueConstraint constraintName=UK_JKUWUVD56ONTGSUHO...', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('4.0.0-KEYCLOAK-5579', 'mposolda@redhat.com', 'META-INF/jpa-changelog-4.0.0.xml', '2018-09-19 08:55:13.02487', 57, 'EXECUTED', '7:c88da39534e99aba51cc89d09fd53936', 'dropForeignKeyConstraint baseTableName=CLIENT_TEMPLATE_ATTRIBUTES, constraintName=FK_CL_TEMPL_ATTR_TEMPL; renameTable newTableName=CLIENT_SCOPE_ATTRIBUTES, oldTableName=CLIENT_TEMPLATE_ATTRIBUTES; renameColumn newColumnName=SCOPE_ID, oldColumnName...', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('authz-4.0.0.CR1', 'psilva@redhat.com', 'META-INF/jpa-changelog-authz-4.0.0.CR1.xml', '2018-09-19 08:55:13.267332', 58, 'EXECUTED', '7:57960fc0b0f0dd0563ea6f8b2e4a1707', 'createTable tableName=RESOURCE_SERVER_PERM_TICKET; addPrimaryKey constraintName=CONSTRAINT_FAPMT, tableName=RESOURCE_SERVER_PERM_TICKET; addForeignKeyConstraint baseTableName=RESOURCE_SERVER_PERM_TICKET, constraintName=FK_FRSRHO213XCX4WNKOG82SSPMT...', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('authz-4.0.0.Beta3', 'psilva@redhat.com', 'META-INF/jpa-changelog-authz-4.0.0.Beta3.xml', '2018-09-19 08:55:13.299005', 59, 'EXECUTED', '7:2b4b8bff39944c7097977cc18dbceb3b', 'addColumn tableName=RESOURCE_SERVER_POLICY; addColumn tableName=RESOURCE_SERVER_PERM_TICKET; addForeignKeyConstraint baseTableName=RESOURCE_SERVER_PERM_TICKET, constraintName=FK_FRSRPO2128CX4WNKOG82SSRFY, referencedTableName=RESOURCE_SERVER_POLICY', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('authz-4.2.0.Final', 'mhajas@redhat.com', 'META-INF/jpa-changelog-authz-4.2.0.Final.xml', '2018-09-19 08:55:13.336152', 60, 'EXECUTED', '7:2aa42a964c59cd5b8ca9822340ba33a8', 'createTable tableName=RESOURCE_URIS; addForeignKeyConstraint baseTableName=RESOURCE_URIS, constraintName=FK_RESOURCE_SERVER_URIS, referencedTableName=RESOURCE_SERVER_RESOURCE; customChange; dropColumn columnName=URI, tableName=RESOURCE_SERVER_RESO...', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('4.2.0-KEYCLOAK-6313', 'wadahiro@gmail.com', 'META-INF/jpa-changelog-4.2.0.xml', '2018-09-19 08:55:13.339331', 61, 'EXECUTED', '7:14d407c35bc4fe1976867756bcea0c36', 'addColumn tableName=REQUIRED_ACTION_PROVIDER', '', NULL, '3.5.4', NULL, NULL, '7347305366');
INSERT INTO public.databasechangelog VALUES ('4.3.0-KEYCLOAK-7984', 'wadahiro@gmail.com', 'META-INF/jpa-changelog-4.3.0.xml', '2018-09-19 08:55:13.346775', 62, 'EXECUTED', '7:241a8030c748c8548e346adee548fa93', 'update tableName=REQUIRED_ACTION_PROVIDER', '', NULL, '3.5.4', NULL, NULL, '7347305366');


--
-- TOC entry 3727 (class 0 OID 16386)
-- Dependencies: 196
-- Data for Name: databasechangeloglock; Type: TABLE DATA; Schema: public; Owner: keycloak
--

INSERT INTO public.databasechangeloglock VALUES (1, false, NULL, NULL);


--
-- TOC entry 3815 (class 0 OID 18183)
-- Dependencies: 284
-- Data for Name: default_client_scope; Type: TABLE DATA; Schema: public; Owner: keycloak
--

INSERT INTO public.default_client_scope VALUES ('master', '48e40558-b38c-45a0-99ad-768e5274205d', false);
INSERT INTO public.default_client_scope VALUES ('master', 'e909616e-c008-4512-9423-3776f068fd6f', true);
INSERT INTO public.default_client_scope VALUES ('master', '56d81fbe-ee25-498a-8d46-753f683023d0', true);
INSERT INTO public.default_client_scope VALUES ('master', '8d25d33e-9811-4ef6-9bd8-7a011f9718db', true);
INSERT INTO public.default_client_scope VALUES ('master', '6866d376-ef81-436c-9c18-ab29c139d539', false);
INSERT INTO public.default_client_scope VALUES ('master', '678b7854-acc8-4bc9-9d2c-d161dbac7294', false);


--
-- TOC entry 3735 (class 0 OID 16430)
-- Dependencies: 204
-- Data for Name: event_entity; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3810 (class 0 OID 17901)
-- Dependencies: 279
-- Data for Name: fed_credential_attribute; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3801 (class 0 OID 17813)
-- Dependencies: 270
-- Data for Name: fed_user_attribute; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3802 (class 0 OID 17819)
-- Dependencies: 271
-- Data for Name: fed_user_consent; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3817 (class 0 OID 18209)
-- Dependencies: 286
-- Data for Name: fed_user_consent_cl_scope; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3803 (class 0 OID 17828)
-- Dependencies: 272
-- Data for Name: fed_user_credential; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3804 (class 0 OID 17838)
-- Dependencies: 273
-- Data for Name: fed_user_group_membership; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3805 (class 0 OID 17841)
-- Dependencies: 274
-- Data for Name: fed_user_required_action; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3806 (class 0 OID 17848)
-- Dependencies: 275
-- Data for Name: fed_user_role_mapping; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3759 (class 0 OID 16832)
-- Dependencies: 228
-- Data for Name: federated_identity; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3811 (class 0 OID 17917)
-- Dependencies: 280
-- Data for Name: federated_user; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3785 (class 0 OID 17471)
-- Dependencies: 254
-- Data for Name: group_attribute; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3784 (class 0 OID 17468)
-- Dependencies: 253
-- Data for Name: group_role_mapping; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3760 (class 0 OID 16838)
-- Dependencies: 229
-- Data for Name: identity_provider; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3761 (class 0 OID 16848)
-- Dependencies: 230
-- Data for Name: identity_provider_config; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3766 (class 0 OID 16978)
-- Dependencies: 235
-- Data for Name: identity_provider_mapper; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3767 (class 0 OID 16984)
-- Dependencies: 236
-- Data for Name: idp_mapper_config; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3783 (class 0 OID 17465)
-- Dependencies: 252
-- Data for Name: keycloak_group; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3736 (class 0 OID 16439)
-- Dependencies: 205
-- Data for Name: keycloak_role; Type: TABLE DATA; Schema: public; Owner: keycloak
--

INSERT INTO public.keycloak_role VALUES ('186d3a91-8920-4fde-93b6-d98226e2e13f', 'master', false, '${role_admin}', 'admin', 'master', NULL, 'master');
INSERT INTO public.keycloak_role VALUES ('9f714ee2-d21d-4e49-8145-775d077c24b6', 'master', false, '${role_create-realm}', 'create-realm', 'master', NULL, 'master');
INSERT INTO public.keycloak_role VALUES ('9565e1bb-8351-4f1b-9194-b519a88aa17f', 'cd5df352-ee3f-41ff-8a25-7ddfb4d39455', true, '${role_create-client}', 'create-client', 'master', 'cd5df352-ee3f-41ff-8a25-7ddfb4d39455', NULL);
INSERT INTO public.keycloak_role VALUES ('357f7f68-664e-455a-9508-86dcf381be68', 'cd5df352-ee3f-41ff-8a25-7ddfb4d39455', true, '${role_view-realm}', 'view-realm', 'master', 'cd5df352-ee3f-41ff-8a25-7ddfb4d39455', NULL);
INSERT INTO public.keycloak_role VALUES ('06944daa-0da3-4c40-b475-0aacf9cba357', 'cd5df352-ee3f-41ff-8a25-7ddfb4d39455', true, '${role_view-users}', 'view-users', 'master', 'cd5df352-ee3f-41ff-8a25-7ddfb4d39455', NULL);
INSERT INTO public.keycloak_role VALUES ('fb26b091-9eee-4a5b-9d91-5980f8fd23d7', 'cd5df352-ee3f-41ff-8a25-7ddfb4d39455', true, '${role_view-clients}', 'view-clients', 'master', 'cd5df352-ee3f-41ff-8a25-7ddfb4d39455', NULL);
INSERT INTO public.keycloak_role VALUES ('fc7f3c4a-e702-470e-91fc-5166135a63a9', 'cd5df352-ee3f-41ff-8a25-7ddfb4d39455', true, '${role_view-events}', 'view-events', 'master', 'cd5df352-ee3f-41ff-8a25-7ddfb4d39455', NULL);
INSERT INTO public.keycloak_role VALUES ('105806c5-f58b-40a7-9f82-0a964742d222', 'cd5df352-ee3f-41ff-8a25-7ddfb4d39455', true, '${role_view-identity-providers}', 'view-identity-providers', 'master', 'cd5df352-ee3f-41ff-8a25-7ddfb4d39455', NULL);
INSERT INTO public.keycloak_role VALUES ('0582be99-3a7e-47f0-9104-740f3472e1d4', 'cd5df352-ee3f-41ff-8a25-7ddfb4d39455', true, '${role_view-authorization}', 'view-authorization', 'master', 'cd5df352-ee3f-41ff-8a25-7ddfb4d39455', NULL);
INSERT INTO public.keycloak_role VALUES ('ca5a428a-5505-452d-a366-2f9d780335e2', 'cd5df352-ee3f-41ff-8a25-7ddfb4d39455', true, '${role_manage-realm}', 'manage-realm', 'master', 'cd5df352-ee3f-41ff-8a25-7ddfb4d39455', NULL);
INSERT INTO public.keycloak_role VALUES ('36c6f320-51b6-4e99-96e0-356dbb1b5ac3', 'cd5df352-ee3f-41ff-8a25-7ddfb4d39455', true, '${role_manage-users}', 'manage-users', 'master', 'cd5df352-ee3f-41ff-8a25-7ddfb4d39455', NULL);
INSERT INTO public.keycloak_role VALUES ('806c5df3-e925-4df0-9137-401adcc1935a', 'cd5df352-ee3f-41ff-8a25-7ddfb4d39455', true, '${role_manage-clients}', 'manage-clients', 'master', 'cd5df352-ee3f-41ff-8a25-7ddfb4d39455', NULL);
INSERT INTO public.keycloak_role VALUES ('6283833b-f5b0-43c7-8f5f-af461008159a', 'cd5df352-ee3f-41ff-8a25-7ddfb4d39455', true, '${role_manage-events}', 'manage-events', 'master', 'cd5df352-ee3f-41ff-8a25-7ddfb4d39455', NULL);
INSERT INTO public.keycloak_role VALUES ('a6994cca-e8e5-461c-8d03-6a73232b5a2d', 'cd5df352-ee3f-41ff-8a25-7ddfb4d39455', true, '${role_manage-identity-providers}', 'manage-identity-providers', 'master', 'cd5df352-ee3f-41ff-8a25-7ddfb4d39455', NULL);
INSERT INTO public.keycloak_role VALUES ('e643acbb-ccb0-4486-bad2-09c157fefb4f', 'cd5df352-ee3f-41ff-8a25-7ddfb4d39455', true, '${role_manage-authorization}', 'manage-authorization', 'master', 'cd5df352-ee3f-41ff-8a25-7ddfb4d39455', NULL);
INSERT INTO public.keycloak_role VALUES ('a938128b-c816-43cb-b594-40a8ceb1e8fd', 'cd5df352-ee3f-41ff-8a25-7ddfb4d39455', true, '${role_query-users}', 'query-users', 'master', 'cd5df352-ee3f-41ff-8a25-7ddfb4d39455', NULL);
INSERT INTO public.keycloak_role VALUES ('2c732570-7691-4acd-a118-8952669777db', 'cd5df352-ee3f-41ff-8a25-7ddfb4d39455', true, '${role_query-clients}', 'query-clients', 'master', 'cd5df352-ee3f-41ff-8a25-7ddfb4d39455', NULL);
INSERT INTO public.keycloak_role VALUES ('5242b3e3-ee56-42d5-aa65-0654a8655303', 'cd5df352-ee3f-41ff-8a25-7ddfb4d39455', true, '${role_query-realms}', 'query-realms', 'master', 'cd5df352-ee3f-41ff-8a25-7ddfb4d39455', NULL);
INSERT INTO public.keycloak_role VALUES ('3b6c844d-cb84-4f10-a32a-9a469b35da5d', 'cd5df352-ee3f-41ff-8a25-7ddfb4d39455', true, '${role_query-groups}', 'query-groups', 'master', 'cd5df352-ee3f-41ff-8a25-7ddfb4d39455', NULL);
INSERT INTO public.keycloak_role VALUES ('3cc4f4bf-e857-4b70-a86f-3230b7b89dd2', '1b950850-25cf-4739-8214-3014b9380d49', true, '${role_view-profile}', 'view-profile', 'master', '1b950850-25cf-4739-8214-3014b9380d49', NULL);
INSERT INTO public.keycloak_role VALUES ('de5b13b1-7ddc-4032-82ac-ce0e7b6aabcf', '1b950850-25cf-4739-8214-3014b9380d49', true, '${role_manage-account}', 'manage-account', 'master', '1b950850-25cf-4739-8214-3014b9380d49', NULL);
INSERT INTO public.keycloak_role VALUES ('03982a16-772c-48f6-8b5b-9629397e5729', '1b950850-25cf-4739-8214-3014b9380d49', true, '${role_manage-account-links}', 'manage-account-links', 'master', '1b950850-25cf-4739-8214-3014b9380d49', NULL);
INSERT INTO public.keycloak_role VALUES ('12ecb6c3-b436-4aac-b529-6c1f0a248554', 'e26c74c2-d698-42ca-bdff-27a2c5b7bbc6', true, '${role_read-token}', 'read-token', 'master', 'e26c74c2-d698-42ca-bdff-27a2c5b7bbc6', NULL);
INSERT INTO public.keycloak_role VALUES ('68317bfc-336c-4ecb-84b9-636c0423fa01', 'cd5df352-ee3f-41ff-8a25-7ddfb4d39455', true, '${role_impersonation}', 'impersonation', 'master', 'cd5df352-ee3f-41ff-8a25-7ddfb4d39455', NULL);
INSERT INTO public.keycloak_role VALUES ('5603901b-9d26-49b6-8de2-e8a632c2a55b', 'master', false, '${role_offline-access}', 'offline_access', 'master', NULL, 'master');
INSERT INTO public.keycloak_role VALUES ('90731e77-9365-4533-af58-ade179850ba3', 'master', false, '${role_uma_authorization}', 'uma_authorization', 'master', NULL, 'master');
INSERT INTO public.keycloak_role VALUES ('790ff8d2-bb9e-4b99-a7b0-9e5fcb2b474d', '8c7f88d9-5059-4e59-9827-85a76634e8e9', true, NULL, 'uma_protection', 'master', '8c7f88d9-5059-4e59-9827-85a76634e8e9', NULL);


--
-- TOC entry 3765 (class 0 OID 16975)
-- Dependencies: 234
-- Data for Name: migration_model; Type: TABLE DATA; Schema: public; Owner: keycloak
--

INSERT INTO public.migration_model VALUES ('SINGLETON', '4.2.0');


--
-- TOC entry 3782 (class 0 OID 17455)
-- Dependencies: 251
-- Data for Name: offline_client_session; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3781 (class 0 OID 17449)
-- Dependencies: 250
-- Data for Name: offline_user_session; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3795 (class 0 OID 17734)
-- Dependencies: 264
-- Data for Name: policy_config; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3757 (class 0 OID 16819)
-- Dependencies: 226
-- Data for Name: protocol_mapper; Type: TABLE DATA; Schema: public; Owner: keycloak
--

INSERT INTO public.protocol_mapper VALUES ('22033bf5-7093-452c-9ccf-0b19337f906d', 'locale', 'openid-connect', 'oidc-usermodel-attribute-mapper', '8662309a-9b69-48e4-b315-fa9fb9d07e94', NULL);
INSERT INTO public.protocol_mapper VALUES ('0d4fdcbf-bb90-40ce-8acd-d1dc6be84864', 'role list', 'saml', 'saml-role-list-mapper', NULL, 'e909616e-c008-4512-9423-3776f068fd6f');
INSERT INTO public.protocol_mapper VALUES ('ec8b80a0-27ea-4269-83a0-6994f21fc69d', 'full name', 'openid-connect', 'oidc-full-name-mapper', NULL, '56d81fbe-ee25-498a-8d46-753f683023d0');
INSERT INTO public.protocol_mapper VALUES ('a0960053-f28f-49c8-a1d8-7a7749ccf46d', 'family name', 'openid-connect', 'oidc-usermodel-property-mapper', NULL, '56d81fbe-ee25-498a-8d46-753f683023d0');
INSERT INTO public.protocol_mapper VALUES ('fe4a23c9-516c-4a1a-ae02-3a5e5dc6c4cf', 'given name', 'openid-connect', 'oidc-usermodel-property-mapper', NULL, '56d81fbe-ee25-498a-8d46-753f683023d0');
INSERT INTO public.protocol_mapper VALUES ('a54bc79e-49a5-4dfd-b08d-e307f23c0e26', 'middle name', 'openid-connect', 'oidc-usermodel-attribute-mapper', NULL, '56d81fbe-ee25-498a-8d46-753f683023d0');
INSERT INTO public.protocol_mapper VALUES ('349e829d-69b5-4802-8b4c-92f616ba9844', 'nickname', 'openid-connect', 'oidc-usermodel-attribute-mapper', NULL, '56d81fbe-ee25-498a-8d46-753f683023d0');
INSERT INTO public.protocol_mapper VALUES ('286086ad-3070-4352-8845-4b24d0101357', 'username', 'openid-connect', 'oidc-usermodel-property-mapper', NULL, '56d81fbe-ee25-498a-8d46-753f683023d0');
INSERT INTO public.protocol_mapper VALUES ('63095e73-5056-4cf1-b2fd-35d97f7e34b8', 'profile', 'openid-connect', 'oidc-usermodel-attribute-mapper', NULL, '56d81fbe-ee25-498a-8d46-753f683023d0');
INSERT INTO public.protocol_mapper VALUES ('2fddd51c-a170-450b-92d0-2b60997ea744', 'picture', 'openid-connect', 'oidc-usermodel-attribute-mapper', NULL, '56d81fbe-ee25-498a-8d46-753f683023d0');
INSERT INTO public.protocol_mapper VALUES ('a4524f6c-1a7f-4199-b242-3c22eb78bc07', 'website', 'openid-connect', 'oidc-usermodel-attribute-mapper', NULL, '56d81fbe-ee25-498a-8d46-753f683023d0');
INSERT INTO public.protocol_mapper VALUES ('49d7ac22-9470-484a-9546-97c6d153f9f2', 'gender', 'openid-connect', 'oidc-usermodel-attribute-mapper', NULL, '56d81fbe-ee25-498a-8d46-753f683023d0');
INSERT INTO public.protocol_mapper VALUES ('e695ad08-2b4b-4cbf-a7cc-bf98f44fb852', 'birthdate', 'openid-connect', 'oidc-usermodel-attribute-mapper', NULL, '56d81fbe-ee25-498a-8d46-753f683023d0');
INSERT INTO public.protocol_mapper VALUES ('02d2447d-32f1-4721-80d9-6943827667bf', 'zoneinfo', 'openid-connect', 'oidc-usermodel-attribute-mapper', NULL, '56d81fbe-ee25-498a-8d46-753f683023d0');
INSERT INTO public.protocol_mapper VALUES ('97623b29-d236-4938-ab40-93f50c9820df', 'locale', 'openid-connect', 'oidc-usermodel-attribute-mapper', NULL, '56d81fbe-ee25-498a-8d46-753f683023d0');
INSERT INTO public.protocol_mapper VALUES ('9e2f2a31-d6df-43d5-a653-ef88b49347f8', 'updated at', 'openid-connect', 'oidc-usermodel-attribute-mapper', NULL, '56d81fbe-ee25-498a-8d46-753f683023d0');
INSERT INTO public.protocol_mapper VALUES ('6c494a0b-9ac0-45e6-907e-92ac8685afef', 'email', 'openid-connect', 'oidc-usermodel-property-mapper', NULL, '8d25d33e-9811-4ef6-9bd8-7a011f9718db');
INSERT INTO public.protocol_mapper VALUES ('adae4d09-7333-43b9-beb4-4b9b572ea0ea', 'email verified', 'openid-connect', 'oidc-usermodel-property-mapper', NULL, '8d25d33e-9811-4ef6-9bd8-7a011f9718db');
INSERT INTO public.protocol_mapper VALUES ('530eaea5-ae25-4f04-8315-7b404cbfc0f5', 'address', 'openid-connect', 'oidc-address-mapper', NULL, '6866d376-ef81-436c-9c18-ab29c139d539');
INSERT INTO public.protocol_mapper VALUES ('07a9d2a5-22cf-425d-a93c-c1964d2b042d', 'phone number', 'openid-connect', 'oidc-usermodel-attribute-mapper', NULL, '678b7854-acc8-4bc9-9d2c-d161dbac7294');
INSERT INTO public.protocol_mapper VALUES ('c04b6530-951b-4392-8496-df4e83638488', 'phone number verified', 'openid-connect', 'oidc-usermodel-attribute-mapper', NULL, '678b7854-acc8-4bc9-9d2c-d161dbac7294');
INSERT INTO public.protocol_mapper VALUES ('02f8c79e-7ac0-4917-86ed-32eb15f8b7ab', 'Client ID', 'openid-connect', 'oidc-usersessionmodel-note-mapper', '8c7f88d9-5059-4e59-9827-85a76634e8e9', NULL);
INSERT INTO public.protocol_mapper VALUES ('f5fea138-c6e0-4cd5-88ab-8ddd6f548b12', 'Client Host', 'openid-connect', 'oidc-usersessionmodel-note-mapper', '8c7f88d9-5059-4e59-9827-85a76634e8e9', NULL);
INSERT INTO public.protocol_mapper VALUES ('6400120f-3590-4ee9-bcf5-29c84d0694e6', 'Client IP Address', 'openid-connect', 'oidc-usersessionmodel-note-mapper', '8c7f88d9-5059-4e59-9827-85a76634e8e9', NULL);
INSERT INTO public.protocol_mapper VALUES ('01736768-6fda-4dc8-a09e-51b5b75da967', 'otoroshi meta', 'openid-connect', 'oidc-usermodel-attribute-mapper', '8c7f88d9-5059-4e59-9827-85a76634e8e9', NULL);


--
-- TOC entry 3758 (class 0 OID 16826)
-- Dependencies: 227
-- Data for Name: protocol_mapper_config; Type: TABLE DATA; Schema: public; Owner: keycloak
--

INSERT INTO public.protocol_mapper_config VALUES ('22033bf5-7093-452c-9ccf-0b19337f906d', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('22033bf5-7093-452c-9ccf-0b19337f906d', 'locale', 'user.attribute');
INSERT INTO public.protocol_mapper_config VALUES ('22033bf5-7093-452c-9ccf-0b19337f906d', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('22033bf5-7093-452c-9ccf-0b19337f906d', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('22033bf5-7093-452c-9ccf-0b19337f906d', 'locale', 'claim.name');
INSERT INTO public.protocol_mapper_config VALUES ('22033bf5-7093-452c-9ccf-0b19337f906d', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config VALUES ('0d4fdcbf-bb90-40ce-8acd-d1dc6be84864', 'false', 'single');
INSERT INTO public.protocol_mapper_config VALUES ('0d4fdcbf-bb90-40ce-8acd-d1dc6be84864', 'Basic', 'attribute.nameformat');
INSERT INTO public.protocol_mapper_config VALUES ('0d4fdcbf-bb90-40ce-8acd-d1dc6be84864', 'Role', 'attribute.name');
INSERT INTO public.protocol_mapper_config VALUES ('ec8b80a0-27ea-4269-83a0-6994f21fc69d', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('ec8b80a0-27ea-4269-83a0-6994f21fc69d', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('ec8b80a0-27ea-4269-83a0-6994f21fc69d', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('a0960053-f28f-49c8-a1d8-7a7749ccf46d', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('a0960053-f28f-49c8-a1d8-7a7749ccf46d', 'lastName', 'user.attribute');
INSERT INTO public.protocol_mapper_config VALUES ('a0960053-f28f-49c8-a1d8-7a7749ccf46d', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('a0960053-f28f-49c8-a1d8-7a7749ccf46d', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('a0960053-f28f-49c8-a1d8-7a7749ccf46d', 'family_name', 'claim.name');
INSERT INTO public.protocol_mapper_config VALUES ('a0960053-f28f-49c8-a1d8-7a7749ccf46d', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config VALUES ('fe4a23c9-516c-4a1a-ae02-3a5e5dc6c4cf', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('fe4a23c9-516c-4a1a-ae02-3a5e5dc6c4cf', 'firstName', 'user.attribute');
INSERT INTO public.protocol_mapper_config VALUES ('fe4a23c9-516c-4a1a-ae02-3a5e5dc6c4cf', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('fe4a23c9-516c-4a1a-ae02-3a5e5dc6c4cf', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('fe4a23c9-516c-4a1a-ae02-3a5e5dc6c4cf', 'given_name', 'claim.name');
INSERT INTO public.protocol_mapper_config VALUES ('fe4a23c9-516c-4a1a-ae02-3a5e5dc6c4cf', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config VALUES ('a54bc79e-49a5-4dfd-b08d-e307f23c0e26', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('a54bc79e-49a5-4dfd-b08d-e307f23c0e26', 'middleName', 'user.attribute');
INSERT INTO public.protocol_mapper_config VALUES ('a54bc79e-49a5-4dfd-b08d-e307f23c0e26', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('a54bc79e-49a5-4dfd-b08d-e307f23c0e26', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('a54bc79e-49a5-4dfd-b08d-e307f23c0e26', 'middle_name', 'claim.name');
INSERT INTO public.protocol_mapper_config VALUES ('a54bc79e-49a5-4dfd-b08d-e307f23c0e26', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config VALUES ('349e829d-69b5-4802-8b4c-92f616ba9844', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('349e829d-69b5-4802-8b4c-92f616ba9844', 'nickname', 'user.attribute');
INSERT INTO public.protocol_mapper_config VALUES ('349e829d-69b5-4802-8b4c-92f616ba9844', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('349e829d-69b5-4802-8b4c-92f616ba9844', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('349e829d-69b5-4802-8b4c-92f616ba9844', 'nickname', 'claim.name');
INSERT INTO public.protocol_mapper_config VALUES ('349e829d-69b5-4802-8b4c-92f616ba9844', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config VALUES ('286086ad-3070-4352-8845-4b24d0101357', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('286086ad-3070-4352-8845-4b24d0101357', 'username', 'user.attribute');
INSERT INTO public.protocol_mapper_config VALUES ('286086ad-3070-4352-8845-4b24d0101357', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('286086ad-3070-4352-8845-4b24d0101357', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('286086ad-3070-4352-8845-4b24d0101357', 'preferred_username', 'claim.name');
INSERT INTO public.protocol_mapper_config VALUES ('286086ad-3070-4352-8845-4b24d0101357', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config VALUES ('63095e73-5056-4cf1-b2fd-35d97f7e34b8', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('63095e73-5056-4cf1-b2fd-35d97f7e34b8', 'profile', 'user.attribute');
INSERT INTO public.protocol_mapper_config VALUES ('63095e73-5056-4cf1-b2fd-35d97f7e34b8', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('63095e73-5056-4cf1-b2fd-35d97f7e34b8', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('63095e73-5056-4cf1-b2fd-35d97f7e34b8', 'profile', 'claim.name');
INSERT INTO public.protocol_mapper_config VALUES ('63095e73-5056-4cf1-b2fd-35d97f7e34b8', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config VALUES ('2fddd51c-a170-450b-92d0-2b60997ea744', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('2fddd51c-a170-450b-92d0-2b60997ea744', 'picture', 'user.attribute');
INSERT INTO public.protocol_mapper_config VALUES ('2fddd51c-a170-450b-92d0-2b60997ea744', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('2fddd51c-a170-450b-92d0-2b60997ea744', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('2fddd51c-a170-450b-92d0-2b60997ea744', 'picture', 'claim.name');
INSERT INTO public.protocol_mapper_config VALUES ('2fddd51c-a170-450b-92d0-2b60997ea744', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config VALUES ('a4524f6c-1a7f-4199-b242-3c22eb78bc07', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('a4524f6c-1a7f-4199-b242-3c22eb78bc07', 'website', 'user.attribute');
INSERT INTO public.protocol_mapper_config VALUES ('a4524f6c-1a7f-4199-b242-3c22eb78bc07', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('a4524f6c-1a7f-4199-b242-3c22eb78bc07', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('a4524f6c-1a7f-4199-b242-3c22eb78bc07', 'website', 'claim.name');
INSERT INTO public.protocol_mapper_config VALUES ('a4524f6c-1a7f-4199-b242-3c22eb78bc07', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config VALUES ('49d7ac22-9470-484a-9546-97c6d153f9f2', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('49d7ac22-9470-484a-9546-97c6d153f9f2', 'gender', 'user.attribute');
INSERT INTO public.protocol_mapper_config VALUES ('49d7ac22-9470-484a-9546-97c6d153f9f2', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('49d7ac22-9470-484a-9546-97c6d153f9f2', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('49d7ac22-9470-484a-9546-97c6d153f9f2', 'gender', 'claim.name');
INSERT INTO public.protocol_mapper_config VALUES ('49d7ac22-9470-484a-9546-97c6d153f9f2', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config VALUES ('e695ad08-2b4b-4cbf-a7cc-bf98f44fb852', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('e695ad08-2b4b-4cbf-a7cc-bf98f44fb852', 'birthdate', 'user.attribute');
INSERT INTO public.protocol_mapper_config VALUES ('e695ad08-2b4b-4cbf-a7cc-bf98f44fb852', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('e695ad08-2b4b-4cbf-a7cc-bf98f44fb852', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('e695ad08-2b4b-4cbf-a7cc-bf98f44fb852', 'birthdate', 'claim.name');
INSERT INTO public.protocol_mapper_config VALUES ('e695ad08-2b4b-4cbf-a7cc-bf98f44fb852', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config VALUES ('02d2447d-32f1-4721-80d9-6943827667bf', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('02d2447d-32f1-4721-80d9-6943827667bf', 'zoneinfo', 'user.attribute');
INSERT INTO public.protocol_mapper_config VALUES ('02d2447d-32f1-4721-80d9-6943827667bf', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('02d2447d-32f1-4721-80d9-6943827667bf', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('02d2447d-32f1-4721-80d9-6943827667bf', 'zoneinfo', 'claim.name');
INSERT INTO public.protocol_mapper_config VALUES ('02d2447d-32f1-4721-80d9-6943827667bf', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config VALUES ('97623b29-d236-4938-ab40-93f50c9820df', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('97623b29-d236-4938-ab40-93f50c9820df', 'locale', 'user.attribute');
INSERT INTO public.protocol_mapper_config VALUES ('97623b29-d236-4938-ab40-93f50c9820df', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('97623b29-d236-4938-ab40-93f50c9820df', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('97623b29-d236-4938-ab40-93f50c9820df', 'locale', 'claim.name');
INSERT INTO public.protocol_mapper_config VALUES ('97623b29-d236-4938-ab40-93f50c9820df', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config VALUES ('9e2f2a31-d6df-43d5-a653-ef88b49347f8', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('9e2f2a31-d6df-43d5-a653-ef88b49347f8', 'updatedAt', 'user.attribute');
INSERT INTO public.protocol_mapper_config VALUES ('9e2f2a31-d6df-43d5-a653-ef88b49347f8', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('9e2f2a31-d6df-43d5-a653-ef88b49347f8', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('9e2f2a31-d6df-43d5-a653-ef88b49347f8', 'updated_at', 'claim.name');
INSERT INTO public.protocol_mapper_config VALUES ('9e2f2a31-d6df-43d5-a653-ef88b49347f8', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config VALUES ('6c494a0b-9ac0-45e6-907e-92ac8685afef', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('6c494a0b-9ac0-45e6-907e-92ac8685afef', 'email', 'user.attribute');
INSERT INTO public.protocol_mapper_config VALUES ('6c494a0b-9ac0-45e6-907e-92ac8685afef', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('6c494a0b-9ac0-45e6-907e-92ac8685afef', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('6c494a0b-9ac0-45e6-907e-92ac8685afef', 'email', 'claim.name');
INSERT INTO public.protocol_mapper_config VALUES ('6c494a0b-9ac0-45e6-907e-92ac8685afef', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config VALUES ('adae4d09-7333-43b9-beb4-4b9b572ea0ea', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('adae4d09-7333-43b9-beb4-4b9b572ea0ea', 'emailVerified', 'user.attribute');
INSERT INTO public.protocol_mapper_config VALUES ('adae4d09-7333-43b9-beb4-4b9b572ea0ea', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('adae4d09-7333-43b9-beb4-4b9b572ea0ea', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('adae4d09-7333-43b9-beb4-4b9b572ea0ea', 'email_verified', 'claim.name');
INSERT INTO public.protocol_mapper_config VALUES ('adae4d09-7333-43b9-beb4-4b9b572ea0ea', 'boolean', 'jsonType.label');
INSERT INTO public.protocol_mapper_config VALUES ('530eaea5-ae25-4f04-8315-7b404cbfc0f5', 'formatted', 'user.attribute.formatted');
INSERT INTO public.protocol_mapper_config VALUES ('530eaea5-ae25-4f04-8315-7b404cbfc0f5', 'country', 'user.attribute.country');
INSERT INTO public.protocol_mapper_config VALUES ('530eaea5-ae25-4f04-8315-7b404cbfc0f5', 'postal_code', 'user.attribute.postal_code');
INSERT INTO public.protocol_mapper_config VALUES ('530eaea5-ae25-4f04-8315-7b404cbfc0f5', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('530eaea5-ae25-4f04-8315-7b404cbfc0f5', 'street', 'user.attribute.street');
INSERT INTO public.protocol_mapper_config VALUES ('530eaea5-ae25-4f04-8315-7b404cbfc0f5', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('530eaea5-ae25-4f04-8315-7b404cbfc0f5', 'region', 'user.attribute.region');
INSERT INTO public.protocol_mapper_config VALUES ('530eaea5-ae25-4f04-8315-7b404cbfc0f5', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('530eaea5-ae25-4f04-8315-7b404cbfc0f5', 'locality', 'user.attribute.locality');
INSERT INTO public.protocol_mapper_config VALUES ('07a9d2a5-22cf-425d-a93c-c1964d2b042d', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('07a9d2a5-22cf-425d-a93c-c1964d2b042d', 'phoneNumber', 'user.attribute');
INSERT INTO public.protocol_mapper_config VALUES ('07a9d2a5-22cf-425d-a93c-c1964d2b042d', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('07a9d2a5-22cf-425d-a93c-c1964d2b042d', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('07a9d2a5-22cf-425d-a93c-c1964d2b042d', 'phone_number', 'claim.name');
INSERT INTO public.protocol_mapper_config VALUES ('07a9d2a5-22cf-425d-a93c-c1964d2b042d', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config VALUES ('c04b6530-951b-4392-8496-df4e83638488', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('c04b6530-951b-4392-8496-df4e83638488', 'phoneNumberVerified', 'user.attribute');
INSERT INTO public.protocol_mapper_config VALUES ('c04b6530-951b-4392-8496-df4e83638488', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('c04b6530-951b-4392-8496-df4e83638488', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('c04b6530-951b-4392-8496-df4e83638488', 'phone_number_verified', 'claim.name');
INSERT INTO public.protocol_mapper_config VALUES ('c04b6530-951b-4392-8496-df4e83638488', 'boolean', 'jsonType.label');
INSERT INTO public.protocol_mapper_config VALUES ('02f8c79e-7ac0-4917-86ed-32eb15f8b7ab', 'clientId', 'user.session.note');
INSERT INTO public.protocol_mapper_config VALUES ('02f8c79e-7ac0-4917-86ed-32eb15f8b7ab', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('02f8c79e-7ac0-4917-86ed-32eb15f8b7ab', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('02f8c79e-7ac0-4917-86ed-32eb15f8b7ab', 'clientId', 'claim.name');
INSERT INTO public.protocol_mapper_config VALUES ('02f8c79e-7ac0-4917-86ed-32eb15f8b7ab', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config VALUES ('f5fea138-c6e0-4cd5-88ab-8ddd6f548b12', 'clientHost', 'user.session.note');
INSERT INTO public.protocol_mapper_config VALUES ('f5fea138-c6e0-4cd5-88ab-8ddd6f548b12', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('f5fea138-c6e0-4cd5-88ab-8ddd6f548b12', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('f5fea138-c6e0-4cd5-88ab-8ddd6f548b12', 'clientHost', 'claim.name');
INSERT INTO public.protocol_mapper_config VALUES ('f5fea138-c6e0-4cd5-88ab-8ddd6f548b12', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config VALUES ('6400120f-3590-4ee9-bcf5-29c84d0694e6', 'clientAddress', 'user.session.note');
INSERT INTO public.protocol_mapper_config VALUES ('6400120f-3590-4ee9-bcf5-29c84d0694e6', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('6400120f-3590-4ee9-bcf5-29c84d0694e6', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('6400120f-3590-4ee9-bcf5-29c84d0694e6', 'clientAddress', 'claim.name');
INSERT INTO public.protocol_mapper_config VALUES ('6400120f-3590-4ee9-bcf5-29c84d0694e6', 'String', 'jsonType.label');
INSERT INTO public.protocol_mapper_config VALUES ('01736768-6fda-4dc8-a09e-51b5b75da967', 'true', 'userinfo.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('01736768-6fda-4dc8-a09e-51b5b75da967', 'meta', 'user.attribute');
INSERT INTO public.protocol_mapper_config VALUES ('01736768-6fda-4dc8-a09e-51b5b75da967', 'true', 'id.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('01736768-6fda-4dc8-a09e-51b5b75da967', 'true', 'access.token.claim');
INSERT INTO public.protocol_mapper_config VALUES ('01736768-6fda-4dc8-a09e-51b5b75da967', 'app_metadata.otoroshi_data', 'claim.name');
INSERT INTO public.protocol_mapper_config VALUES ('01736768-6fda-4dc8-a09e-51b5b75da967', 'String', 'jsonType.label');


--
-- TOC entry 3737 (class 0 OID 16446)
-- Dependencies: 206
-- Data for Name: realm; Type: TABLE DATA; Schema: public; Owner: keycloak
--

INSERT INTO public.realm VALUES ('master', 60, 300, 60, NULL, NULL, NULL, true, false, 0, NULL, 'master', 0, NULL, false, false, false, false, 'EXTERNAL', 1800, 36000, false, false, 'cd5df352-ee3f-41ff-8a25-7ddfb4d39455', 1800, false, NULL, false, false, false, false, 0, 1, 30, 6, 'HmacSHA1', 'totp', '97e757c6-46b1-4391-9588-78e7c457dd46', '0c7ad963-11b3-411c-ba19-7c0ded73aa6a', 'bff3e92c-2061-498c-9ecc-d6f2581998fe', '2a0c1b2b-77a2-4282-8eda-246a8cf4890b', '9829c39e-3c1c-4c30-8ddf-28791a94b456', 2592000, false, 900, true, false, '930b1832-546f-4afa-9cef-0f10348ea8fe', 0, false);


--
-- TOC entry 3738 (class 0 OID 16464)
-- Dependencies: 207
-- Data for Name: realm_attribute; Type: TABLE DATA; Schema: public; Owner: keycloak
--

INSERT INTO public.realm_attribute VALUES ('_browser_header.xContentTypeOptions', 'nosniff', 'master');
INSERT INTO public.realm_attribute VALUES ('_browser_header.xRobotsTag', 'none', 'master');
INSERT INTO public.realm_attribute VALUES ('_browser_header.xFrameOptions', 'SAMEORIGIN', 'master');
INSERT INTO public.realm_attribute VALUES ('_browser_header.contentSecurityPolicy', 'frame-src ''self''; frame-ancestors ''self''; object-src ''none'';', 'master');
INSERT INTO public.realm_attribute VALUES ('_browser_header.xXSSProtection', '1; mode=block', 'master');
INSERT INTO public.realm_attribute VALUES ('_browser_header.strictTransportSecurity', 'max-age=31536000; includeSubDomains', 'master');
INSERT INTO public.realm_attribute VALUES ('bruteForceProtected', 'false', 'master');
INSERT INTO public.realm_attribute VALUES ('permanentLockout', 'false', 'master');
INSERT INTO public.realm_attribute VALUES ('maxFailureWaitSeconds', '900', 'master');
INSERT INTO public.realm_attribute VALUES ('minimumQuickLoginWaitSeconds', '60', 'master');
INSERT INTO public.realm_attribute VALUES ('waitIncrementSeconds', '60', 'master');
INSERT INTO public.realm_attribute VALUES ('quickLoginCheckMilliSeconds', '1000', 'master');
INSERT INTO public.realm_attribute VALUES ('maxDeltaTimeSeconds', '43200', 'master');
INSERT INTO public.realm_attribute VALUES ('failureFactor', '30', 'master');
INSERT INTO public.realm_attribute VALUES ('displayName', 'Keycloak', 'master');
INSERT INTO public.realm_attribute VALUES ('displayNameHtml', '<div class="kc-logo-text"><span>Keycloak</span></div>', 'master');
INSERT INTO public.realm_attribute VALUES ('offlineSessionMaxLifespanEnabled', 'false', 'master');
INSERT INTO public.realm_attribute VALUES ('offlineSessionMaxLifespan', '5184000', 'master');


--
-- TOC entry 3787 (class 0 OID 17481)
-- Dependencies: 256
-- Data for Name: realm_default_groups; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3739 (class 0 OID 16470)
-- Dependencies: 208
-- Data for Name: realm_default_roles; Type: TABLE DATA; Schema: public; Owner: keycloak
--

INSERT INTO public.realm_default_roles VALUES ('master', '5603901b-9d26-49b6-8de2-e8a632c2a55b');
INSERT INTO public.realm_default_roles VALUES ('master', '90731e77-9365-4533-af58-ade179850ba3');


--
-- TOC entry 3764 (class 0 OID 16967)
-- Dependencies: 233
-- Data for Name: realm_enabled_event_types; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3740 (class 0 OID 16473)
-- Dependencies: 209
-- Data for Name: realm_events_listeners; Type: TABLE DATA; Schema: public; Owner: keycloak
--

INSERT INTO public.realm_events_listeners VALUES ('master', 'jboss-logging');


--
-- TOC entry 3741 (class 0 OID 16476)
-- Dependencies: 210
-- Data for Name: realm_required_credential; Type: TABLE DATA; Schema: public; Owner: keycloak
--

INSERT INTO public.realm_required_credential VALUES ('password', 'password', true, true, 'master');


--
-- TOC entry 3742 (class 0 OID 16484)
-- Dependencies: 211
-- Data for Name: realm_smtp_config; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3762 (class 0 OID 16858)
-- Dependencies: 231
-- Data for Name: realm_supported_locales; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3743 (class 0 OID 16496)
-- Dependencies: 212
-- Data for Name: redirect_uris; Type: TABLE DATA; Schema: public; Owner: keycloak
--

INSERT INTO public.redirect_uris VALUES ('1b950850-25cf-4739-8214-3014b9380d49', '/auth/realms/master/account/*');
INSERT INTO public.redirect_uris VALUES ('8662309a-9b69-48e4-b315-fa9fb9d07e94', '/auth/admin/master/console/*');
INSERT INTO public.redirect_uris VALUES ('8c7f88d9-5059-4e59-9827-85a76634e8e9', 'http://privateapps.foo.bar:8889/*');


--
-- TOC entry 3780 (class 0 OID 17312)
-- Dependencies: 249
-- Data for Name: required_action_config; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3779 (class 0 OID 17304)
-- Dependencies: 248
-- Data for Name: required_action_provider; Type: TABLE DATA; Schema: public; Owner: keycloak
--

INSERT INTO public.required_action_provider VALUES ('8eab603f-e9b7-4210-9406-250de540309d', 'VERIFY_EMAIL', 'Verify Email', 'master', true, false, 'VERIFY_EMAIL', 50);
INSERT INTO public.required_action_provider VALUES ('a43a98ea-9e10-49f2-87af-f4cf94268c47', 'UPDATE_PROFILE', 'Update Profile', 'master', true, false, 'UPDATE_PROFILE', 40);
INSERT INTO public.required_action_provider VALUES ('a0809063-801a-4d63-b373-eb1cf4d49340', 'CONFIGURE_TOTP', 'Configure OTP', 'master', true, false, 'CONFIGURE_TOTP', 10);
INSERT INTO public.required_action_provider VALUES ('59bec7ad-c05a-4260-9bdc-1215f8d88afe', 'UPDATE_PASSWORD', 'Update Password', 'master', true, false, 'UPDATE_PASSWORD', 30);
INSERT INTO public.required_action_provider VALUES ('a1ddbc01-6a5c-41d1-96f6-385acd964026', 'terms_and_conditions', 'Terms and Conditions', 'master', false, false, 'terms_and_conditions', 20);


--
-- TOC entry 3819 (class 0 OID 18266)
-- Dependencies: 288
-- Data for Name: resource_attribute; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3797 (class 0 OID 17762)
-- Dependencies: 266
-- Data for Name: resource_policy; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3796 (class 0 OID 17747)
-- Dependencies: 265
-- Data for Name: resource_scope; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3791 (class 0 OID 17681)
-- Dependencies: 260
-- Data for Name: resource_server; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3818 (class 0 OID 18224)
-- Dependencies: 287
-- Data for Name: resource_server_perm_ticket; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3794 (class 0 OID 17719)
-- Dependencies: 263
-- Data for Name: resource_server_policy; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3792 (class 0 OID 17689)
-- Dependencies: 261
-- Data for Name: resource_server_resource; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3793 (class 0 OID 17704)
-- Dependencies: 262
-- Data for Name: resource_server_scope; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3820 (class 0 OID 18285)
-- Dependencies: 289
-- Data for Name: resource_uris; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3744 (class 0 OID 16499)
-- Dependencies: 213
-- Data for Name: scope_mapping; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3798 (class 0 OID 17777)
-- Dependencies: 267
-- Data for Name: scope_policy; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3746 (class 0 OID 16505)
-- Dependencies: 215
-- Data for Name: user_attribute; Type: TABLE DATA; Schema: public; Owner: keycloak
--

INSERT INTO public.user_attribute VALUES ('meta', '{"nio_admin":"true"}', '614ef060-a249-4f4a-b24f-f5f63a230f80', '8685fcf6-018c-4c1f-9e70-b7cb5dc64843');


--
-- TOC entry 3768 (class 0 OID 16990)
-- Dependencies: 237
-- Data for Name: user_consent; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3816 (class 0 OID 18199)
-- Dependencies: 285
-- Data for Name: user_consent_client_scope; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3747 (class 0 OID 16511)
-- Dependencies: 216
-- Data for Name: user_entity; Type: TABLE DATA; Schema: public; Owner: keycloak
--

INSERT INTO public.user_entity VALUES ('581897a3-1caa-4406-9af6-5dfeffc5c75c', NULL, '10da4f63-2a94-4aee-af47-41aef0a03389', false, true, NULL, NULL, NULL, 'master', 'keycloak', 1537347323342, NULL, 0);
INSERT INTO public.user_entity VALUES ('bde69123-4f4c-462a-b730-8145dd545944', 'service-account-otoroshi@placeholder.org', 'service-account-otoroshi@placeholder.org', false, true, NULL, NULL, NULL, 'master', 'service-account-otoroshi', 1537347544865, '8c7f88d9-5059-4e59-9827-85a76634e8e9', 0);
INSERT INTO public.user_entity VALUES ('614ef060-a249-4f4a-b24f-f5f63a230f80', 'admin@nio.io', 'admin@nio.io', true, true, NULL, 'Admin', 'Admin', 'master', 'nioadmin', 1537347727444, NULL, 0);


--
-- TOC entry 3748 (class 0 OID 16520)
-- Dependencies: 217
-- Data for Name: user_federation_config; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3775 (class 0 OID 17122)
-- Dependencies: 244
-- Data for Name: user_federation_mapper; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3776 (class 0 OID 17128)
-- Dependencies: 245
-- Data for Name: user_federation_mapper_config; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3749 (class 0 OID 16526)
-- Dependencies: 218
-- Data for Name: user_federation_provider; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3786 (class 0 OID 17478)
-- Dependencies: 255
-- Data for Name: user_group_membership; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3750 (class 0 OID 16532)
-- Dependencies: 219
-- Data for Name: user_required_action; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3751 (class 0 OID 16535)
-- Dependencies: 220
-- Data for Name: user_role_mapping; Type: TABLE DATA; Schema: public; Owner: keycloak
--

INSERT INTO public.user_role_mapping VALUES ('5603901b-9d26-49b6-8de2-e8a632c2a55b', '581897a3-1caa-4406-9af6-5dfeffc5c75c');
INSERT INTO public.user_role_mapping VALUES ('3cc4f4bf-e857-4b70-a86f-3230b7b89dd2', '581897a3-1caa-4406-9af6-5dfeffc5c75c');
INSERT INTO public.user_role_mapping VALUES ('de5b13b1-7ddc-4032-82ac-ce0e7b6aabcf', '581897a3-1caa-4406-9af6-5dfeffc5c75c');
INSERT INTO public.user_role_mapping VALUES ('90731e77-9365-4533-af58-ade179850ba3', '581897a3-1caa-4406-9af6-5dfeffc5c75c');
INSERT INTO public.user_role_mapping VALUES ('186d3a91-8920-4fde-93b6-d98226e2e13f', '581897a3-1caa-4406-9af6-5dfeffc5c75c');
INSERT INTO public.user_role_mapping VALUES ('5603901b-9d26-49b6-8de2-e8a632c2a55b', 'bde69123-4f4c-462a-b730-8145dd545944');
INSERT INTO public.user_role_mapping VALUES ('3cc4f4bf-e857-4b70-a86f-3230b7b89dd2', 'bde69123-4f4c-462a-b730-8145dd545944');
INSERT INTO public.user_role_mapping VALUES ('de5b13b1-7ddc-4032-82ac-ce0e7b6aabcf', 'bde69123-4f4c-462a-b730-8145dd545944');
INSERT INTO public.user_role_mapping VALUES ('90731e77-9365-4533-af58-ade179850ba3', 'bde69123-4f4c-462a-b730-8145dd545944');
INSERT INTO public.user_role_mapping VALUES ('790ff8d2-bb9e-4b99-a7b0-9e5fcb2b474d', 'bde69123-4f4c-462a-b730-8145dd545944');
INSERT INTO public.user_role_mapping VALUES ('5603901b-9d26-49b6-8de2-e8a632c2a55b', '614ef060-a249-4f4a-b24f-f5f63a230f80');
INSERT INTO public.user_role_mapping VALUES ('3cc4f4bf-e857-4b70-a86f-3230b7b89dd2', '614ef060-a249-4f4a-b24f-f5f63a230f80');
INSERT INTO public.user_role_mapping VALUES ('de5b13b1-7ddc-4032-82ac-ce0e7b6aabcf', '614ef060-a249-4f4a-b24f-f5f63a230f80');
INSERT INTO public.user_role_mapping VALUES ('90731e77-9365-4533-af58-ade179850ba3', '614ef060-a249-4f4a-b24f-f5f63a230f80');


--
-- TOC entry 3752 (class 0 OID 16538)
-- Dependencies: 221
-- Data for Name: user_session; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3763 (class 0 OID 16861)
-- Dependencies: 232
-- Data for Name: user_session_note; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3745 (class 0 OID 16502)
-- Dependencies: 214
-- Data for Name: username_login_failure; Type: TABLE DATA; Schema: public; Owner: keycloak
--



--
-- TOC entry 3753 (class 0 OID 16551)
-- Dependencies: 222
-- Data for Name: web_origins; Type: TABLE DATA; Schema: public; Owner: keycloak
--

INSERT INTO public.web_origins VALUES ('8c7f88d9-5059-4e59-9827-85a76634e8e9', 'http://privateapps.foo.bar:8889');


--
-- TOC entry 3284 (class 2606 OID 17931)
-- Name: username_login_failure CONSTRAINT_17-2; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.username_login_failure
    ADD CONSTRAINT "CONSTRAINT_17-2" PRIMARY KEY (realm_id, username);


--
-- TOC entry 3252 (class 2606 OID 17933)
-- Name: keycloak_role UK_J3RWUVD56ONTGSUHOGM184WW2-2; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.keycloak_role
    ADD CONSTRAINT "UK_J3RWUVD56ONTGSUHOGM184WW2-2" UNIQUE (name, client_realm_constraint);


--
-- TOC entry 3495 (class 2606 OID 18113)
-- Name: client_auth_flow_bindings c_cli_flow_bind; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.client_auth_flow_bindings
    ADD CONSTRAINT c_cli_flow_bind PRIMARY KEY (client_id, binding_name);


--
-- TOC entry 3497 (class 2606 OID 18172)
-- Name: client_scope_client c_cli_scope_bind; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.client_scope_client
    ADD CONSTRAINT c_cli_scope_bind PRIMARY KEY (client_id, scope_id);


--
-- TOC entry 3492 (class 2606 OID 17969)
-- Name: client_initial_access cnstr_client_init_acc_pk; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.client_initial_access
    ADD CONSTRAINT cnstr_client_init_acc_pk PRIMARY KEY (id);


--
-- TOC entry 3404 (class 2606 OID 17528)
-- Name: realm_default_groups con_group_id_def_groups; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.realm_default_groups
    ADD CONSTRAINT con_group_id_def_groups UNIQUE (group_id);


--
-- TOC entry 3452 (class 2606 OID 17864)
-- Name: broker_link constr_broker_link_pk; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.broker_link
    ADD CONSTRAINT constr_broker_link_pk PRIMARY KEY (identity_provider, user_id);


--
-- TOC entry 3379 (class 2606 OID 17325)
-- Name: client_user_session_note constr_cl_usr_ses_note; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.client_user_session_note
    ADD CONSTRAINT constr_cl_usr_ses_note PRIMARY KEY (client_session, name);


--
-- TOC entry 3229 (class 2606 OID 18082)
-- Name: client_default_roles constr_client_default_roles; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.client_default_roles
    ADD CONSTRAINT constr_client_default_roles PRIMARY KEY (client_id, role_id);


--
-- TOC entry 3478 (class 2606 OID 17884)
-- Name: component_config constr_component_config_pk; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.component_config
    ADD CONSTRAINT constr_component_config_pk PRIMARY KEY (id);


--
-- TOC entry 3481 (class 2606 OID 17882)
-- Name: component constr_component_pk; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.component
    ADD CONSTRAINT constr_component_pk PRIMARY KEY (id);


--
-- TOC entry 3470 (class 2606 OID 17880)
-- Name: fed_user_required_action constr_fed_required_action; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.fed_user_required_action
    ADD CONSTRAINT constr_fed_required_action PRIMARY KEY (required_action, user_id);


--
-- TOC entry 3454 (class 2606 OID 17866)
-- Name: fed_user_attribute constr_fed_user_attr_pk; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.fed_user_attribute
    ADD CONSTRAINT constr_fed_user_attr_pk PRIMARY KEY (id);


--
-- TOC entry 3457 (class 2606 OID 17868)
-- Name: fed_user_consent constr_fed_user_consent_pk; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.fed_user_consent
    ADD CONSTRAINT constr_fed_user_consent_pk PRIMARY KEY (id);


--
-- TOC entry 3462 (class 2606 OID 17874)
-- Name: fed_user_credential constr_fed_user_cred_pk; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.fed_user_credential
    ADD CONSTRAINT constr_fed_user_cred_pk PRIMARY KEY (id);


--
-- TOC entry 3466 (class 2606 OID 17876)
-- Name: fed_user_group_membership constr_fed_user_group; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.fed_user_group_membership
    ADD CONSTRAINT constr_fed_user_group PRIMARY KEY (group_id, user_id);


--
-- TOC entry 3474 (class 2606 OID 17878)
-- Name: fed_user_role_mapping constr_fed_user_role; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.fed_user_role_mapping
    ADD CONSTRAINT constr_fed_user_role PRIMARY KEY (role_id, user_id);


--
-- TOC entry 3490 (class 2606 OID 17924)
-- Name: federated_user constr_federated_user; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.federated_user
    ADD CONSTRAINT constr_federated_user PRIMARY KEY (id);


--
-- TOC entry 3406 (class 2606 OID 18072)
-- Name: realm_default_groups constr_realm_default_groups; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.realm_default_groups
    ADD CONSTRAINT constr_realm_default_groups PRIMARY KEY (realm_id, group_id);


--
-- TOC entry 3340 (class 2606 OID 18089)
-- Name: realm_enabled_event_types constr_realm_enabl_event_types; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.realm_enabled_event_types
    ADD CONSTRAINT constr_realm_enabl_event_types PRIMARY KEY (realm_id, value);


--
-- TOC entry 3271 (class 2606 OID 18091)
-- Name: realm_events_listeners constr_realm_events_listeners; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.realm_events_listeners
    ADD CONSTRAINT constr_realm_events_listeners PRIMARY KEY (realm_id, value);


--
-- TOC entry 3335 (class 2606 OID 18093)
-- Name: realm_supported_locales constr_realm_supported_locales; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.realm_supported_locales
    ADD CONSTRAINT constr_realm_supported_locales PRIMARY KEY (realm_id, value);


--
-- TOC entry 3328 (class 2606 OID 16879)
-- Name: identity_provider constraint_2b; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.identity_provider
    ADD CONSTRAINT constraint_2b PRIMARY KEY (internal_id);


--
-- TOC entry 3312 (class 2606 OID 16799)
-- Name: client_attributes constraint_3c; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.client_attributes
    ADD CONSTRAINT constraint_3c PRIMARY KEY (client_id, name);


--
-- TOC entry 3250 (class 2606 OID 16563)
-- Name: event_entity constraint_4; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.event_entity
    ADD CONSTRAINT constraint_4 PRIMARY KEY (id);


--
-- TOC entry 3324 (class 2606 OID 16881)
-- Name: federated_identity constraint_40; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.federated_identity
    ADD CONSTRAINT constraint_40 PRIMARY KEY (identity_provider, user_id);


--
-- TOC entry 3258 (class 2606 OID 16565)
-- Name: realm constraint_4a; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.realm
    ADD CONSTRAINT constraint_4a PRIMARY KEY (id);


--
-- TOC entry 3241 (class 2606 OID 16567)
-- Name: client_session_role constraint_5; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.client_session_role
    ADD CONSTRAINT constraint_5 PRIMARY KEY (client_session, role_id);


--
-- TOC entry 3307 (class 2606 OID 16569)
-- Name: user_session constraint_57; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.user_session
    ADD CONSTRAINT constraint_57 PRIMARY KEY (id);


--
-- TOC entry 3298 (class 2606 OID 16571)
-- Name: user_federation_provider constraint_5c; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.user_federation_provider
    ADD CONSTRAINT constraint_5c PRIMARY KEY (id);


--
-- TOC entry 3314 (class 2606 OID 16801)
-- Name: client_session_note constraint_5e; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.client_session_note
    ADD CONSTRAINT constraint_5e PRIMARY KEY (client_session, name);


--
-- TOC entry 3234 (class 2606 OID 16575)
-- Name: client constraint_7; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.client
    ADD CONSTRAINT constraint_7 PRIMARY KEY (id);


--
-- TOC entry 3238 (class 2606 OID 16577)
-- Name: client_session constraint_8; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.client_session
    ADD CONSTRAINT constraint_8 PRIMARY KEY (id);


--
-- TOC entry 3281 (class 2606 OID 16579)
-- Name: scope_mapping constraint_81; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.scope_mapping
    ADD CONSTRAINT constraint_81 PRIMARY KEY (client_id, role_id);


--
-- TOC entry 3316 (class 2606 OID 16803)
-- Name: client_node_registrations constraint_84; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.client_node_registrations
    ADD CONSTRAINT constraint_84 PRIMARY KEY (client_id, name);


--
-- TOC entry 3263 (class 2606 OID 16581)
-- Name: realm_attribute constraint_9; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.realm_attribute
    ADD CONSTRAINT constraint_9 PRIMARY KEY (name, realm_id);


--
-- TOC entry 3274 (class 2606 OID 16583)
-- Name: realm_required_credential constraint_92; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.realm_required_credential
    ADD CONSTRAINT constraint_92 PRIMARY KEY (realm_id, type);


--
-- TOC entry 3254 (class 2606 OID 16585)
-- Name: keycloak_role constraint_a; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.keycloak_role
    ADD CONSTRAINT constraint_a PRIMARY KEY (id);


--
-- TOC entry 3357 (class 2606 OID 18076)
-- Name: admin_event_entity constraint_admin_event_entity; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.admin_event_entity
    ADD CONSTRAINT constraint_admin_event_entity PRIMARY KEY (id);


--
-- TOC entry 3369 (class 2606 OID 17194)
-- Name: authenticator_config_entry constraint_auth_cfg_pk; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.authenticator_config_entry
    ADD CONSTRAINT constraint_auth_cfg_pk PRIMARY KEY (authenticator_id, name);


--
-- TOC entry 3365 (class 2606 OID 17192)
-- Name: authentication_execution constraint_auth_exec_pk; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.authentication_execution
    ADD CONSTRAINT constraint_auth_exec_pk PRIMARY KEY (id);


--
-- TOC entry 3362 (class 2606 OID 17190)
-- Name: authentication_flow constraint_auth_flow_pk; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.authentication_flow
    ADD CONSTRAINT constraint_auth_flow_pk PRIMARY KEY (id);


--
-- TOC entry 3359 (class 2606 OID 17188)
-- Name: authenticator_config constraint_auth_pk; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.authenticator_config
    ADD CONSTRAINT constraint_auth_pk PRIMARY KEY (id);


--
-- TOC entry 3377 (class 2606 OID 17198)
-- Name: client_session_auth_status constraint_auth_status_pk; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.client_session_auth_status
    ADD CONSTRAINT constraint_auth_status_pk PRIMARY KEY (client_session, authenticator);


--
-- TOC entry 3304 (class 2606 OID 16587)
-- Name: user_role_mapping constraint_c; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.user_role_mapping
    ADD CONSTRAINT constraint_c PRIMARY KEY (role_id, user_id);


--
-- TOC entry 3243 (class 2606 OID 18070)
-- Name: composite_role constraint_composite_role; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.composite_role
    ADD CONSTRAINT constraint_composite_role PRIMARY KEY (composite, child_role);


--
-- TOC entry 3484 (class 2606 OID 18078)
-- Name: credential_attribute constraint_credential_attr; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.credential_attribute
    ADD CONSTRAINT constraint_credential_attr PRIMARY KEY (id);


--
-- TOC entry 3355 (class 2606 OID 17015)
-- Name: client_session_prot_mapper constraint_cs_pmp_pk; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.client_session_prot_mapper
    ADD CONSTRAINT constraint_cs_pmp_pk PRIMARY KEY (client_session, protocol_mapper_id);


--
-- TOC entry 3333 (class 2606 OID 16883)
-- Name: identity_provider_config constraint_d; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.identity_provider_config
    ADD CONSTRAINT constraint_d PRIMARY KEY (identity_provider_id, name);


--
-- TOC entry 3438 (class 2606 OID 17741)
-- Name: policy_config constraint_dpc; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.policy_config
    ADD CONSTRAINT constraint_dpc PRIMARY KEY (policy_id, name);


--
-- TOC entry 3276 (class 2606 OID 16589)
-- Name: realm_smtp_config constraint_e; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.realm_smtp_config
    ADD CONSTRAINT constraint_e PRIMARY KEY (realm_id, name);


--
-- TOC entry 3247 (class 2606 OID 16591)
-- Name: credential constraint_f; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.credential
    ADD CONSTRAINT constraint_f PRIMARY KEY (id);


--
-- TOC entry 3296 (class 2606 OID 16593)
-- Name: user_federation_config constraint_f9; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.user_federation_config
    ADD CONSTRAINT constraint_f9 PRIMARY KEY (user_federation_provider_id, name);


--
-- TOC entry 3510 (class 2606 OID 18228)
-- Name: resource_server_perm_ticket constraint_fapmt; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.resource_server_perm_ticket
    ADD CONSTRAINT constraint_fapmt PRIMARY KEY (id);


--
-- TOC entry 3423 (class 2606 OID 17696)
-- Name: resource_server_resource constraint_farsr; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.resource_server_resource
    ADD CONSTRAINT constraint_farsr PRIMARY KEY (id);


--
-- TOC entry 3433 (class 2606 OID 17726)
-- Name: resource_server_policy constraint_farsrp; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.resource_server_policy
    ADD CONSTRAINT constraint_farsrp PRIMARY KEY (id);


--
-- TOC entry 3449 (class 2606 OID 17796)
-- Name: associated_policy constraint_farsrpap; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.associated_policy
    ADD CONSTRAINT constraint_farsrpap PRIMARY KEY (policy_id, associated_policy_id);


--
-- TOC entry 3443 (class 2606 OID 17766)
-- Name: resource_policy constraint_farsrpp; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.resource_policy
    ADD CONSTRAINT constraint_farsrpp PRIMARY KEY (resource_id, policy_id);


--
-- TOC entry 3428 (class 2606 OID 17711)
-- Name: resource_server_scope constraint_farsrs; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.resource_server_scope
    ADD CONSTRAINT constraint_farsrs PRIMARY KEY (id);


--
-- TOC entry 3440 (class 2606 OID 17751)
-- Name: resource_scope constraint_farsrsp; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.resource_scope
    ADD CONSTRAINT constraint_farsrsp PRIMARY KEY (resource_id, scope_id);


--
-- TOC entry 3446 (class 2606 OID 17781)
-- Name: scope_policy constraint_farsrsps; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.scope_policy
    ADD CONSTRAINT constraint_farsrsps PRIMARY KEY (scope_id, policy_id);


--
-- TOC entry 3289 (class 2606 OID 16595)
-- Name: user_entity constraint_fb; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.user_entity
    ADD CONSTRAINT constraint_fb PRIMARY KEY (id);


--
-- TOC entry 3487 (class 2606 OID 18080)
-- Name: fed_credential_attribute constraint_fed_credential_attr; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.fed_credential_attribute
    ADD CONSTRAINT constraint_fed_credential_attr PRIMARY KEY (id);


--
-- TOC entry 3375 (class 2606 OID 17202)
-- Name: user_federation_mapper_config constraint_fedmapper_cfg_pm; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.user_federation_mapper_config
    ADD CONSTRAINT constraint_fedmapper_cfg_pm PRIMARY KEY (user_federation_mapper_id, name);


--
-- TOC entry 3371 (class 2606 OID 17200)
-- Name: user_federation_mapper constraint_fedmapperpm; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.user_federation_mapper
    ADD CONSTRAINT constraint_fedmapperpm PRIMARY KEY (id);


--
-- TOC entry 3508 (class 2606 OID 18213)
-- Name: fed_user_consent_cl_scope constraint_fgrntcsnt_clsc_pm; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.fed_user_consent_cl_scope
    ADD CONSTRAINT constraint_fgrntcsnt_clsc_pm PRIMARY KEY (user_consent_id, scope_id);


--
-- TOC entry 3505 (class 2606 OID 18203)
-- Name: user_consent_client_scope constraint_grntcsnt_clsc_pm; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.user_consent_client_scope
    ADD CONSTRAINT constraint_grntcsnt_clsc_pm PRIMARY KEY (user_consent_id, scope_id);


--
-- TOC entry 3350 (class 2606 OID 17009)
-- Name: user_consent constraint_grntcsnt_pm; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.user_consent
    ADD CONSTRAINT constraint_grntcsnt_pm PRIMARY KEY (id);


--
-- TOC entry 3391 (class 2606 OID 17495)
-- Name: keycloak_group constraint_group; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.keycloak_group
    ADD CONSTRAINT constraint_group PRIMARY KEY (id);


--
-- TOC entry 3398 (class 2606 OID 17502)
-- Name: group_attribute constraint_group_attribute_pk; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.group_attribute
    ADD CONSTRAINT constraint_group_attribute_pk PRIMARY KEY (id);


--
-- TOC entry 3395 (class 2606 OID 17516)
-- Name: group_role_mapping constraint_group_role; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.group_role_mapping
    ADD CONSTRAINT constraint_group_role PRIMARY KEY (role_id, group_id);


--
-- TOC entry 3345 (class 2606 OID 17005)
-- Name: identity_provider_mapper constraint_idpm; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.identity_provider_mapper
    ADD CONSTRAINT constraint_idpm PRIMARY KEY (id);


--
-- TOC entry 3348 (class 2606 OID 17251)
-- Name: idp_mapper_config constraint_idpmconfig; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.idp_mapper_config
    ADD CONSTRAINT constraint_idpmconfig PRIMARY KEY (idp_mapper_id, name);


--
-- TOC entry 3343 (class 2606 OID 17003)
-- Name: migration_model constraint_migmod; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.migration_model
    ADD CONSTRAINT constraint_migmod PRIMARY KEY (id);


--
-- TOC entry 3388 (class 2606 OID 18139)
-- Name: offline_client_session constraint_offl_cl_ses_pk3; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.offline_client_session
    ADD CONSTRAINT constraint_offl_cl_ses_pk3 PRIMARY KEY (user_session_id, client_id, client_storage_provider, external_client_id, offline_flag);


--
-- TOC entry 3386 (class 2606 OID 17462)
-- Name: offline_user_session constraint_offl_us_ses_pk2; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.offline_user_session
    ADD CONSTRAINT constraint_offl_us_ses_pk2 PRIMARY KEY (user_session_id, offline_flag);


--
-- TOC entry 3318 (class 2606 OID 16877)
-- Name: protocol_mapper constraint_pcm; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.protocol_mapper
    ADD CONSTRAINT constraint_pcm PRIMARY KEY (id);


--
-- TOC entry 3322 (class 2606 OID 17244)
-- Name: protocol_mapper_config constraint_pmconfig; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.protocol_mapper_config
    ADD CONSTRAINT constraint_pmconfig PRIMARY KEY (protocol_mapper_id, name);


--
-- TOC entry 3266 (class 2606 OID 18068)
-- Name: realm_default_roles constraint_realm_default_roles; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.realm_default_roles
    ADD CONSTRAINT constraint_realm_default_roles PRIMARY KEY (realm_id, role_id);


--
-- TOC entry 3278 (class 2606 OID 18095)
-- Name: redirect_uris constraint_redirect_uris; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.redirect_uris
    ADD CONSTRAINT constraint_redirect_uris PRIMARY KEY (client_id, value);


--
-- TOC entry 3384 (class 2606 OID 17323)
-- Name: required_action_config constraint_req_act_cfg_pk; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.required_action_config
    ADD CONSTRAINT constraint_req_act_cfg_pk PRIMARY KEY (required_action_id, name);


--
-- TOC entry 3381 (class 2606 OID 17321)
-- Name: required_action_provider constraint_req_act_prv_pk; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.required_action_provider
    ADD CONSTRAINT constraint_req_act_prv_pk PRIMARY KEY (id);


--
-- TOC entry 3301 (class 2606 OID 17196)
-- Name: user_required_action constraint_required_action; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.user_required_action
    ADD CONSTRAINT constraint_required_action PRIMARY KEY (required_action, user_id);


--
-- TOC entry 3286 (class 2606 OID 17319)
-- Name: user_attribute constraint_user_attribute_pk; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.user_attribute
    ADD CONSTRAINT constraint_user_attribute_pk PRIMARY KEY (id);


--
-- TOC entry 3401 (class 2606 OID 17509)
-- Name: user_group_membership constraint_user_group; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.user_group_membership
    ADD CONSTRAINT constraint_user_group PRIMARY KEY (group_id, user_id);


--
-- TOC entry 3338 (class 2606 OID 16887)
-- Name: user_session_note constraint_usn_pk; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.user_session_note
    ADD CONSTRAINT constraint_usn_pk PRIMARY KEY (user_session, name);


--
-- TOC entry 3309 (class 2606 OID 18097)
-- Name: web_origins constraint_web_origins; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.web_origins
    ADD CONSTRAINT constraint_web_origins PRIMARY KEY (client_id, value);


--
-- TOC entry 3415 (class 2606 OID 17662)
-- Name: client_scope_attributes pk_cl_tmpl_attr; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.client_scope_attributes
    ADD CONSTRAINT pk_cl_tmpl_attr PRIMARY KEY (scope_id, name);


--
-- TOC entry 3410 (class 2606 OID 17621)
-- Name: client_scope pk_cli_template; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.client_scope
    ADD CONSTRAINT pk_cli_template PRIMARY KEY (id);


--
-- TOC entry 3227 (class 2606 OID 16390)
-- Name: databasechangeloglock pk_databasechangeloglock; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.databasechangeloglock
    ADD CONSTRAINT pk_databasechangeloglock PRIMARY KEY (id);


--
-- TOC entry 3421 (class 2606 OID 18041)
-- Name: resource_server pk_resource_server; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.resource_server
    ADD CONSTRAINT pk_resource_server PRIMARY KEY (id);


--
-- TOC entry 3419 (class 2606 OID 17650)
-- Name: client_scope_role_mapping pk_template_scope; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.client_scope_role_mapping
    ADD CONSTRAINT pk_template_scope PRIMARY KEY (scope_id, role_id);


--
-- TOC entry 3503 (class 2606 OID 18188)
-- Name: default_client_scope r_def_cli_scope_bind; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.default_client_scope
    ADD CONSTRAINT r_def_cli_scope_bind PRIMARY KEY (realm_id, scope_id);


--
-- TOC entry 3514 (class 2606 OID 18274)
-- Name: resource_attribute res_attr_pk; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.resource_attribute
    ADD CONSTRAINT res_attr_pk PRIMARY KEY (id);


--
-- TOC entry 3393 (class 2606 OID 17953)
-- Name: keycloak_group sibling_names; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.keycloak_group
    ADD CONSTRAINT sibling_names UNIQUE (realm_id, parent_group, name);


--
-- TOC entry 3331 (class 2606 OID 16934)
-- Name: identity_provider uk_2daelwnibji49avxsrtuf6xj33; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.identity_provider
    ADD CONSTRAINT uk_2daelwnibji49avxsrtuf6xj33 UNIQUE (provider_alias, realm_id);


--
-- TOC entry 3232 (class 2606 OID 16597)
-- Name: client_default_roles uk_8aelwnibji49avxsrtuf6xjow; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.client_default_roles
    ADD CONSTRAINT uk_8aelwnibji49avxsrtuf6xjow UNIQUE (role_id);


--
-- TOC entry 3236 (class 2606 OID 16599)
-- Name: client uk_b71cjlbenv945rb6gcon438at; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.client
    ADD CONSTRAINT uk_b71cjlbenv945rb6gcon438at UNIQUE (realm_id, client_id);


--
-- TOC entry 3412 (class 2606 OID 18141)
-- Name: client_scope uk_cli_scope; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.client_scope
    ADD CONSTRAINT uk_cli_scope UNIQUE (realm_id, name);


--
-- TOC entry 3292 (class 2606 OID 16603)
-- Name: user_entity uk_dykn684sl8up1crfei6eckhd7; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.user_entity
    ADD CONSTRAINT uk_dykn684sl8up1crfei6eckhd7 UNIQUE (realm_id, email_constraint);


--
-- TOC entry 3426 (class 2606 OID 18034)
-- Name: resource_server_resource uk_frsr6t700s9v50bu18ws5ha6; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.resource_server_resource
    ADD CONSTRAINT uk_frsr6t700s9v50bu18ws5ha6 UNIQUE (name, owner, resource_server_id);


--
-- TOC entry 3512 (class 2606 OID 18245)
-- Name: resource_server_perm_ticket uk_frsr6t700s9v50bu18ws5pmt; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.resource_server_perm_ticket
    ADD CONSTRAINT uk_frsr6t700s9v50bu18ws5pmt UNIQUE (owner, requester, resource_server_id, resource_id, scope_id);


--
-- TOC entry 3436 (class 2606 OID 18032)
-- Name: resource_server_policy uk_frsrpt700s9v50bu18ws5ha6; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.resource_server_policy
    ADD CONSTRAINT uk_frsrpt700s9v50bu18ws5ha6 UNIQUE (name, resource_server_id);


--
-- TOC entry 3431 (class 2606 OID 18036)
-- Name: resource_server_scope uk_frsrst700s9v50bu18ws5ha6; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.resource_server_scope
    ADD CONSTRAINT uk_frsrst700s9v50bu18ws5ha6 UNIQUE (name, resource_server_id);


--
-- TOC entry 3269 (class 2606 OID 16605)
-- Name: realm_default_roles uk_h4wpd7w4hsoolni3h0sw7btje; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.realm_default_roles
    ADD CONSTRAINT uk_h4wpd7w4hsoolni3h0sw7btje UNIQUE (role_id);


--
-- TOC entry 3353 (class 2606 OID 18115)
-- Name: user_consent uk_jkuwuvd56ontgsuhogm8uewrt; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.user_consent
    ADD CONSTRAINT uk_jkuwuvd56ontgsuhogm8uewrt UNIQUE (client_id, client_storage_provider, external_client_id, user_id);


--
-- TOC entry 3261 (class 2606 OID 16611)
-- Name: realm uk_orvsdmla56612eaefiq6wl5oi; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.realm
    ADD CONSTRAINT uk_orvsdmla56612eaefiq6wl5oi UNIQUE (name);


--
-- TOC entry 3294 (class 2606 OID 17926)
-- Name: user_entity uk_ru8tt6t700s9v50bu18ws5ha6; Type: CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.user_entity
    ADD CONSTRAINT uk_ru8tt6t700s9v50bu18ws5ha6 UNIQUE (realm_id, username);


--
-- TOC entry 3450 (class 1259 OID 17975)
-- Name: idx_assoc_pol_assoc_pol_id; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_assoc_pol_assoc_pol_id ON public.associated_policy USING btree (associated_policy_id);


--
-- TOC entry 3360 (class 1259 OID 17979)
-- Name: idx_auth_config_realm; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_auth_config_realm ON public.authenticator_config USING btree (realm_id);


--
-- TOC entry 3366 (class 1259 OID 17977)
-- Name: idx_auth_exec_flow; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_auth_exec_flow ON public.authentication_execution USING btree (flow_id);


--
-- TOC entry 3367 (class 1259 OID 17976)
-- Name: idx_auth_exec_realm_flow; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_auth_exec_realm_flow ON public.authentication_execution USING btree (realm_id, flow_id);


--
-- TOC entry 3363 (class 1259 OID 17978)
-- Name: idx_auth_flow_realm; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_auth_flow_realm ON public.authentication_flow USING btree (realm_id);


--
-- TOC entry 3498 (class 1259 OID 18220)
-- Name: idx_cl_clscope; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_cl_clscope ON public.client_scope_client USING btree (scope_id);


--
-- TOC entry 3230 (class 1259 OID 17981)
-- Name: idx_client_def_roles_client; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_client_def_roles_client ON public.client_default_roles USING btree (client_id);


--
-- TOC entry 3493 (class 1259 OID 18019)
-- Name: idx_client_init_acc_realm; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_client_init_acc_realm ON public.client_initial_access USING btree (realm_id);


--
-- TOC entry 3239 (class 1259 OID 17983)
-- Name: idx_client_session_session; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_client_session_session ON public.client_session USING btree (session_id);


--
-- TOC entry 3413 (class 1259 OID 18218)
-- Name: idx_clscope_attrs; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_clscope_attrs ON public.client_scope_attributes USING btree (scope_id);


--
-- TOC entry 3499 (class 1259 OID 18219)
-- Name: idx_clscope_cl; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_clscope_cl ON public.client_scope_client USING btree (client_id);


--
-- TOC entry 3319 (class 1259 OID 18215)
-- Name: idx_clscope_protmap; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_clscope_protmap ON public.protocol_mapper USING btree (client_scope_id);


--
-- TOC entry 3416 (class 1259 OID 18216)
-- Name: idx_clscope_role; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_clscope_role ON public.client_scope_role_mapping USING btree (scope_id);


--
-- TOC entry 3479 (class 1259 OID 17985)
-- Name: idx_compo_config_compo; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_compo_config_compo ON public.component_config USING btree (component_id);


--
-- TOC entry 3482 (class 1259 OID 17984)
-- Name: idx_component_realm; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_component_realm ON public.component USING btree (realm_id);


--
-- TOC entry 3244 (class 1259 OID 17986)
-- Name: idx_composite; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_composite ON public.composite_role USING btree (composite);


--
-- TOC entry 3245 (class 1259 OID 17987)
-- Name: idx_composite_child; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_composite_child ON public.composite_role USING btree (child_role);


--
-- TOC entry 3485 (class 1259 OID 17988)
-- Name: idx_credential_attr_cred; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_credential_attr_cred ON public.credential_attribute USING btree (credential_id);


--
-- TOC entry 3500 (class 1259 OID 18221)
-- Name: idx_defcls_realm; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_defcls_realm ON public.default_client_scope USING btree (realm_id);


--
-- TOC entry 3501 (class 1259 OID 18222)
-- Name: idx_defcls_scope; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_defcls_scope ON public.default_client_scope USING btree (scope_id);


--
-- TOC entry 3488 (class 1259 OID 17989)
-- Name: idx_fed_cred_attr_cred; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_fed_cred_attr_cred ON public.fed_credential_attribute USING btree (credential_id);


--
-- TOC entry 3325 (class 1259 OID 17680)
-- Name: idx_fedidentity_feduser; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_fedidentity_feduser ON public.federated_identity USING btree (federated_user_id);


--
-- TOC entry 3326 (class 1259 OID 17679)
-- Name: idx_fedidentity_user; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_fedidentity_user ON public.federated_identity USING btree (user_id);


--
-- TOC entry 3455 (class 1259 OID 18098)
-- Name: idx_fu_attribute; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_fu_attribute ON public.fed_user_attribute USING btree (user_id, realm_id, name);


--
-- TOC entry 3458 (class 1259 OID 18119)
-- Name: idx_fu_cnsnt_ext; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_fu_cnsnt_ext ON public.fed_user_consent USING btree (user_id, client_storage_provider, external_client_id);


--
-- TOC entry 3459 (class 1259 OID 18099)
-- Name: idx_fu_consent; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_fu_consent ON public.fed_user_consent USING btree (user_id, client_id);


--
-- TOC entry 3460 (class 1259 OID 18100)
-- Name: idx_fu_consent_ru; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_fu_consent_ru ON public.fed_user_consent USING btree (realm_id, user_id);


--
-- TOC entry 3463 (class 1259 OID 18101)
-- Name: idx_fu_credential; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_fu_credential ON public.fed_user_credential USING btree (user_id, type);


--
-- TOC entry 3464 (class 1259 OID 18102)
-- Name: idx_fu_credential_ru; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_fu_credential_ru ON public.fed_user_credential USING btree (realm_id, user_id);


--
-- TOC entry 3467 (class 1259 OID 18103)
-- Name: idx_fu_group_membership; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_fu_group_membership ON public.fed_user_group_membership USING btree (user_id, group_id);


--
-- TOC entry 3468 (class 1259 OID 18104)
-- Name: idx_fu_group_membership_ru; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_fu_group_membership_ru ON public.fed_user_group_membership USING btree (realm_id, user_id);


--
-- TOC entry 3471 (class 1259 OID 18105)
-- Name: idx_fu_required_action; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_fu_required_action ON public.fed_user_required_action USING btree (user_id, required_action);


--
-- TOC entry 3472 (class 1259 OID 18106)
-- Name: idx_fu_required_action_ru; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_fu_required_action_ru ON public.fed_user_required_action USING btree (realm_id, user_id);


--
-- TOC entry 3475 (class 1259 OID 18107)
-- Name: idx_fu_role_mapping; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_fu_role_mapping ON public.fed_user_role_mapping USING btree (user_id, role_id);


--
-- TOC entry 3476 (class 1259 OID 18108)
-- Name: idx_fu_role_mapping_ru; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_fu_role_mapping_ru ON public.fed_user_role_mapping USING btree (realm_id, user_id);


--
-- TOC entry 3399 (class 1259 OID 17990)
-- Name: idx_group_attr_group; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_group_attr_group ON public.group_attribute USING btree (group_id);


--
-- TOC entry 3396 (class 1259 OID 17991)
-- Name: idx_group_role_mapp_group; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_group_role_mapp_group ON public.group_role_mapping USING btree (group_id);


--
-- TOC entry 3346 (class 1259 OID 17993)
-- Name: idx_id_prov_mapp_realm; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_id_prov_mapp_realm ON public.identity_provider_mapper USING btree (realm_id);


--
-- TOC entry 3329 (class 1259 OID 17992)
-- Name: idx_ident_prov_realm; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_ident_prov_realm ON public.identity_provider USING btree (realm_id);


--
-- TOC entry 3255 (class 1259 OID 17994)
-- Name: idx_keycloak_role_client; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_keycloak_role_client ON public.keycloak_role USING btree (client);


--
-- TOC entry 3256 (class 1259 OID 17995)
-- Name: idx_keycloak_role_realm; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_keycloak_role_realm ON public.keycloak_role USING btree (realm);


--
-- TOC entry 3320 (class 1259 OID 17996)
-- Name: idx_protocol_mapper_client; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_protocol_mapper_client ON public.protocol_mapper USING btree (client_id);


--
-- TOC entry 3264 (class 1259 OID 17999)
-- Name: idx_realm_attr_realm; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_realm_attr_realm ON public.realm_attribute USING btree (realm_id);


--
-- TOC entry 3408 (class 1259 OID 18214)
-- Name: idx_realm_clscope; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_realm_clscope ON public.client_scope USING btree (realm_id);


--
-- TOC entry 3407 (class 1259 OID 18000)
-- Name: idx_realm_def_grp_realm; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_realm_def_grp_realm ON public.realm_default_groups USING btree (realm_id);


--
-- TOC entry 3267 (class 1259 OID 18001)
-- Name: idx_realm_def_roles_realm; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_realm_def_roles_realm ON public.realm_default_roles USING btree (realm_id);


--
-- TOC entry 3272 (class 1259 OID 18003)
-- Name: idx_realm_evt_list_realm; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_realm_evt_list_realm ON public.realm_events_listeners USING btree (realm_id);


--
-- TOC entry 3341 (class 1259 OID 18002)
-- Name: idx_realm_evt_types_realm; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_realm_evt_types_realm ON public.realm_enabled_event_types USING btree (realm_id);


--
-- TOC entry 3259 (class 1259 OID 17998)
-- Name: idx_realm_master_adm_cli; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_realm_master_adm_cli ON public.realm USING btree (master_admin_client);


--
-- TOC entry 3336 (class 1259 OID 18004)
-- Name: idx_realm_supp_local_realm; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_realm_supp_local_realm ON public.realm_supported_locales USING btree (realm_id);


--
-- TOC entry 3279 (class 1259 OID 18005)
-- Name: idx_redir_uri_client; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_redir_uri_client ON public.redirect_uris USING btree (client_id);


--
-- TOC entry 3382 (class 1259 OID 18006)
-- Name: idx_req_act_prov_realm; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_req_act_prov_realm ON public.required_action_provider USING btree (realm_id);


--
-- TOC entry 3444 (class 1259 OID 18007)
-- Name: idx_res_policy_policy; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_res_policy_policy ON public.resource_policy USING btree (policy_id);


--
-- TOC entry 3441 (class 1259 OID 18008)
-- Name: idx_res_scope_scope; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_res_scope_scope ON public.resource_scope USING btree (scope_id);


--
-- TOC entry 3434 (class 1259 OID 18037)
-- Name: idx_res_serv_pol_res_serv; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_res_serv_pol_res_serv ON public.resource_server_policy USING btree (resource_server_id);


--
-- TOC entry 3424 (class 1259 OID 18038)
-- Name: idx_res_srv_res_res_srv; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_res_srv_res_res_srv ON public.resource_server_resource USING btree (resource_server_id);


--
-- TOC entry 3429 (class 1259 OID 18039)
-- Name: idx_res_srv_scope_res_srv; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_res_srv_scope_res_srv ON public.resource_server_scope USING btree (resource_server_id);


--
-- TOC entry 3417 (class 1259 OID 18217)
-- Name: idx_role_clscope; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_role_clscope ON public.client_scope_role_mapping USING btree (role_id);


--
-- TOC entry 3282 (class 1259 OID 18012)
-- Name: idx_scope_mapping_role; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_scope_mapping_role ON public.scope_mapping USING btree (role_id);


--
-- TOC entry 3447 (class 1259 OID 18013)
-- Name: idx_scope_policy_policy; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_scope_policy_policy ON public.scope_policy USING btree (policy_id);


--
-- TOC entry 3389 (class 1259 OID 17669)
-- Name: idx_us_sess_id_on_cl_sess; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_us_sess_id_on_cl_sess ON public.offline_client_session USING btree (user_session_id);


--
-- TOC entry 3506 (class 1259 OID 18223)
-- Name: idx_usconsent_clscope; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_usconsent_clscope ON public.user_consent_client_scope USING btree (user_consent_id);


--
-- TOC entry 3287 (class 1259 OID 17676)
-- Name: idx_user_attribute; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_user_attribute ON public.user_attribute USING btree (user_id);


--
-- TOC entry 3351 (class 1259 OID 17673)
-- Name: idx_user_consent; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_user_consent ON public.user_consent USING btree (user_id);


--
-- TOC entry 3248 (class 1259 OID 17677)
-- Name: idx_user_credential; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_user_credential ON public.credential USING btree (user_id);


--
-- TOC entry 3290 (class 1259 OID 17670)
-- Name: idx_user_email; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_user_email ON public.user_entity USING btree (email);


--
-- TOC entry 3402 (class 1259 OID 17672)
-- Name: idx_user_group_mapping; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_user_group_mapping ON public.user_group_membership USING btree (user_id);


--
-- TOC entry 3302 (class 1259 OID 17678)
-- Name: idx_user_reqactions; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_user_reqactions ON public.user_required_action USING btree (user_id);


--
-- TOC entry 3305 (class 1259 OID 17671)
-- Name: idx_user_role_mapping; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_user_role_mapping ON public.user_role_mapping USING btree (user_id);


--
-- TOC entry 3372 (class 1259 OID 18015)
-- Name: idx_usr_fed_map_fed_prv; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_usr_fed_map_fed_prv ON public.user_federation_mapper USING btree (federation_provider_id);


--
-- TOC entry 3373 (class 1259 OID 18016)
-- Name: idx_usr_fed_map_realm; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_usr_fed_map_realm ON public.user_federation_mapper USING btree (realm_id);


--
-- TOC entry 3299 (class 1259 OID 18017)
-- Name: idx_usr_fed_prv_realm; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_usr_fed_prv_realm ON public.user_federation_provider USING btree (realm_id);


--
-- TOC entry 3310 (class 1259 OID 18018)
-- Name: idx_web_orig_client; Type: INDEX; Schema: public; Owner: keycloak
--

CREATE INDEX idx_web_orig_client ON public.web_origins USING btree (client_id);


--
-- TOC entry 3564 (class 2606 OID 17203)
-- Name: client_session_auth_status auth_status_constraint; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.client_session_auth_status
    ADD CONSTRAINT auth_status_constraint FOREIGN KEY (client_session) REFERENCES public.client_session(id);


--
-- TOC entry 3548 (class 2606 OID 16888)
-- Name: identity_provider fk2b4ebc52ae5c3b34; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.identity_provider
    ADD CONSTRAINT fk2b4ebc52ae5c3b34 FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3541 (class 2606 OID 16804)
-- Name: client_attributes fk3c47c64beacca966; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.client_attributes
    ADD CONSTRAINT fk3c47c64beacca966 FOREIGN KEY (client_id) REFERENCES public.client(id);


--
-- TOC entry 3547 (class 2606 OID 16898)
-- Name: federated_identity fk404288b92ef007a6; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.federated_identity
    ADD CONSTRAINT fk404288b92ef007a6 FOREIGN KEY (user_id) REFERENCES public.user_entity(id);


--
-- TOC entry 3543 (class 2606 OID 17079)
-- Name: client_node_registrations fk4129723ba992f594; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.client_node_registrations
    ADD CONSTRAINT fk4129723ba992f594 FOREIGN KEY (client_id) REFERENCES public.client(id);


--
-- TOC entry 3542 (class 2606 OID 16809)
-- Name: client_session_note fk5edfb00ff51c2736; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.client_session_note
    ADD CONSTRAINT fk5edfb00ff51c2736 FOREIGN KEY (client_session) REFERENCES public.client_session(id);


--
-- TOC entry 3551 (class 2606 OID 16928)
-- Name: user_session_note fk5edfb00ff51d3472; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.user_session_note
    ADD CONSTRAINT fk5edfb00ff51d3472 FOREIGN KEY (user_session) REFERENCES public.user_session(id);


--
-- TOC entry 3519 (class 2606 OID 16614)
-- Name: client_session_role fk_11b7sgqw18i532811v7o2dv76; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.client_session_role
    ADD CONSTRAINT fk_11b7sgqw18i532811v7o2dv76 FOREIGN KEY (client_session) REFERENCES public.client_session(id);


--
-- TOC entry 3532 (class 2606 OID 16619)
-- Name: redirect_uris fk_1burs8pb4ouj97h5wuppahv9f; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.redirect_uris
    ADD CONSTRAINT fk_1burs8pb4ouj97h5wuppahv9f FOREIGN KEY (client_id) REFERENCES public.client(id);


--
-- TOC entry 3537 (class 2606 OID 16624)
-- Name: user_federation_provider fk_1fj32f6ptolw2qy60cd8n01e8; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.user_federation_provider
    ADD CONSTRAINT fk_1fj32f6ptolw2qy60cd8n01e8 FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3556 (class 2606 OID 17041)
-- Name: client_session_prot_mapper fk_33a8sgqw18i532811v7o2dk89; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.client_session_prot_mapper
    ADD CONSTRAINT fk_33a8sgqw18i532811v7o2dk89 FOREIGN KEY (client_session) REFERENCES public.client_session(id);


--
-- TOC entry 3530 (class 2606 OID 16634)
-- Name: realm_required_credential fk_5hg65lybevavkqfki3kponh9v; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.realm_required_credential
    ADD CONSTRAINT fk_5hg65lybevavkqfki3kponh9v FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3604 (class 2606 OID 18275)
-- Name: resource_attribute fk_5hrm2vlf9ql5fu022kqepovbr; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.resource_attribute
    ADD CONSTRAINT fk_5hrm2vlf9ql5fu022kqepovbr FOREIGN KEY (resource_id) REFERENCES public.resource_server_resource(id);


--
-- TOC entry 3535 (class 2606 OID 16639)
-- Name: user_attribute fk_5hrm2vlf9ql5fu043kqepovbr; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.user_attribute
    ADD CONSTRAINT fk_5hrm2vlf9ql5fu043kqepovbr FOREIGN KEY (user_id) REFERENCES public.user_entity(id);


--
-- TOC entry 3538 (class 2606 OID 16649)
-- Name: user_required_action fk_6qj3w1jw9cvafhe19bwsiuvmd; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.user_required_action
    ADD CONSTRAINT fk_6qj3w1jw9cvafhe19bwsiuvmd FOREIGN KEY (user_id) REFERENCES public.user_entity(id);


--
-- TOC entry 3523 (class 2606 OID 16654)
-- Name: keycloak_role fk_6vyqfe4cn4wlq8r6kt5vdsj5c; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.keycloak_role
    ADD CONSTRAINT fk_6vyqfe4cn4wlq8r6kt5vdsj5c FOREIGN KEY (realm) REFERENCES public.realm(id);


--
-- TOC entry 3531 (class 2606 OID 16659)
-- Name: realm_smtp_config fk_70ej8xdxgxd0b9hh6180irr0o; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.realm_smtp_config
    ADD CONSTRAINT fk_70ej8xdxgxd0b9hh6180irr0o FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3515 (class 2606 OID 16669)
-- Name: client_default_roles fk_8aelwnibji49avxsrtuf6xjow; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.client_default_roles
    ADD CONSTRAINT fk_8aelwnibji49avxsrtuf6xjow FOREIGN KEY (role_id) REFERENCES public.keycloak_role(id);


--
-- TOC entry 3526 (class 2606 OID 16674)
-- Name: realm_attribute fk_8shxd6l3e9atqukacxgpffptw; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.realm_attribute
    ADD CONSTRAINT fk_8shxd6l3e9atqukacxgpffptw FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3520 (class 2606 OID 16679)
-- Name: composite_role fk_a63wvekftu8jo1pnj81e7mce2; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.composite_role
    ADD CONSTRAINT fk_a63wvekftu8jo1pnj81e7mce2 FOREIGN KEY (composite) REFERENCES public.keycloak_role(id);


--
-- TOC entry 3560 (class 2606 OID 17223)
-- Name: authentication_execution fk_auth_exec_flow; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.authentication_execution
    ADD CONSTRAINT fk_auth_exec_flow FOREIGN KEY (flow_id) REFERENCES public.authentication_flow(id);


--
-- TOC entry 3559 (class 2606 OID 17218)
-- Name: authentication_execution fk_auth_exec_realm; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.authentication_execution
    ADD CONSTRAINT fk_auth_exec_realm FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3558 (class 2606 OID 17213)
-- Name: authentication_flow fk_auth_flow_realm; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.authentication_flow
    ADD CONSTRAINT fk_auth_flow_realm FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3557 (class 2606 OID 17208)
-- Name: authenticator_config fk_auth_realm; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.authenticator_config
    ADD CONSTRAINT fk_auth_realm FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3518 (class 2606 OID 16684)
-- Name: client_session fk_b4ao2vcvat6ukau74wbwtfqo1; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.client_session
    ADD CONSTRAINT fk_b4ao2vcvat6ukau74wbwtfqo1 FOREIGN KEY (session_id) REFERENCES public.user_session(id);


--
-- TOC entry 3539 (class 2606 OID 16689)
-- Name: user_role_mapping fk_c4fqv34p1mbylloxang7b1q3l; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.user_role_mapping
    ADD CONSTRAINT fk_c4fqv34p1mbylloxang7b1q3l FOREIGN KEY (user_id) REFERENCES public.user_entity(id);


--
-- TOC entry 3595 (class 2606 OID 18173)
-- Name: client_scope_client fk_c_cli_scope_client; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.client_scope_client
    ADD CONSTRAINT fk_c_cli_scope_client FOREIGN KEY (client_id) REFERENCES public.client(id);


--
-- TOC entry 3596 (class 2606 OID 18178)
-- Name: client_scope_client fk_c_cli_scope_scope; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.client_scope_client
    ADD CONSTRAINT fk_c_cli_scope_scope FOREIGN KEY (scope_id) REFERENCES public.client_scope(id);


--
-- TOC entry 3575 (class 2606 OID 18162)
-- Name: client_scope_attributes fk_cl_scope_attr_scope; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.client_scope_attributes
    ADD CONSTRAINT fk_cl_scope_attr_scope FOREIGN KEY (scope_id) REFERENCES public.client_scope(id);


--
-- TOC entry 3577 (class 2606 OID 18157)
-- Name: client_scope_role_mapping fk_cl_scope_rm_role; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.client_scope_role_mapping
    ADD CONSTRAINT fk_cl_scope_rm_role FOREIGN KEY (role_id) REFERENCES public.keycloak_role(id);


--
-- TOC entry 3576 (class 2606 OID 18152)
-- Name: client_scope_role_mapping fk_cl_scope_rm_scope; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.client_scope_role_mapping
    ADD CONSTRAINT fk_cl_scope_rm_scope FOREIGN KEY (scope_id) REFERENCES public.client_scope(id);


--
-- TOC entry 3565 (class 2606 OID 17331)
-- Name: client_user_session_note fk_cl_usr_ses_note; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.client_user_session_note
    ADD CONSTRAINT fk_cl_usr_ses_note FOREIGN KEY (client_session) REFERENCES public.client_session(id);


--
-- TOC entry 3545 (class 2606 OID 18147)
-- Name: protocol_mapper fk_cli_scope_mapper; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.protocol_mapper
    ADD CONSTRAINT fk_cli_scope_mapper FOREIGN KEY (client_scope_id) REFERENCES public.client_scope(id);


--
-- TOC entry 3594 (class 2606 OID 17970)
-- Name: client_initial_access fk_client_init_acc_realm; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.client_initial_access
    ADD CONSTRAINT fk_client_init_acc_realm FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3590 (class 2606 OID 17890)
-- Name: component_config fk_component_config; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.component_config
    ADD CONSTRAINT fk_component_config FOREIGN KEY (component_id) REFERENCES public.component(id);


--
-- TOC entry 3591 (class 2606 OID 17885)
-- Name: component fk_component_realm; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.component
    ADD CONSTRAINT fk_component_realm FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3592 (class 2606 OID 17912)
-- Name: credential_attribute fk_cred_attr; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.credential_attribute
    ADD CONSTRAINT fk_cred_attr FOREIGN KEY (credential_id) REFERENCES public.credential(id);


--
-- TOC entry 3573 (class 2606 OID 17534)
-- Name: realm_default_groups fk_def_groups_group; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.realm_default_groups
    ADD CONSTRAINT fk_def_groups_group FOREIGN KEY (group_id) REFERENCES public.keycloak_group(id);


--
-- TOC entry 3572 (class 2606 OID 17529)
-- Name: realm_default_groups fk_def_groups_realm; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.realm_default_groups
    ADD CONSTRAINT fk_def_groups_realm FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3527 (class 2606 OID 16699)
-- Name: realm_default_roles fk_evudb1ppw84oxfax2drs03icc; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.realm_default_roles
    ADD CONSTRAINT fk_evudb1ppw84oxfax2drs03icc FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3593 (class 2606 OID 17907)
-- Name: fed_credential_attribute fk_fed_cred_attr; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.fed_credential_attribute
    ADD CONSTRAINT fk_fed_cred_attr FOREIGN KEY (credential_id) REFERENCES public.fed_user_credential(id);


--
-- TOC entry 3563 (class 2606 OID 17238)
-- Name: user_federation_mapper_config fk_fedmapper_cfg; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.user_federation_mapper_config
    ADD CONSTRAINT fk_fedmapper_cfg FOREIGN KEY (user_federation_mapper_id) REFERENCES public.user_federation_mapper(id);


--
-- TOC entry 3562 (class 2606 OID 17233)
-- Name: user_federation_mapper fk_fedmapperpm_fedprv; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.user_federation_mapper
    ADD CONSTRAINT fk_fedmapperpm_fedprv FOREIGN KEY (federation_provider_id) REFERENCES public.user_federation_provider(id);


--
-- TOC entry 3561 (class 2606 OID 17228)
-- Name: user_federation_mapper fk_fedmapperpm_realm; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.user_federation_mapper
    ADD CONSTRAINT fk_fedmapperpm_realm FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3589 (class 2606 OID 17802)
-- Name: associated_policy fk_frsr5s213xcx4wnkog82ssrfy; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.associated_policy
    ADD CONSTRAINT fk_frsr5s213xcx4wnkog82ssrfy FOREIGN KEY (associated_policy_id) REFERENCES public.resource_server_policy(id);


--
-- TOC entry 3587 (class 2606 OID 17787)
-- Name: scope_policy fk_frsrasp13xcx4wnkog82ssrfy; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.scope_policy
    ADD CONSTRAINT fk_frsrasp13xcx4wnkog82ssrfy FOREIGN KEY (policy_id) REFERENCES public.resource_server_policy(id);


--
-- TOC entry 3600 (class 2606 OID 18229)
-- Name: resource_server_perm_ticket fk_frsrho213xcx4wnkog82sspmt; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.resource_server_perm_ticket
    ADD CONSTRAINT fk_frsrho213xcx4wnkog82sspmt FOREIGN KEY (resource_server_id) REFERENCES public.resource_server(id);


--
-- TOC entry 3578 (class 2606 OID 18047)
-- Name: resource_server_resource fk_frsrho213xcx4wnkog82ssrfy; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.resource_server_resource
    ADD CONSTRAINT fk_frsrho213xcx4wnkog82ssrfy FOREIGN KEY (resource_server_id) REFERENCES public.resource_server(id);


--
-- TOC entry 3601 (class 2606 OID 18234)
-- Name: resource_server_perm_ticket fk_frsrho213xcx4wnkog83sspmt; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.resource_server_perm_ticket
    ADD CONSTRAINT fk_frsrho213xcx4wnkog83sspmt FOREIGN KEY (resource_id) REFERENCES public.resource_server_resource(id);


--
-- TOC entry 3602 (class 2606 OID 18239)
-- Name: resource_server_perm_ticket fk_frsrho213xcx4wnkog84sspmt; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.resource_server_perm_ticket
    ADD CONSTRAINT fk_frsrho213xcx4wnkog84sspmt FOREIGN KEY (scope_id) REFERENCES public.resource_server_scope(id);


--
-- TOC entry 3588 (class 2606 OID 17797)
-- Name: associated_policy fk_frsrpas14xcx4wnkog82ssrfy; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.associated_policy
    ADD CONSTRAINT fk_frsrpas14xcx4wnkog82ssrfy FOREIGN KEY (policy_id) REFERENCES public.resource_server_policy(id);


--
-- TOC entry 3586 (class 2606 OID 17782)
-- Name: scope_policy fk_frsrpass3xcx4wnkog82ssrfy; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.scope_policy
    ADD CONSTRAINT fk_frsrpass3xcx4wnkog82ssrfy FOREIGN KEY (scope_id) REFERENCES public.resource_server_scope(id);


--
-- TOC entry 3603 (class 2606 OID 18280)
-- Name: resource_server_perm_ticket fk_frsrpo2128cx4wnkog82ssrfy; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.resource_server_perm_ticket
    ADD CONSTRAINT fk_frsrpo2128cx4wnkog82ssrfy FOREIGN KEY (policy_id) REFERENCES public.resource_server_policy(id);


--
-- TOC entry 3580 (class 2606 OID 18042)
-- Name: resource_server_policy fk_frsrpo213xcx4wnkog82ssrfy; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.resource_server_policy
    ADD CONSTRAINT fk_frsrpo213xcx4wnkog82ssrfy FOREIGN KEY (resource_server_id) REFERENCES public.resource_server(id);


--
-- TOC entry 3582 (class 2606 OID 17752)
-- Name: resource_scope fk_frsrpos13xcx4wnkog82ssrfy; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.resource_scope
    ADD CONSTRAINT fk_frsrpos13xcx4wnkog82ssrfy FOREIGN KEY (resource_id) REFERENCES public.resource_server_resource(id);


--
-- TOC entry 3584 (class 2606 OID 17767)
-- Name: resource_policy fk_frsrpos53xcx4wnkog82ssrfy; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.resource_policy
    ADD CONSTRAINT fk_frsrpos53xcx4wnkog82ssrfy FOREIGN KEY (resource_id) REFERENCES public.resource_server_resource(id);


--
-- TOC entry 3585 (class 2606 OID 17772)
-- Name: resource_policy fk_frsrpp213xcx4wnkog82ssrfy; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.resource_policy
    ADD CONSTRAINT fk_frsrpp213xcx4wnkog82ssrfy FOREIGN KEY (policy_id) REFERENCES public.resource_server_policy(id);


--
-- TOC entry 3583 (class 2606 OID 17757)
-- Name: resource_scope fk_frsrps213xcx4wnkog82ssrfy; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.resource_scope
    ADD CONSTRAINT fk_frsrps213xcx4wnkog82ssrfy FOREIGN KEY (scope_id) REFERENCES public.resource_server_scope(id);


--
-- TOC entry 3579 (class 2606 OID 18052)
-- Name: resource_server_scope fk_frsrso213xcx4wnkog82ssrfy; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.resource_server_scope
    ADD CONSTRAINT fk_frsrso213xcx4wnkog82ssrfy FOREIGN KEY (resource_server_id) REFERENCES public.resource_server(id);


--
-- TOC entry 3521 (class 2606 OID 16704)
-- Name: composite_role fk_gr7thllb9lu8q4vqa4524jjy8; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.composite_role
    ADD CONSTRAINT fk_gr7thllb9lu8q4vqa4524jjy8 FOREIGN KEY (child_role) REFERENCES public.keycloak_role(id);


--
-- TOC entry 3599 (class 2606 OID 18204)
-- Name: user_consent_client_scope fk_grntcsnt_clsc_usc; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.user_consent_client_scope
    ADD CONSTRAINT fk_grntcsnt_clsc_usc FOREIGN KEY (user_consent_id) REFERENCES public.user_consent(id);


--
-- TOC entry 3555 (class 2606 OID 17026)
-- Name: user_consent fk_grntcsnt_user; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.user_consent
    ADD CONSTRAINT fk_grntcsnt_user FOREIGN KEY (user_id) REFERENCES public.user_entity(id);


--
-- TOC entry 3570 (class 2606 OID 17503)
-- Name: group_attribute fk_group_attribute_group; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.group_attribute
    ADD CONSTRAINT fk_group_attribute_group FOREIGN KEY (group_id) REFERENCES public.keycloak_group(id);


--
-- TOC entry 3567 (class 2606 OID 17496)
-- Name: keycloak_group fk_group_realm; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.keycloak_group
    ADD CONSTRAINT fk_group_realm FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3568 (class 2606 OID 17517)
-- Name: group_role_mapping fk_group_role_group; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.group_role_mapping
    ADD CONSTRAINT fk_group_role_group FOREIGN KEY (group_id) REFERENCES public.keycloak_group(id);


--
-- TOC entry 3569 (class 2606 OID 17522)
-- Name: group_role_mapping fk_group_role_role; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.group_role_mapping
    ADD CONSTRAINT fk_group_role_role FOREIGN KEY (role_id) REFERENCES public.keycloak_role(id);


--
-- TOC entry 3528 (class 2606 OID 16709)
-- Name: realm_default_roles fk_h4wpd7w4hsoolni3h0sw7btje; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.realm_default_roles
    ADD CONSTRAINT fk_h4wpd7w4hsoolni3h0sw7btje FOREIGN KEY (role_id) REFERENCES public.keycloak_role(id);


--
-- TOC entry 3552 (class 2606 OID 16970)
-- Name: realm_enabled_event_types fk_h846o4h0w8epx5nwedrf5y69j; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.realm_enabled_event_types
    ADD CONSTRAINT fk_h846o4h0w8epx5nwedrf5y69j FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3529 (class 2606 OID 16714)
-- Name: realm_events_listeners fk_h846o4h0w8epx5nxev9f5y69j; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.realm_events_listeners
    ADD CONSTRAINT fk_h846o4h0w8epx5nxev9f5y69j FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3553 (class 2606 OID 17016)
-- Name: identity_provider_mapper fk_idpm_realm; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.identity_provider_mapper
    ADD CONSTRAINT fk_idpm_realm FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3554 (class 2606 OID 17252)
-- Name: idp_mapper_config fk_idpmconfig; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.idp_mapper_config
    ADD CONSTRAINT fk_idpmconfig FOREIGN KEY (idp_mapper_id) REFERENCES public.identity_provider_mapper(id);


--
-- TOC entry 3524 (class 2606 OID 17084)
-- Name: keycloak_role fk_kjho5le2c0ral09fl8cm9wfw9; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.keycloak_role
    ADD CONSTRAINT fk_kjho5le2c0ral09fl8cm9wfw9 FOREIGN KEY (client) REFERENCES public.client(id);


--
-- TOC entry 3540 (class 2606 OID 16724)
-- Name: web_origins fk_lojpho213xcx4wnkog82ssrfy; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.web_origins
    ADD CONSTRAINT fk_lojpho213xcx4wnkog82ssrfy FOREIGN KEY (client_id) REFERENCES public.client(id);


--
-- TOC entry 3516 (class 2606 OID 18083)
-- Name: client_default_roles fk_nuilts7klwqw2h8m2b5joytky; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.client_default_roles
    ADD CONSTRAINT fk_nuilts7klwqw2h8m2b5joytky FOREIGN KEY (client_id) REFERENCES public.client(id);


--
-- TOC entry 3533 (class 2606 OID 16734)
-- Name: scope_mapping fk_ouse064plmlr732lxjcn1q5f1; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.scope_mapping
    ADD CONSTRAINT fk_ouse064plmlr732lxjcn1q5f1 FOREIGN KEY (client_id) REFERENCES public.client(id);


--
-- TOC entry 3534 (class 2606 OID 16739)
-- Name: scope_mapping fk_p3rh9grku11kqfrs4fltt7rnq; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.scope_mapping
    ADD CONSTRAINT fk_p3rh9grku11kqfrs4fltt7rnq FOREIGN KEY (role_id) REFERENCES public.keycloak_role(id);


--
-- TOC entry 3517 (class 2606 OID 16744)
-- Name: client fk_p56ctinxxb9gsk57fo49f9tac; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.client
    ADD CONSTRAINT fk_p56ctinxxb9gsk57fo49f9tac FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3544 (class 2606 OID 16893)
-- Name: protocol_mapper fk_pcm_realm; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.protocol_mapper
    ADD CONSTRAINT fk_pcm_realm FOREIGN KEY (client_id) REFERENCES public.client(id);


--
-- TOC entry 3522 (class 2606 OID 16749)
-- Name: credential fk_pfyr0glasqyl0dei3kl69r6v0; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.credential
    ADD CONSTRAINT fk_pfyr0glasqyl0dei3kl69r6v0 FOREIGN KEY (user_id) REFERENCES public.user_entity(id);


--
-- TOC entry 3546 (class 2606 OID 17245)
-- Name: protocol_mapper_config fk_pmconfig; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.protocol_mapper_config
    ADD CONSTRAINT fk_pmconfig FOREIGN KEY (protocol_mapper_id) REFERENCES public.protocol_mapper(id);


--
-- TOC entry 3597 (class 2606 OID 18189)
-- Name: default_client_scope fk_r_def_cli_scope_realm; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.default_client_scope
    ADD CONSTRAINT fk_r_def_cli_scope_realm FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3598 (class 2606 OID 18194)
-- Name: default_client_scope fk_r_def_cli_scope_scope; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.default_client_scope
    ADD CONSTRAINT fk_r_def_cli_scope_scope FOREIGN KEY (scope_id) REFERENCES public.client_scope(id);


--
-- TOC entry 3574 (class 2606 OID 18142)
-- Name: client_scope fk_realm_cli_scope; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.client_scope
    ADD CONSTRAINT fk_realm_cli_scope FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3566 (class 2606 OID 17326)
-- Name: required_action_provider fk_req_act_realm; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.required_action_provider
    ADD CONSTRAINT fk_req_act_realm FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3605 (class 2606 OID 18288)
-- Name: resource_uris fk_resource_server_uris; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.resource_uris
    ADD CONSTRAINT fk_resource_server_uris FOREIGN KEY (resource_id) REFERENCES public.resource_server_resource(id);


--
-- TOC entry 3550 (class 2606 OID 16923)
-- Name: realm_supported_locales fk_supported_locales_realm; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.realm_supported_locales
    ADD CONSTRAINT fk_supported_locales_realm FOREIGN KEY (realm_id) REFERENCES public.realm(id);


--
-- TOC entry 3536 (class 2606 OID 16769)
-- Name: user_federation_config fk_t13hpu1j94r2ebpekr39x5eu5; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.user_federation_config
    ADD CONSTRAINT fk_t13hpu1j94r2ebpekr39x5eu5 FOREIGN KEY (user_federation_provider_id) REFERENCES public.user_federation_provider(id);


--
-- TOC entry 3525 (class 2606 OID 17064)
-- Name: realm fk_traf444kk6qrkms7n56aiwq5y; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.realm
    ADD CONSTRAINT fk_traf444kk6qrkms7n56aiwq5y FOREIGN KEY (master_admin_client) REFERENCES public.client(id);


--
-- TOC entry 3571 (class 2606 OID 17510)
-- Name: user_group_membership fk_user_group_user; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.user_group_membership
    ADD CONSTRAINT fk_user_group_user FOREIGN KEY (user_id) REFERENCES public.user_entity(id);


--
-- TOC entry 3581 (class 2606 OID 17742)
-- Name: policy_config fkdc34197cf864c4e43; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.policy_config
    ADD CONSTRAINT fkdc34197cf864c4e43 FOREIGN KEY (policy_id) REFERENCES public.resource_server_policy(id);


--
-- TOC entry 3549 (class 2606 OID 16903)
-- Name: identity_provider_config fkdc4897cf864c4e43; Type: FK CONSTRAINT; Schema: public; Owner: keycloak
--

ALTER TABLE ONLY public.identity_provider_config
    ADD CONSTRAINT fkdc4897cf864c4e43 FOREIGN KEY (identity_provider_id) REFERENCES public.identity_provider(internal_id);


-- Completed on 2018-09-19 11:15:30 CEST

--
-- PostgreSQL database dump complete
--

