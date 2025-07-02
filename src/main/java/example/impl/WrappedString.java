package example.impl;

public class WrappedString {

  private String wrappedString = null;

  public WrappedString() {}

  public String getWrappedString() {
    if (this.wrappedString == null) {
      return "";
    } else {
      return wrappedString;
    }
  }

  public void setWrappedString(String wrappedString) {
    this.wrappedString = wrappedString;
  }
}
