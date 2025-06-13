## SPA.md for updates on the SPA status.

### Goal: To create a client side SPA which gathers metadata for AI analysis

#### Who

- No record is made of individuals.
  - No cookies
  - No local storage
  - All data entered should be fictitious
  - All information is used to anonymously identify patterns from a large group of users.
  - Information is stored in memory only
  - This uses a non persistent SessionID to group information, but it is not a secure cookie

#### When

- New session, page forward/back or browser refresh detection
  - Clear all data from form and start a new session

#### Reporting

- At some time in the future I am hoping to add Reporting
  - Reports about mean/median/std deviation about various information collected
  - Visualization (using D3) of various information collected

#### Types of metadata

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

#### I do not think it is necessary to capture these but you could:

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
