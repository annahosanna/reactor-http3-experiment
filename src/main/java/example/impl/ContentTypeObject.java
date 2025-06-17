package example.impl;

import java.util.concurrent.atomic.AtomicBoolean;

// Your asking yourself, isn't there already a class for this?
// It has to do with scope
public class ContentTypeObject {

  private AtomicBoolean isHtml = new AtomicBoolean();
  private AtomicBoolean isJson = new AtomicBoolean();
  private AtomicBoolean isText = new AtomicBoolean();

  public ContentTypeObject() {
    this.isHtml.set(false);
    this.isJson.set(false);
    this.isText.set(false);
  }

  public void setIsHtml() {
    this.isHtml.set(true);
  }

  public void setIsJson() {
    this.isJson.set(true);
  }

  public void setIsText() {
    this.isText.set(true);
  }

  public boolean getIsText() {
    return this.isText.get();
  }

  public boolean getIsHtml() {
    return this.isHtml.get();
  }

  public boolean getIsJson() {
    return this.isJson.get();
  }
}
