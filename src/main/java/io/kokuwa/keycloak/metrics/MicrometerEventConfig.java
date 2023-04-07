package io.kokuwa.keycloak.metrics;


public class MicrometerEventConfig {

  private static final String KEY_EVENT_REPLACE_IDS = "KC_METRICS_EVENT_REPLACE_IDS";
  private static final String KEY_DATA_COUNT = "KC_METRICS_DATA_COUNT";
  private static final String KEY_DATA_COUNT_INTERVAL = "KC_METRICS_DATA_COUNT_INTERVAL";
  private static final long DEFAULT_DATA_COUNT_INTERVAL = 60;
  
  private final boolean replaceIds;
  private final boolean dataCount;
  private final long dataCountInterval;

  public static MicrometerEventConfig getConfig() {
    long interval = DEFAULT_DATA_COUNT_INTERVAL;
    try {
      interval = Long.parseLong(System.getenv(KEY_DATA_COUNT_INTERVAL));
    } catch (Exception ignore) {}
    return new MicrometerEventConfig("true".equals(System.getenv(KEY_EVENT_REPLACE_IDS)),
                                     "true".equals(System.getenv(KEY_DATA_COUNT)),
                                     interval);
  }

  private MicrometerEventConfig(boolean replaceIds, boolean dataCount, long dataCountInterval) {
    this.replaceIds = replaceIds;
    this.dataCount = dataCount;
    this.dataCountInterval = dataCountInterval;
  }

  public boolean replaceIds() {
    return replaceIds;
  }

  public boolean dataCount() {
    return dataCount;
  }

  public long dataCountInterval() {
    return dataCountInterval;
  }

  @Override
  public String toString() {
    StringBuilder o = new StringBuilder();
    o.append("replaceIds: ").append(replaceIds).append(", ");
    o.append("dataCount: ").append(dataCount).append(", ");
    o.append("dataCountInterval: ").append(dataCountInterval);
    return o.toString();
  }

}
