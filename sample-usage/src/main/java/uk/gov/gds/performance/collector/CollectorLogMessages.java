package uk.gov.gds.performance.collector;

import com.equalexperts.logging.LogMessage;

public enum CollectorLogMessages implements LogMessage {
    SUCCESS("GDS-000000", "Successfully published %d records"),
    UNKNOWN_ERROR("GDS-000001", "An unknown error occurred:"),
    COULD_NOT_CONNECT_TO_PERFORMANCE_PLATFORM("GDS-000002", "Could not connect to the performance platform %s"),
    PERFORMANCE_PLATFORM_TEST_QUERY_FAILED("GDS-000003", "Test query to the performance platform %s failed with response code %d"),
    NO_RESULTS_FOUND_FOR_DATE_RANGE("GDS-000004", "No results found in the date range %s to %s"),
    INVALID_CONFIGURATION_FILE("GDS-000005", "Invalid configuration file format %s"),
    CONFIGURATION_FILE_NOT_FOUND("GDS-000006", "Configuration file %s not found"),
    COULD_NOT_CONNECT_TO_DATABASE("GDS-000007", "Could not connect to the database:"),
    ALL_CONNECTIVITY_CHECKS_PASSED("GDS-000008", "All connectivity checks passed");

    //region LogMessage implementation
    private final String messageCode;
    private final String messagePattern;

    CollectorLogMessages(String messageCode, String messagePattern) {
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
