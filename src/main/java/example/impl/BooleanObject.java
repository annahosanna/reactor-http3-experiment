package example.impl;

import java.util.concurrent.atomic.AtomicBoolean;

// Your asking yourself, isn't there already a class for this?
// It has to do with scope
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
}
