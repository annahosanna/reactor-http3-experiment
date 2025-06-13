CREATE TABLE keystroke_data (
    id IDENTITY PRIMARY KEY, -- Auto-incrementing unique ID
    event_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- Automatically set timestamp
    event_type VARCHAR(50), -- e.g., "keydown"
    key_value VARCHAR(10),  -- e.g., "a", "space"
    session_id VARCHAR(100) -- Optional session tracking
);