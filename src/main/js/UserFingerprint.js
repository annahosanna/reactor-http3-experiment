<script>
  const form = document.getElementById('userForm'); // your form ID
  const fields = form.querySelectorAll('input, textarea');
  const fieldMetadata = {};
  const sessionMetadata = {
    mouseMoves: 0,
    scrollDepth: 0,
    mousePath: [],
    sessionStart: Date.now()
  };

  // Scroll depth tracking
  window.addEventListener('scroll', () => {
    const scrolled = window.scrollY + window.innerHeight;
    const docHeight = document.documentElement.scrollHeight;
    const percent = Math.round((scrolled / docHeight) * 100);
    sessionMetadata.scrollDepth = Math.max(sessionMetadata.scrollDepth, percent);
  });

  // Mouse movement tracking
  document.addEventListener('mousemove', (e) => {
    sessionMetadata.mouseMoves++;
    if (sessionMetadata.mouseMoves % 20 === 0) {
      sessionMetadata.mousePath.push({ x: e.clientX, y: e.clientY, t: Date.now() });
    }
  });

  // Field interaction tracking
  fields.forEach(field => {
    const name = field.name;
    fieldMetadata[name] = {
      focusTime: null,
      totalTime: 0,
      keystrokes: 0,
      backspaces: 0,
      pasteDetected: false,
      changes: 0,
      value: ""
    };

    field.addEventListener('focus', () => {
      fieldMetadata[name].focusTime = Date.now();
    });

    field.addEventListener('keydown', (e) => {
      fieldMetadata[name].keystrokes++;
      if (e.key === 'Backspace') {
        fieldMetadata[name].backspaces++;
      }
    });

    field.addEventListener('input', () => {
      fieldMetadata[name].changes++;
    });

    field.addEventListener('paste', () => {
      fieldMetadata[name].pasteDetected = true;
    });

    field.addEventListener('blur', () => {
      const now = Date.now();
      const meta = fieldMetadata[name];

      if (meta.focusTime) {
        meta.totalTime += now - meta.focusTime;
        meta.focusTime = null;
      }

      meta.value = field.value;

      const payload = {
        field: name,
        data: meta,
        session: {
          scrollDepth: sessionMetadata.scrollDepth,
          mouseMoves: sessionMetadata.mouseMoves,
          mousePathSample: sessionMetadata.mousePath.slice(-5),
          sessionTime: now - sessionMetadata.sessionStart
        },
        timestamp: new Date().toISOString()
      };

      // Send data to server
      fetch('https://your-server.com/api/field-event', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      })
      .then(res => res.json())
      .then(response => console.log('Server response:', response))
      .catch(err => console.error('Error sending field metadata:', err));
    });
  });
</script>