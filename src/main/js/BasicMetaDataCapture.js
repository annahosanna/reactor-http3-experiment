<script>
  const metadata = {};

  document.querySelectorAll('#userForm input').forEach(input => {
    const fieldName = input.name;
    metadata[fieldName] = {
      focusTime: null,
      blurTime: null,
      totalTime: 0,
      keystrokes: 0,
      backspaces: 0,
      pasteDetected: false,
      changes: 0,
      startTyping: null,
      lastKeyTime: null,
      typingSpeed: null
    };

    input.addEventListener('focus', () => {
      metadata[fieldName].focusTime = Date.now();
    });

    input.addEventListener('blur', () => {
      const now = Date.now();
      metadata[fieldName].blurTime = now;
      if (metadata[fieldName].focusTime) {
        metadata[fieldName].totalTime += now - metadata[fieldName].focusTime;
      }
    });

    input.addEventListener('input', (e) => {
      metadata[fieldName].changes += 1;

      const now = Date.now();
      if (!metadata[fieldName].startTyping) {
        metadata[fieldName].startTyping = now;
      } else {
        metadata[fieldName].typingSpeed =
          (e.target.value.length / ((now - metadata[fieldName].startTyping) / 1000)).toFixed(2); // chars/sec
      }
    });

    input.addEventListener('keydown', (e) => {
      metadata[fieldName].keystrokes += 1;
      if (e.key === 'Backspace') {
        metadata[fieldName].backspaces += 1;
      }
    });

    input.addEventListener('paste', () => {
      metadata[fieldName].pasteDetected = true;
    });
  });

  document.getElementById('userForm').addEventListener('submit', (e) => {
    e.preventDefault();
    console.log('Form metadata:', metadata);

    // Submit data manually if needed
    // fetch('/submit', {
    //   method: 'POST',
    //   headers: { 'Content-Type': 'application/json' },
    //   body: JSON.stringify(metadata)
    // });
  });
</script>
