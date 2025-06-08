CREATE TABLE form_events (
  id IDENTITY PRIMARY KEY,
  user_id VARCHAR(255),
  session_id VARCHAR(255),
  event_type VARCHAR(50),
  field_id VARCHAR(100),
  event_data JSON,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO
  form_events (
    user_id,
    session_id,
    event_type,
    field_id,
    event_data
  )
VALUES
  (
    'user-123',
    'sess-001',
    'focus',
    'email',
    '{"timestamp":1000}'
  );

SELECT
  *
FROM
  form_events
ORDER BY
  created_at ASC;
