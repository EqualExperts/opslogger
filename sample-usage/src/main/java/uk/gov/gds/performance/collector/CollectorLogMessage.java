package uk.gov.gds.performance.collector;

import com.equalexperts.logging.LogMessage;

public enum CollectorLogMessage implements LogMessage {
    Success("GDS-000000", "Successfully published %d records"),
    UnknownError("GDS-000001", "An unknown error occurred:"),
    CouldNotConnectToPerformancePlatform("GDS-000002", "Could not connect to the performance platform %s"),
    PerformancePlatformTestQueryFailed("GDS-000003", "Test query to the performance platform %s failed with response code %d"),
    NoResultsFoundForDateRange("GDS-000004", "No results found in the date range %s to %s"),
    InvalidConfigurationFile("GDS-000005", "Invalid configuration file format %s"),
    ConfigurationFileNotFound("GDS-000006", "Configuration file %s not found"),
    CouldNotConnectToDatabase("GDS-000007", "Could not connect to the database:"),
    AllConnectivityChecksPassed("GDS-000008", "All connectivity checks passed"),
    SampleMessage("GDS-000009", "Logging sample message %d");

    //region LogMessage implementation
    private final String messageCode;
    private final String messagePattern;

    CollectorLogMessage(String messageCode, String messagePattern) {
        this.messageCode = messageCode;
        this.messagePattern = messagePattern;
    }

    @Override
    public String getMessageCode() {
        return messageCode;
    }

    @Override
    public String getMessagePattern() {
        return messagePattern;
    }
    //endregion
}
