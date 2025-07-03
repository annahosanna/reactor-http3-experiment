package example.impl;

public class WrappedString {

  private String wrappedString = null;

  public WrappedString() {}

  public String getWrappedString() {
    if (this.wrappedString == null) {
      return "No string retrieved";
    } else {
      return wrappedString;
    }
  }

  public void setWrappedString(String wrappedString) {
    if (wrappedString == null) {
      this.wrappedString = "No string set";
    } else {
      this.wrappedString = wrappedString;
    }
  }
}
