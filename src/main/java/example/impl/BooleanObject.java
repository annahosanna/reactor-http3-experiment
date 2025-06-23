package example.impl;

import java.util.concurrent.atomic.AtomicBoolean;

// Your asking yourself, isn't there already a class for this?
// The purpose of this simple class is to provide an ability to
// change the value of a boolean, without using an 'if'
public class BooleanObject {

  private AtomicBoolean value = new AtomicBoolean();

  public BooleanObject() {
    this.value.set(false);
  }

  public boolean getValue() {
    return this.value.get();
  }

  public void setValue(boolean value) {
    this.value.set(value);
  }

  // This returns false if the value was already true
  public boolean flipToTrue() {
    return this.value.compareAndSet(false, true);
  }

  // This returns false if the value was already false
  public boolean flipToFalse() {
    return this.value.compareAndSet(true, false);
  }
}
