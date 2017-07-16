-- Cassandra schema for Onpar
-- (Use the .sql ext just for syntax highlighting)

-- Create keyspace & user
CREATE KEYSPACE "onpar" WITH REPLICATION = {'class':'SimpleStrategy','replication_factor':'1'};
GO

CREATE USER 'onpar' WITH PASSWORD 's3cr3tp2ssw0rd' NOSUPERUSER;
GO

GRANT ALL PERMISSIONS ON KEYSPACE "onpar" TO "onpar";
GO
GRANT ALL PERMISSIONS ON KEYSPACE "onpar" TO "onpar";
GO

CREATE TABLE mappings_stats (
    m_mapping               VARCHAR,
    m_namespace             VARCHAR,
    m_key                   VARCHAR,
    m_value                 COUNTER,
    PRIMARY KEY (m_mapping, m_namespace, m_key)
) WITH COMPACT STORAGE;

-- ===== Tables to store 1-1 mappings
CREATE TABLE mapoo_data (
    m_namespace             VARCHAR,
    m_type                  VARCHAR,
    m_key                   VARCHAR,
    m_data                  BLOB,
    PRIMARY KEY (m_namespace, m_type, m_key)
) WITH COMPACT STORAGE;

-- ===== Tables to store n-1 mappings
-- Map: object -> target
CREATE TABLE mapmo_objtarget (
    m_namespace             VARCHAR,
    m_object                VARCHAR,
    m_data                  BLOB,
    PRIMARY KEY (m_namespace, m_object)
) WITH COMPACT STORAGE;

-- Map: target -> object
CREATE TABLE mapmo_targetobj (
    m_namespace             VARCHAR,
    m_target                VARCHAR,
    m_object                VARCHAR,
    m_data                  BLOB,
    PRIMARY KEY (m_namespace, m_target, m_object)
) WITH COMPACT STORAGE;

-- Stats
CREATE TABLE mapmo_stats (
    m_namespace             VARCHAR,
    m_key                   VARCHAR,
    m_value                 COUNTER,
    PRIMARY KEY (m_namespace, m_key)
) WITH COMPACT STORAGE;

-- ===== Tables to store n-n mappings
-- Map: object -> target
CREATE TABLE mapmm_objtarget (
    m_namespace             VARCHAR,
    m_object                VARCHAR,
    m_target                VARCHAR,
    m_data                  BLOB,
    PRIMARY KEY (m_namespace, m_object, m_target)
) WITH COMPACT STORAGE;

-- Map: target -> object
CREATE TABLE mapmm_targetobj (
    m_namespace             VARCHAR,
    m_target                VARCHAR,
    m_object                VARCHAR,
    m_data                  BLOB,
    PRIMARY KEY (m_namespace, m_target, m_object)
) WITH COMPACT STORAGE;

-- Stats
CREATE TABLE mapmm_stats (
    m_namespace             VARCHAR,
    m_key                   VARCHAR,
    m_value                 COUNTER,
    PRIMARY KEY (m_namespace, m_key)
) WITH COMPACT STORAGE;
