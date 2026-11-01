package fr.uge.project.ruge.testsupport;

public abstract class MockitoTestSupport {
  static {
    System.setProperty("net.bytebuddy.experimental", "true");
  }
}
