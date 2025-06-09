<script>
fetch('https://ipapi.co/json/')
  .then(res => res.json())
  .then(data => {
    console.log('IP-based location:', data);

    // Optionally send to your server
    fetch('https://your-server.com/api/location', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });
  })
  .catch(err => {
    console.error('IP location failed:', err);
  });
</script>
