function computeMouseJitterScore(vectors) {
  // vectors = array of mouse movement vectors with {distance, angularChange, timestamp}

  // Count vectors classified as jitter (small distance, sharp angle changes)
  const JITTER_DIST_THRESHOLD = 3;
  const JITTER_ANGLE_THRESHOLD = 90;

  const jitterVectors = vectors.filter(v =>
    v.distance < JITTER_DIST_THRESHOLD &&
    v.angularChange !== null &&
    v.angularChange > JITTER_ANGLE_THRESHOLD
  );

  // Compute rate per minute
  if (vectors.length === 0) return 0;
  const durationMinutes = (vectors[vectors.length - 1].timestampEnd - vectors[0].timestampStart) / 60000;
  if (durationMinutes <= 0) return 0;

  return jitterVectors.length / durationMinutes; // jitter events per minute
}

function computeKeyboardJitterScore(keyEvents) {
  // keyEvents = array of {timestamp, key, type}
  // Calculate standard deviation of inter-key intervals (keydown only)
  const keydowns = keyEvents.filter(e => e.type === 'keydown');
  if (keydowns.length < 2) return 0;

  const intervals = [];
  for (let i = 1; i < keydowns.length; i++) {
    intervals.push(keydowns[i].timestamp - keydowns[i-1].timestamp);
  }
  const mean = intervals.reduce((a,b) => a+b, 0) / intervals.length;
  const variance = intervals.reduce((a,b) => a + (b - mean)**2, 0) / intervals.length;
  const stdDev = Math.sqrt(variance);

  return stdDev; // higher = more variable (jitter)
}