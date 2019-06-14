--liquibase formatted sql

--- gen_uuid

--changeset nio:1
CREATE OR REPLACE FUNCTION gen_random_uuid()
  RETURNS uuid
  LANGUAGE sql
AS
$function$
SELECT md5(random()::text || clock_timestamp()::text)::uuid
$function$
;
--rollback drop function gen_random_uuid;

--changeset nio:2
CREATE TABLE IF NOT EXISTS accounts
(
  id      UUID primary key default gen_random_uuid(),
  tenant  varchar(100) not null,
  payload jsonb
);
--rollback drop table accounts;

--changeset nio:3
CREATE TABLE IF NOT EXISTS nio_accounts
(
  id      UUID primary key default gen_random_uuid(),
  tenant  varchar(100) not null,
  payload jsonb
);
--rollback drop table nio_accounts;

--changeset nio:4
CREATE TABLE IF NOT EXISTS api_keys
(
  id      UUID primary key default gen_random_uuid(),
  tenant  varchar(100) not null,
  payload jsonb
);
--rollback drop table api_keys;

--changeset nio:5
CREATE TABLE IF NOT EXISTS catchup_lock
(
  id      UUID primary key default gen_random_uuid(),
  payload jsonb
);
--rollback drop table catchup_lock;

--changeset nio:6
CREATE TABLE IF NOT EXISTS consent_facts
(
  id      UUID primary key default gen_random_uuid(),
  tenant  varchar(100) not null,
  payload jsonb
);
--rollback drop table consent_facts;

--changeset nio:7
CREATE TABLE IF NOT EXISTS deletion_tasks
(
  id      UUID primary key default gen_random_uuid(),
  tenant  varchar(100) not null,
  payload jsonb
);
--rollback drop table deletion_tasks;

--changeset nio:8
CREATE TABLE IF NOT EXISTS extraction_tasks
(
  id      UUID primary key default gen_random_uuid(),
  tenant  varchar(100) not null,
  payload jsonb
);
--rollback drop table extraction_tasks;

--changeset nio:9
CREATE TABLE IF NOT EXISTS last_consent_facts
(
  id      UUID primary key default gen_random_uuid(),
  tenant  varchar(100) not null,
  payload jsonb
);
--rollback drop table last_consent_facts;

--changeset nio:10
CREATE TABLE IF NOT EXISTS organisations
(
  id      UUID primary key default gen_random_uuid(),
  tenant  varchar(100) not null,
  payload jsonb
);
--rollback drop table organisations;

--changeset nio:11
CREATE TABLE IF NOT EXISTS tenants
(
  id      UUID primary key default gen_random_uuid(),
  payload jsonb
);
--rollback drop table tenants;

--changeset nio:12
CREATE TABLE IF NOT EXISTS user_extract_tasks
(
  id      UUID primary key default gen_random_uuid(),
  tenant  varchar(100) not null,
  payload jsonb
);
--rollback drop table user_extract_tasks;

--changeset nio:13
CREATE TABLE IF NOT EXISTS users
(
  id      UUID primary key default gen_random_uuid(),
  tenant  varchar(100) not null,
  payload jsonb
);
--rollback drop table users;

--changeset nio:14
CREATE INDEX accounts_id_idx ON accounts USING BTREE ((payload->>'_id'));
--rollback drop INDEX accounts_id_idx;

--changeset nio:15
CREATE INDEX accounts_json_idx ON accounts using gin (payload);
--rollback drop INDEX accounts_json_idx;

--changeset nio:16
CREATE INDEX accounts_json_ops_idx ON accounts USING gin (payload jsonb_ops);
--rollback drop INDEX accounts_json_ops_idx;

--changeset nio:17
CREATE INDEX accounts_json_path_ops_idx ON accounts USING gin (payload jsonb_path_ops);
--rollback drop INDEX accounts_json_path_ops_idx;

--changeset nio:18
CREATE INDEX nio_accounts_id_idx ON nio_accounts USING BTREE ((payload->>'_id'));
--rollback drop INDEX nio_accounts_id_idx;

