### SPA.md for updates on the SPA status.

#### Goal: To create a client side SPA which gathers metadata for AI analysis

##### Who

- No record is made of individuals.
  - No cookies
  - No local storage
  - No names
  - All information is used to anonymously identify patterns from a large group of users.

##### When

- New session, page forward/back or browser refresh detection
  - Clear all data from form and start a new session

##### Types of metadata

- Form field visit
  - Order and revisit can be determined later
- Form field visit duration

- Keystrokes

  - Character Count
  - Backspace count
    - Error correction/answer changes without leaving form field

- Device Type (Mobile, etc.)
- Copy/Paste Detection
- IP Address
  - goelocation
  - Weather
  - Holidays
  - Visiting from different country

##### I do not think it is necessary to capture these but you could:

- Typing habits

  - Typing speed (but could be used for fraud detection)
  - Pause duration
  - Character frequency
  - Typing rhythm
  - Per key pair timing
  - letter Jitter/Variability (as compared to other users)
  - Partial field updates on return to field
    - Backspace frequency

- Mouse movements
  - jitter detection
  - Input device detection
  - Mouse movement detection
  - Mouse button detection
- Input type (mouse touch etc.)
