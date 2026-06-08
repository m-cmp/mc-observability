-- Rename MCI -> Infra, VM -> Node in the mc-o11y-manager schema.
--
-- The JPA entity moved from @Table(name="vm") to @Table(name="node") and the
-- identity columns mci_id/vm_id to infra_id/node_id. Because the service runs
-- with hibernate ddl-auto=update (no migration tool), apply this BEFORE
-- starting the renamed build so existing agent-registration rows are preserved
-- instead of being orphaned in the old `vm` table.
--
-- Idempotent-ish: guarded with information_schema checks for MariaDB/MySQL.
-- Run against the mc-o11y-manager database.

-- 1) table vm -> node
SET @ddl := (
  SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.tables
            WHERE table_schema = DATABASE() AND table_name = 'vm')
    AND NOT EXISTS (SELECT 1 FROM information_schema.tables
            WHERE table_schema = DATABASE() AND table_name = 'node'),
    'RENAME TABLE vm TO node',
    'SELECT 1'));
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 2) column mci_id -> infra_id
SET @ddl := (
  SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'node' AND column_name = 'mci_id'),
    'ALTER TABLE node CHANGE COLUMN mci_id infra_id VARCHAR(255) NOT NULL',
    'SELECT 1'));
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 3) column vm_id -> node_id
SET @ddl := (
  SELECT IF(
    EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'node' AND column_name = 'vm_id'),
    'ALTER TABLE node CHANGE COLUMN vm_id node_id VARCHAR(255) NOT NULL',
    'SELECT 1'));
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