--changeset nio:19
CREATE INDEX nio_accounts_json_idx ON nio_accounts using gin (payload);
--rollback drop INDEX nio_accounts_json_idx;

--changeset nio:20
CREATE INDEX nio_accounts_json_ops_idx ON nio_accounts USING gin (payload jsonb_ops);
--rollback drop INDEX nio_accounts_json_ops_idx;

--changeset nio:21
CREATE INDEX nio_accounts_json_path_ops_idx ON nio_accounts USING gin (payload jsonb_path_ops);
--rollback drop INDEX nio_accounts_json_path_ops_idx;

--changeset nio:22
CREATE INDEX api_keys_id_idx ON api_keys USING BTREE ((payload->>'_id'));
--rollback drop INDEX api_keys_id_idx;

--changeset nio:23
CREATE INDEX api_keys_json_idx ON api_keys using gin (payload);
--rollback drop INDEX api_keys_json_idx;

--changeset nio:24
CREATE INDEX api_keys_json_ops_idx ON api_keys USING gin (payload jsonb_ops);
--rollback drop INDEX api_keys_json_ops_idx;

--changeset nio:25
CREATE INDEX api_keys_json_path_ops_idx ON api_keys USING gin (payload jsonb_path_ops);
--rollback drop INDEX api_keys_json_path_ops_idx;

--changeset nio:26
CREATE INDEX catchup_lock_id_idx ON catchup_lock USING BTREE ((payload->>'_id'));
--rollback drop INDEX catchup_lock_id_idx;

--changeset nio:27
CREATE INDEX catchup_lock_json_idx ON catchup_lock using gin (payload);
--rollback drop INDEX catchup_lock_json_idx;

--changeset nio:28
CREATE INDEX catchup_lock_json_ops_idx ON catchup_lock USING gin (payload jsonb_ops);
--rollback drop INDEX catchup_lock_json_ops_idx;

--changeset nio:29
CREATE INDEX catchup_lock_json_path_ops_idx ON catchup_lock USING gin (payload jsonb_path_ops);
--rollback drop INDEX catchup_lock_json_path_ops_idx;

--changeset nio:30
CREATE INDEX consent_facts_id_idx ON consent_facts USING BTREE ((payload->>'_id'));
--rollback drop INDEX consent_facts_id_idx;

--changeset nio:31
CREATE INDEX consent_facts_json_idx ON consent_facts using gin (payload);
--rollback drop INDEX consent_facts_json_idx;

--changeset nio:32
CREATE INDEX consent_facts_json_ops_idx ON consent_facts USING gin (payload jsonb_ops);
--rollback drop INDEX consent_facts_json_ops_idx;

--changeset nio:33
CREATE INDEX consent_facts_json_path_ops_idx ON consent_facts USING gin (payload jsonb_path_ops);
--rollback drop INDEX consent_facts_json_path_ops_idx;

--changeset nio:34
CREATE INDEX deletion_tasks_id_idx ON deletion_tasks USING BTREE ((payload->>'_id'));
--rollback drop INDEX deletion_tasks_id_idx;

--changeset nio:35
CREATE INDEX deletion_tasks_json_idx ON deletion_tasks using gin (payload);
--rollback drop INDEX deletion_tasks_json_idx;

--changeset nio:36
CREATE INDEX deletion_tasks_json_ops_idx ON deletion_tasks USING gin (payload jsonb_ops);
--rollback drop INDEX deletion_tasks_json_ops_idx;

--changeset nio:37
CREATE INDEX deletion_tasks_json_path_ops_idx ON deletion_tasks USING gin (payload jsonb_path_ops);
--rollback drop INDEX deletion_tasks_json_path_ops_idx;

--changeset nio:38
CREATE INDEX extraction_tasks_id_idx ON extraction_tasks USING BTREE ((payload->>'_id'));
--rollback drop INDEX extraction_tasks_id_idx;

--changeset nio:39
CREATE INDEX extraction_tasks_json_idx ON extraction_tasks using gin (payload);
--rollback drop INDEX extraction_tasks_json_idx;

