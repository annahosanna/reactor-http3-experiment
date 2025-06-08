<script>
  const data = {
    name: "John Doe",
    email: "john@example.com",
    typingSpeed: 3.5,
    totalTime: 2500
  };

  fetch('https://your-server.com/api/submit', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(data)
  })
  .then(response => {
    if (!response.ok) throw new Error('Network response was not ok');
    return response.json();
  })
  .then(result => {
    console.log('Success:', result);
  })
  .catch(error => {
    console.error('Error sending data:', error);
  });
</script>
