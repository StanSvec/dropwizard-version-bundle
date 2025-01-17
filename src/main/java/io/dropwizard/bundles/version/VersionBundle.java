package io.dropwizard.bundles.version;


import io.dropwizard.core.Configuration;
import io.dropwizard.core.ConfiguredBundle;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A Dropwizard bundle that will expose a version number of the application via a servlet on the
 * admin console port.  The way that the bundle discovers the application's version number is
 * configurable via a {@code VersionSupplier}.  The provided {@code VersionSupplier} implementation
 * will be called a single time and the value it returns will be memoized for the life of the JVM.
 */
public class VersionBundle<T> implements ConfiguredBundle<T> {
  private static final String DEFAULT_URL = "/version";

  private final VersionSupplier supplier;
  private final String url;

  /**
   * Construct the VersionBundle using the provided version number supplier.  The version number
   * will be exposed on the Dropwizard admin port on the default URL.
   *
   * @param supplier The version number supplier.
   */
  public VersionBundle(VersionSupplier supplier) {
    this(supplier, DEFAULT_URL);
  }

  /**
   * Construct a VersionBundle using the provided version number supplier.  The version number will
   * be exposed on the Dropwizard admin port at the specified URL.
   *
   * @param supplier The version number supplier.
   * @param url      The URL to expose the version number on.
   */
  public VersionBundle(VersionSupplier supplier, String url) {
    checkNotNull(supplier);
    checkNotNull(url);

    this.supplier = supplier;
    this.url = url;
  }

  @Override
  public void initialize(Bootstrap<?> bootstrap) {
    // Nothing to do here
  }

  @Override
  public void run(T configuration, Environment environment) {
    VersionServlet servlet = new VersionServlet(supplier, environment.getObjectMapper());
    environment.admin().addServlet("version", servlet).addMapping(url);
  }
}