--changeset nio:40
CREATE INDEX extraction_tasks_json_ops_idx ON extraction_tasks USING gin (payload jsonb_ops);
--rollback drop INDEX extraction_tasks_json_ops_idx;

--changeset nio:41
CREATE INDEX extraction_tasks_json_path_ops_idx ON extraction_tasks USING gin (payload jsonb_path_ops);
--rollback drop INDEX extraction_tasks_json_path_ops_idx;

--changeset nio:42
CREATE INDEX last_consent_facts_id_idx ON last_consent_facts USING BTREE ((payload->>'_id'));
--rollback drop INDEX last_consent_facts_id_idx;

--changeset nio:43
CREATE INDEX last_consent_facts_json_idx ON last_consent_facts using gin (payload);
--rollback drop INDEX last_consent_facts_json_idx;

--changeset nio:44
CREATE INDEX last_consent_facts_json_ops_idx ON last_consent_facts USING gin (payload jsonb_ops);
--rollback drop INDEX last_consent_facts_json_ops_idx;

--changeset nio:45
CREATE INDEX last_consent_facts_json_path_ops_idx ON last_consent_facts USING gin (payload jsonb_path_ops);
--rollback drop INDEX last_consent_facts_json_path_ops_idx;

--changeset nio:46
CREATE INDEX organisations_id_idx ON organisations USING BTREE ((payload->>'_id'));
--rollback drop INDEX organisations_id_idx;

--changeset nio:47
CREATE INDEX organisations_json_idx ON organisations using gin (payload);
--rollback drop INDEX organisations_json_idx;

--changeset nio:48
CREATE INDEX organisations_json_ops_idx ON organisations USING gin (payload jsonb_ops);
--rollback drop INDEX organisations_json_ops_idx;

--changeset nio:49
CREATE INDEX organisations_json_path_ops_idx ON organisations USING gin (payload jsonb_path_ops);
--rollback drop INDEX organisations_json_path_ops_idx;

--changeset nio:50
CREATE INDEX tenants_id_idx ON tenants USING BTREE ((payload->>'_id'));
--rollback drop INDEX tenants_id_idx;

--changeset nio:51
CREATE INDEX tenants_json_idx ON tenants using gin (payload);
--rollback drop INDEX tenants_json_idx;

--changeset nio:52
CREATE INDEX tenants_json_ops_idx ON tenants USING gin (payload jsonb_ops);
--rollback drop INDEX tenants_json_ops_idx;

--changeset nio:53
CREATE INDEX tenants_json_path_ops_idx ON tenants USING gin (payload jsonb_path_ops);
--rollback drop INDEX tenants_json_path_ops_idx;

--changeset nio:54
CREATE INDEX user_extract_tasks_id_idx ON user_extract_tasks USING BTREE ((payload->>'_id'));
--rollback drop INDEX user_extract_tasks_id_idx;

--changeset nio:55
CREATE INDEX user_extract_tasks_json_idx ON user_extract_tasks using gin (payload);
--rollback drop INDEX user_extract_tasks_json_idx;

--changeset nio:56
CREATE INDEX user_extract_tasks_json_ops_idx ON user_extract_tasks USING gin (payload jsonb_ops);
--rollback drop INDEX user_extract_tasks_json_ops_idx;

--changeset nio:57
CREATE INDEX user_extract_tasks_json_path_ops_idx ON user_extract_tasks USING gin (payload jsonb_path_ops);
--rollback drop INDEX user_extract_tasks_json_path_ops_idx;

--changeset nio:58
CREATE INDEX users_id_idx ON users USING BTREE ((payload->>'_id'));
--rollback drop INDEX users_id_idx;

--changeset nio:59
CREATE INDEX users_json_idx ON users using gin (payload);
--rollback drop INDEX users_json_idx;

--changeset nio:60
CREATE INDEX users_json_ops_idx ON users USING gin (payload jsonb_ops);
--rollback drop INDEX users_json_ops_idx;

--changeset nio:61
CREATE INDEX users_json_path_ops_idx ON users USING gin (payload jsonb_path_ops);
--rollback drop INDEX users_json_path_ops_idx;