class MovingAverage {
  constructor(windowSize) {
    this.windowSize = windowSize;
    this.window = [];
    this.sum = 0;
  }

  add(value) {
    this.window.push(value);
    this.sum += value;

    if (this.window.length > this.windowSize) {
      this.sum -= this.window.shift();
    }

    return this.getAverage();
  }

  getAverage() {
    if (this.window.length === 0) return 0;
    return this.sum / this.window.length;
  }
}

// Example usage:
const ma = new MovingAverage(3);

console.log(ma.add(10)); // 10
console.log(ma.add(20)); // 15
console.log(ma.add(30)); // 20
console.log(ma.add(40)); // 30
console.log(ma.add(50)); // 40