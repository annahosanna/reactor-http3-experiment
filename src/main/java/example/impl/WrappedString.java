package example.impl;

public class WrappedString {

  private String wrappedString = null;
  private BooleanObject hasSetWrappedString = new BooleanObject();

  public WrappedString() {}

  public String getWrappedString() {
    if (this.wrappedString == null) {
      System.out.println("Cannot get a null valued wrapped string.");
      return "";
    } else {
      return wrappedString;
    }
  }

  public void setWrappedString(String wrappedString) {
    if (wrappedString == null) {
      System.out.println("Cannot set a wrapped string to null.");
      return;
    }
    if (this.hasSetWrappedString.flipToTrue()) {
      // Treat it like final
      this.wrappedString = wrappedString;
    }
  }
}
