ALTER TABLE notifications
ADD COLUMN dedup_key VARCHAR(150);

CREATE UNIQUE INDEX uk_notifications_dedup_key ON notifications(dedup_key);
